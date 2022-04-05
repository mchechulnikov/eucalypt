package eucalypt.docker

import kotlinx.coroutines.coroutineScope
import java.util.*

class DockerContainer private constructor(
    val name: String,
    private val eventsFeed: DockerEventsFeed
) {
    private var status: String = "unknown"

    init {
        eventsFeed.subscribe(name) {
            status = it.status.lowercase(Locale.getDefault())
        }
    }

    companion object {
        suspend fun run(name: String, image: DockerImage, eventsFeed: DockerEventsFeed): DockerContainer {
            Docker.runContainer(name, image.toString())
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