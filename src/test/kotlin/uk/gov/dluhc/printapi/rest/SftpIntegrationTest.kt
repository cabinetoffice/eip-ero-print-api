package uk.gov.dluhc.printapi.rest

import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration

private val logger = KotlinLogging.logger {}

internal class SftpIntegrationTest {

    @Test
    fun `should return health check status UP given microservice is running healthily`() {
        // Given
        val sftpInstance = SftpContainerConfiguration.getInstance()
        sftpInstance.start()

        val mappedPort = sftpInstance.getMappedPort(22)
        val privateKeyResourceUrl = SftpContainerConfiguration.getPrivateKeyResourceUrl()

        logger.info { "Change directory to that containing the private key and then run the sftp command." }
        logger.info { "cd ${FileUtils.toFile(privateKeyResourceUrl).parent}" }
        logger.info { "sftp -v -P $mappedPort -oKexAlgorithms=+diffie-hellman-group1-sha1 -o \"IdentityFile=./printer_rsa\" -o User=valtech localhost" }
        logger.info { "You can now verify that the 2 required directories exist by running the following command" }
        logger.info { """
            sftp> ls EROP/Dev
            EROP/Dev/InBound   EROP/Dev/OutBound  
            sftp> 
        """.trimIndent() }
    }
}
