package eucalypt.docker

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class DockerContainer private constructor(
    val name: String,
    private val settings: DockerContainerSettings,
    private val eventsFeed: DockerEventsFeed
) {
    private var currentState: DockerContainerState = DockerContainerState.UNKNOWN
    private val stateChangeChannel = Channel<DockerContainerState>(Channel.UNLIMITED)

    val stateChannel: ReceiveChannel<DockerContainerState>
        get() = stateChangeChannel

    companion object {
        suspend fun run(
            name: String,
            settings: DockerContainerSettings,
            eventsFeed: DockerEventsFeed
        ): DockerContainer {
            val container = DockerContainer(name, settings, eventsFeed)
            eventsFeed.subscribe(name) { container.handleEvent(it) }

            Docker.runContainer(name, settings)

            return container
        }
    }

    fun exec(command: String, argument: String) = Docker.exec(name, command, argument)

    suspend fun rerun() {
        Docker.removeContainer(name)
        Docker.runContainer(name, settings)
    }

    suspend fun remove() {
        Docker.removeContainer(name)
        eventsFeed.unsubscribe(name)
        stateChangeChannel.cancel()
        currentState = DockerContainerState.REMOVED
    }

    override fun toString(): String = name

    private suspend fun handleEvent(event: DockerEvent) {
        val supposedState = when (event.status) {
            "create" -> DockerContainerState.NOT_READY
            "start" -> DockerContainerState.READY
            "unpause" -> DockerContainerState.NEED_RESTART
            "restart" -> DockerContainerState.NEED_RESTART
            "pause" -> DockerContainerState.NEED_RESTART
            "kill" -> DockerContainerState.NEED_RESTART
            "die" -> DockerContainerState.NEED_RESTART
            "oom" -> DockerContainerState.NEED_RESTART
            "stop" -> DockerContainerState.NEED_RESTART
            "rename" -> DockerContainerState.NEED_RESTART
            "destroy" -> DockerContainerState.REMOVED
            else -> DockerContainerState.UNKNOWN
        }

        if (supposedState != DockerContainerState.UNKNOWN) {
            currentState = supposedState
            stateChangeChannel.send(currentState)
        }
    }
}

enum class DockerContainerState {
    UNKNOWN,
    NOT_READY,
    READY,
    NEED_RESTART,
    REMOVED,
}