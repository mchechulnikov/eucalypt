package eucalypt.executing.pool

import eucalypt.executing.executors.*
import eucalypt.utils.every
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ExecutorsPoolImpl(
    private val settings: ExecutorsPoolSettings,
    private val executorsFactory: ExecutorsFactory,
) : ExecutorsPool {
    private val executors = mutableMapOf<String, Poolable>()

    suspend fun start() {
        settings.types.forEach { addNewExecutor(it) }
        runServing()
    }

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
        val isExecutorReady = runBlocking { waitReadiness(newExecutor) }
        if (!isExecutorReady) {
            throw ExecutorsPoolException("There are no ready executors but pool isn't exceeded. Has pool been started?")
        }

        // then run background extension if needed
        coroutineScope {
            launch { extendIfNeeded(type) }
        }

        return Result.success(newExecutor as ReservableExecutor)
    }

    private suspend fun addNewExecutor(type: ExecutorType): Poolable {
        val executor = executorsFactory.create(type)
        executors[executor.id] = executor
        return executor
    }

    private suspend fun waitReadiness(executor: Poolable): Boolean {
        var attempts = 0
        while (true) {
            if (executor.currentState.isReady) {
                return true
            }

            delay(settings.readinessProbeIntervalMs)
            if (attempts > settings.maxReadinessProbeAttempts) {
                break
            }
            attempts++
        }

        return false
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
            if (executors.size * 2 > settings.maxSize)
                settings.maxSize - executors.size
            else executors.size * 2

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

        fun isTimeoutHappened(executor: Poolable) =
            executor.stateTimestamp + settings.executionTimeoutMs < currentTimeMs

        val hangedExecutors = executors.values
            .filter { it.currentState.isTimeoutable && isTimeoutHappened(it) }
        if (hangedExecutors.isEmpty()) {
            return
        }

        coroutineScope {
            launch { hangedExecutors.forEach { it.reset() } }
        }
    }

    private fun getReadyExecutorOrNull(type: ExecutorType): Poolable? =
        executors.values.firstOrNull { it.type == type && it.currentState == ExecutorState.READY }
}
