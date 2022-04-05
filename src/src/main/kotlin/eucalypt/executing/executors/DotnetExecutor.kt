package eucalypt.executing.executors

import eucalypt.executing.ExecutorType
import eucalypt.docker.DockerContainer

internal class DotnetExecutor(
    type: ExecutorType,
    dockerContainer: DockerContainer
) : BaseExecutor(type, dockerContainer) {
    override fun buildCliCommand(script: String) =
        // TODO build csproj and other staff like one command
        "dotnet run --project ./eucalypt-runner.csproj $script"
}