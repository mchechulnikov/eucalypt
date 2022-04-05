package eucalypt.executing.executors

import eucalypt.executing.ExecutorState
import eucalypt.executing.ExecutorType
import eucalypt.executing.pool.ReservableExecutor

interface PoolableExecutor : ReservableExecutor, ServableExecutor {
    val type: ExecutorType
    val currentState: ExecutorState
    val stateTimestamp: Long
    fun release()
}
