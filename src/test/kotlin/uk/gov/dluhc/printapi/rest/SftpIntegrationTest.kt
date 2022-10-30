package uk.gov.dluhc.printapi.rest

import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelSftp.LsEntry
import com.jcraft.jsch.JSch
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.USERNAME
import java.net.URL
import java.util.Vector


private val logger = KotlinLogging.logger {}

private const val SESSION_TIMEOUT = 10000

private const val CHANNEL_TIMEOUT = 5000

internal class SftpIntegrationTest {

    @Test
    fun `should return health check status UP given microservice is running healthily`() {
        // Given
        val privateKeyResourceUrl: URL = SftpContainerConfiguration.getPrivateKeyResourceUrl()
        val sftpInstance = SftpContainerConfiguration.getInstance()
        val mappedPort: Int = sftpInstance.getMappedPort(22)

        outputConnectionString(mappedPort, privateKeyResourceUrl)

        val sshClient = SshClient(
            username = SftpContainerConfiguration.USERNAME,
            privateKeyResourceUrl = SftpContainerConfiguration.getPrivateKeyResourceUrl(),
            host = SftpContainerConfiguration.HOSTNAME,
            port = sftpInstance.getMappedPort(22),
        )
        val cmd = SshClient.lsCommand("/EROP/Dev")
        val response: Vector<LsEntry> = sshClient.createSession(cmd)

        logger.info { "ls = $response" }
        assertThat(response).hasSize(4)
        assertThat(response).anyMatch { it.filename == "InBound" }
        assertThat(response).anyMatch { it.filename == "OutBound" }
    }

    private fun getResponse(jsch: JSch, mappedPort: Int): Vector<LsEntry> {
        jsch.getSession(USERNAME, "localhost", mappedPort).run {
            val runCommandResponse: Vector<LsEntry>?
            try {
                setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password")
                setConfig("StrictHostKeyChecking", "no")
                connect(SESSION_TIMEOUT)
                val sftp: Channel = openChannel("sftp")
                runCommandResponse = runCommand(sftp as ChannelSftp)
            } finally {
                disconnect()
            }
            return runCommandResponse!!
        }
    }

    private fun createJschWithPrivateKey(privateKeyResourceUrl: URL): JSch {
        val jsch = JSch()
        val privateKeyPath = privateKeyResourceUrl.path
        jsch.addIdentity(privateKeyPath)
        return jsch
    }

    private fun runCommand(sftp: ChannelSftp): Vector<LsEntry> {
        try{
            sftp.connect(CHANNEL_TIMEOUT)
            return doCommand(sftp)
        } finally {
            sftp.exit()
        }
    }

    private fun doCommand(channelSftp: ChannelSftp): Vector<LsEntry> = channelSftp.ls("/EROP/Dev") as Vector<LsEntry>

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
