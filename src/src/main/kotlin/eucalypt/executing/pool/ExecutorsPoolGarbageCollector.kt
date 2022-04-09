package eucalypt.executing.pool

interface ExecutorsPoolGarbageCollector {
    suspend fun collect()
}

