package uk.gov.dluhc.printapi.testsupport.testdata

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import uk.gov.dluhc.printapi.testsupport.RsaKeyPair
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

const val UNAUTHORIZED_BEARER_TOKEN: String =
    "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQHdpbHRzaGlyZS5nb3YudWsiLCJpYXQiOjE1MTYyMzkwMjIsImF1dGhvcml0aWVzIjpbImVyby1hZG1pbiJdfQ.-pxW8z2xb-AzNLTRP_YRnm9fQDcK6CLt6HimtS8VcDY"

fun getBearerToken(
    eroId: String = aValidRandomEroId(),
    email: String = "an-ero-user@$eroId.gov.uk",
    groups: List<String> = listOf("ero-$eroId", "ero-vc-admin-$eroId")
): String =
    "Bearer ${buildAccessToken(eroId, email, groups)}"

fun buildAccessToken(
    eroId: String = aValidRandomEroId(),
    email: String = "an-ero-user@$eroId.gov.uk",
    groups: List<String> = listOf("ero-$eroId", "ero-vc-admin-$eroId")
): String =
    Jwts.builder()
        .setSubject(UUID.randomUUID().toString())
        .setClaims(
            mapOf(
                "cognito:groups" to groups,
                "email" to email
            )
        )
        .setIssuedAt(Date.from(Instant.now()))
        .setExpiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
        .signWith(RsaKeyPair.privateKey, SignatureAlgorithm.RS256)
        .compact()
