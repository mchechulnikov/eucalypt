package eucalypt.executing.executors

import eucalypt.executing.ExecutorType
import eucalypt.docker.DockerContainer
import eucalypt.docker.DockerEventsFeed
import eucalypt.docker.DockerImage
import eucalypt.Host
import java.util.*

class ExecutorsFactory(
    private val config: ExecutorsFactoryConfig,
    private val dockerEventsFeed: DockerEventsFeed
) {
    suspend fun create(type: ExecutorType): BaseExecutor {
        return when (type) {
            ExecutorType.DOTNET -> {
                val container = getDockerContainer(type)
                DotnetExecutor(type, container)
            }
            ExecutorType.GO ->
                throw NotImplementedError("Golang executor is not implemented yet")
            ExecutorType.JAVA ->
                throw NotImplementedError("Java executor is not implemented yet")
        }
    }

    private suspend fun getDockerContainer(type: ExecutorType): DockerContainer =
        DockerContainer.run(getNewContainerName(type), getImage(type), dockerEventsFeed)


    private fun getNewContainerName(type: ExecutorType): String {
        val prefix = config.containerNamePrefix
        val factoryID = config.factoryID
        val typeName = type.name.lowercase(Locale.getDefault())
        val id = UUID.randomUUID().toString()

        return "$prefix-$factoryID-$typeName-$id"
    }

    private fun getImage(type: ExecutorType): DockerImage =
        Host.images[type.name.lowercase(Locale.getDefault())]!!
}