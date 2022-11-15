package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.mapper.PrintDetailsMapper
import uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage
import uk.gov.dluhc.printapi.rds.mapper.CertificateMapper
import uk.gov.dluhc.printapi.rds.repository.CertificateRepository
import javax.transaction.Transactional
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient as EroClient

@Service
class PrintService(
    private val eroClient: EroClient,
    private val printDetailsMapper: PrintDetailsMapper,
    private val printDetailsRepository: PrintDetailsRepository,
    private val certificateMapper: CertificateMapper,
    private val certificateRepository: CertificateRepository
) {
    @Transactional
    fun savePrintMessage(message: SendApplicationToPrintMessage) {
        val ero = eroClient.getElectoralRegistrationOffice(message.gssCode!!)
        val localAuthority = ero.localAuthorities.first { it.gssCode == message.gssCode }
        val printDetails = printDetailsMapper.toPrintDetails(message, ero, localAuthority.name)
        printDetailsRepository.save(printDetails)

        val certificate = certificateMapper.toCertificate(message, ero, localAuthority.name)
        certificateRepository.save(certificate)
    }
}
