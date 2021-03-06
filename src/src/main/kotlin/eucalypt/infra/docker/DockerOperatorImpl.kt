package eucalypt.infra.docker

import eucalypt.infra.docker.commands.DockerEventsCommand
import eucalypt.infra.docker.commands.DockerExecCommand
import eucalypt.infra.docker.commands.DockerRunCommand
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

internal class DockerOperatorImpl : DockerOperator {
    override suspend fun getContainerNames(namePrefix: String) : List<String> {
        val r = runDocker(arrayOf("ps", "-a", "--filter", "name=$namePrefix", "--format", "{{.Names}}"))
        return r.split("\n").filter { it.isNotEmpty() }
    }

    override suspend fun runContainer(cmd: DockerRunCommand) {
        val args =
            """
                run -d -it
                --name ${cmd.containerName}
                --memory=${cmd.memoryMB}m
                --cpus=${cmd.cpus}
                --network ${if (cmd.isNetworkDisabled) "none" else "bridge"}
                ${
                    if (cmd.tmpfsDir != null) 
                        "--mount type=tmpfs,destination=${cmd.tmpfsDir},tmpfs-size=${cmd.tmpfsSizeBytes}"
                    else ""
                }
                -u ${cmd.user}
                ${cmd.image}
                ${cmd.command}
            """.trimIndent().replace('\n', ' ').split(" ").filter { it.isNotEmpty() }.toTypedArray()
        runDocker(args);
    }

    override suspend fun removeContainer(container: String) {
        runDocker(arrayOf("rm", "-f", container))
    }

    override suspend fun removeContainers(containers: List<String>) {
        runDocker(arrayOf("rm", "-f") + containers)
    }

    override fun exec(container: String, cmd: DockerExecCommand): Pair<Job, Channel<String>> {
        val channel = Channel<String>(100, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val args =
            """
                exec 
                ${if (cmd.workdir != null) "-w ${cmd.workdir}" else ""}
                -u ${cmd.user}
                $container
            """
                .trimIndent()
                .replace('\n', ' ')
                .split(" ")
                .filter { it.isNotEmpty() }
                .toTypedArray()

        return readStreamByLines("docker", args + cmd.command, channel) to channel
    }

    override fun monitorEvents(cmd: DockerEventsCommand): Pair<Job, ReceiveChannel<String>> {
        val channel = Channel<String>(Channel.UNLIMITED)
        val args = buildString {
            appendLine("events")
            appendLine("--filter container=${cmd.containerNamePrefix}")
            cmd.eventTypes.forEach { appendLine("--filter event=$it") }
            appendLine("--format ${cmd.format}")
            appendLine("--since ${cmd.sinceMs}")
        }
            .trimIndent()
            .replace('\n', ' ')
            .split(" ")
            .filter { it.isNotEmpty() }
            .toTypedArray()

        return readStreamByLines("docker", args, channel) to channel
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