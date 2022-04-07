package eucalypt.docker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DockerTest {
    private val imageName = "busybox"
    private val imageTag = "1.35.0"
    private val tmpTag = "tmp"
    private val containersNames = mutableListOf<String>()
    private val fullImageName = "$imageName:$imageTag"
    private val fullLatestImageName = "$imageName:latest"
    private val fullTmpImageName = "$imageName:$imageTag"

    @BeforeAll
    fun beforeAll() = runBlocking {
        removeImage(fullTmpImageName)
        removeImage(fullLatestImageName)
        pullImage(fullImageName)
    }

    @AfterAll
    fun afterAll() = runBlocking {
        removeImage(fullImageName)
        removeImage(fullTmpImageName)
        removeImage(fullLatestImageName)
    }

    @AfterTest
    fun tearDown() = runBlocking {
        removeAllGeneratedContainers()
    }

    @Test
    fun `isImageExists - image exists - true`() = runBlocking {
        // arrange
        pullImage(fullImageName)

        // act, assert
        assertTrue { Docker.isImageExists(fullImageName) }
    }

    @Test
    fun `isImageExists - image doesn't exist - false`() = runBlocking {
        // arrange
        assertTrue { Docker.isImageExists(fullImageName) }
        changeImageTag(imageName, imageTag, tmpTag)
        removeImage(fullImageName)

        // act, assert
        assertFalse { Docker.isImageExists(fullImageName) }

        // restore
        changeImageTag(imageName, tmpTag, imageTag)
        removeImage(fullTmpImageName)
        removeImage(fullLatestImageName)
    }

    @Test
    fun `pullImage - image doesn't exists - image pulled`() = runBlocking {
        // arrange
        changeImageTag(imageName, imageTag, tmpTag)
        removeImage(fullImageName)

        // act
        Docker.pullImage(fullImageName)

        // assert
        assertNotEquals("", runCmd("docker", "images -q $fullImageName"))

        // restore
        changeImageTag(imageName, tmpTag, imageTag)
        removeImage(fullTmpImageName)
        removeImage(fullLatestImageName)
    }

    @Test
    fun `runContainer - happy path - container created`() = runBlocking {
        // arrange
        val containerName = generateContainerName()

        // act
        Docker.runContainer(containerName, imageName)

        // assert
        assertContainerExists(containerName)
    }

    @Test
    fun `restartContainer - happy path - container restarted`() = runBlocking {
        // arrange
        val containerName = runContainer()
        val startTime = getContainerStartDateTime(containerName)

        // act
        Docker.restartContainer(containerName)

        // assert
        assertContainerExists(containerName)
        assertNotEquals(startTime, getContainerStartDateTime(containerName))
    }

    @Test
    fun `removeContainer - happy path - container doesn't exist`() = runBlocking {
        // arrange
        val containerName = runContainer()

        // act
        Docker.removeContainer(containerName)

        // assert
        assertContainerDoesNotExists(containerName)
    }

    @Test
    fun `exec - happy path - expected result`() = runBlocking {
        // arrange
        val containerName = runContainer()

        // act
        val result = Docker.exec(containerName, "ls", "")

        // assert
        assertEquals("/\n", result)
    }

    @Test
    fun `monitorEvents - pause & unpause container - events received`() = runBlocking {
        // arrange
        val containerName = runContainer()
        val channel = Channel<String>(Channel.UNLIMITED)

        // act
        val job = Docker.monitorEvents(containerName, channel)
        var log = ""
        val logJob = launch {
            repeat(2) {
                val event = channel.receive()
                log += event
            }
        }
        pauseContainer(containerName)
        unpauseContainer(containerName)

        // assert
        assertEquals("$containerName,pause$containerName,unpause", log)

        // restore
        logJob.cancel()
        job.cancel()
    }

    private fun assertContainerExists(container: String) {
        val actual = runCmd("docker", "ps -a -q -f name=$container").trim()
        assertNotEquals("", actual)
    }

    private fun assertContainerDoesNotExists(container: String) {
        val actual = runCmd("docker", "ps -a -q -f name=$container").trim()
        assertEquals("", actual)
    }

    private fun runContainer(): String {
        val containerName = generateContainerName()
        runCmd("docker", "run -d -it --name $containerName $imageName")
        return containerName
    }

    private fun pullImage(image: String) {
        runCmd("docker", "pull $image")
    }

    private fun removeImage(image: String) {
        runCmd("docker", "rmi -f $image")
    }

    private fun changeImageTag(image: String, oldTag: String, newTag: String) {
        runCmd("docker", "tag $image:$oldTag $image:$newTag")
    }

    private fun removeAllGeneratedContainers() {
        val containersNames = this.containersNames.fold ("") { acc, name -> "$acc $name" }
        runCmd("docker", "rm -f $containersNames")
    }

    private fun generateContainerName(): String {
        val name = "test-${UUID.randomUUID().toString().substring(0, 16)}"
        containersNames.add(name)

        return name
    }

    private fun pauseContainer(container: String) {
        runCmd("docker", "pause $container")
    }

    private fun unpauseContainer(container: String) {
        runCmd("docker", "unpause $container")
    }

    private fun getContainerStartDateTime(containerName: String): ZonedDateTime {
        val dateTimeString = runCmd("docker", "inspect $containerName --format {{.State.StartedAt}}").trim()
        return ZonedDateTime.parse(dateTimeString)
    }

    private fun runCmd(cmd: String, args: String): String = runBlocking(Dispatchers.IO) {
        val argsElements = args.split(" ").toTypedArray()

        val process = ProcessBuilder(cmd, *argsElements).start()
        val stdout = process.inputStream.bufferedReader().readText()

        process.waitFor()

        stdout
    }
}