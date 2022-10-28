package uk.gov.dluhc.printapi.config

import org.springframework.context.annotation.Bean
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import java.net.URL


/**
 * Configuration class exposing an sftp service.
 */
class SftpContainerConfiguration {

    companion object {
        private const val USER = "valtech"
        private const val PASSWORD = "Password"
        private const val PUBLIC_KEY_FILENAME = "ssh/id_rsa.pub"
        private const val PRIVATE_KEY_FILENAME = "ssh/printer_rsa"
        private const val PORT = 22
        private const val IMAGE_NAME = "atmoz/sftp:debian"
        private const val USER_ID = 1001
        private const val GROUP_ID = 100
        private val DIRECTORIES = listOf("EROP/Dev/InBound", "EROP/Dev/OutBound")
        private var container: GenericContainer<*>? = null

        private fun getPublicKeyResourceUrl(): URL? = ClassLoader.getSystemResource(PUBLIC_KEY_FILENAME)

        fun getPrivateKeyResourceUrl(): URL? = ClassLoader.getSystemResource(PRIVATE_KEY_FILENAME)

        fun getInstance(): GenericContainer<*> {
            val publicKeyResourceUrl: URL = getPublicKeyResourceUrl()!!

            if (container == null) {
                container = GenericContainer(
                        ImageFromDockerfile().withDockerfileFromBuilder {
                            it.from(IMAGE_NAME)
                                    .build()
                        },
                )
                        .withFileSystemBind(publicKeyResourceUrl.file, "/home/$USER/.ssh/keys/id_rsa.pub", BindMode.READ_ONLY)
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
