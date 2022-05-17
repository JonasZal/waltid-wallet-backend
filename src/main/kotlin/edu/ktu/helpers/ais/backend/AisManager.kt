package edu.ktu.helpers.ais.backend

import com.beust.klaxon.Klaxon
import eu.ebsi.TrustedIssuersRegistry.DeqarAttestation
import eu.ebsi.TrustedIssuersRegistry.Issuer
import eu.ebsi.TrustedIssuersRegistry.IssuerInformation
import id.walt.vclib.model.VerifiableCredential
import id.walt.webwallet.backend.auth.UserData
import id.walt.webwallet.backend.auth.UserInfo
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

object AisManager {

    fun checkIssuerAccreditation(did : String) : AccreditationCheckResult {
        var issuerRegistryClient: OkHttpClient = OkHttpClient();
        val decoder: Base64.Decoder = Base64.getDecoder()
        var issuerInfo: IssuerInformation? = null
        var issuerAttestationInfo: DeqarAttestation? = null

        val request =
            Request.Builder().url("https://api.preprod.ebsi.eu/trusted-issuers-registry/v2/issuers/$did").build()
        val response = issuerRegistryClient.newCall(request).execute()

        if (response.code != 200) return AccreditationCheckResult(
            organizationName = "",
            isAccredited = false,
            accreditedBy = "",
            accreditationInformationLocation = ""
        )

        val issuerRegistryResult = response.body?.string() ?: ""

        val issuerData = Klaxon().parse<Issuer>(issuerRegistryResult)

        issuerData?.attributes?.forEach {

            val attribute = String(decoder.decode(it.body))
            try {
                if (attribute.contains("https://www.w3.org/2018/credentials/v1", true)) {
                    issuerAttestationInfo = Klaxon().parse<DeqarAttestation>(attribute)
                } else {
                    issuerInfo = Klaxon().parse<IssuerInformation>(attribute)
                }
            } catch (e: Exception) {
            }
        }

        val organizationName =
            if (did.equals("did:ebsi:znjjv6JTecDiNjwzoabvuuk") && issuerInfo?.name.equals("issuer")) {
                "Tampere university"
            } else {
                issuerInfo?.name ?: ""
            }

        var accreditedBy = if (issuerAttestationInfo?.issuer.equals("did:ebsi:zk4bhCepWSYp9RhZkRPiwUL")) {
            "The European Quality Assurance Register for Higher Education (EQAR)"
        } else {
            issuerAttestationInfo?.issuer ?: ""
        }
        var accreditationInformationLocation = issuerAttestationInfo?.id ?: ""

        return AccreditationCheckResult(
            organizationName = organizationName,
            isAccredited = true,
            accreditedBy = accreditedBy,
            accreditationInformationLocation = accreditationInformationLocation
        )
    }

    fun getKtuModulesList(): Collection<KtuModule>{

        return listOf(
            KtuModule(code = "ECIU001", description = "Development and supporting of informal networks for transformation of Kaunas towards the Sustainable Learning City", status = "Closed"),
            KtuModule(code = "ECIU002", description = "One ticket system travelling across Lithuania", status = "Closed"),
            KtuModule(code = "ECIU003", description = "Transition of a City Towards Circular Economy", status = "Open"),
            KtuModule(code = "ECIU004", description = "Transforming a Municipality into a Digital City", status = "Closed"),
            KtuModule(code = "ECIU005", description = "New ways of modern energy consumer engagement", status = "Closed"),
            KtuModule(code = "ECIU006", description = "Changing the game in household waste sorting", status = "Closed"),
            KtuModule(code = "ECIU007", description = "Increasing energy sustainability among consumers", status = "Closed"))
    }

    fun createNewUser(userId : String, userPassword : String, userName : String, userFamilyName : String): String {

        return try {
            AisUserManager.addUserData( UserData(userId, userPassword, "ktudiplomas.xml", userName, userFamilyName, "") )
            "User created"
        } catch (e: Exception) {
            "Can't create user. Error: " + e.message
        }
    }

    fun getEnrolledModules(userInfo: UserInfo): Collection<String> {
        return AisUserManager.getEnrolledModules(userInfo)
    }

    fun enrolUserToCourse(userInfo: UserInfo, courseId: String): String {
        return AisUserManager.enrolUserToCourse(userInfo, courseId)
    }

}