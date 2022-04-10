package eucalypt.http

interface HTTPServer {
    suspend fun run(onStart: suspend () -> Unit, onShutdown: suspend () -> Unit)
}

