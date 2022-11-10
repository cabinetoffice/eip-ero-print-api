package uk.gov.dluhc.printapi.service

import uk.gov.dluhc.printapi.database.entity.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.Status.RECEIVED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.entity.Status.SENT_TO_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.mapper.StatusMapper
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse.Status.SUCCESS
import java.time.Clock
import java.time.OffsetDateTime

class PrintResponseProcessingService(
    private val printDetailsRepository: PrintDetailsRepository,
    private val idFactory: IdFactory,
    private val clock: Clock,
    private val statusMapper: StatusMapper
) {
    /**
     * Updates print details for the batches based on the batch responses' statuses.
     * If a batch is successful, a new printRequestStatus will be added for each corresponding print details with status
     * being RECEIVED_BY_PRINT_PROVIDER.
     *
     * If a batch is not successful, the status will be set to SENT_TO_PRINT_PROVIDER and a new requestId will be set.
     * The reason for the new requestId is, the print provider does not support duplicate print requests with the same requestId.
     *
     * dateCreated will be the timestamp the new printRequestStatus is created and eventDateTime will be the timestamp from
     * the print provider's batch response.
     */
    fun processBatchResponses(batchResponses: List<BatchResponse>) {
        batchResponses.forEach { batchResponse ->
            with(batchResponse) {
                val newStatus =
                    if (status == SUCCESS) RECEIVED_BY_PRINT_PROVIDER else PENDING_ASSIGNMENT_TO_BATCH

                val printDetails =
                    printDetailsRepository.getAllByStatusAndBatchId(SENT_TO_PRINT_PROVIDER, batchId)

                printDetails.forEach {
                    it.addStatus(
                        status = newStatus,
                        dateCreated = OffsetDateTime.now(clock),
                        eventDateTime = timestamp,
                        message = message
                    )

                    if (status != SUCCESS) {
                        it.requestId = idFactory.requestId()
                        it.batchId = null
                    }
                }

                printDetailsRepository.updateItems(printDetails)
            }
        }
    }

    fun processPrintResponse(printResponse: ProcessPrintResponseMessage) {
        val newStatus = statusMapper.toStatusEntityEnum(printResponse.statusStep, printResponse.status)
        val printDetails = printDetailsRepository.getByRequestId(printResponse.requestId)

        with(printResponse) {
            printDetails.addStatus(
                status = newStatus,
                dateCreated = OffsetDateTime.now(clock),
                eventDateTime = timestamp,
                message = message
            )
        }

        printDetailsRepository.updateItems(listOf(printDetails))
    }
}
