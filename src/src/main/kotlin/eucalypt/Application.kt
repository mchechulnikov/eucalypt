package eucalypt

import eucalypt.business.executing.pool.ExecutorsPoolManager
import eucalypt.http.HTTPServer
import eucalypt.infra.docker.DockerMonitorManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject

suspend fun main() = coroutineScope {
    // DI
    startKoin { modules(compositionRoot) }

    val dockerMonitorManager : DockerMonitorManager by inject(DockerMonitorManager::class.java)
    val pool : ExecutorsPoolManager by inject(ExecutorsPoolManager::class.java)
    val server : HTTPServer by inject(HTTPServer::class.java)

    server.run(
        {
            // before
            launch { dockerMonitorManager.start() }
            launch { pool.start() }
        },
        {
            // on shutdown
            dockerMonitorManager.stop()
            pool.stop()
        }
    )
}