package eucalypt.executing.executors

interface Executor {
    suspend fun execute(script: String): String
}

