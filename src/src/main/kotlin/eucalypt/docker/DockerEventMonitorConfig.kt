package eucalypt.docker

data class DockerEventMonitorConfig(
    var containersPrefix: String = "eucalypt-executor-"
)