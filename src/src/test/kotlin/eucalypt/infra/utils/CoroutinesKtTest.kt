package eucalypt.infra.utils

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test

internal class CoroutinesKtTest {
    @Test
    fun `every - while coroutine is active - block invokes`() = runBlocking {
        // arrange
        var count = 0
        val callback = mockk<suspend () -> Unit>()
        coEvery { callback() } coAnswers { count++ }

        // act
        val job = CoroutineScope(Dispatchers.Unconfined).launch {
            every(10, callback)
        }
        delay(100)
        val exactly = count
        job.cancel()
        job.join()
        delay(200)

        // assert
        coVerify(exactly = exactly) { callback() }
    }
}