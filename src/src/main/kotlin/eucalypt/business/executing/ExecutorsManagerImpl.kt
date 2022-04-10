package eucalypt.business.executing

import eucalypt.business.executing.executors.*
import eucalypt.business.executing.executors.ExecutorType
import eucalypt.business.executing.pool.ExecutorsPool
import kotlinx.coroutines.delay
import org.slf4j.Logger

internal class ExecutorsManagerImpl (
    private val settings: ExecutorsManagerSettings,
    private val pool: ExecutorsPool,
    private val logger: Logger,
) : ExecutorsManager {
    override suspend fun borrowExecutor(type: ExecutorType): Result<Executor> {
        repeat(settings.borrowAttemptsCount) {
            pool.getAvailableExecutor(type)
                .onFailure {
                    logger.info("Failed to borrow executor of type $type. Trying again...")
                    delay(settings.borrowAttemptDelayMs)
                }
                .onSuccess {
                    if (it.tryReserve()) {
                        logger.info("Executor of type $type is borrowed")
                        return Result.success(it)
                    }
                }
        }

        logger.info("Failed to borrow executor of type $type after ${settings.borrowAttemptsCount} attempts")
        return Result.failure(Error("Failed to borrow executor of type $type"))
    }

    override suspend fun redeemExecutor(executor: Executor) =
        (executor as ReservableExecutor).release()
}
