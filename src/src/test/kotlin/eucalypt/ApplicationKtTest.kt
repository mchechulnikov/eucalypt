package eucalypt

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class ApplicationKtTest {
    private val host = "http://localhost:8080"
    private val client = HttpClient(CIO)

    @Test
    fun `main - POST dotnet hello world - result string`() = runBlocking {
        // arrange
        CoroutineScope(Dispatchers.IO).launch { main(); } // run server
        delay(3000)

        // act
        val response = client.post("$host/dotnet") { setBody("using System; Console.WriteLine(\"Hello World!\");") }
        val result = response.body<String>()

        // assertw
        assertTrue(result.contains("Script executed successfully"))
        assertTrue(result.contains("Hello World!"))
    }
}

