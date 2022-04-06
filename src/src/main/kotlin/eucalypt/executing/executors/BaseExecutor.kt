package eucalypt.executing.executors

import eucalypt.docker.DockerContainer
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseExecutor protected constructor(
    override val type: ExecutorType,
    private val dockerContainer: DockerContainer
) : Poolable, ReservableExecutor, Executor {
    private var reservationTimestamp: Long = 0
    private val isReserved: AtomicBoolean = AtomicBoolean(false)

    override var currentState: ExecutorState = ExecutorState.NEW
    override var stateTimestamp: Long = System.currentTimeMillis()

    override val id get() = dockerContainer.name

    companion object {
        const val imageName = "eucalypt/executor"
    }

    override suspend fun execute(script: String): String {
        if (script.isBlank()) {
            throw IllegalArgumentException("Script is empty")
        }

        setState(ExecutorState.EXECUTING)
        val command = buildExecCommand(script)

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

    protected abstract fun buildExecCommand(script: String): String

    private fun setState(state: ExecutorState) {
        this.currentState = state
        this.stateTimestamp = System.currentTimeMillis()
    }
}
