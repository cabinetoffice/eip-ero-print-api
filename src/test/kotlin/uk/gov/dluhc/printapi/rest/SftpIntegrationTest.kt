package uk.gov.dluhc.printapi.rest

import mu.KotlinLogging
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration

private val logger = KotlinLogging.logger {}

internal class SftpIntegrationTest {

    @Test
    fun `should return health check status UP given microservice is running healthily`() {
        // Given
        val sftpInstance = SftpContainerConfiguration.getInstance()
        sftpInstance.start();
        val mappedPort = sftpInstance.getMappedPort(22)

        logger.info { "mapped port = $mappedPort" }
    }
}
