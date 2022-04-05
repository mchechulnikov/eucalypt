package eucalypt.docker

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class DockerImageTest {
    @Test
    fun `ctor - empty name - exception`() {
        assertFailsWith<IllegalArgumentException>(
            message = "Docker image name must not be empty",
            block = {
                DockerImage("", "tag")
            }
        )
    }

    @Test
    fun `ctor - empty tag - exception`() {
        assertFailsWith<IllegalArgumentException>(
            message = "Docker image tag must not be empty",
            block = {
                DockerImage("name", "")
            }
        )
    }

    @Test
    fun `ctor - happy path - no exception`() {
        assertDoesNotThrow { DockerImage("name", "tag") }
    }

    @Test
    fun `toString - happy path - image created`() {
        // arrange
        val image = DockerImage("name", "tag")

        // act
        val result = image.toString()

        // assert
        assertEquals ("name:tag", result)
    }
}