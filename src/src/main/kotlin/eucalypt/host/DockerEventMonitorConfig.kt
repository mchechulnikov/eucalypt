package eucalypt.host

data class DockerEventMonitorConfig(
    var containersPrefix: String = "eucalypt-executor-"
)