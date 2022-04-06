package eucalypt.docker

data class DockerExecParams (
    val container: String,
    val command: String,
    val user: String?,
)