package eucalypt.infra.docker

import eucalypt.business.executing.executors.BaseExecutor
import eucalypt.infra.docker.commands.DockerEventsCommand
import eucalypt.infra.docker.commands.DockerExecCommand
import eucalypt.infra.docker.commands.DockerRunCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.koin.core.context.startKoin
import org.koin.dsl.module
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
    private val containersNames = mutableListOf<String>()
    private val fullImageName = "$imageName:$imageTag"
    private val fullLatestImageName = "$imageName:latest"
    private val fullTmpImageName = "$imageName:$imageTag"

    @BeforeAll
    fun beforeAll() = runBlocking {
        startKoin{ modules(module { single<DockerOperator> { DockerOperatorImpl() } }) }
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
    fun `getContainerNames - several containers - return only with name prefix`() = runBlocking {
        // arrange
        val containerNamePrefixA = "test-A"
        val containerNamePrefixB = "test-B"
        runContainer("$containerNamePrefixA-1")
        runContainer("$containerNamePrefixA-2")
        runContainer("$containerNamePrefixA-3")
        runContainer("$containerNamePrefixB-1")
        runContainer("$containerNamePrefixB-2")

        // act
        val namesA = dockerOperator.getContainerNames(containerNamePrefixA)
        val namesB = dockerOperator.getContainerNames(containerNamePrefixB)

        // assert
        assertEquals(3, namesA.size)
        namesA.forEach { name ->
            assertEquals(containerNamePrefixA, name.substring(0, containerNamePrefixA.length))
        }
        assertEquals(2, namesB.size)
        namesB.forEach { name ->
            assertEquals(containerNamePrefixB, name.substring(0, containerNamePrefixB.length))
        }
    }

    @Test
    fun `runContainer - happy path - container created`() = runBlocking {
        // arrange
        val cmd = DockerRunCommand(
            containerName = generateContainerName(),
            image = DockerImage(BaseExecutor.imageName, "dotnet6").toString(),
            memoryMB = 20,
            cpus = 1.0,
            isNetworkDisabled = true,
            tmpfsDir = "/tmp-dir",
            tmpfsSizeBytes = 4 * 1024 * 1024,
            user = "root",
        )

        // act
        dockerOperator.runContainer(cmd)

        // assert
        assertContainerExists(cmd.containerName)
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

    @Test
    fun `removeContainers - happy path - containers don't exist`() = runBlocking {
        // arrange
        val container1 = runContainer()
        val container2 = runContainer()

        // act
        dockerOperator.removeContainers(listOf(container1, container2))

        // assert
        assertContainerDoesNotExists(container1)
        assertContainerDoesNotExists(container2)
    }

    @Test
    fun `exec - happy path pwd - root dir`() = runBlocking {
        // arrange
        val containerName = runContainer()
        val cmd = DockerExecCommand(
            command = listOf("pwd"),
            user = "root",
        )

        // act
        val (job, channel) = dockerOperator.exec(containerName, cmd)
        var log = ""
        val logJob = launch {
            repeat(2) {
                val event = channel.receive()
                log += event
            }
        }
        job.join()

        // assert
        assertEquals("/", log)

        // restore
        logJob.cancel()
        job.cancel()
    }

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

    private fun runContainer(containerName: String): String {
        runCmd("docker", "run -d -it --name $containerName $imageName")
        return containerName
    }

    private fun pullImage(image: String) {
        runCmd("docker", "pull $image")
    }

    private fun removeImage(image: String) {
        runCmd("docker", "rmi -f $image")
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