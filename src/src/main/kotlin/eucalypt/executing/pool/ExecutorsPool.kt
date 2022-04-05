package eucalypt.executing.pool

import eucalypt.executing.ExecutorType

interface ExecutorsPool {
    suspend fun getAvailableExecutor(type: ExecutorType): Result<ReservableExecutor>
    fun returnExecutor(executorID: String)
}

