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
    startKoin { modules(compositionRoot) }

    val dockerMonitorManager : DockerMonitorManager by inject(DockerMonitorManager::class.java)
    val pool : ExecutorsPoolManager by inject(ExecutorsPoolManager::class.java)

    launch { dockerMonitorManager.start() }
    launch { pool.start() }

    runKtorServer {
        // on shutdown
        CoroutineScope(coroutineContext).launch {
            dockerMonitorManager.stop()
            pool.stop()
        }
    }
}

private fun runKtorServer(after: () -> Unit) {
    // run server
    val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(DefaultHeaders) {
            header("X-Engine", "Ktor") // will send this header with each response
        }
        install(CallLogging) {
            level = Level.INFO
            filter { call -> call.request.path().startsWith("/") }
        }
        install(ContentNegotiation) { json() }
        configureRouting()
    }

    server.start(wait = true)

    // graceful shutdown
    Runtime.getRuntime().addShutdownHook(Thread {
        after()
        server.stop(1, 5, TimeUnit.SECONDS)
    })
    Thread.currentThread().join()
}