package eucalypt.business.executing.executors

import eucalypt.infra.docker.DockerContainer
import eucalypt.infra.docker.DockerContainerState
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.slf4j.Logger
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class DotnetExecutorTest {
    private val executorType = ExecutorType.DOTNET6

    @MockK
    lateinit var dockerContainer: DockerContainer

    @MockK
    lateinit var logger: Logger

    @BeforeEach
    fun beforeEach() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { dockerContainer.name } returns "test"
    }

    @Test
    fun `init - container stopped, executor in NEW state - state not changed`() : Unit = runBlocking {
        // arrange
        val dockerStateChannel = Channel<DockerContainerState>(1)
        every { dockerContainer.stateChannel } returns dockerStateChannel
        val executor = DotnetExecutor(executorType, dockerContainer, logger)

        // act
        executor.init()
        dockerStateChannel.send(DockerContainerState.STOPPED)
        delay(50)

        // assert
        assertEquals(executor.currentState, ExecutorState.NEW)
    }

    @Test
    fun `init - container stopped, executor in READY state - state changed`() : Unit = runBlocking {
        // arrange
        val dockerStateChannel = Channel<DockerContainerState>(1)
        every { dockerContainer.stateChannel } returns dockerStateChannel
        val executor = DotnetExecutor(executorType, dockerContainer, logger)
        executor.currentState = ExecutorState.READY

        // act
        executor.init()
        dockerStateChannel.send(DockerContainerState.STOPPED)
        delay(50)

        // assert
        assertEquals(executor.currentState, ExecutorState.RESET)
        assertFalse(executor.readiness.receive())
    }

    @Test
    fun `init - container running - state changed`() : Unit = runBlocking {
        // arrange
        val dockerStateChannel = Channel<DockerContainerState>(1)
        every { dockerContainer.stateChannel } returns dockerStateChannel
        val executor = DotnetExecutor(executorType, dockerContainer, logger)

        // act
        executor.init()
        dockerStateChannel.send(DockerContainerState.RUNNING)
        delay(50)

        // assert
        assertEquals(executor.currentState, ExecutorState.READY)
        assertTrue(executor.readiness.receive())
    }

    @Test
    fun `init - container paused - state changed`() : Unit = runBlocking {
        // arrange
        val dockerStateChannel = Channel<DockerContainerState>(1)
        every { dockerContainer.stateChannel } returns dockerStateChannel
        val executor = DotnetExecutor(executorType, dockerContainer, logger)

        // act
        executor.init()
        dockerStateChannel.send(DockerContainerState.PAUSED)
        delay(50)

        // assert
        assertEquals(executor.currentState, ExecutorState.RESET)
        assertFalse(executor.readiness.receive())
    }

    @Test
    fun `init - container deleted, state reset - state doesn't change`() : Unit = runBlocking {
        // arrange
        val dockerStateChannel = Channel<DockerContainerState>(1)
        every { dockerContainer.stateChannel } returns dockerStateChannel
        val executor = DotnetExecutor(executorType, dockerContainer, logger)
        executor.currentState = ExecutorState.RESET

        // act
        executor.init()
        dockerStateChannel.send(DockerContainerState.DELETED)
        delay(50)

        // assert
        assertEquals(executor.currentState, ExecutorState.RESET)
    }

    @Test
    fun `init - container deleted, state not reset - state changed`() : Unit = runBlocking {
        // arrange
        val dockerStateChannel = Channel<DockerContainerState>(1)
        every { dockerContainer.stateChannel } returns dockerStateChannel
        val executor = DotnetExecutor(executorType, dockerContainer, logger)

        // act
        executor.init()
        dockerStateChannel.send(DockerContainerState.DELETED)
        delay(50)

        // assert
        assertEquals(executor.currentState, ExecutorState.ELIMINATED)
        assertFalse(executor.readiness.receive())
    }

    @Test
    fun `init - container in unknown state - state changed`() : Unit = runBlocking {
        // arrange
        val dockerStateChannel = Channel<DockerContainerState>(1)
        every { dockerContainer.stateChannel } returns dockerStateChannel
        val executor = DotnetExecutor(executorType, dockerContainer, logger)

        // act
        executor.init()
        dockerStateChannel.send(DockerContainerState.UNKNOWN)
        delay(50)

        // assert
        assertEquals(executor.currentState, ExecutorState.RESET)
        assertFalse(executor.readiness.receive())
    }

    @Test
    fun `execute - empty script - throws`(): Unit = runBlocking {
        // arrange
        val executor = DotnetExecutor(executorType, dockerContainer, logger)

        // act, assert
        assertThrows<IllegalArgumentException> { runBlocking { executor.execute("") } }
    }

    @Test
    fun `execute - happy path - expected result`(): Unit = runBlocking {
        // arrange
        val jobMock = mockk<Job>()
        val channelMock = mockk<ReceiveChannel<String>>()
        every { dockerContainer.exec(any()) } returns (jobMock to channelMock)
        val executor = DotnetExecutor(executorType, dockerContainer, logger)

        // act
        val (job, channel) = executor.execute("test")

        // assert
        assertEquals(jobMock, job)
        assertEquals(channelMock, channel)
    }

    @Test
    fun `execute - happy path - state updated`(): Unit = runBlocking {
        // arrange
        every { dockerContainer.exec(any()) } returns mockk()
        val executor = DotnetExecutor(executorType, dockerContainer, logger)

        // act
        executor.execute("test")

        // assert
        assertFalse(executor.readiness.receive())
        assertEquals(ExecutorState.EXECUTING, executor.currentState)
    }

    @Test
    fun `execute - happy path - cmd built properly`(): Unit = runBlocking {
        // arrange
        val script = UUID.randomUUID().toString()
        every { dockerContainer.exec(any()) } returns mockk()
        val executor = DotnetExecutor(executorType, dockerContainer, logger)

        // act
        executor.execute(script)

        // assert
        verify {
            dockerContainer.exec(match {
                it.command == listOf("/restore-dir/exec.sh", script)
                && it.user == "executor"
            })
        }
    }

    @Test
    fun `tryReserve - happy path - true, state changed`(): Unit = runBlocking {
        // arrange
        val executor = DotnetExecutor(executorType, dockerContainer, logger)

        // act
        val result = executor.tryReserve()

        // assert
        assertTrue(result)
        assertFalse(executor.readiness.receive())
        assertEquals(ExecutorState.RESERVED, executor.currentState)
    }

    @Test
    fun `tryReserve - has been already reserved - false`(): Unit = runBlocking {
        // arrange
        val executor = DotnetExecutor(executorType, dockerContainer, logger)
        executor.tryReserve()

        // act
        val result = executor.tryReserve()

        // assert
        assertFalse(result)
    }

    @Test
    fun `release - happy path - status changed`() : Unit = runBlocking {
        // arrange
        val executor = DotnetExecutor(executorType, dockerContainer, logger)
        executor.tryReserve()

        // act
        executor.release()

        // assert
        assertFalse(executor.readiness.receive())
        assertEquals(ExecutorState.RESET, executor.currentState)
    }

    @Test
    fun `release - happy path - docker container rerun`() : Unit = runBlocking {
        // arrange
        val executor = DotnetExecutor(executorType, dockerContainer, logger)
        executor.tryReserve()

        // act
        executor.release()

        // assert
        coVerify { dockerContainer.rerun() }
    }

    @Test
    fun `reset - happy path - status changed`() : Unit = runBlocking {
        // arrange
        val executor = DotnetExecutor(executorType, dockerContainer, logger)

        // act
        executor.reset()

        // assert
        assertFalse(executor.readiness.receive())
        assertEquals(ExecutorState.RESET, executor.currentState)
    }

    @Test
    fun `reset - happy path - docker container rerun`() : Unit = runBlocking {
        // arrange
        val executor = DotnetExecutor(executorType, dockerContainer, logger)

        // act
        executor.reset()

        // assert
        coVerify { dockerContainer.rerun() }
    }

    @Test
    fun `eliminate -  happy path - status changed`() : Unit = runBlocking {
        // arrange
        val executor = DotnetExecutor(executorType, dockerContainer, logger)

        // act
        executor.eliminate()

        // assert
        assertFalse(executor.readiness.receive())
        assertEquals(ExecutorState.ELIMINATED, executor.currentState)
    }

    @Test
    fun `eliminate - happy path - docker container removed`() : Unit = runBlocking {
        // arrange
        val executor = DotnetExecutor(executorType, dockerContainer, logger)

        // act
        executor.eliminate()

        // assert
        coVerify { dockerContainer.remove() }
    }
}