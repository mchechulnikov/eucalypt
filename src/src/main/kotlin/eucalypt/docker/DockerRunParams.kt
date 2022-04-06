package eucalypt.docker

data class DockerRunParams (
    val name: String,
    val image: String,
    val user: String?,
    val memory: Int?,
    val cpu: Double?,
    val disableNetwork: Boolean = true,
)

