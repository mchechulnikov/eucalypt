package eucalypt.business.executing

import eucalypt.business.executing.executors.Executor
import eucalypt.business.executing.executors.ExecutorType

interface ExecutorsManager {
    suspend fun borrowExecutor(type: ExecutorType): Result<Executor>
    suspend fun redeemExecutor(executor: Executor)
}
