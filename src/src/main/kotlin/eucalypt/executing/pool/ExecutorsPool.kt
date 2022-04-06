package eucalypt.executing.pool

import eucalypt.executing.executors.ExecutorType
import eucalypt.executing.executors.ReservableExecutor

interface ExecutorsPool {
    suspend fun getAvailableExecutor(type: ExecutorType): Result<ReservableExecutor>
}

