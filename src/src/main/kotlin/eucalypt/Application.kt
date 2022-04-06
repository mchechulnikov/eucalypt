package eucalypt

import eucalypt.docker.DockerMonitorManager
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.event.Level

suspend fun main(): Unit = runBlocking {
    startKoin { modules(compositionRoot) }

    val dockerMonitorJob = launch { runDockerMonitor() }

    runKtorServer()

    dockerMonitorJob.cancel()
}

private fun CoroutineScope.runKtorServer() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(DefaultHeaders) {
            header("X-Engine", "Ktor") // will send this header with each response
        }
        install(CallLogging) {
            level = Level.INFO
            filter { call -> call.request.path().startsWith("/") }
        }
        install(ContentNegotiation) { json() }
        configureRouting()
    }.start(wait = true)
}

private suspend fun runDockerMonitor() {
    val dockerMonitorManager : DockerMonitorManager by inject(DockerMonitorManager::class.java)
    dockerMonitorManager.start()
}
