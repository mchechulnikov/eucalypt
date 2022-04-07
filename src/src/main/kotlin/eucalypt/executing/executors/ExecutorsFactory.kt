package eucalypt.executing.executors

interface ExecutorsFactory {
    suspend fun create(namePrefix: String, type: ExecutorType): BaseExecutor
}