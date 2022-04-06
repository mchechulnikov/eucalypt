package eucalypt.docker

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

internal object Docker {
    suspend fun isImageExists(image: String): Boolean =
        runDocker(arrayOf("images", "-q", image)).isNotEmpty()

    suspend fun pullImage(image: String) {
        runDocker(arrayOf("pull", image))
    }

    // TODO extract settings of run to config
    suspend fun runContainer(name: String, image: String) {
        runDocker(arrayOf("run", "-d", "-it", "-m=100", "--cpus=1.5", "--network", "none", "--name", name, image))
    }

    suspend fun restartContainer(container: String) {
        runDocker(arrayOf("restart", "-t", "0", container))
    }

    suspend fun removeContainer(container: String) {
        runDocker(arrayOf("rm", "-f", container))
    }

    suspend fun exec(container: String, command: String): String {
        return runDocker(arrayOf("exec", container, command))
    }

    fun monitorEvents(containerNamePrefix: String, eventsChannel: Channel<String>): Job {
        val args =
            arrayOf(
                "events",
                "--filter", "container=$containerNamePrefix",
                "--format", "{{.Actor.Attributes.name}},{{.Status}}",
                "--since", System.currentTimeMillis().toString(),
            )
        return readStream("docker", args, eventsChannel)
    }

    private suspend fun runDocker(args: Array<String>) = runCmd("docker", args)

    private fun runCmd(cmd: String, args: Array<String>): String = runBlocking(Dispatchers.IO) {
        val process = ProcessBuilder(cmd, *args).start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()

        process.waitFor()

        if (process.exitValue() != 0) {
            throw DockerException("Process '$cmd' failed with exit code ${process.exitValue()}\n$stderr")
        }

        stdout
    }

    private fun readStream(cmd: String, args: Array<String>, channel: Channel<String>): Job {
        val builder = ProcessBuilder(cmd, *args)
        builder.redirectErrorStream(true) // so we can ignore the error stream

        return CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            val process = builder.start()
            val stdout = process.inputStream.bufferedReader()
            val stderr = process.errorStream.bufferedReader()

            try {
                while (isActive) {
                    val line = stdout.readLine() ?: break
                    channel.send(line)
                }
            } catch (e: CancellationException) {
                channel.close(e)
            } catch (e: Throwable) {
                val exp = DockerException(
                    "Error reading docker output\nOriginal exception: ${e.message}\nSTDERR:\n${stderr.readText()}"
                )
                channel.close(exp)
            } finally {
                stdout.close()
                process.destroy()
            }
        }
    }
}