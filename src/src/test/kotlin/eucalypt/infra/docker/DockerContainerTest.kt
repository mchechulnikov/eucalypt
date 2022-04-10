package eucalypt.infra.docker

import eucalypt.infra.docker.commands.DockerExecCommand
import eucalypt.infra.docker.commands.DockerRunCommand
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DockerContainerTest {
    @MockK
    lateinit var dockerOperator: DockerOperator

    @MockK
    lateinit var eventsFeed: DockerEventsFeed

    private val name = "test"
    private val runCmd = DockerRunCommand(
        containerName = name,
        command = "echo TEST",
        image = "test",
        memoryMB = 42,
        cpus = 4.2,
        isNetworkDisabled = true,
        tmpfsDir = "/tmp-dir",
        tmpfsSizeBytes = 42,
        user = "test"
    )

    @BeforeAll
    fun beforeAll() = MockKAnnotations.init(this, relaxUnitFun = true)

    @Test
    fun `run - invoked - subscribe by DockerEventFeed and runContainer by DockerOperator`() = runBlocking {
        // act
        val dockerContainer = DockerContainer.run(name, runCmd, dockerOperator, eventsFeed)

        // assert
        assertNotNull(dockerContainer)
        coVerify { eventsFeed.subscribe(name, any()) }
        coVerify { dockerOperator.runContainer(runCmd) }
    }

    @Test
    fun `getStateChannel - container ran - initialized`() = runBlocking {
        // assert
        val dockerContainer = DockerContainer.run(name, runCmd, dockerOperator, eventsFeed)

        // act
        val stateChannel = dockerContainer.stateChannel

        // assert
        assertNotNull(stateChannel)
    }

    @Test
    fun `getStateChannel - docker events raised - state changed`() = runBlocking {
        mapOf(
            "create" to DockerContainerState.STOPPED,
            "start" to DockerContainerState.RUNNING,
            "restart" to DockerContainerState.RUNNING,
            "unpause" to DockerContainerState.RUNNING,
            "pause" to DockerContainerState.PAUSED,
            "kill" to DockerContainerState.STOPPED,
            "die" to DockerContainerState.STOPPED,
            "oom" to DockerContainerState.STOPPED,
            "stop" to DockerContainerState.STOPPED,
            "destroy" to DockerContainerState.DELETED,
            "ANYTHING" to DockerContainerState.UNKNOWN,
        ).forEach { (event, state) ->
            // arrange
            every { eventsFeed.subscribe(name, captureLambda()) } answers {
                lambda<suspend (DockerEvent) -> Unit>().coInvoke(DockerEvent(name, event))
            }
            val dockerContainer = DockerContainer.run(name, runCmd, dockerOperator, eventsFeed)

            // act
            val stateChannel = dockerContainer.stateChannel

            // assert
            assertEquals(state, stateChannel.receive())
        }
    }

    @Test
    fun `exec - happy path - container exec() run`() = runBlocking {
        // arrange
        val execCmd = DockerExecCommand(
            command = listOf("echo", "TEST"),
            user = "test"
        )
        val mockJob = mockk<Job>()
        val mockChannel = mockk<ReceiveChannel<String>>()
        every { dockerOperator.exec(name, any()) } returns (mockJob to mockChannel)
        val dockerContainer = DockerContainer.run(name, runCmd, dockerOperator, eventsFeed)

        // act
        val (job, channel) = dockerContainer.exec(execCmd)

        // assert
        assertEquals(mockJob, job)
        assertEquals(mockChannel, channel)
        verify { dockerOperator.exec(name, execCmd) }
    }

    @Test
    fun `rerun - invoked - remove and run container by DockerOperator`() = runBlocking {
        // arrange
        val dockerContainer = DockerContainer.run(name, runCmd, dockerOperator, eventsFeed)

        // act
        dockerContainer.rerun()

        // assert
        coVerify { dockerOperator.removeContainer(name) }
        coVerify { dockerOperator.runContainer(runCmd) }
    }

    @Test
    fun `remove - invoked - unsubscribe by DockerEventFeed and remove by DockerOperator`() = runBlocking {
        // arrange
        val dockerContainer = DockerContainer.run(name, runCmd, dockerOperator, eventsFeed)

        // act
        dockerContainer.remove()

        // assert
        coVerify { eventsFeed.unsubscribe(name) }
        coVerify { dockerOperator.removeContainer(name) }
    }

    @Test
    fun `toString - invoked - container name`() = runBlocking {
        // arrange
        val dockerContainer = DockerContainer.run(name, runCmd, dockerOperator, eventsFeed)

        // act
        val result = dockerContainer.toString()

        // assert
        assertEquals(name, result)
    }
}