package eucalypt.docker

import kotlinx.coroutines.Job

interface DockerMonitorManager {
    suspend fun start()
    fun stop()
}
