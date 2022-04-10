package eucalypt.infra.docker.commands

data class DockerEventsCommand(
    val containerNamePrefix: String,
    val eventTypes: List<String>,
    val format: String,
    val sinceMs: String,
)