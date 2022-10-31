package uk.gov.dluhc.printapi.rest

import com.jcraft.jsch.ChannelSftp.LsEntry
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration
import java.net.URL
import java.util.Vector


private val logger = KotlinLogging.logger {}

private const val SESSION_TIMEOUT = 10000

private const val CHANNEL_TIMEOUT = 5000

internal class SftpIntegrationTest {

    @Test
    fun `should list files on remote sftp server`() {
        // Given
        val privateKeyResourceUrl: URL = SftpContainerConfiguration.getPrivateKeyResourceUrl()
        val sftpInstance = SftpContainerConfiguration.getInstance()
        val mappedPort: Int = sftpInstance.getMappedPort(22)

        outputConnectionString(mappedPort, privateKeyResourceUrl)

        val sshClient = SshClient(
            username = SftpContainerConfiguration.USERNAME,
            privateKeyResourceUrl = privateKeyResourceUrl,
            host = SftpContainerConfiguration.HOSTNAME,
            port = mappedPort,
        )
        val cmdLs = SshClient.createLsCommand("/EROP/Dev")
        val response: Vector<LsEntry> = sshClient.createSessionAndExecute(cmdLs)

        logger.info { "ls = $response" }
        assertThat(response).hasSize(4)
        assertThat(response).anyMatch { it.filename == "InBound" }
        assertThat(response).anyMatch { it.filename == "OutBound" }
    }

    private fun outputConnectionString(
        mappedPort: Int,
        privateKeyResourceUrl: URL,
    ) {
        val cmd = "sftp -v -P $mappedPort " +
                "-oStrictHostKeyChecking=no " +
                "-oKexAlgorithms=+diffie-hellman-group1-sha1 " +
                "-o 'IdentityFile=/home/valtech/IdeaProjects/eip-ero-print-api/build/resources/test/ssh/printer_rsa' " +
                "-o User=valtech " +
                "localhost"

        logger.info { "Change directory to that containing the private key and then run the sftp command." }
        logger.info { "cd ${FileUtils.toFile(privateKeyResourceUrl).parent}" }
        logger.info { cmd }
        logger.info { "You can now verify that the 2 required directories exist by running the following command" }
        logger.info {
            """
                sftp> ls EROP/Dev
                EROP/Dev/InBound   EROP/Dev/OutBound  
                sftp> 
            """.trimIndent()
        }
    }
}
