package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.http.AbortableInputStream
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPrintRequestsFilename
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aFileDetailsBuilder
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoBucket
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoBucketPath
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoZipPath
import uk.gov.dluhc.printapi.testsupport.testdata.zip.photoLocationBuilder
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.lang.RuntimeException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@ExtendWith(MockitoExtension::class)
internal class ZipOutputStreamProducerRunnableTest {

    @Mock
    private lateinit var s3Client: S3Client

    @Mock
    private lateinit var printRequestsFileProducer: PrintRequestsFileProducer

    @Test
    fun `should run and produce Zip Output Stream`() {
        // Given
        val zipOutputStream = PipedOutputStream()
        val sftpInputStream = PipedInputStream(zipOutputStream)

        val s3Bucket = aPhotoBucket()
        val s3Path = aPhotoBucketPath()
        val s3ResourceContents = "S3 Resource Contents"
        val psvFileContents = "PSV File contents"
        val printRequestsFilename = aValidPrintRequestsFilename()
        val zipPath = aPhotoZipPath()
        val fileDetails = aFileDetailsBuilder(
            printRequestsFilename = printRequestsFilename,
            photoLocations = listOf(
                photoLocationBuilder(
                    zipPath = zipPath,
                    sourceBucket = s3Bucket,
                    sourcePath = s3Path
                )
            )
        )
        val zipOutputStreamProducerRunnable = createRunnable(zipOutputStream, sftpInputStream, fileDetails)
        mockPrintRequestFileProducerWritePsvContents(psvFileContents)
        mockS3ClientResponse(zipOutputStream, s3ResourceContents)

        // When
        zipOutputStreamProducerRunnable.run()

        // Then
        verify(printRequestsFileProducer).writeFileToStream(any<ZipOutputStream>(), eq(fileDetails.printRequests))
        verify(s3Client).getObject(
            argThat<GetObjectRequest> { request -> request.bucket().equals(s3Bucket) && request.key().equals(s3Path) },
            any<ResponseTransformer<GetObjectResponse, ZipOutputStream>>()
        )
        val generatedZip = ZipInputStream(sftpInputStream)
        val psvFile = generatedZip.nextEntry
        assertThat(psvFile).isNotNull
        assertThat(psvFile!!.name).isEqualTo(printRequestsFilename)
        val psvContents = String(generatedZip.readBytes())
        assertThat(psvContents).isEqualTo(psvFileContents)
        val photoFile = generatedZip.nextEntry
        assertThat(photoFile).isNotNull
        assertThat(photoFile!!.name).isEqualTo(zipPath)
        val photoFileContents = String(generatedZip.readBytes())
        assertThat(photoFileContents).isEqualTo(s3ResourceContents)
    }

    @Test
    fun `should fail to run as error raised`() {
        // Given
        val zipOutputStream = PipedOutputStream()
        val sftpInputStream = PipedInputStream(zipOutputStream)
        val fileDetails = aFileDetailsBuilder()
        val zipOutputStreamProducerRunnable = createRunnable(zipOutputStream, sftpInputStream, fileDetails)
        doThrow(RuntimeException::class).`when`(printRequestsFileProducer).writeFileToStream(any(), any())

        // When
        val error = catchException { zipOutputStreamProducerRunnable.run() }

        // Then
        verify(printRequestsFileProducer).writeFileToStream(any<ZipOutputStream>(), eq(fileDetails.printRequests))
        verifyNoInteractions(s3Client)
        assertThat(error).isNotNull
    }

    private fun createRunnable(
        zipOutputStream: PipedOutputStream,
        sftpInputStream: PipedInputStream,
        fileDetails: FileDetails
    ) = ZipOutputStreamProducerRunnable(
        s3Client,
        zipOutputStream,
        sftpInputStream,
        fileDetails,
        printRequestsFileProducer
    )

    private fun mockPrintRequestFileProducerWritePsvContents(psvFileContents: String) {
        doAnswer {
            val psvFile = it.arguments[0] as ZipOutputStream
            psvFile.write(psvFileContents.encodeToByteArray())
        }.`when`(printRequestsFileProducer).writeFileToStream(any(), any())
    }

    private fun mockS3ClientResponse(
        zipOutputStream: PipedOutputStream,
        s3ResourceContents: String
    ) {
        doAnswer {
            val responseTransformer = it.arguments[1] as ResponseTransformer<OutputStream, PipedOutputStream>
            val s3ResourceInputStream =
                AbortableInputStream.create(ByteArrayInputStream(s3ResourceContents.encodeToByteArray()))
            responseTransformer.transform(zipOutputStream, s3ResourceInputStream)
        }.`when`(s3Client)
            .getObject(any<GetObjectRequest>(), any<ResponseTransformer<GetObjectResponse, ZipOutputStream>>())
    }
}
