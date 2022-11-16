package uk.gov.dluhc.printapi.database.repository

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.id.UUIDGenerator
import java.io.Serializable

class UseExistingOrGenerateUUID : UUIDGenerator() {
    companion object {
        const val NAME = "uk.gov.dluhc.printapi.database.repository.UseExistingOrGenerateUUID"
    }

    override fun generate(session: SharedSessionContractImplementor, entity: Any?): Serializable {
        val id = session.getEntityPersister(null, entity).classMetadata.getIdentifier(entity, session)
        return id ?: super.generate(session, entity)
    }
}
