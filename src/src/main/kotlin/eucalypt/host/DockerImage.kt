package eucalypt.host

data class DockerImage(val name: String, val tag: String) {
    init {
        require(name.isNotEmpty()) { "Docker image name must not be empty" }
        require(tag.isNotEmpty()) { "Docker image tag must not be empty" }
    }

    override fun toString(): String = "$name:$tag"
}