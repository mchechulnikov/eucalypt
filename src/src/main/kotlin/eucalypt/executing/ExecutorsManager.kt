package eucalypt.executing

import eucalypt.executing.executors.Executor
import eucalypt.executing.executors.ExecutorType
import eucalypt.executing.executors.ReservableExecutor

interface ExecutorsManager {
    suspend fun borrowExecutor(type: ExecutorType): Result<Executor>
    suspend fun redeemExecutor(executor: Executor)
}
