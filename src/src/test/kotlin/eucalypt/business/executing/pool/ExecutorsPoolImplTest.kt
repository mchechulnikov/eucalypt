package eucalypt.business.executing.pool

import eucalypt.business.executing.executors.BaseExecutor
import eucalypt.business.executing.executors.ExecutorState
import eucalypt.business.executing.executors.ExecutorType
import eucalypt.business.executing.executors.ExecutorsFactory
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.slf4j.Logger

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class ExecutorsPoolImplTest {
    private val executorsType = ExecutorType.DOTNET6

    @MockK
    lateinit var settings: ExecutorsPoolSettings

    @MockK
    lateinit var executorsFactory: ExecutorsFactory

    @MockK
    lateinit var garbageCollector: ExecutorsPoolGarbageCollector

    @MockK
    lateinit var logger: Logger

    @BeforeEach
    fun beforeEach() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { settings.name } returns "test"
        every { settings.maxSize } returns 0
        every { settings.minReadyExecutorsCount } returns 1
        every { settings.types } returns listOf(executorsType)
        every { settings.isShrinkEnabled } returns false
        every { settings.shrinkIntervalMs } returns 1000
        every { settings.isDetectHangedEnabled } returns false
        every { settings.detectHangedIntervalMs } returns 1000
        every { settings.hangingTimeoutMs } returns 5000
    }

    @Test
    fun `getAvailableExecutor - pool not started, max size 1 - throws`(): Unit = runBlocking {
        // arrange
        val readinessChannel = mockk<ReceiveChannel<Boolean>> { coEvery { receive() } returns false }
        val executor = mockk<BaseExecutor> {
            every { id } returns "test-1"
            every { currentState } returns ExecutorState.NEW
            every { readiness } returns readinessChannel
        }
        every { settings.maxSize } returns 1
        coEvery { executorsFactory.create(any(), any()) } returns executor
        val pool = ExecutorsPoolImpl(settings, executorsFactory, garbageCollector, logger)

        // act, assert
        assertThrows<ExecutorsPoolException> {
            runBlocking { pool.getAvailableExecutor(executorsType) }
        }
    }

    @Test
    fun `getAvailableExecutor - pool started, no any ready, max size 0 - failed result`(): Unit = runBlocking {
        // arrange
        val readinessChannel = mockk<ReceiveChannel<Boolean>> { coEvery { receive() } returns false }
        val executor = mockk<BaseExecutor> {
            every { id } returns "test-1"
            every { type } returns executorsType
            every { currentState } returns ExecutorState.NEW
            every { readiness } returns readinessChannel
        }
        every { settings.maxSize } returns 0
        every { settings.minReadyExecutorsCount } returns 1
        coEvery { executorsFactory.create(any(), any()) } returns executor
        val pool = ExecutorsPoolImpl(settings, executorsFactory, garbageCollector, logger)
        pool.start()

        // act
        val result = pool.getAvailableExecutor(executorsType)

        // assert
        assertTrue(result.isFailure)
    }

    @Test
    fun `getAvailableExecutor - pool started, there is ready, max size 1 - success result`(): Unit = runBlocking {
        // arrange
        val readinessChannel = mockk<ReceiveChannel<Boolean>>()
        val executor = mockk<BaseExecutor> {
            every { id } returns "test-1"
            every { type } returns executorsType
            every { currentState } returns ExecutorState.READY
            every { readiness } returns readinessChannel
        }
        every { settings.maxSize } returns 0
        every { settings.minReadyExecutorsCount } returns 1
        coEvery { executorsFactory.create(any(), any()) } returns executor
        val pool = ExecutorsPoolImpl(settings, executorsFactory, garbageCollector, logger)
        pool.start()

        // act
        val result = pool.getAvailableExecutor(executorsType)

        // assert
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getAvailableExecutor - pool started, no any ready, max size 1 - extend, success result`(): Unit = runBlocking {
        // arrange
        val readinessChannel = mockk<ReceiveChannel<Boolean>>()
        val notReadyExecutor = mockk<BaseExecutor> {
            every { id } returns "test-1"
            every { type } returns executorsType
            every { currentState } returns ExecutorState.NEW
            every { readiness } returns readinessChannel
        }
        val readyExecutor = mockk<BaseExecutor> {
            every { id } returns "test-2"
            every { type } returns executorsType
            every { currentState } returns ExecutorState.READY
            every { readiness } returns readinessChannel
        }
        every { settings.maxSize } returns 2
        every { settings.minReadyExecutorsCount } returns 1
        coEvery { executorsFactory.create(any(), any()) } returns notReadyExecutor andThen readyExecutor
        val pool = ExecutorsPoolImpl(settings, executorsFactory, garbageCollector, logger)
        pool.start()

        // act
        val result = pool.getAvailableExecutor(executorsType)

        // assert
        assertTrue(result.isSuccess)
    }

    @Test
    fun `start - invoked - GC invoked`() = runBlocking {
        // arrange
        val readinessChannel = mockk<ReceiveChannel<Boolean>>()
        val readyExecutor = mockk<BaseExecutor> {
            every { id } returns "test-1"
            every { type } returns executorsType
            every { currentState } returns ExecutorState.READY
            every { readiness } returns readinessChannel
        }
        coEvery { executorsFactory.create(any(), any()) } returns readyExecutor
        val pool = ExecutorsPoolImpl(settings, executorsFactory, garbageCollector, logger)

        // act
        pool.start()

        // assert
        coVerify { garbageCollector.collect() }
    }

    @Test
    fun `start - invoked - extend`() = runBlocking {
        // arrange
        val readinessChannel = mockk<ReceiveChannel<Boolean>>()
        val readyExecutor = mockk<BaseExecutor> {
            every { id } returns "test-1"
            every { type } returns executorsType
            every { currentState } returns ExecutorState.READY
            every { readiness } returns readinessChannel
        }
        every { settings.maxSize } returns 2
        every { settings.minReadyExecutorsCount } returns 1
        coEvery { executorsFactory.create(any(), any()) } returns readyExecutor
        val pool = ExecutorsPoolImpl(settings, executorsFactory, garbageCollector, logger)

        // act
        pool.start()

        // assert
        coVerify { executorsFactory.create(any(), any()) }
    }

    @Test
    fun `start - invoked - shrink invoked`() = runBlocking {
        // arrange
        val shrinkRunInterval = 100L
        val readinessChannel = mockk<ReceiveChannel<Boolean>>() { coEvery { receive() } returns true }
        val readyExecutor1 = mockk<BaseExecutor> {
            every { id } returns "test-1"
            every { type } returns executorsType
            every { currentState } returns ExecutorState.READY
            every { readiness } returns readinessChannel
            every { stateTimestamp } returns 0L
        }
        val readyExecutor2 = mockk<BaseExecutor> {
            every { id } returns "test-2"
            every { type } returns executorsType
            every { currentState } returns ExecutorState.READY
            every { readiness } returns readinessChannel
            every { stateTimestamp } returns 0L
        }
        val readyExecutor3 = mockk<BaseExecutor> {
            every { id } returns "test-3"
            every { type } returns executorsType
            every { currentState } returns ExecutorState.READY
            every { readiness } returns readinessChannel
            every { stateTimestamp } returns 0L
        }
        every { settings.maxSize } returns 2
        every { settings.minReadyExecutorsCount } returns 1
        every { settings.isShrinkEnabled } returns true
        every { settings.shrinkIntervalMs } returns shrinkRunInterval
        coEvery {
            executorsFactory.create(any(), any())
        } returns readyExecutor1 andThen readyExecutor2 andThen readyExecutor3
        val pool = ExecutorsPoolImpl(settings, executorsFactory, garbageCollector, logger)

        // act
        // get pressure on pool to make it extend
        every { readyExecutor1.currentState } returns ExecutorState.RESERVED
        pool.getAvailableExecutor(executorsType)
        every { readyExecutor2.currentState } returns ExecutorState.RESERVED
        pool.getAvailableExecutor(executorsType)
        // make redundant ready executors; the first should be shrunk
        every { readyExecutor1.currentState } returns ExecutorState.READY
        every { readyExecutor2.currentState } returns ExecutorState.READY
        pool.start()
        delay(shrinkRunInterval * 2)

        // assert
        coVerify { readyExecutor1.eliminate() }
    }

    @Test
    fun `start - invoked - hanged detected`() = runBlocking {
        // arrange
        val detectHangedRunInterval = 100L
        val readinessChannel = mockk<ReceiveChannel<Boolean>>() { coEvery { receive() } returns true }
        val executor = mockk<BaseExecutor> {
            every { id } returns "test-1"
            every { type } returns executorsType
            every { currentState } returns ExecutorState.READY
            every { readiness } returns readinessChannel
            every { stateTimestamp } returns 0L
        }
        every { settings.maxSize } returns 2
        every { settings.minReadyExecutorsCount } returns 1
        every { settings.isDetectHangedEnabled } returns true
        every { settings.detectHangedIntervalMs } returns detectHangedRunInterval
        coEvery { executorsFactory.create(any(), any()) } returns executor
        val pool = ExecutorsPoolImpl(settings, executorsFactory, garbageCollector, logger)

        // act
        pool.start()
        every { executor.currentState } returns ExecutorState.EXECUTING
        every { executor.stateTimestamp } returns 0L
        delay(detectHangedRunInterval * 2)

        // assert
        coVerify { executor.reset() }
    }

    @Test
    fun `stop - pool is not running - doesn't throw`() {
        // arrange
        val pool = ExecutorsPoolImpl(settings, executorsFactory, garbageCollector, logger)

        // act, assert
        assertDoesNotThrow { runBlocking { pool.stop() } }
    }

    @Test
    fun `stop - pool is running - executors eliminated `() = runBlocking {
        // arrange
        val readinessChannel = mockk<ReceiveChannel<Boolean>>() { coEvery { receive() } returns true }
        val executor = mockk<BaseExecutor> {
            every { id } returns "test-1"
            every { type } returns executorsType
            every { currentState } returns ExecutorState.READY
            every { readiness } returns readinessChannel
            every { stateTimestamp } returns 0L
            coEvery { eliminate() } just Runs
        }
        every { settings.maxSize } returns 1
        every { settings.minReadyExecutorsCount } returns 1
        coEvery { executorsFactory.create(any(), any()) } returns executor
        val pool = ExecutorsPoolImpl(settings, executorsFactory, garbageCollector, logger)

        // act
        pool.start()
        pool.stop()

        // assert
        coVerify { executor.eliminate() }
    }
}