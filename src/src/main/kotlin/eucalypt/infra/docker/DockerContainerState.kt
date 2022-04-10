package eucalypt.infra.docker

enum class DockerContainerState {
    UNKNOWN,
    STOPPED,
    RUNNING,
    PAUSED,
    DELETED
}