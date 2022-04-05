package eucalypt

import io.ktor.http.*
import kotlinx.serialization.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.event.Level

suspend fun main() {
    //Host.init()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(DefaultHeaders) {
            header("X-Engine", "Ktor") // will send this header with each response
        }
        install(CallLogging) {
            level = Level.INFO
            filter { call -> call.request.path().startsWith("/") }
        }
        install(ContentNegotiation) { json() }

        routing {
            get("/") {
                call.respondText("Code executing server")
            }

            post("/dotnet") {
                val script = call.receive<String>()
                call.respondText(script, status = HttpStatusCode.OK)
            }

            post("/java") {
                call.respondText(
                    "Java code isn't supported yet",
                    status = HttpStatusCode.NotAcceptable
                )
            }

            post("/go") {
                call.respondText(
                    "Go code isn't supported yet",
                    status = HttpStatusCode.NotAcceptable
                )
            }
        }
    }.start(wait = true)
}

@Serializable
data class Customer(val id: String, val firstName: String, val lastName: String, val email: String)