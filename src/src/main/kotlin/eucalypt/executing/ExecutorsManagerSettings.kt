package eucalypt.executing

interface ExecutorsManagerSettings {
    val borrowAttemptsCount: Int
    val borrowAttemptDelayMs: Long
}
