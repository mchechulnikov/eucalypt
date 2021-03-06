package eucalypt.business.executing.executors

import eucalypt.infra.docker.DockerContainerImpl
import eucalypt.infra.docker.DockerEventsFeed
import eucalypt.infra.docker.DockerOperator
import org.slf4j.Logger
import java.util.*

internal class ExecutorsFactoryImpl(
    private val dockerEventsFeed: DockerEventsFeed,
    private val dockerOperator: DockerOperator,
    private val executorsLogger: Logger,
) : ExecutorsFactory {
    override suspend fun create(namePrefix: String, type: ExecutorType): BaseExecutor {
        if (namePrefix.isEmpty()) {
            throw IllegalArgumentException("namePrefix must not be empty")
        }

        val containerName = getNewContainerName(namePrefix, type)

        val executor = when (type) {
            ExecutorType.DOTNET6 -> {
                val dockerContainer = DockerContainerImpl.run(
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

