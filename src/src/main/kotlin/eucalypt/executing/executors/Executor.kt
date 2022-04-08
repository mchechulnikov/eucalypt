package eucalypt.executing.executors

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel

interface Executor {
    val typeName: String
    val executingBy: String
    suspend fun execute(script: String): Pair<Job, ReceiveChannel<String>>
}

