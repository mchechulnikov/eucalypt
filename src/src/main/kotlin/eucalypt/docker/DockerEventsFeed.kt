package eucalypt.docker

import java.nio.channels.Channel

interface DockerEventsFeed {
    fun subscribe(container: String, callback: (DockerEvent) -> Unit)
    fun unsubscribe(container: String)
    val events: Channel<DockerEvent>
}

