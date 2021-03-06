package eucalypt.infra.docker.commands

data class DockerRunCommand(
    val containerName: String,
    val command: String = "",
    val image: String,
    val memoryMB: Int = 100,
    val cpus: Double = 1.5,
    val isNetworkDisabled: Boolean = true,
    val tmpfsDir: String? = null,
    val tmpfsSizeBytes: Long = 10 * 1024 * 1024,
    val user: String,
)

