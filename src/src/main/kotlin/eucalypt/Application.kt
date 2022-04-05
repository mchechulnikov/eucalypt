package eucalypt

import eucalypt.host.Host
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import eucalypt.plugins.*

suspend fun main() {
    Host.init()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        configureHTTP()
        configureMonitoring()
        configureSerialization()
    }.start(wait = true)
}
