package eucalypt.executing.pool

import eucalypt.executing.executors.*
import eucalypt.utils.every
import kotlinx.coroutines.*
import org.slf4j.Logger

class ExecutorsPoolImpl(
    private val settings: ExecutorsPoolSettings,
    private val executorsFactory: ExecutorsFactory,
    private val garbageCollector: ExecutorsPoolGarbageCollector,
    private val logger: Logger,
) : ExecutorsPool, ExecutorsPoolManager {
    private val executors = mutableMapOf<String, Poolable>()
    private val servingScope = CoroutineScope(Dispatchers.Unconfined)

    override suspend fun getAvailableExecutor(type: ExecutorType): Result<ReservableExecutor> {
        val executor = getReadyExecutorOrNull(type)
        if (executor != null) {
            return Result.success(executor as ReservableExecutor)
        }

        // try to extend pool
        if (executors.size > settings.maxSize) {
            return Result.failure(Error("Pool size exceeded"))
        }

        // first create one executor and wait for readiness
        val newExecutor = addNewExecutor(type)
        if (newExecutor.currentState != ExecutorState.READY && !newExecutor.readiness.receive()) {
            throw ExecutorsPoolException("There are no ready executors but pool isn't exceeded. Has pool been started?")
        }

        // then run background extension if needed
        servingScope.launch { extendIfNeeded(type) }

        return Result.success(newExecutor as ReservableExecutor)
    }

    override suspend fun start() {
        logger.info("Starting executors pool '${settings.name}'")

        garbageCollector.collect()
        settings.types.forEach { extendIfNeeded(it) }
        servingScope.launch { runServing() }

        logger.info("Executors pool '${settings.name}' started")
    }

    override suspend fun stop() {
        logger.info("Stopping executors pool '${settings.name}'")


        // TODO remove all in batch
        executors.values.forEach { it.eliminate() }
        val executorsIds = executors.values.map { it.id }
        executorsIds.forEach { executors.remove(it) }

        servingScope.cancel()

        logger.info("Executors pool '${settings.name}' stopped")
    }

    private suspend fun addNewExecutor(type: ExecutorType): Poolable {
        val executor = executorsFactory.create(settings.name, type)
        executors[executor.id] = executor

        logger.info( "Created new executor '${executor.id}' of type '${type.name}'")

        return executor
    }

    private suspend fun runServing() = coroutineScope {
        launch { every(settings.shrinkIntervalMs, ::shrinkIfNeeded) }
        launch { every(settings.detectHangedIntervalMs, ::detectHanged) }
    }

    private suspend fun extendIfNeeded(type: ExecutorType) {
        val readyExecutors = executors.values.filter { it.type == type }
        if (readyExecutors.size >= settings.minReadyExecutorsCount) {
            return
        }

        val addCount =
            if (executors.isEmpty())
                settings.minReadyExecutorsCount
            else if (executors.size * 2 > settings.maxSize)
                settings.maxSize - executors.size
            else executors.size * 2

        logger.info("Extending executors pool '${settings.name}' by $addCount executors")

        // TODO we can do one operation for all executors by time
        repeat(addCount) { addNewExecutor(type) }
    }

    private suspend fun shrinkIfNeeded() = coroutineScope {
        for (type in settings.types) {
            val readyExecutors = executors.values
                .filter { it.currentState.isReady && it.type == type }

            if (readyExecutors.size <= settings.minReadyExecutorsCount * 2) {
                continue
            }

            logger.info(
                "Shrinking executors pool '${settings.name}' " +
                "by ${readyExecutors.size / 2} executors of type $type"
            )

            // TODO we can do one operation for all executors by time
            readyExecutors
                .take(readyExecutors.size / 2)
                .forEach {
                    launch { it.eliminate() }
                    executors.remove(it.id)
                }
        }
    }

    private suspend fun detectHanged() {
        val currentTimeMs = System.currentTimeMillis()

        fun isTimeoutHappened(executor: Poolable) =
            executor.stateTimestamp + settings.hangingTimeoutMs < currentTimeMs

        val hangedExecutors = executors.values
            .filter { it.currentState.isTimeoutable && isTimeoutHappened(it) }
        if (hangedExecutors.isEmpty()) {
            return
        }

        logger.info(
            "Detected hanged executors in pool '${settings.name}': " +
            hangedExecutors.joinToString(", ") { it.id }
        )

        servingScope.launch {
            hangedExecutors.forEach { it.reset() }
        }
    }

    private fun getReadyExecutorOrNull(type: ExecutorType): Poolable? =
        executors.values.firstOrNull { it.type == type && it.currentState == ExecutorState.READY }
}
