package eucalypt.executing

import eucalypt.executing.pool.ExecutorsPool
import kotlinx.coroutines.delay

class ExecutorsManagerImpl (
    private val config: ExecutorsManagerConfig,
    private val pool: ExecutorsPool
) : ExecutorsManager {
    override suspend fun borrowExecutor(type: ExecutorType): Result<Executor> {
        repeat(config.borrowAttemptsCount) {
            pool.getAvailableExecutor(type)
                .onFailure { delay(config.borrowAttemptDelayMs) }
                .onSuccess {
                    if (it.tryReserve()) {
                        return Result.success(it)
                    }
                }
        }

        return Result.failure(Error("Failed to borrow executor of type $type"))
    }

    override fun redeemExecutor(executorID: String) =
        pool.returnExecutor(executorID)
}
