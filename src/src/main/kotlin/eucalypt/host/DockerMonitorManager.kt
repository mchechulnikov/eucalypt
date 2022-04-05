package eucalypt.host

interface DockerMonitorManager {
    suspend fun start()
    fun stop()
}
