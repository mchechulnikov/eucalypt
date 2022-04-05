package eucalypt.executing.executors

interface ServableExecutor  {
    suspend fun reset()
    suspend fun eliminate()
}