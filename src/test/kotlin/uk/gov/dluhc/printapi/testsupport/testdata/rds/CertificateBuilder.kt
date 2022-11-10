package uk.gov.dluhc.printapi.testsupport.testdata.rds

import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.rds.entity.Address
import uk.gov.dluhc.printapi.rds.entity.Certificate
import uk.gov.dluhc.printapi.rds.entity.Delivery
import uk.gov.dluhc.printapi.rds.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.rds.entity.PrintRequest
import uk.gov.dluhc.printapi.rds.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAddressPostcode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAddressStreet
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReceivedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateFormat
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateLanguage
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryClass
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryMethod
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssuingAuthority
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPrintRequestStatusEventDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceType
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSuggestedExpiryDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacVersion
import uk.gov.dluhc.printapi.testsupport.testdata.aValidWebsite
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.time.Instant

fun certificateBuilder(
    printRequests: List<PrintRequest> = listOf(printRequestBuilder()),
): Certificate {
    val certificate = Certificate(
        vacNumber = aValidVacNumber(),
        sourceType = aValidSourceType(),
        sourceReference = aValidSourceReference(),
        applicationReference = aValidApplicationReference(),
        applicationReceivedDateTime = aValidApplicationReceivedDateTime(),
        issuingAuthority = aValidIssuingAuthority(),
        issueDate = aValidIssueDate(),
        suggestedExpiryDate = aValidSuggestedExpiryDate(),
        gssCode = aGssCode(),
        status = aValidCertificateStatus()
    )
    printRequests.forEach { printRequest -> certificate.addPrintRequest(printRequest) }
    return certificate
}

fun printRequestBuilder(
    printRequestStatuses: List<PrintRequestStatus> = listOf(printRequestStatusBuilder()),
    requestDateTime: Instant? = aValidRequestDateTime(),
): PrintRequest {
    val deliveryAddress = Address(
        street = aValidAddressStreet(),
        postcode = aValidAddressPostcode()
    )
    val delivery = Delivery(
        addressee = aValidDeliveryName(),
        address = deliveryAddress,
        deliveryClass = aValidDeliveryClass(),
        deliveryMethod = aValidDeliveryMethod()
    )
    val eroEnglish = ElectoralRegistrationOffice(
        address = Address(
            street = aValidAddressStreet(),
            postcode = aValidAddressPostcode()
        ),
        name = aValidEroName(),
        phoneNumber = aValidPhoneNumber(),
        emailAddress = aValidEmailAddress(),
        website = aValidWebsite()
    )
    val printRequest = PrintRequest(
        requestId = aValidRequestId(),
        vacVersion = aValidVacVersion(),
        requestDateTime = requestDateTime,
        firstName = aValidFirstName(),
        surname = aValidSurname(),
        certificateLanguage = aValidCertificateLanguage(),
        certificateFormat = aValidCertificateFormat(),
        photoLocationArn = aPhotoArn(),
        delivery = delivery,
        eroEnglish = eroEnglish,
        eroWelsh = null,
        userId = aValidUserId()
    )
    printRequestStatuses.forEach { printRequestStatus -> printRequest.addPrintRequestStatus(printRequestStatus) }
    return printRequest
}

fun printRequestStatusBuilder(
    status: Status = aValidCertificateStatus(),
    eventDateTime: Instant = aValidPrintRequestStatusEventDateTime(),
): PrintRequestStatus {
    return PrintRequestStatus(
        status = status,
        eventDateTime = eventDateTime
    )
}
