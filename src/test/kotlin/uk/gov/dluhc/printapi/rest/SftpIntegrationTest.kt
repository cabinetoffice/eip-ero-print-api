package uk.gov.dluhc.printapi.rest

import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelSftp.LsEntry
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.USERNAME
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Properties
import java.util.Vector


private val logger = KotlinLogging.logger {}

private const val SESSION_TIMEOUT = 10000

private const val CHANNEL_TIMEOUT = 5000

internal class SftpIntegrationTest {

    @Test
    fun `should return health check status UP given microservice is running healthily`() {
        // Given
        val privateKeyResourceUrl: URL = SftpContainerConfiguration.getPrivateKeyResourceUrl()

//        val key: ByteArray = Files.readAllBytes(Paths.get(privateKeyResourceUrl.file))
//        val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")
//        val keySpec = PKCS8EncodedKeySpec(key)
//        val finalKey: PrivateKey = keyFactory.generatePrivate(keySpec)
//        println(finalKey.algorithm)

        val sftpInstance = SftpContainerConfiguration.getInstance()
        val mappedPort = sftpInstance.getMappedPort(22)

        logger.info { "Change directory to that containing the private key and then run the sftp command." }
        logger.info { "cd ${FileUtils.toFile(privateKeyResourceUrl).parent}" }
        logger.info { "sftp -v -P $mappedPort -oKexAlgorithms=+diffie-hellman-group1-sha1 -o \"IdentityFile=./printer_rsa\" -o User=valtech localhost" }
        logger.info { "You can now verify that the 2 required directories exist by running the following command" }
        logger.info {
            """
            sftp> ls EROP/Dev
            EROP/Dev/InBound   EROP/Dev/OutBound  
            sftp> 
        """.trimIndent()
        }

        val jsch = JSch()
        var jschSession: Session? = null
        val privateKeyPath = privateKeyResourceUrl.path
        try {
            jsch.addIdentity(privateKeyPath)
            jschSession = jsch.getSession(USERNAME, "localhost", mappedPort)
            jschSession.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password")
            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            jschSession.setConfig(config)
        } catch (e: JSchException) {
            throw RuntimeException("Failed to create Jsch Session object.", e)
        }

        try {
            // 10 seconds session timeout
            jschSession.connect(SESSION_TIMEOUT)
            val sftp: Channel = jschSession.openChannel("sftp")

            // 5 seconds timeout
            sftp.connect(CHANNEL_TIMEOUT)
            val channelSftp: ChannelSftp = sftp as ChannelSftp

            // transfer file from local to remote server
            val ls: Vector<LsEntry> = channelSftp.ls("/EROP/Dev") as Vector<LsEntry>
            logger.info { "ls = $ls" }

            assertThat(ls).hasSize(4)
            assertThat(ls).anyMatch { it.filename == "InBound" }
            assertThat(ls).anyMatch { it.filename == "OutBound" }

            // download file from remote server to local
            // channelSftp.get(remoteFile, localFile);
            channelSftp.exit()
        } finally {
            if (jschSession != null) {
                jschSession.disconnect()
            }
        }
    }
}
