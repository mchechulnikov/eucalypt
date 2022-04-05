package eucalypt.docker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel

internal class DockerEventsMonitor (
    private val config: DockerEventMonitorConfig
): DockerMonitorManager, DockerEventsFeed {
    private var eventsChannel: Channel<String> = Channel(Channel.UNLIMITED)
    private var monitorJob: Job? = null
    private val subscribers = mutableMapOf<String, (DockerEvent) -> Unit>()

    override fun subscribe(container: String, callback: (DockerEvent) -> Unit) {
        subscribers[container] = callback
    }

    override fun unsubscribe(container: String) {
        subscribers.remove(container)
    }

    override suspend fun start() {
        if (monitorJob != null) {
            throw IllegalStateException("Docker monitor already started")
        }

        monitorJob = Docker.monitorEvents(config.containersPrefix, eventsChannel)
        while (true) {
            val rawEvent = eventsChannel.receive()
            val event = parseEvent(rawEvent)

            subscribers[event.container]?.invoke(event)
        }
    }

    override fun stop() {
        monitorJob?.cancel(CancellationException("Docker monitor stopped"))
        monitorJob = null
    }

    private fun parseEvent(event: String): DockerEvent =
        event.split(",").let { return DockerEvent(it[0], it[1]) }
}

