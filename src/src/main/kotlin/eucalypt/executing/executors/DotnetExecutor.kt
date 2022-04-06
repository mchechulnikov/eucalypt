package eucalypt.executing.executors

import eucalypt.docker.DockerContainer
import eucalypt.docker.DockerImage
import eucalypt.docker.DockerContainerSettings

internal class DotnetExecutor(
    type: ExecutorType,
    dockerContainer: DockerContainer
) : BaseExecutor(type, dockerContainer) {
    // quotes are required for the script body to save multiline structure
    override fun buildExecCommand(script: String) = "/app/entrypoint.sh \"$script\""

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