package eucalypt.business.executing

import eucalypt.business.executing.executors.Executor
import eucalypt.business.executing.executors.ExecutorType
import eucalypt.business.executing.executors.ReservableExecutor
import eucalypt.business.executing.pool.ExecutorsPool
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.slf4j.Logger
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class ExecutorsManagerImplTest {
    @MockK
    lateinit var settings: ExecutorsManagerSettings

    @MockK
    lateinit var pool: ExecutorsPool

    @MockK
    lateinit var logger: Logger

    @BeforeEach
    fun beforeEach() = MockKAnnotations.init(this, relaxUnitFun = true)

    @Test
    fun `borrowExecutor - no available executors - failed result`() = runBlocking{
        // arrange
        val attemptsCount = 2
        val executorType = ExecutorType.DOTNET6
        every { settings.borrowAttemptsCount } returns attemptsCount
        every { settings.borrowAttemptDelayMs } returns 0
        coEvery { pool.getAvailableExecutor(executorType) } returns Result.failure(Error())
        val executorsManager = ExecutorsManagerImpl(settings, pool, logger)

        // act
        val result = executorsManager.borrowExecutor(executorType)

        // assert
        assertTrue(result.isFailure)
        coVerify { pool.getAvailableExecutor(executorType) }
        verify(exactly = attemptsCount + 1) { logger.info(any()) }
    }

    @Test
    fun `borrowExecutor - happy path - success result`() = runBlocking{
        // arrange
        val executor = mockk<ReservableExecutor> {
            coEvery { tryReserve() } returns false andThen true
        }
        val executorType = ExecutorType.DOTNET6
        every { settings.borrowAttemptsCount } returns 2
        every { settings.borrowAttemptDelayMs } returns 0
        coEvery { pool.getAvailableExecutor(executorType) } returns Result.success(executor)
        val executorsManager = ExecutorsManagerImpl(settings, pool, logger)

        // act
        val result = executorsManager.borrowExecutor(executorType)

        // assert
        assertTrue(result.isSuccess)
        coVerify { pool.getAvailableExecutor(executorType) }
        coVerify { executor.tryReserve() }
        verify(exactly = 1) { logger.info(any()) }
    }

    @Test
    fun `redeemExecutor - not reservable executor - throws`(): Unit = runBlocking {
        // arrange
        val executor = mockk<Executor>()
        val executorManager = ExecutorsManagerImpl(settings, pool, logger)

        // act, assert
        assertThrows<IllegalArgumentException> { runBlocking { executorManager.redeemExecutor(executor) } }
    }

    @Test
    fun `redeemExecutor - reservable executor - doesn't throws, release invokes`(): Unit = runBlocking {
        // arrange
        val executor = mockk<ReservableExecutor> { coEvery { release() } returns Unit }
        val executorManager = ExecutorsManagerImpl(settings, pool, logger)

        // act, assert
        assertDoesNotThrow { runBlocking { executorManager.redeemExecutor(executor) } }
        coVerify { executor.release() }
    }
}