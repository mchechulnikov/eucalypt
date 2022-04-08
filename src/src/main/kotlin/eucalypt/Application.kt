package eucalypt

import eucalypt.docker.DockerMonitorManager
import eucalypt.executing.pool.ExecutorsPoolManager
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.event.Level

suspend fun main() = coroutineScope {
    startKoin { modules(compositionRoot) }

    val dockerMonitorManager : DockerMonitorManager by inject(DockerMonitorManager::class.java)
    val pool : ExecutorsPoolManager by inject(ExecutorsPoolManager::class.java)

    launch { dockerMonitorManager.start() }
    launch { pool.start() }

    runKtorServer {
        // on shutdown
        runBlocking {
            dockerMonitorManager.stop()
            pool.stop()
        }
    }
}

private fun runKtorServer(after: () -> Unit) {
    // run server
    val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(DefaultHeaders)
        install(CallLogging) {
            level = Level.INFO
            filter { call -> call.request.path().startsWith("/") }
        }

        install(ContentNegotiation) { json() }

        // for graceful shutdown
        install(ShutDownUrl.ApplicationCallPlugin) {
            shutDownUrl = "/shutdown"
            exitCodeSupplier = { after(); 0 }
        }

        configureRouting()
    }

    server.start(wait = true)
}