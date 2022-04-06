package eucalypt

import eucalypt.running.ScriptRunner
import eucalypt.running.ScriptType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.java.KoinJavaComponent.inject

fun Application.configureRouting() {

    val scriptRunner: ScriptRunner by inject(ScriptRunner::class.java)

    routing {
        get("/") {
            call.respondText("Code executing server")
        }

        post("/dotnet") {
            val script = call.receive<String>()

            scriptRunner
                .run(script, ScriptType.DOTNET)
                .onSuccess {
                    call.respondText(script, status = HttpStatusCode.OK)
                }
                .onFailure {
                    call.respondText(
                        text = it.message ?: "Unknown error",
                        status = HttpStatusCode.InternalServerError
                    )
                }
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
}