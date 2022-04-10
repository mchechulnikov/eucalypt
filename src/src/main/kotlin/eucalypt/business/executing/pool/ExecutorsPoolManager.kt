package eucalypt.business.executing.pool

interface ExecutorsPoolManager {
    suspend fun start()
    suspend fun stop()
}