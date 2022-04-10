package eucalypt.docker

import eucalypt.infra.docker.DockerOperator
import eucalypt.infra.docker.commands.DockerEventsCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.koin.java.KoinJavaComponent.inject
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DockerOperatorImplTest {
    private val dockerOperator : DockerOperator by inject(DockerOperator::class.java)

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
    fun `runContainer - happy path - container created`() = runBlocking {
        // arrange
        val containerName = generateContainerName()

        // act
        dockerOperator.runContainer(containerName, imageName)

        // assert
        assertContainerExists(containerName)
    }

    @Test
    fun `removeContainer - happy path - container doesn't exist`() = runBlocking {
        // arrange
        val containerName = runContainer()

        // act
        dockerOperator.removeContainer(containerName)

        // assert
        assertContainerDoesNotExists(containerName)
    }

//    @Test
//    fun `exec - happy path - expected result`() = runBlocking {
//        // arrange
//        val containerName = runContainer()
//
//        // act
//        val result = Docker.exec(containerName, "ls", "")
//
//        // assert
//        assertEquals("/\n", result)
//    }

    @Test
    fun `monitorEvents - pause & unpause container - events received`() = runBlocking {
        // arrange
        val containerName = runContainer()

        // act
        val (job, channel) = dockerOperator.monitorEvents(DockerEventsCommand(
            containerNamePrefix = containerName,
            eventTypes = listOf("pause", "unpause"),
            format = "{{.Actor.Attributes.name}},{{.Status}}",
            sinceMs = System.currentTimeMillis().toString(),
        ))
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

    private fun runCmd(cmd: String, args: String): String = runBlocking(Dispatchers.IO) {
        val argsElements = args.split(" ").toTypedArray()

        val process = ProcessBuilder(cmd, *argsElements).start()
        val stdout = process.inputStream.bufferedReader().readText()

        process.waitFor()

        stdout
    }
}