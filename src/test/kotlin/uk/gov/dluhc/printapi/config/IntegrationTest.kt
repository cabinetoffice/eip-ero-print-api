package uk.gov.dluhc.printapi.config

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelSftp.LsEntry
import io.awspring.cloud.messaging.core.QueueMessagingTemplate
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.io.ByteArrayResource
import org.springframework.integration.file.remote.session.CachingSessionFactory
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import software.amazon.awssdk.services.s3.S3Client
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.PRINT_REQUEST_UPLOAD_PATH
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.PRINT_RESPONSE_DOWNLOAD_PATH
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.WiremockService

/**
 * Base class used to bring up the entire Spring ApplicationContext
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [IntegrationTest.IntegrationTestConfiguration::class],
)
@ActiveProfiles("test")
@AutoConfigureWebTestClient(timeout = "PT5M")
internal abstract class IntegrationTest {

    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @Autowired
    protected lateinit var wireMockService: WiremockService

    @Autowired
    protected lateinit var sqsMessagingTemplate: QueueMessagingTemplate

    @Autowired
    protected lateinit var dynamoDbClient: DynamoDbClient

    @Autowired
    protected lateinit var dynamoDbConfiguration: DynamoDbConfiguration

    @Autowired
    protected lateinit var printDetailsRepository: PrintDetailsRepository

    @Autowired
    @Qualifier("sftpInboundTemplate")
    protected lateinit var sftpInboundTemplate: SftpRemoteFileTemplate

    @Autowired
    @Qualifier("sftpOutboundTemplate")
    protected lateinit var sftpOutboundTemplate: SftpRemoteFileTemplate

    @Autowired
    protected lateinit var s3Client: S3Client

    @Value("\${sqs.send-application-to-print-queue-name}")
    protected lateinit var sendApplicationToPrintQueueName: String

    @Value("\${sqs.process-print-request-batch-queue-name}")
    protected lateinit var processPrintRequestBatchQueueName: String

    @BeforeEach
    fun clearLogAppender() {
        TestLogAppender.reset()
    }

    @BeforeEach
    fun clearDatabase() {
        clearTable(dynamoDbConfiguration.printDetailsTableName)
    }

    @BeforeEach
    fun clearSftpUploadDirectory() {
        getSftpInboundDirectoryFileNames()
            .forEach { path -> sftpInboundTemplate.remove("$PRINT_REQUEST_UPLOAD_PATH/$path") }
    }

    @BeforeEach
    fun clearSftpDownloadDirectory() {
        getSftpOutboundDirectoryFileNames()
            .forEach { path -> sftpOutboundTemplate.remove("$PRINT_RESPONSE_DOWNLOAD_PATH/$path") }
    }

    @BeforeEach
    fun resetWireMock() {
        wireMockService.resetAllStubsAndMappings()
    }

    companion object {
        val localStackContainer = LocalStackContainerConfiguration.getInstance()
        val sftpContainer = SftpContainerConfiguration.getInstance()
    }

    @TestConfiguration
    class IntegrationTestConfiguration {
        @Bean
        @Primary
        fun testSftpSessionFactory(properties: SftpProperties): SessionFactory<ChannelSftp.LsEntry> {
            val factory = DefaultSftpSessionFactory(true)
            factory.setHost(properties.host)
            factory.setPort(sftpContainer.getMappedPort(SftpContainerConfiguration.DEFAULT_SFTP_PORT))
            factory.setUser(properties.user)
            factory.setPrivateKey(ByteArrayResource(properties.privateKey.encodeToByteArray()))
            factory.setAllowUnknownKeys(true)
            return CachingSessionFactory(factory)
        }
    }

    protected fun clearTable(tableName: String, partitionKey: String = "id", sortKey: String? = null) {
        val response = dynamoDbClient.scan(ScanRequest.builder().tableName(tableName).build())
        response.items().forEach {
            val keys = mutableMapOf<String, AttributeValue>(
                partitionKey to AttributeValue.builder().s(it[partitionKey]!!.s()).build(),
            )

            if (sortKey != null) {
                keys[sortKey] = AttributeValue.builder().s(it[partitionKey]!!.s()).build()
            }

            val deleteRequest = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(keys)
                .build()

            dynamoDbClient.deleteItem(deleteRequest)
        }
    }

    protected fun getSftpOutboundDirectoryFileNames() =
        getSftpDirectoryFileNames(sftpOutboundTemplate, PRINT_RESPONSE_DOWNLOAD_PATH)

    private fun getSftpInboundDirectoryFileNames() =
        getSftpDirectoryFileNames(sftpInboundTemplate, PRINT_REQUEST_UPLOAD_PATH)

    private fun getSftpDirectoryFileNames(
        sftpTemplate: SftpRemoteFileTemplate,
        directory: String
    ): List<String> {
        return sftpTemplate.list(directory)
            .map(LsEntry::getFilename)
            .filterNot { path -> path.equals(".") }
            .filterNot { path -> path.equals("..") }
            .toList()
    }
}
