package eucalypt.business.executing.executors

interface ReservableExecutor : Executor {
    suspend fun tryReserve(): Boolean
    suspend fun release()
}

