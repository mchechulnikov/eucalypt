package eucalypt.business.executing.executors

import eucalypt.infra.docker.DockerContainer
import eucalypt.infra.docker.DockerEventsFeed
import eucalypt.infra.docker.DockerOperator
import eucalypt.infra.utils.LoggerFactory
import java.util.*

internal class ExecutorsFactoryImpl(
    private val dockerEventsFeed: DockerEventsFeed,
    private val dockerOperator: DockerOperator,
    loggerFactory: LoggerFactory
) : ExecutorsFactory {
    private val executorsLogger = loggerFactory.getLogger(Executor::class.java)

    override suspend fun create(namePrefix: String, type: ExecutorType): BaseExecutor {
        val containerName = getNewContainerName(namePrefix, type)

        val executor = when (type) {
            ExecutorType.DOTNET6 -> {
                val dockerContainer = DockerContainer.run(
                    containerName,
                    DotnetExecutor.buildRunCommand(containerName),
                    dockerOperator,
                    dockerEventsFeed
                )
                DotnetExecutor(type, dockerContainer, executorsLogger)
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

