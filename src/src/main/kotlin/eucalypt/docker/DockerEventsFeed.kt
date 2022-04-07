package eucalypt.docker

import java.nio.channels.Channel

interface DockerEventsFeed {
    fun subscribe(container: String, callback: suspend (DockerEvent) -> Unit)
    fun unsubscribe(container: String)
}

