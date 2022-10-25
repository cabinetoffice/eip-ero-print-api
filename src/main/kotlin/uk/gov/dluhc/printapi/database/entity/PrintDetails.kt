package uk.gov.dluhc.printapi.database.entity

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@DynamoDbBean
data class PrintDetails(
    @get:DynamoDbPartitionKey var id: UUID? = null,
    @get:DynamoDbSecondaryPartitionKey(indexNames = [REQUEST_ID_INDEX_NAME]) var requestId: String? = null,
    var sourceReference: String? = null,
    var applicationReference: String? = null,
    @get:DynamoDbSecondaryPartitionKey(indexNames = [SOURCE_TYPE_GSS_CODE_INDEX_NAME]) var sourceType: SourceType? = null,
    var vacNumber: String? = null,
    var requestDateTime: OffsetDateTime? = null,
    var firstName: String? = null,
    var middleNames: String? = null,
    var surname: String? = null,
    var certificateLanguage: CertificateLanguage? = null,
    var photoLocation: String? = null,
    var delivery: CertificateDelivery? = null,
    @get:DynamoDbSortKey @get:DynamoDbSecondarySortKey(indexNames = [SOURCE_TYPE_GSS_CODE_INDEX_NAME]) var gssCode: String? = null,
    var issuingAuthority: String? = null,
    var issueDate: LocalDate = LocalDate.now(),
    var eroEnglish: ElectoralRegistrationOffice? = null,
    var eroWelsh: ElectoralRegistrationOffice? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PrintDetails

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id)"
    }

    companion object {
        const val REQUEST_ID_INDEX_NAME = "RequestIdIndex"
        const val SOURCE_TYPE_GSS_CODE_INDEX_NAME = "SourceTypeGssCodeIndex"
    }
}