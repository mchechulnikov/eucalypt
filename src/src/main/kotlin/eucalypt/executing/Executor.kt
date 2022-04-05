package eucalypt.executing

interface Executor {
    val id: String
    suspend fun execute(script: String): String
}

