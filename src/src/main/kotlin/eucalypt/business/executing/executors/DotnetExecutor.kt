package eucalypt.business.executing.executors

import eucalypt.infra.docker.DockerContainer
import eucalypt.infra.docker.commands.DockerExecCommand
import eucalypt.infra.docker.DockerImage
import eucalypt.infra.docker.commands.DockerRunCommand
import org.slf4j.Logger

internal class DotnetExecutor(
    type: ExecutorType,
    dockerContainer: DockerContainer,
    logger: Logger
) : BaseExecutor(type, dockerContainer, logger) {
    override val parameters: ExecutorParameters = DotnetExecutor.parameters

    override fun buildExecCommand(script: String) =
        DockerExecCommand(
            command = listOf("/restore-dir/exec.sh", script),
            user = "executor",
        )

    companion object {
        private val parameters: ExecutorParameters = ExecutorParameters (
            executorTypeName = ".NET SDK 6.0",
            memoryMB = 100,
            cpuCores = 1.5,
            isNetworkDisabled = true,
            spaceSizeMB = 100
        )

        fun buildRunCommand(containerName: String) = DockerRunCommand(
            containerName = containerName,
            image = DockerImage(imageName, "dotnet6").toString(),
            memoryMB = parameters.memoryMB,
            cpus = parameters.cpuCores,
            isNetworkDisabled = parameters.isNetworkDisabled,
            tmpfsDir = "/exec-dir",
            tmpfsSizeBytes = parameters.spaceSizeMB * 1024 * 1024,
            user = "executor",
        )
    }
}