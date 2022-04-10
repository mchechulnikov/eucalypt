package eucalypt.business

import eucalypt.business.executing.ExecutorsManager
import eucalypt.business.executing.executors.ExecutorParameters
import eucalypt.business.executing.executors.ReservableExecutor
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.Logger
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ScriptRunnerImplTest {
    @MockK
    lateinit var settings: ScriptRunnerSettings

    @MockK
    lateinit var executorsManager: ExecutorsManager

    @MockK
    lateinit var logger: Logger

    private val executorParameters = ExecutorParameters(
        executorTypeName = "",
        memoryMB = 100,
        cpuCores = 1.0,
        isNetworkDisabled = true,
        spaceSizeMB = 100,
    )

    @BeforeAll
    fun beforeAll() = MockKAnnotations.init(this, relaxUnitFun = true)

    @Test
    fun `run - script is empty - failed result`() = runBlocking {
        // arrange
        val scriptRunner = ScriptRunnerImpl(settings, executorsManager, logger)

        // act
        val result = scriptRunner.run("", ScriptType.DOTNET)

        // assert
        assertTrue(result.isFailure)
    }

    @Test
    fun `run - executor can't be borrowed - failed result`() = runBlocking {
        // arrange
        coEvery { executorsManager.borrowExecutor(any()) } coAnswers { Result.failure(Error()) }
        val scriptRunner = ScriptRunnerImpl(settings, executorsManager, logger)

        // act
        val result = scriptRunner.run("test", ScriptType.DOTNET)

        // assert
        assertTrue(result.isFailure)
        verify { logger.error(any()) }
    }

    @Test
    fun `run - happy path - success result`() = runBlocking {
        // arrange
        val executionResult = UUID.randomUUID().toString()
        val channel = mockk<ReceiveChannel<String>> {
            coEvery { receive() } returns executionResult
            every { isEmpty } returns false andThen false andThen true
            coEvery { cancel() } returns Unit
        }
        every { settings.runningTimeoutMs } returns 1
        val executor = mockk<ReservableExecutor> {
            every { parameters } returns executorParameters
            coEvery { execute(any()) } returns (mockk<Job> { coEvery { join() } returns Unit } to channel)
        }
        coEvery { executorsManager.borrowExecutor(any()) } coAnswers { Result.success(executor) }
        val scriptRunner = ScriptRunnerImpl(settings, executorsManager, logger)

        // act
        val result = scriptRunner.run("test", ScriptType.DOTNET)

        // assert
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertTrue(result.getOrNull().toString().contains(executionResult))
        assertTrue(result.getOrNull().toString().contains("Script executed successfully"))
        coVerify { channel.cancel() }
        coVerify { executorsManager.redeemExecutor(executor) }
    }

    @Test
    fun `run - timeout - success result`() = runBlocking {
        // arrange
        every { settings.runningTimeoutMs } returns 10
        val executionResult = UUID.randomUUID().toString()
        val executorJob = mockk<Job> {
            coEvery { join() } coAnswers { delay(1000) }
        }
        val executorChannel = mockk<ReceiveChannel<String>> {
            coEvery { receive() } coAnswers { executionResult }
            every { isEmpty } returns false andThen false andThen true
            coEvery { cancel() } returns Unit
        }
        every { settings.runningTimeoutMs } returns 1
        val executor = mockk<ReservableExecutor> {
            every { parameters } returns executorParameters
            coEvery { execute(any()) } returns (executorJob to executorChannel)
        }
        coEvery { executorsManager.borrowExecutor(any()) } coAnswers { Result.success(executor) }
        val scriptRunner = ScriptRunnerImpl(settings, executorsManager, logger)

        // act
        val result = scriptRunner.run("test", ScriptType.DOTNET)

        // assert
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertTrue(result.getOrNull().toString().contains(executionResult))
        assertTrue(result.getOrNull().toString().contains("Script running timeout exceeded"))
        coVerify { executorChannel.cancel() }
        coVerify { executorsManager.redeemExecutor(executor) }
    }

    @Test
    fun `run - no output - success result`() = runBlocking {
        // arrange
        val executorJob = mockk<Job> { coEvery { join() } returns Unit }
        val executorChannel = mockk<ReceiveChannel<String>> {
            coEvery { receive() } coAnswers { "" }
            every { isEmpty } returns true
            coEvery { cancel() } returns Unit
        }
        every { settings.runningTimeoutMs } returns 1
        val executor = mockk<ReservableExecutor> {
            every { parameters } returns executorParameters
            coEvery { execute(any()) } returns (executorJob to executorChannel)
        }
        coEvery { executorsManager.borrowExecutor(any()) } coAnswers { Result.success(executor) }
        val scriptRunner = ScriptRunnerImpl(settings, executorsManager, logger)

        // act
        val result = scriptRunner.run("test", ScriptType.DOTNET)

        // assert
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertTrue(result.getOrNull().toString().contains("No output"))
        coVerify { executorChannel.cancel() }
        coVerify { executorsManager.redeemExecutor(executor) }
    }
}