package eucalypt.docker

interface DockerEventsFeed {
    fun subscribe(container: String, callback: (DockerEvent) -> Unit)
    fun unsubscribe(container: String)
}

