package eucalypt.business.executing.executors

import eucalypt.infra.docker.DockerEventsFeed
import eucalypt.infra.docker.DockerOperator
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockkConstructor
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.slf4j.Logger
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class ExecutorsFactoryImplTest {

    @MockK
    lateinit var eventsFeed: DockerEventsFeed

    @MockK
    lateinit var dockerOperator: DockerOperator

    @MockK
    lateinit var logger: Logger

    @BeforeEach
    fun beforeEach() = MockKAnnotations.init(this, relaxUnitFun = true)

    @Test
    fun `create - empty namePrefix - throws`() {
        // arrange
        val factory = ExecutorsFactoryImpl(eventsFeed, dockerOperator, logger)

        // act, assert
        assertThrows<IllegalArgumentException> {
            runBlocking { factory.create("", ExecutorType.DOTNET6) }
        }
    }

    @Test
    fun `create - not supported executor type - throws`(): Unit = runBlocking {
        // arrange
        val factory = ExecutorsFactoryImpl(eventsFeed, dockerOperator, logger)

        // act, assert
        assertThrows<NotImplementedError> {
            runBlocking { factory.create("test", ExecutorType.JAVA) }
        }
    }

    @Test
    fun `create - DOTNET6 executor - created executor is DotnetExecutor and init`(): Unit = runBlocking {
        // arrange
        mockkConstructor(DotnetExecutor::class)
        val factory = ExecutorsFactoryImpl(eventsFeed, dockerOperator, logger)

        // act
        val executor = factory.create("test", ExecutorType.DOTNET6)

        // assert
        assertTrue(executor is DotnetExecutor)
        coVerify(exactly = 1) { executor.init() }
    }
}