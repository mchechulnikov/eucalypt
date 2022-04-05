package eucalypt.executing.pool

import eucalypt.executing.Executor

interface ReservableExecutor : Executor {
    fun tryReserve(): Boolean
}

