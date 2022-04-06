package eucalypt.executing.executors

import eucalypt.docker.DockerContainer
import eucalypt.docker.DockerEventsFeed
import java.util.*

internal class ExecutorsFactoryImpl(
    private val settings: ExecutorsFactorySettings,
    private val dockerEventsFeed: DockerEventsFeed
) : ExecutorsFactory {
    override suspend fun create(type: ExecutorType): BaseExecutor {
        return when (type) {
            ExecutorType.DOTNET6 -> {
                DotnetExecutor(
                    type,
                    DockerContainer.run(
                        name = getNewContainerName(type),
                        settings = DotnetExecutor.containerSettings,
                        eventsFeed = dockerEventsFeed
                    )
                )
            }
            else -> throw NotImplementedError("Executor $type is not implemented yet")
        }
    }

    private fun getNewContainerName(type: ExecutorType): String {
        val prefix = settings.containersPrefix
        val factoryID = settings.factoryID
        val typeName = type.name.lowercase(Locale.getDefault())
        val id = UUID.randomUUID().toString().substring(0, 8)

        return "$prefix-$factoryID-$typeName-$id"
    }
}

