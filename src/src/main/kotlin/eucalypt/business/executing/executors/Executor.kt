package eucalypt.business.executing.executors

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel

interface Executor {
    val parameters: ExecutorParameters
    suspend fun execute(script: String): Pair<Job, ReceiveChannel<String>>
}

