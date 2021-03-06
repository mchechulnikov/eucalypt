package eucalypt.http

import eucalypt.business.ScriptRunner
import eucalypt.business.ScriptType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.java.KoinJavaComponent.inject

fun Application.configureRouting() {

    val scriptRunner: ScriptRunner by inject(ScriptRunner::class.java)

    routing {
        post("/dotnet") {
            val script = call.receive<String>()

            scriptRunner.run(script, ScriptType.DOTNET)
                .onSuccess {
                    call.respondText(it, status = HttpStatusCode.OK)
                }
                .onFailure {
                    call.respondText(
                        text = it.message ?: "Unknown error",
                        status = HttpStatusCode.InternalServerError
                    )
                }
        }
    }
}