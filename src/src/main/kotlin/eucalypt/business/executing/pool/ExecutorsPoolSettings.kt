package eucalypt.business.executing.pool

import eucalypt.business.executing.executors.ExecutorType

interface ExecutorsPoolSettings {
    val name: String
    val maxSize: Int
    val minReadyExecutorsCount: Int
    val detectHangedIntervalMs: Long
    val shrinkIntervalMs: Long
    val readinessProbeIntervalMs: Long
    val maxReadinessProbeAttempts: Int
    val hangingTimeoutMs: Long
    val types: List<ExecutorType>
}

