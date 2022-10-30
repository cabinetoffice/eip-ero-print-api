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
        const val USERNAME = "valtech"
        const val PASSWORD = "Password"
        const val PUBLIC_KEY_FILENAME = "ssh/printer_rsa.pub"
        const val PRIVATE_KEY_FILENAME = "ssh/printer_rsa"
        const val PORT = 22
        const val IMAGE_NAME = "atmoz/sftp:debian"
        const val USER_ID = 1001
        const val GROUP_ID = 100
        val DIRECTORIES = listOf("EROP/Dev/InBound", "EROP/Dev/OutBound")
        var container: GenericContainer<*>? = null

        fun getConnectionUrl() = "sftp://$USERNAME@localhost:${getMappedPort()}/${DIRECTORIES[0]}"

        fun getPublicKeyResourceUrl(): URL = ClassLoader.getSystemResource(PUBLIC_KEY_FILENAME)

        fun getPrivateKeyResourceUrl(): URL = ClassLoader.getSystemResource(PRIVATE_KEY_FILENAME)

        fun getInstance(): GenericContainer<*> {
            synchronized(this) {
                val publicKeyResourceUrl: URL = getPublicKeyResourceUrl()

                if (container == null) {
                    container = GenericContainer(
                        ImageFromDockerfile().withDockerfileFromBuilder {
                            it.from(IMAGE_NAME)
                                .build()
                        },
                    )
                        .withFileSystemBind(publicKeyResourceUrl.file, "/home/$USERNAME/.ssh/keys/id_rsa.pub", BindMode.READ_ONLY)
                        .withExposedPorts(PORT)
                        .withCommand("$USERNAME:$PASSWORD:$USER_ID:$GROUP_ID:${DIRECTORIES.joinToString(",")}")
                    container!!.start()
                }
                return container!!
            }
        }

        fun getMappedPort() = container!!.getMappedPort(22)
    }

    @Bean
//    @Lazy
    fun sftpContainer(): GenericContainer<*> = getInstance()

}
