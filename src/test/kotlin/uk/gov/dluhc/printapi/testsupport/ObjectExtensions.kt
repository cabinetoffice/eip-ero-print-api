package uk.gov.dluhc.printapi.testsupport

import uk.gov.dluhc.printapi.testsupport.DeepCopyObjectMapper.Companion.objectMapper

fun <T> T.deepCopy(): T {
    return objectMapper.readValue(objectMapper.writeValueAsString(this), this!!::class.java)
}
