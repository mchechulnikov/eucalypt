package eucalypt.executing.executors

import eucalypt.docker.DockerContainer
import eucalypt.docker.DockerEventsFeed
import eucalypt.utils.LoggerFactory
import java.util.*

internal class ExecutorsFactoryImpl(
    private val dockerEventsFeed: DockerEventsFeed,
    private val loggerFactory: LoggerFactory
) : ExecutorsFactory {
    override suspend fun create(namePrefix: String, type: ExecutorType): BaseExecutor {
        val containerName = getNewContainerName(namePrefix, type)

        val executor = when (type) {
            ExecutorType.DOTNET6 -> {
                val dockerContainer = DockerContainer.run(containerName, DotnetExecutor.containerSettings, dockerEventsFeed)
                val logger = loggerFactory.getLogger(Executor::class.java)
                DotnetExecutor(type, dockerContainer, logger)
            }
            else -> throw NotImplementedError("Executor $type is not implemented yet")
        }

        executor.init()

        return executor
    }

    private fun getNewContainerName(namePrefix: String, type: ExecutorType): String {
        val typeName = type.name.lowercase(Locale.getDefault())
        val id = UUID.randomUUID().toString().substring(0, 8)

        return "$namePrefix-$typeName-$id"
    }
}

