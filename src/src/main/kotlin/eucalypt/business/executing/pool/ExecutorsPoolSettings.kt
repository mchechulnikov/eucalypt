package eucalypt.business.executing.pool

import eucalypt.business.executing.executors.ExecutorType

interface ExecutorsPoolSettings {
    val name: String
    val maxSize: Int
    val minReadyExecutorsCount: Int
    val isShrinkEnabled: Boolean
    val shrinkIntervalMs: Long
    val isDetectHangedEnabled: Boolean
    val detectHangedIntervalMs: Long
    val hangingTimeoutMs: Long
    val types: List<ExecutorType>
}

