package eu.ebsi.TrustedIssuersRegistry

import com.beust.klaxon.Json
import id.walt.vclib.credentials.VerifiableId
import id.walt.vclib.model.*
import id.walt.vclib.registry.VerifiableCredentialMetadata
import id.walt.vclib.schema.SchemaService

data class DeqarAttestation (
    @Json(name = "@context") @field:SchemaService.PropertyName(name = "@context") @field:SchemaService.Required
    var context: List<String> = listOf(
        "https://www.w3.org/2018/credentials/v1"
    ),
    @Json(serializeNull = false) override var id: String? = null,
    @Json(serializeNull = false) override var issuer: String? = null,
    @Json(serializeNull = false) override var issued: String? = null,
    @Json(serializeNull = false) override var validFrom: String? = null,
    @Json(serializeNull = false) override var expirationDate: String? = null,
    @Json(serializeNull = false) override var credentialSubject: DeqarAttestationSubject? = null,
    @Json(serializeNull = false) override var credentialSchema: CredentialSchema? = null,
    @Json(serializeNull = false) override var proof: Proof? = null
) : AbstractVerifiableCredential<DeqarAttestation.DeqarAttestationSubject>(type) {
    companion object : VerifiableCredentialMetadata(
        type = listOf("DeqarAttestation"),
        template = {
            DeqarAttestation()
        }
    )

    data class DeqarAttestationSubject(
        @Json(serializeNull = false) override var id: String? = null,
        @Json(serializeNull = false) var authorizationClaims: AuthorizationClaims? = null,
    ) : CredentialSubject()

    data class AuthorizationClaims(
        @Json(serializeNull = false) var accreditationType: String? = null,
        @Json(serializeNull = false) var decision: String? = null,
        @Json(serializeNull = false) var report: List<String>? = null,
        @Json(serializeNull = false) var limitJurisdiction: List<String>? = null,
        @Json(serializeNull = false) var limitQFLevel: List<String>? = null,
    )

}