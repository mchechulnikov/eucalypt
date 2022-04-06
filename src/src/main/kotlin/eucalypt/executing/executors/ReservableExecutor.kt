package eucalypt.executing.executors

interface ReservableExecutor : Executor {
    fun tryReserve(): Boolean
    fun release()
}

