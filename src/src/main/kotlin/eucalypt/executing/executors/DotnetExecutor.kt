package eucalypt.executing.executors

import eucalypt.docker.DockerContainer
import eucalypt.docker.DockerImage
import eucalypt.docker.DockerContainerSettings
import org.slf4j.Logger

internal class DotnetExecutor(
    type: ExecutorType,
    dockerContainer: DockerContainer,
    logger: Logger
) : BaseExecutor(type, dockerContainer, logger) {
    override fun buildExecCommand(script: String): Pair<String, String> = "/app/entrypoint.sh" to script

    override val typeName: String = ".NET SDK 6.0"
    override val executingBy: String = "dotnet run --no-restore"

    companion object {
        val containerSettings = DockerContainerSettings(
            image = DockerImage(imageName, "dotnet6").toString(),
            memoryMB = 100,
            cpus = 1.5,
            isNetworkDisabled = true,
            user = "csexec",
        )
    }
}