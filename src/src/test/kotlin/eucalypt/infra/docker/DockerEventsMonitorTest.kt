package eucalypt.infra.docker

import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.slf4j.Logger

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class DockerEventsMonitorTest {
    @MockK
    lateinit var settings: DockerEventMonitorSettings

    @MockK
    lateinit var dockerOperator: DockerOperator

    @MockK
    lateinit var logger: Logger

    @BeforeEach
    fun beforeEach() = MockKAnnotations.init(this, relaxUnitFun = true)

    @Test
    fun `subscribe - without start - doesn't throw`() {
        // arrange
        val monitor = DockerEventsMonitor(settings, dockerOperator, logger)

        // act, assert
        assertDoesNotThrow { monitor.subscribe("test", mockk()) }
    }

    @Test
    fun `subscribe - after start and event raised - callback invoked`() = runBlocking {
        // arrange
        val containerPrefix = "test"

        val monitorJob = mockk<Job>()
        val monitorChannel = Channel<String>(Channel.UNLIMITED)
        every { settings.containersPrefix } returns containerPrefix
        every {
            dockerOperator.monitorEvents(match { it.containerNamePrefix == containerPrefix })
        } returns (monitorJob to monitorChannel)
        val callback = mockk<suspend (DockerEvent) -> Unit>()
        val monitor = DockerEventsMonitor(settings, dockerOperator, logger)
        monitor.start()

        // act, assert
        assertDoesNotThrow { monitor.subscribe(containerPrefix, callback) }
        monitorChannel.send("$containerPrefix,status")
        coVerify { callback.invoke(match { it.container == containerPrefix && it.status == "status" }) }
    }

    @Test
    fun `subscribe - invoked twice for same container - IllegalArgumentException`() {
        // arrange
        val container = "test"
        val monitor = DockerEventsMonitor(settings, dockerOperator, logger)

        // act, assert
        assertDoesNotThrow { monitor.subscribe(container, mockk()) }
        assertThrows<IllegalArgumentException> { monitor.subscribe(container, mockk()) }
    }

    @Test
    fun `unsubscribe - after subscribe - doesn't throw`() {
        // arrange
        val container = "test"
        val monitor = DockerEventsMonitor(settings, dockerOperator, logger)
        monitor.subscribe(container, mockk())

        // act, assert
        assertDoesNotThrow { monitor.unsubscribe("test") }
    }

    @Test
    fun `unsubscribe - invoked twice for same container - IllegalArgumentException`() {
        // arrange
        val container = "test"
        val monitor = DockerEventsMonitor(settings, dockerOperator, logger)

        // act, assert
        assertThrows<IllegalArgumentException> { monitor.unsubscribe(container) }
    }

    @Test
    fun `start - invoked once - monitorEvents by DockerOperator`(): Unit = runBlocking{
        // arrange
        val containerPrefix = "test"
        every { settings.containersPrefix } returns containerPrefix
        every { dockerOperator.monitorEvents(any()) } answers { mockk<Job>() to mockk() }
        val monitor = DockerEventsMonitor(settings, dockerOperator, logger)

        // act
        monitor.start()

        // assert
        verify {
            dockerOperator.monitorEvents(match {
                it.containerNamePrefix == containerPrefix
                && it.eventTypes == listOf(
                    "create", "start", "restart",
                    "pause", "unpause", "kill",
                    "die", "oom", "stop", "destroy",
                )
                && it.format == "{{.Actor.Attributes.name}},{{.Status}}"
                && it.sinceMs != "0"
            })
        }
    }

    @Test
    fun `start - invoked twice - throws`(): Unit = runBlocking {
        // arrange
        val containerPrefix = "test"
        every { settings.containersPrefix } returns containerPrefix
        every { dockerOperator.monitorEvents(any()) } answers { mockk<Job>() to mockk() }
        val monitor = DockerEventsMonitor(settings, dockerOperator, logger)

        // act, assert
        monitor.start()
        assertThrows<DockerMonitorException> { runBlocking { monitor.start() } }
    }

    @Test
    fun `stop - invoked after start - doesn't throw, cancel job, cancel channel`(): Unit = runBlocking {
        // arrange
        val containerPrefix = "test"
        every { settings.containersPrefix } returns containerPrefix
        val mockJob = mockk<Job>()
        every { mockJob.cancel(any()) } returns Unit
        val channel = Channel<String>(Channel.UNLIMITED)
        every { dockerOperator.monitorEvents(any()) } answers { mockJob to channel }
        val monitor = DockerEventsMonitor(settings, dockerOperator, logger)
        monitor.start()

        // act, assert
        assertDoesNotThrow { monitor.stop() }
        verify { mockJob.cancel(any()) }
    }
}