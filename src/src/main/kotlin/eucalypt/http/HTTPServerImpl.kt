package eucalypt.http

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit

class HTTPServerImpl(private val logger: Logger) : HTTPServer {
    override suspend fun run(onStart: suspend () -> Unit, onShutdown: suspend () -> Unit) {
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
                    logger.info("Shutting down HTTP server gracefully by URL")
                    runBlocking { onShutdown() }
                    0
                }
            }

            configureRouting()
        }

        logger.info("Starting HTTP server...")
        runBlocking {
            logger.info("Pre-launch operations")
            onStart()
        }
        server.start(wait = false)

        // for graceful shutdown by SIGTERM
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Shutting down HTTP server gracefully by signal")
            runBlocking { onShutdown() }
            server.stop(1, 2, TimeUnit.SECONDS)
        })

        withContext(Dispatchers.IO) {
            Thread.currentThread().join()
        }
    }
}