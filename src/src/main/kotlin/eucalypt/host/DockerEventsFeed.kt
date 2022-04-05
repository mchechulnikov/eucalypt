package eucalypt.host

interface DockerEventsFeed {
    fun subscribe(container: String, callback: (DockerEvent) -> Unit)
    fun unsubscribe(container: String)
}

