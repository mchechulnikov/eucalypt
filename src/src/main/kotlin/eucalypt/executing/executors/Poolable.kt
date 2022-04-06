package eucalypt.executing.executors

interface Poolable {
    val id: String
    val type: ExecutorType
    val currentState: ExecutorState
    val stateTimestamp: Long
    suspend fun reset()
    suspend fun eliminate()
}
