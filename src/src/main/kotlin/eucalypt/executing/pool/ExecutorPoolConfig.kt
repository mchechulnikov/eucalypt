package eucalypt.executing.pool

import eucalypt.executing.ExecutorType

data class ExecutorPoolConfig(
    val minSize: Int = 5,
    val maxSize: Int = 20,
    val minReadyExecutorsSize: Int = 3,
    val detectHangedIntervalMs: Long = 10_000,
    val shrinkIntervalMs: Long = 30_000,
    val readinessProbeIntervalMs: Long = 200,
    val executionTimeoutMs: Long = 20_000,
    val types: List<ExecutorType> = listOf(ExecutorType.DOTNET),
)

