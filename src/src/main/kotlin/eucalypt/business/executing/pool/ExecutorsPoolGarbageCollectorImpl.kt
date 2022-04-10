package eucalypt.business.executing.pool

import eucalypt.infra.docker.DockerOperator
import eucalypt.infra.docker.DockerOperatorImpl
import org.slf4j.Logger

class ExecutorsPoolGarbageCollectorImpl(
    private val settings: ExecutorsPoolSettings,
    private val dockerOperator: DockerOperator,
    private val logger: Logger,
) : ExecutorsPoolGarbageCollector {
    override suspend fun collect() {
        val deadContainersNames = dockerOperator.getContainerNames(settings.name)
        if (deadContainersNames.isEmpty()) {
            logger.info("No dead containers found. Nothing to clean")
            return
        }

        logger.info("Found dead containers: $deadContainersNames")
        dockerOperator.removeContainers(deadContainersNames)
        logger.info("Removed ${deadContainersNames.size} dead containers from previous runs")
    }
}