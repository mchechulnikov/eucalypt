package eucalypt.executing

interface ExecutorsManager {
    suspend fun borrowExecutor(type: ExecutorType): Result<Executor>
    fun redeemExecutor(executorID: String)
}
