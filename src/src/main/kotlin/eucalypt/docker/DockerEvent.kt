package eucalypt.docker

data class DockerEvent(
    val container: String,
    val status: String,
)


