package eucalypt.executing

import eucalypt.executing.executors.Executor
import eucalypt.executing.executors.ExecutorType
import eucalypt.executing.executors.ReservableExecutor
import eucalypt.executing.pool.ExecutorsPool
import kotlinx.coroutines.delay

internal class ExecutorsManagerImpl (
    private val settings: ExecutorsManagerSettings,
    private val pool: ExecutorsPool
) : ExecutorsManager {
    override suspend fun borrowExecutor(type: ExecutorType): Result<Executor> {
        repeat(settings.borrowAttemptsCount) {
            pool.getAvailableExecutor(type)
                .onFailure { delay(settings.borrowAttemptDelayMs) }
                .onSuccess {
                    if (it.tryReserve()) {
                        return Result.success(it)
                    }
                }
        }

        return Result.failure(Error("Failed to borrow executor of type $type"))
    }

    override fun redeemExecutor(executor: Executor) = (executor as ReservableExecutor).release()
}
