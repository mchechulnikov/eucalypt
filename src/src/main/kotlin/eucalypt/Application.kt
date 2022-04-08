package eucalypt

import eucalypt.docker.DockerMonitorManager
import eucalypt.executing.pool.ExecutorsPoolManager
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import kotlinx.coroutines.*
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit

suspend fun main() = coroutineScope {
    // DI
    startKoin { modules(compositionRoot) }

    val dockerMonitorManager : DockerMonitorManager by inject(DockerMonitorManager::class.java)
    val pool : ExecutorsPoolManager by inject(ExecutorsPoolManager::class.java)

    // before
    launch { dockerMonitorManager.start() }
    launch { pool.start() }

    runKtorServer {
        // on shutdown
        dockerMonitorManager.stop()
        pool.stop()
    }
}

private fun runKtorServer(onShutdown: suspend () -> Unit) {
    // run server
    val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(DefaultHeaders)
        install(CallLogging) {
            level = Level.INFO
            filter { call -> call.request.path().startsWith("/") }
        }

        install(ContentNegotiation) { json() }

        // for graceful shutdown by HTTP request
        install(ShutDownUrl.ApplicationCallPlugin) {
            shutDownUrl = "/shutdown"
            exitCodeSupplier = {
                runBlocking { onShutdown() }
                0
            }
        }

        configureRouting()
    }

    server.start(wait = false)

    // for graceful shutdown by SIGTERM
    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking { onShutdown() }
        server.stop(1, 3, TimeUnit.SECONDS)
    })
    Thread.currentThread().join()
}