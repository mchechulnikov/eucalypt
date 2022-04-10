package eucalypt.business.executing.pool

import eucalypt.infra.docker.DockerOperator
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.Logger

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class ExecutorsPoolGarbageCollectorImplTest {
    @MockK
    lateinit var settings: ExecutorsPoolSettings

    @MockK
    lateinit var dockerOperator: DockerOperator

    @MockK
    lateinit var logger: Logger

    @BeforeEach
    fun beforeEach() = MockKAnnotations.init(this, relaxUnitFun = true)

    @Test
    fun `collect - no garbage - logged, nothing removed`() = runBlocking {
        // arrange
        every { settings.name } returns "test"
        coEvery { dockerOperator.getContainerNames(any()) } answers { listOf() }
        val collector = ExecutorsPoolGarbageCollectorImpl(settings, dockerOperator, logger)

        // act
        collector.collect()

        // assert
        verify { logger.info(any()) }
        coVerify(exactly = 0) { dockerOperator.removeContainers(any()) }
    }

    @Test
    fun `collect - there is some garbage - logged, all removed`() = runBlocking {
        // arrange
        val garbage = listOf("test1", "test2")
        every { settings.name } returns "test"
        coEvery { dockerOperator.getContainerNames(any()) } answers { garbage }
        val collector = ExecutorsPoolGarbageCollectorImpl(settings, dockerOperator, logger)

        // act
        collector.collect()

        // assert
        verify(exactly = 2) { logger.info(any()) }
        coVerify(exactly = 1) { dockerOperator.removeContainers(garbage) }
    }
}