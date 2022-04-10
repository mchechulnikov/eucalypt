package eucalypt.infra.docker

import eucalypt.infra.docker.commands.DockerEventsCommand
import eucalypt.infra.docker.commands.DockerExecCommand
import eucalypt.infra.docker.commands.DockerRunCommand
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

interface DockerOperator {
    suspend fun getContainerNames(namePrefix: String) : List<String>
    suspend fun runContainer(cmd: DockerRunCommand)
    suspend fun removeContainer(container: String)
    suspend fun removeContainers(containers: List<String>)
    fun exec(container: String, cmd: DockerExecCommand): Pair<Job, Channel<String>>
    fun monitorEvents(cmd: DockerEventsCommand): Pair<Job, ReceiveChannel<String>>
}