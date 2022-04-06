package eucalypt.executing.pool

import eucalypt.executing.executors.ExecutorType

interface ExecutorsPoolSettings {
    val maxSize: Int
    val minReadyExecutorsCount: Int
    val detectHangedIntervalMs: Long
    val shrinkIntervalMs: Long
    val readinessProbeIntervalMs: Long
    val maxReadinessProbeAttempts: Int
    val executionTimeoutMs: Long
    val types: List<ExecutorType>
}

