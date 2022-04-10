package eucalypt.business.executing.pool

import eucalypt.business.executing.executors.ExecutorType
import eucalypt.business.executing.executors.ReservableExecutor


interface ExecutorsPool {
    suspend fun getAvailableExecutor(type: ExecutorType): Result<ReservableExecutor>
}

