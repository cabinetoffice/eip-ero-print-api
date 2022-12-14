package uk.gov.dluhc.printapi.jobs

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.service.PrintResponseFileReadinessService

@Component
class ProcessPrintResponsesBatchJob(
    private val printResponseFileReadinessService: PrintResponseFileReadinessService
) {

    @Scheduled(cron = "\${jobs.process-print-responses.cron}")
    @SchedulerLock(name = "\${jobs.process-print-responses.name}")
    fun pollAndProcessPrintResponses() {
        printResponseFileReadinessService.markAndSubmitPrintResponseFileForProcessing()
    }
}
