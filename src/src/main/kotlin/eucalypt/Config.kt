package eucalypt

import eucalypt.business.executing.ExecutorsManagerSettings
import eucalypt.business.executing.executors.ExecutorType
import eucalypt.business.executing.pool.ExecutorsPoolSettings
import eucalypt.business.ScriptRunnerSettings
import eucalypt.infra.docker.DockerEventMonitorSettings

internal object Config {
    private const val poolName = "eucalypt-executor-main"

    object ScriptRunnerConfig : ScriptRunnerSettings {
        override val runningTimeoutMs: Long = 10_000
    }

    object ExecutorsManagerConfig : ExecutorsManagerSettings {
        override var borrowAttemptsCount: Int = 3
        override var borrowAttemptDelayMs: Long = 500
    }

    object ExecutorsPoolConfig : ExecutorsPoolSettings {
        override var name: String = poolName
        override var maxSize: Int = 20
        override var minReadyExecutorsCount: Int = 3
        override val isShrinkEnabled: Boolean = true
        override var shrinkIntervalMs: Long = 30_000
        override val isDetectHangedEnabled: Boolean = true
        override var detectHangedIntervalMs: Long = 10_000
        override var hangingTimeoutMs: Long = 20_000
        override var types: List<ExecutorType> = listOf(ExecutorType.DOTNET6)
    }

    object DockerEventMonitorConfig : DockerEventMonitorSettings {
        override var containersPrefix: String = poolName
    }
}
