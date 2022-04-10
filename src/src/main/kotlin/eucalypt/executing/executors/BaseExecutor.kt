package eucalypt.executing.executors

import eucalypt.docker.DockerContainer
import eucalypt.docker.DockerContainerState
import eucalypt.docker.commands.DockerExecCommand
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseExecutor protected constructor(
    override val type: ExecutorType,
    private val dockerContainer: DockerContainer,
    private val logger: Logger,
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
        }
    }

    override suspend fun execute(script: String): Pair<Job, ReceiveChannel<String>> {
        if (script.isBlank()) {
            throw IllegalArgumentException("Script is empty")
        }

        setState(ExecutorState.EXECUTING)

        val cmd = buildExecCommand(script)
        return dockerContainer.exec(cmd)
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
        dockerContainer.rerun()
    }

    override suspend fun eliminate() {
        setState(ExecutorState.ELIMINATED)
        dockerContainer.remove()
    }

    override suspend fun release() {
        setState(ExecutorState.RELEASED)
        unreserve()

        scope.launch { reset() }
    }

    protected abstract fun buildExecCommand(script: String): DockerExecCommand

    private fun setState(state: ExecutorState) {
        val oldState = currentState
        this.currentState = state
        this.stateTimestamp = System.currentTimeMillis()

        logger.info("Executor '$id' state changed $oldState -> $state")
    }

    private suspend fun applyDockerState(state: DockerContainerState) = coroutineScope {
        if (state != DockerContainerState.READY) {
            return@coroutineScope
        }

        if (isReserved.get()) {
            unreserve()
        }

        setState(ExecutorState.READY)
        readinessChannel.send(true)
    }

    private fun unreserve() {
        reservationTimestamp = 0
        isReserved.set(false)
    }
}
