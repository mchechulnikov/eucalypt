package eucalypt.docker

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach

internal class DockerEventsMonitor (
    private val settings: DockerEventMonitorSettings
): DockerMonitorManager, DockerEventsFeed {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var eventsChannel: Channel<String> = Channel(Channel.UNLIMITED)
    private var monitorJob: Job? = null
    private val subscribers = mutableMapOf<String, suspend (DockerEvent) -> Unit>()

    override fun subscribe(container: String, callback: suspend (DockerEvent) -> Unit) {
        subscribers[container] = callback
    }

    override fun unsubscribe(container: String) {
        subscribers.remove(container)
    }

    override suspend fun start() {
        if (monitorJob != null) {
            throw DockerMonitorException("Docker monitor already started")
        }

        monitorJob = Docker.monitorEvents(settings.containersPrefix, eventsChannel)
        scope.launch {
            while (true) {
                val rawEvent = eventsChannel.receive()
                if (rawEvent.isNotBlank()) {
                    val event = parseEvent(rawEvent.trim())
                    subscribers[event.container]?.invoke(event)
                }
            }
        }
    }

    override fun stop() {
        monitorJob?.cancel(CancellationException("Docker monitor stopped"))
        monitorJob = null
        eventsChannel.cancel()
        scope.cancel()
    }

    private fun parseEvent(event: String): DockerEvent =
        event.split(",").let { return DockerEvent(it[0], it[1]) }
}