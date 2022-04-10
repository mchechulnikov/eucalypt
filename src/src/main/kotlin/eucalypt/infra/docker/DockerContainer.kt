package eucalypt.infra.docker

import eucalypt.infra.docker.commands.DockerExecCommand
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel

interface DockerContainer {
    val name: String
    val stateChannel: ReceiveChannel<DockerContainerState>
    fun exec(command: DockerExecCommand): Pair<Job, ReceiveChannel<String>>
    suspend fun rerun()
    suspend fun remove()
}