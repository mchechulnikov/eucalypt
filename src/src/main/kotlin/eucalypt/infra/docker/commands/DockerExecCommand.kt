package eucalypt.infra.docker.commands

data class DockerExecCommand (
    val command: List<String>,
    val workdir: String? = null,
    val user: String,
)