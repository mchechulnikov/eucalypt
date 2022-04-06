package eucalypt.executing.executors

interface ExecutorsFactory {
    suspend fun create(type: ExecutorType): BaseExecutor
}