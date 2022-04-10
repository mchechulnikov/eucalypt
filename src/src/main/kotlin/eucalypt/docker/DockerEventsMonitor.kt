package eucalypt.docker

import eucalypt.docker.commands.DockerEventsCommand
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import org.slf4j.Logger

internal class DockerEventsMonitor (
    private val settings: DockerEventMonitorSettings,
    private val logger: Logger
): DockerMonitorManager, DockerEventsFeed {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var monitorJob: Job? = null
    private var eventsChannel: ReceiveChannel<String>? = null
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

        val (job, channel) = Docker.monitorEvents(DockerEventsCommand(
            containerNamePrefix = settings.containersPrefix,
            eventTypes = listOf(
                "create", "start", "restart", "pause", "unpause",
                "kill", "die", "oom", "stop", "rename", "destroy",
            ),
            format = "{{.Actor.Attributes.name}},{{.Status}}",
            sinceMs = System.currentTimeMillis().toString(),
        ))
        monitorJob = job
        eventsChannel = channel

        scope.launch {
            while (true) {
                val rawEvent = channel.receive()
                if (rawEvent.isNotBlank()) {
                    val event = parseEvent(rawEvent.trim())
                    subscribers[event.container]?.invoke(event)
                }
            }
        }

        logger.info("Docker events monitor started")
    }

    override fun stop() {
        monitorJob?.cancel(CancellationException("Docker monitor stopped"))
        monitorJob = null
        eventsChannel?.cancel()
        eventsChannel = null
        scope.cancel()

        logger.info("Docker events monitor stopped")
    }

    private fun parseEvent(event: String): DockerEvent =
        event.split(",").let { return DockerEvent(it[0], it[1]) }
}