package uk.gov.dluhc.printapi.database.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.dto.StatusDto

internal class DtoStatusMapperTest {

    private val dtoStatusMapper = DtoStatusMapper()

    @ParameterizedTest
    @CsvSource(
        value = [
            "PENDING_ASSIGNMENT_TO_BATCH, PENDING_ASSIGNMENT_TO_BATCH",
            "ASSIGNED_TO_BATCH, ASSIGNED_TO_BATCH",
            "SENT_TO_PRINT_PROVIDER, SENT_TO_PRINT_PROVIDER",
            "RECEIVED_BY_PRINT_PROVIDER, RECEIVED_BY_PRINT_PROVIDER",
            "VALIDATED_BY_PRINT_PROVIDER, VALIDATED_BY_PRINT_PROVIDER",
            "IN_PRODUCTION, IN_PRODUCTION",
            "DISPATCHED, DISPATCHED",
            "NOT_DELIVERED, NOT_DELIVERED",
            "PRINT_PROVIDER_VALIDATION_FAILED, PRINT_PROVIDER_VALIDATION_FAILED",
            "PRINT_PROVIDER_PRODUCTION_FAILED, PRINT_PROVIDER_PRODUCTION_FAILED",
            "PRINT_PROVIDER_DISPATCH_FAILED, PRINT_PROVIDER_DISPATCH_FAILED",
        ]
    )
    fun `should map entity Status to DTO status`(status: Status, expected: StatusDto) {
        // Given

        // When
        val actual = dtoStatusMapper.toDtoStatus(status)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
