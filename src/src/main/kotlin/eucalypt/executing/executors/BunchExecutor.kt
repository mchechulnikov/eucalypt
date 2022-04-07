package eucalypt.executing.executors

import kotlinx.coroutines.channels.ReceiveChannel

class BunchExecutor(executors: List<Executor>, override val id: String,
                    override val type: ExecutorType,
                    override val currentState: ExecutorState,
                    override val stateTimestamp: Long, override val readiness: ReceiveChannel<Boolean>
) : Poolable {
    override suspend fun reset() {
        TODO("Not yet implemented")
    }

    override suspend fun eliminate() {
        TODO("Not yet implemented")
    }
}


