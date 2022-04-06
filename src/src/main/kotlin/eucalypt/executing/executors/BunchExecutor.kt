package eucalypt.executing.executors

class BunchExecutor(executors: List<Executor>, override val id: String,
                    override val type: ExecutorType,
                    override val currentState: ExecutorState,
                    override val stateTimestamp: Long
) : Poolable {
    override suspend fun reset() {
        TODO("Not yet implemented")
    }

    override suspend fun eliminate() {
        TODO("Not yet implemented")
    }
}


