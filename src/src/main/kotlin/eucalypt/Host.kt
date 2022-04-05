package eucalypt

import eucalypt.docker.Docker
import eucalypt.docker.DockerImage

object Host {
    val images = mapOf(
        "dotnet" to DockerImage("mcr.microsoft.com/dotnet/sdk", "6.0"),
    )

    suspend fun init() {
        images.values.forEach {
            val imageFullName = it.toString()
            if (!Docker.isImageExists(imageFullName)) {
                Docker.pullImage(imageFullName)
            }
        }
    }
}