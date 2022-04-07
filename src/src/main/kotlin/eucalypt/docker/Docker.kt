package eucalypt.docker

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

internal object Docker {
    suspend fun isImageExists(image: String): Boolean =
        runDocker(arrayOf("images", "-q", image)).isNotEmpty()

    suspend fun pullImage(image: String) {
        runDocker(arrayOf("pull", image))
    }

    suspend fun runContainer(name: String, image: String) {
        runDocker(arrayOf("run", "-d", "-it", "-m=100", "--cpus=1.5", "--network", "none", "--name", name, image))
    }

    suspend fun runContainer(name: String, settings: DockerContainerSettings) {
        val args =
            """
                run -d -it
                --name $name
                --memory=${settings.memoryMB}m
                --cpus=${settings.cpus}
                --network ${if (settings.isNetworkDisabled) "none" else "bridge"}
                -u ${settings.user}
                ${settings.image}
            """.trimIndent().replace('\n', ' ').split(" ").toTypedArray()
        runDocker(args);
    }

    suspend fun restartContainer(container: String) {
        // -t 0 provides instant restart
        runDocker(arrayOf("restart", "-t", "0", container))
    }

    suspend fun removeContainer(container: String) {
        runDocker(arrayOf("rm", "-f", container))
    }

    suspend fun exec(container: String, cmd1: String, cmd2: String): String {
        return runCmdIgnoringError("docker", (arrayOf("exec", container, cmd1, cmd2)))
    }

    fun monitorEvents(containerNamePrefix: String, eventsChannel: Channel<String>): Job {
        val args =
            arrayOf(
                "events",
                "--filter", "container=$containerNamePrefix",
                "--filter", "event=restart",
                "--filter", "event=create",
                "--filter", "event=start",
//                "--filter", "event=pause",
//                "--filter", "event=unpause",
//                "--filter", "event=kill",
//                "--filter", "event=die",
//                "--filter", "event=oom",
//                "--filter", "event=stop",
//                "--filter", "event=rename",
//                "--filter", "event=destroy",
                "--format", "{{.Actor.Attributes.name}},{{.Status}}",
                "--since", System.currentTimeMillis().toString(),
            )
        return readStreamByLines("docker", args, eventsChannel)
    }

    private suspend fun runDocker(args: Array<String>) = runCmd("docker", args)

    private suspend fun runCmd(cmd: String, args: Array<String>): String = withContext(Dispatchers.IO) {
        val process = ProcessBuilder(cmd, *args).start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()

        process.waitFor()

        if (process.exitValue() != 0) {
            throw DockerException("Process '$cmd' failed with exit code ${process.exitValue()}\n$stderr\n$stdout")
        }

        stdout
    }

    private suspend fun runCmdIgnoringError(cmd: String, args: Array<String>): String = withContext(Dispatchers.IO) {
        val builder = ProcessBuilder(cmd, *args)
        builder.redirectErrorStream(true)
        val process = builder.start()
        val stdout = process.inputStream.bufferedReader().readText()

        process.waitFor()

        stdout
    }

    private fun readStreamByLines(cmd: String, args: Array<String>, channel: Channel<String>): Job {
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