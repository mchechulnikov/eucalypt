package eucalypt.executing.executors

data class ExecutorParameters (
    val executorTypeName: String,
    val memoryMB: Int,
    val cpuCores: Double,
    val isNetworkDisabled: Boolean,
    val spaceSizeMB: Long,
)