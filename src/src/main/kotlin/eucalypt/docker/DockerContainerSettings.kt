package eucalypt.docker

data class DockerContainerSettings (
    val image: String,
    val memoryMB: Int = 100,
    val cpus: Double = 1.5,
    val isNetworkDisabled: Boolean = true,
    val user: String = "root",
)