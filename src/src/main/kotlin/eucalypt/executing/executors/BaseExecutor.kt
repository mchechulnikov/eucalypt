package eucalypt.executing.executors

import eucalypt.executing.ExecutorState
import eucalypt.executing.ExecutorType
import eucalypt.host.DockerContainer
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseExecutor protected constructor(
    override val type: ExecutorType,
    private val dockerContainer: DockerContainer
) : PoolableExecutor {
    private var reservationTimestamp: Long = 0
    private val isReserved: AtomicBoolean = AtomicBoolean(false)

    override var currentState: ExecutorState = ExecutorState.NEW
    override var stateTimestamp: Long = System.currentTimeMillis()

    override val id get() = dockerContainer.name

    override suspend fun execute(script: String): String {
        if (script.isBlank()) {
            throw IllegalArgumentException("Script is empty")
        }

        setState(ExecutorState.EXECUTING)
        val command = buildCliCommand(script)

        return dockerContainer.exec(command)
    }

    override fun tryReserve(): Boolean {
        if (!isReserved.compareAndSet(false, true)) {
            return false
        }

        reservationTimestamp = System.currentTimeMillis()
        setState(ExecutorState.RESERVED)

        return true
    }

    override suspend fun reset() {
        setState(ExecutorState.RESET)
        dockerContainer.restart()
    }

    override suspend fun eliminate() {
        setState(ExecutorState.ELIMINATED)
        dockerContainer.remove()
    }

    override fun release() {
        setState(ExecutorState.RELEASED)
        reservationTimestamp = 0
        isReserved.set(false)
    }

    protected abstract fun buildCliCommand(script: String): String

    private fun setState(state: ExecutorState) {
        this.currentState = state
        this.stateTimestamp = System.currentTimeMillis()
    }
}
