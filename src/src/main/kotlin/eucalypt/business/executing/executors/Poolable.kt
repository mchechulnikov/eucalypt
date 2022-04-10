package eucalypt.business.executing.executors

import kotlinx.coroutines.channels.ReceiveChannel

interface Poolable {
    val id: String
    val type: ExecutorType
    val currentState: ExecutorState
    val stateTimestamp: Long
    val readiness: ReceiveChannel<Boolean>
    suspend fun reset()
    suspend fun eliminate()
}
