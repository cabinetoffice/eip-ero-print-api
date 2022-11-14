package uk.gov.dluhc.printapi.rds.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.dluhc.printapi.database.entity.CertificateFormat
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.rds.entity.Certificate
import uk.gov.dluhc.printapi.rds.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.rds.entity.PrintRequest
import uk.gov.dluhc.printapi.rds.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidLocalAuthorityName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.rds.rdsDeliveryBuilder
import uk.gov.dluhc.printapi.testsupport.testdata.rds.rdsElectoralRegistrationOfficeBuilder
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoZipPath
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.util.stream.Stream

class CertificateToPrintRequestMapperTest {

    private lateinit var mapper: CertificateToPrintRequestMapperImpl

    @BeforeEach
    fun setup() {
        mapper = CertificateToPrintRequestMapperImpl()
        ReflectionTestUtils.setField(mapper, "instantMapper", InstantMapper())
    }

    companion object {
        @JvmStatic
        private fun welshEro(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(null),
                Arguments.of(rdsElectoralRegistrationOfficeBuilder(name = aValidLocalAuthorityName())),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("welshEro")
    fun `should map ERO response to dto with optional Welsh ERO translation`(eroWelsh: ElectoralRegistrationOffice?) {
        // Given
        val requestId: String = aValidRequestId()
        val sourceReference: String = aValidSourceReference()
        val applicationReference: String = aValidApplicationReference()
        val vacNumber: String = aValidVacNumber()
        val vacVersion = "1"
        val sourceType: SourceType = SourceType.VOTER_CARD
        val requestDateTime: Instant = Instant.ofEpochMilli(0)
        val firstName = "John"
        val middleNames = "Anthony Barry"
        val surname = "Doe"
        val certificateLanguage = CertificateLanguage.EN
        val certificateFormat = CertificateFormat.STANDARD
        val delivery = rdsDeliveryBuilder()
        val gssCode: String = getRandomGssCode()
        val issuingAuthority: String = aValidLocalAuthorityName()
        val issueDate = LocalDate.of(2022, 10, 21)
        val suggestedExpiryDate = LocalDate.of(2032, 10, 21)
        val eroEnglish: ElectoralRegistrationOffice = rdsElectoralRegistrationOfficeBuilder(name = issuingAuthority)
        val photoLocation = aPhotoArn()
        val statusHistory = mutableListOf(
            PrintRequestStatus(
                status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                eventDateTime = Instant.now()
            )
        )
        val batchId = aValidBatchId()
        val printRequest = PrintRequest(
            requestId = requestId,
            vacVersion = vacVersion,
            requestDateTime = requestDateTime,
            firstName = firstName,
            middleNames = middleNames,
            surname = surname,
            certificateLanguage = certificateLanguage,
            certificateFormat = certificateFormat,
            photoLocationArn = photoLocation,
            delivery = delivery,
            eroEnglish = eroEnglish,
            eroWelsh = eroWelsh,
            batchId = batchId,
            userId = aValidUserId(),
            statusHistory = statusHistory
        )
        val certificate = Certificate(
            vacNumber = vacNumber,
            sourceType = sourceType,
            sourceReference = sourceReference,
            applicationReference = applicationReference,
            applicationReceivedDateTime = Instant.now(),
            issuingAuthority = issuingAuthority,
            issueDate = issueDate,
            suggestedExpiryDate = suggestedExpiryDate,
            status = Status.PENDING_ASSIGNMENT_TO_BATCH,
            gssCode = gssCode,
            printRequests = mutableListOf(printRequest)
        )
        val photoZipPath: String = aPhotoZipPath()

        val expected = uk.gov.dluhc.printapi.printprovider.models.PrintRequest()
        expected.requestId = requestId
        expected.issuingAuthorityEn = eroEnglish.name
        expected.issueDate = issueDate
        expected.suggestedExpiryDate = suggestedExpiryDate
        expected.requestDateTime = requestDateTime.atOffset(UTC)
        expected.cardFirstname = firstName
        expected.cardMiddleNames = middleNames
        expected.cardSurname = surname
        expected.cardVersion = vacVersion
        expected.cardNumber = vacNumber
        expected.certificateLanguage = uk.gov.dluhc.printapi.printprovider.models.PrintRequest.CertificateLanguage.EN
        expected.certificateFormat = uk.gov.dluhc.printapi.printprovider.models.PrintRequest.CertificateFormat.STANDARD
        expected.deliveryOption = uk.gov.dluhc.printapi.printprovider.models.PrintRequest.DeliveryOption.STANDARD
        expected.photo = photoZipPath
        expected.deliveryName = delivery.addressee
        expected.deliveryStreet = delivery.address?.street
        expected.deliveryProperty = delivery.address?.property
        expected.deliveryLocality = delivery.address?.locality
        expected.deliveryTown = delivery.address?.town
        expected.deliveryArea = delivery.address?.area
        expected.deliveryPostcode = delivery.address?.postcode
        expected.eroNameEn = eroEnglish.name
        expected.eroPhoneNumberEn = eroEnglish.phoneNumber
        expected.eroEmailAddressEn = eroEnglish.emailAddress
        expected.eroWebsiteEn = eroEnglish.website
        expected.eroDeliveryStreetEn = eroEnglish.address?.street
        expected.eroDeliveryPropertyEn = eroEnglish.address?.property
        expected.eroDeliveryLocalityEn = eroEnglish.address?.locality
        expected.eroDeliveryTownEn = eroEnglish.address?.town
        expected.eroDeliveryAreaEn = eroEnglish.address?.area
        expected.eroDeliveryPostcodeEn = eroEnglish.address?.postcode
        // Optional Welsh translation expectations
        expected.issuingAuthorityCy = eroWelsh?.name
        expected.eroNameCy = eroWelsh?.name
        expected.eroPhoneNumberCy = eroWelsh?.phoneNumber
        expected.eroEmailAddressCy = eroWelsh?.emailAddress
        expected.eroWebsiteCy = eroWelsh?.website
        expected.eroDeliveryStreetCy = eroWelsh?.address?.street
        expected.eroDeliveryPropertyCy = eroWelsh?.address?.property
        expected.eroDeliveryLocalityCy = eroWelsh?.address?.locality
        expected.eroDeliveryTownCy = eroWelsh?.address?.town
        expected.eroDeliveryAreaCy = eroWelsh?.address?.area
        expected.eroDeliveryPostcodeCy = eroWelsh?.address?.postcode

        // When
        val actual = mapper.map(certificate, printRequest, photoZipPath)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected)
    }
}
