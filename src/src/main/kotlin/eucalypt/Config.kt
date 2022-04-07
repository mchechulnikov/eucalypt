package eucalypt

import eucalypt.docker.DockerEventMonitorSettings
import eucalypt.executing.executors.ExecutorType
import eucalypt.executing.ExecutorsManagerSettings
import eucalypt.executing.pool.ExecutorsPoolSettings
import java.util.*

internal object Config {
    private const val poolName = "eucalypt-executor-main"

    object ExecutorsManagerConfig : ExecutorsManagerSettings {
        override var borrowAttemptsCount: Int = 3
        override var borrowAttemptDelayMs: Long = 500
    }

    object ExecutorsPoolConfig : ExecutorsPoolSettings {
        override var name: String = poolName
        override var maxSize: Int = 20
        override var minReadyExecutorsCount: Int = 3
        override var detectHangedIntervalMs: Long = 10_000
        override var shrinkIntervalMs: Long = 30_000
        override var readinessProbeIntervalMs: Long = 1000
        override var maxReadinessProbeAttempts: Int = 3
        override var executionTimeoutMs: Long = 60_000
        override var types: List<ExecutorType> = listOf(ExecutorType.DOTNET6)
    }

    object DockerEventMonitorConfig : DockerEventMonitorSettings {
        override var containersPrefix: String = poolName
    }
}