package eucalypt.business.executing.executors

import eucalypt.infra.docker.DockerContainer
import eucalypt.infra.docker.DockerContainerState
import eucalypt.infra.docker.commands.DockerExecCommand
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
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
    private val readinessChannel: Channel<Boolean> = Channel(1, BufferOverflow.DROP_OLDEST)

    override var currentState: ExecutorState = ExecutorState.NEW
    override var stateTimestamp: Long = System.currentTimeMillis()
    override val id get() = dockerContainer.name
    override val readiness: ReceiveChannel<Boolean> get() = readinessChannel

    companion object {
        const val imageName = "eucalypt/executor"
    }

    suspend fun init() {
        scope.launch {
            while (isActive) applyDockerState(dockerContainer.stateChannel.receive())
        }
    }

    override suspend fun execute(script: String): Pair<Job, ReceiveChannel<String>> {
        if (script.isBlank()) {
            throw IllegalArgumentException("Script is empty")
        }

        logger.info("Executor '$id' started executing script")

        setState(ExecutorState.EXECUTING)
        readinessChannel.send(false)

        val cmd = buildExecCommand(script)
        return dockerContainer.exec(cmd)
    }

    override suspend fun tryReserve(): Boolean {
        logger.info("Executor '$id' is trying to reserve")
        if (!isReserved.compareAndSet(false, true)) {
            logger.info("Executor '$id' has already been reserved")
            return false
        }

        reservationTimestamp = System.currentTimeMillis()
        setState(ExecutorState.RESERVED)
        readinessChannel.send(false)

        return true
    }

    override suspend fun release() {
        setState(ExecutorState.RELEASED)
        readinessChannel.send(false)

        unreserve()

        scope.launch { reset() }

        logger.info("Executor '$id' released")
    }

    override suspend fun reset() {
        setState(ExecutorState.RESET)
        readinessChannel.send(false)
        dockerContainer.rerun()
    }

    override suspend fun eliminate() {
        setState(ExecutorState.ELIMINATED)
        readinessChannel.send(false)
        dockerContainer.remove()
    }

    protected abstract fun buildExecCommand(script: String): DockerExecCommand

    private fun setState(state: ExecutorState) {
        val oldState = currentState
        if (oldState == state) {
            return
        }

        this.currentState = state
        this.stateTimestamp = System.currentTimeMillis()

        logger.info("Executor '$id' state changed $oldState -> $state")
    }

    private suspend fun applyDockerState(state: DockerContainerState) = coroutineScope {
        when (state) {
            DockerContainerState.STOPPED -> {
                if (currentState != ExecutorState.NEW) {
                    setState(ExecutorState.RESET)
                    readinessChannel.send(false)
                }
            }
            DockerContainerState.RUNNING -> {
                setState(ExecutorState.READY)
                readinessChannel.send(true)
            }
            DockerContainerState.PAUSED -> {
                setState(ExecutorState.RESET)
                readinessChannel.send(false)
            }
            DockerContainerState.DELETED -> {
                if (currentState != ExecutorState.RESET) {
                    setState(ExecutorState.ELIMINATED)
                    readinessChannel.send(false)
                }
            }
            else -> {
                setState(ExecutorState.RESET)
                readinessChannel.send(false)
            }
        }
    }

    private fun unreserve() {
        reservationTimestamp = 0
        isReserved.set(false)
    }
}
