package eucalypt.executing.pool

interface ExecutorsPoolManager {
    suspend fun start()
    suspend fun stop()
}