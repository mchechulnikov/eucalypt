package eucalypt.docker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import java.util.*

class DockerContainer private constructor(
    val name: String,
    private val
    private val eventsFeed: DockerEventsFeed
) {
    private var status: String = "unknown"
    private var statusChangeChannel = Channel<String>(Channel.UNLIMITED)

    init {
        eventsFeed.subscribe(name) {
            status = it.status.lowercase(Locale.getDefault())
        }
    }

    companion object {
        suspend fun run(
            name: String,
            settings: DockerContainerSettings,
            eventsFeed: DockerEventsFeed
        ): DockerContainer {
            Docker.runContainer(name, settings)

            return DockerContainer(name, eventsFeed)
        }
    }

    suspend fun exec(command: String) = Docker.exec(name, command)

    suspend fun restart() = coroutineScope {
        Docker.restartContainer(name)
    }

    suspend fun remove() {
        Docker.removeContainer(name)
        eventsFeed.unsubscribe(name)
        status = "removed"
    }

    fun isReady(): Boolean =
        when (status) {
            "running" -> true
            else -> false
        }

    fun needRestart(): Boolean =
        when (status) {
            "pause" -> true
            "exited" -> true
            "killed" -> true
            "die" -> true
            else -> false
        }

    override fun toString(): String = name
}


fun CoroutineScope.filter(numbers: ReceiveChannel<Int>, prime: Int) = produce<Int> {
    for (x in numbers) if (x % prime != 0) send(x)
}
