package eucalypt.executing.pool

import eucalypt.docker.Docker
import org.slf4j.Logger

class ExecutorsPoolGarbageCollectorImpl(
    private val settings: ExecutorsPoolSettings,
    private val logger: Logger,
) : ExecutorsPoolGarbageCollector {
    override suspend fun collect() {
        val deadContainersNames = Docker.getContainerNames(settings.name)
        if (deadContainersNames.isEmpty()) {
            logger.info("No dead containers found. Nothing to clean")
            return
        }

        logger.info("Found dead containers: $deadContainersNames")
        Docker.removeContainers(deadContainersNames)
        logger.info("Removed ${deadContainersNames.size} dead containers from previous runs")
    }
}