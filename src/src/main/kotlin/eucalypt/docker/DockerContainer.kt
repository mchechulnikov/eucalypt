package eucalypt.docker

import eucalypt.docker.commands.DockerExecCommand
import eucalypt.docker.commands.DockerRunCommand
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class DockerContainer private constructor(
    val name: String,
    private val runCommand: DockerRunCommand,
    private val eventsFeed: DockerEventsFeed
) {
    private var currentState: DockerContainerState = DockerContainerState.STOPPED
    private val stateChangeChannel = Channel<DockerContainerState>(Channel.UNLIMITED)

    val stateChannel: ReceiveChannel<DockerContainerState>
        get() = stateChangeChannel

    companion object {
        suspend fun run(
            name: String,
            command: DockerRunCommand,
            eventsFeed: DockerEventsFeed
        ): DockerContainer {
            val container = DockerContainer(name, command, eventsFeed)
            eventsFeed.subscribe(name) { container.handleEvent(it) }

            Docker.runContainer(command)

            return container
        }
    }

    fun exec(command: DockerExecCommand) = Docker.exec(name, command)

    suspend fun rerun() {
        Docker.removeContainer(name)
        Docker.runContainer(runCommand)
    }

    suspend fun remove() {
        eventsFeed.unsubscribe(name)
        Docker.removeContainer(name)
        stateChangeChannel.cancel()
    }

    override fun toString(): String = name

    private suspend fun handleEvent(event: DockerEvent) {
        // States transition based on
        // https://medium.com/devopsion/life-and-death-of-a-container-146dfc62f808
        // NOTE: This is not a complete implementation of the Docker container lifecycle
        val supposedState = when (event.status) {
            "create" -> DockerContainerState.STOPPED
            "start" -> DockerContainerState.RUNNING
            "restart" -> DockerContainerState.RUNNING   // docker restart: die -> start -> restart -> RUNNING
            "unpause" -> DockerContainerState.RUNNING
            "pause" -> DockerContainerState.PAUSED
            "kill" -> DockerContainerState.STOPPED      // if there is no start event, it's considered as a STOPPED
            "die" -> DockerContainerState.STOPPED       // if there is no start event, it's considered as a STOPPED
            "oom" -> DockerContainerState.STOPPED       // if there is no start event, it's considered as a STOPPED
            "stop" -> DockerContainerState.STOPPED
            "destroy" -> DockerContainerState.DELETED
            else -> DockerContainerState.UNKNOWN
        }

        currentState = supposedState
        stateChangeChannel.send(currentState)
    }
}

