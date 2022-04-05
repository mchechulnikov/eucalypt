package eucalypt.executing.pool

import eucalypt.executing.ExecutorState
import eucalypt.executing.ExecutorType
import eucalypt.executing.executors.ExecutorsFactory
import eucalypt.executing.executors.PoolableExecutor
import eucalypt.utils.every
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ExecutorsPoolImpl(
    private val config: ExecutorPoolConfig,
    private val executorsFactory: ExecutorsFactory,
) : ExecutorsPool {
    private val executors = mutableMapOf<String, PoolableExecutor>()

    suspend fun start() {
        config.types.forEach { addNewExecutor(it) }
        runServing()
    }

    override suspend fun getAvailableExecutor(type: ExecutorType): Result<ReservableExecutor> {
        // try to get ready executor from pool
        val executor = getReadyExecutorOrNull(type)
        if (executor != null) {
            return Result.success(executor)
        }

        // try to extend pool
        if (executors.size > config.maxSize) {
            return Result.failure(Error("Pool size exceeded"))
        }

        val newExecutor = addNewExecutor(type)

        coroutineScope {
            launch { extendIfNeeded(type) }
        }

        return Result.success(newExecutor)
    }

    override fun returnExecutor(executorID: String) {
        if (executorID.isBlank())
            throw IllegalArgumentException("Executor ID cannot be empty")
        if (executorID !in executors.keys)
            throw IllegalArgumentException("Executor with id $executorID not found")

        executors[executorID]?.release()
    }

    private suspend fun addNewExecutor(type: ExecutorType): PoolableExecutor {
        val executor = executorsFactory.create(type)
        executors[executor.id] = executor

        runBlocking {
            while (!executor.currentState.isReady) {
                delay(config.readinessProbeIntervalMs)
            }
        }

        return executor
    }

    private suspend fun runServing() = coroutineScope {
        launch { every(config.shrinkIntervalMs, ::shrinkIfNeeded) }
        launch { every(config.detectHangedIntervalMs, ::detectHanged) }
    }

    private suspend fun extendIfNeeded(type: ExecutorType) {
        val readyExecutors = executors.values.filter { it.type == type }
        if (readyExecutors.size >= config.minReadyExecutorsSize) {
            return
        }

        val addCount =
            if (executors.size * 2 > config.maxSize)
                config.maxSize - executors.size
            else executors.size * 2

        // TODO we can do one operation for all executors by time
        repeat(addCount) { addNewExecutor(type) }
    }

    private suspend fun shrinkIfNeeded() = coroutineScope {
        for (type in config.types) {
            val readyExecutors = executors.values
                .filter { it.currentState.isReady && it.type == type }

            if (readyExecutors.size <= config.minReadyExecutorsSize * 2) {
                continue
            }

            // TODO we can do one operation for all executors by time
            readyExecutors
                .take(readyExecutors.size / 2)
                .forEach {
                    launch { it.eliminate() }
                    executors.remove(it.id)     // TODO remove after elimination?
                }
        }
    }

    private suspend fun detectHanged() {
        val currentTimeMs = System.currentTimeMillis()

        fun isTimeoutHappened(executor: PoolableExecutor) =
            executor.stateTimestamp + config.executionTimeoutMs < currentTimeMs

        val hangedExecutors = executors.values
            .filter { it.currentState.isTimeoutable && isTimeoutHappened(it) }
        if (hangedExecutors.isEmpty()) {
            return
        }

        coroutineScope {
            launch { hangedExecutors.forEach { it.reset() } }
        }
    }

    private fun getReadyExecutorOrNull(type: ExecutorType): PoolableExecutor? =
        executors.values.firstOrNull { it.type == type && it.currentState == ExecutorState.READY }
}