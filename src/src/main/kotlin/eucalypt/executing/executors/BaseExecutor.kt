package eucalypt.executing.executors

import eucalypt.docker.DockerContainer
import eucalypt.docker.DockerContainerState
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseExecutor protected constructor(
    override val type: ExecutorType,
    private val dockerContainer: DockerContainer
) : Poolable, ReservableExecutor, Executor {
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private var reservationTimestamp: Long = 0
    private val isReserved: AtomicBoolean = AtomicBoolean(false)
    private val readinessChannel: Channel<Boolean> = Channel(Channel.UNLIMITED)

    override var currentState: ExecutorState = ExecutorState.NEW

    override var stateTimestamp: Long = System.currentTimeMillis()

    override val id get() = dockerContainer.name
    override val readiness: ReceiveChannel<Boolean> get() = readinessChannel

    companion object {
        const val imageName = "eucalypt/executor"
    }

    suspend fun init() {
        scope.launch {
            while (true) {
                val newState = dockerContainer.stateChannel.receive()
                applyDockerState(newState)
            }
//            dockerContainer.stateChannel.consumeEach { applyDockerState(it) }
        }
    }

    override suspend fun execute(script: String): String {
        if (script.isBlank()) {
            throw IllegalArgumentException("Script is empty")
        }

        setState(ExecutorState.EXECUTING)

        val (cmd, arg) = buildExecCommand(script)
        return dockerContainer.exec(cmd, arg)
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

    override suspend fun release() {
        setState(ExecutorState.RELEASED)
        reservationTimestamp = 0
        isReserved.set(false)

        scope.launch { reset() }
    }

    protected abstract fun buildExecCommand(script: String): Pair<String, String>

    private fun setState(state: ExecutorState) {
        this.currentState = state
        this.stateTimestamp = System.currentTimeMillis()
    }

    private suspend fun applyDockerState(state: DockerContainerState) = coroutineScope {
        if (state == DockerContainerState.READY) {
            setState(ExecutorState.READY)
            readinessChannel.send(true)
        }
    }
}
