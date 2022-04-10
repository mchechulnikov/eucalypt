package eucalypt.docker

data class DockerEventsCommand(
    val containerNamePrefix: String,
    val eventTypes: List<String>,
    val format: String,
    val sinceMs: String,
)