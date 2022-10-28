package uk.gov.dluhc.printapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile

/**
 * Configuration class exposing an sftp service.
 */
class SftpContainerConfiguration {

    companion object {
        private const val USER = "valtech"
        private const val PASSWORD = "Password"
        private const val PORT = 22
        private const val IMAGE_NAME = "atmoz/sftp:latest"
        private const val USER_ID = 1001
        private const val GROUP_ID = 100
        private val DIRECTORIES = listOf("EROP/Dev/InBound", "EROP/Dev/OutBound")
        private var container: GenericContainer<*>? = null

        fun getInstance(): GenericContainer<*> {
            if (container == null) {
                container = GenericContainer(
                        ImageFromDockerfile().withDockerfileFromBuilder {
                            it.from(IMAGE_NAME)
                                    .build()
                        },
                )
                        .withFileSystemBind("E:\\IdeaProjects\\eip-ero-print-api\\src\\test\\resources\\ssh\\id_rsa.pub", "/home/$USER/.ssh/keys/id_rsa.pub", BindMode.READ_ONLY)
                        .withExposedPorts(PORT)
                        .withCommand("$USER:$PASSWORD:$USER_ID:$GROUP_ID:${DIRECTORIES.joinToString(",")}")
            }
            return container!!
        }
    }

    @Bean
//    @Lazy
    fun sftpContainer(): GenericContainer<*> = getInstance()

}
