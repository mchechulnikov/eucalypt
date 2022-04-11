package eucalypt.http

interface HTTPServerSettings {
    val port: Int
    val host: String
    val gracefulShutdownTimeoutSec: Long
}