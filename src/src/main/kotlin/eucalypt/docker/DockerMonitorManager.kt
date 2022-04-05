package eucalypt.docker

interface DockerMonitorManager {
    suspend fun start()
    fun stop()
}
