package eucalypt.http

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.Logger
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class HTTPServerImplTest {
    @MockK
    lateinit var logger: Logger

    @BeforeEach
    fun beforeEach() = MockKAnnotations.init(this, relaxUnitFun = true)

    @Test
    fun `run - happy path + stop - before and after invokes`() = runBlocking {
        // arrange
        var count = 0
        val server = HTTPServerImpl(logger)

        // act
        CoroutineScope(Dispatchers.IO).launch {
            server.run(
                { count++ },
                { count++ }
            )
        }
        Runtime.getRuntime().addShutdownHook(Thread {
            // assert
            assertEquals(2, count)
        })
        CoroutineScope(Dispatchers.IO).launch { delay(500); stop() }
        delay(3000)
    }

    private suspend fun stop() = coroutineScope {
        try {
            HttpClient(CIO).use { client ->
                client.post("http://localhost;8080/shutdown")
            }
        } catch (_: Exception) {
        }
    }
}