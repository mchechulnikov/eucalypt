package eucalypt.executing.executors

import eucalypt.docker.DockerContainer
import eucalypt.docker.DockerExecCommand
import eucalypt.docker.DockerImage
import eucalypt.docker.DockerRunCommand
import org.slf4j.Logger

internal class DotnetExecutor(
    type: ExecutorType,
    dockerContainer: DockerContainer,
    logger: Logger
) : BaseExecutor(type, dockerContainer, logger) {
    override val typeName: String = ".NET SDK 6.0"
    override val executingBy: String = "dotnet run --no-restore"

    override fun buildExecCommand(script: String) =
        DockerExecCommand(
            command = listOf("/restore-dir/exec.sh", script),
            user = "executor",
        )

    companion object {
        fun buildRunCommand(containerName: String) = DockerRunCommand(
            containerName = containerName,
            image = DockerImage(imageName, "dotnet6").toString(),
            memoryMB = 100,
            cpus = 1.5,
            isNetworkDisabled = true,
            tmpfsDir = "/exec-dir",
            user = "root",
        )
    }
}