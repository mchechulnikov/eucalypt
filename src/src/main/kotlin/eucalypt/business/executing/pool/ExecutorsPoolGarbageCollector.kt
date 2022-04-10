package eucalypt.business.executing.pool

interface ExecutorsPoolGarbageCollector {
    suspend fun collect()
}

