package eucalypt.executing

data class ExecutorsManagerConfig(
    val borrowAttemptsCount: Int = 3,
    val borrowAttemptDelayMs: Long = 500,
)
