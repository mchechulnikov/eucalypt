package eucalypt.docker

data class DockerExecCommand (
    val command: List<String>,
    val workdir: String? = null,
    val user: String,
)