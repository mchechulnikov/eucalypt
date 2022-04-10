package eucalypt.docker

enum class DockerContainerState {
    UNKNOWN,
    STOPPED,
    RUNNING,
    PAUSED,
    DELETED
}