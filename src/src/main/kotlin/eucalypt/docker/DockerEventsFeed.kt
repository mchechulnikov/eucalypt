package eucalypt.docker

interface DockerEventsFeed {
    fun subscribe(container: String, callback: suspend (DockerEvent) -> Unit)
    fun unsubscribe(container: String)
}

