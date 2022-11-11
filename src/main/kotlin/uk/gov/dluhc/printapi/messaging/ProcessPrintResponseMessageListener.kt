package uk.gov.dluhc.printapi.messaging

import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.service.PrintResponseProcessingService
import javax.validation.Valid

private val logger = KotlinLogging.logger { }

/**
 * Implementation of [MessageListener] to handle [ProcessPrintResponseMessage] messages
 */
@Component
class ProcessPrintResponseMessageListener(
    private val printResponseProcessingService: PrintResponseProcessingService
) : MessageListener<ProcessPrintResponseMessage> {

    @SqsListener("\${sqs.process-print-response-queue-name}")
    override fun handleMessage(@Valid @Payload payload: ProcessPrintResponseMessage) {
        with(payload) {
            logger.info { "Begin processing PrintResponse with requestId ${payload.requestId}" }
            printResponseProcessingService.processPrintResponse(payload)
        }
    }
}
