package edu.ktu.helpers.ais.backend

import edu.ktu.helpers.StudentCredentialsGenerator
import id.walt.issuer.backend.IssuableCredential
import id.walt.issuer.backend.Issuables
import id.walt.vclib.credentials.Europass
import id.walt.vclib.credentials.VerifiableId
import id.walt.vclib.model.AbstractVerifiableCredential
import id.walt.vclib.model.CredentialSubject
import id.walt.vclib.templates.VcTemplateManager
import id.walt.webwallet.backend.auth.UserInfo

object AisCredentialsManager {

    fun listIssuableCredentialsFor(user: UserInfo): Issuables {
        val cred = listOf(getStudentIdCredential(user), getStudentDiplomaCredential(user))
        return Issuables(cred)
    }

    private fun getStudentIdCredential (user: UserInfo) : IssuableCredential
    {
        val studInfo = AisUserManager.getUserData(user.email!!)
        var studId : VerifiableId = VcTemplateManager.loadTemplate("VerifiableId") as VerifiableId

        studId.credentialSubject = VerifiableId.VerifiableIdSubject(
            id = "did:ebsi:2AEMAqXWKYMu1JHPAgGcga4dxu7ThgfgN95VyJBJGZbSJUtp",
            familyName = studInfo!!.familyName,
            firstName = studInfo!!.name,
            personalIdentifier = "KTUSTUD" + studInfo!!.name.uppercase(),
            nameAndFamilyNameAtBirth = studInfo!!.name + " " + studInfo!!.familyName,
            dateOfBirth = "1990-01-01"
        )
        return IssuableCredential(
            studId!!.credentialSchema!!.id,
            studId.type.last(),
            mapOf(
                Pair(
                    "credentialSubject",
                    (studId as AbstractVerifiableCredential<out CredentialSubject>).credentialSubject!!
                )
            )
        )
    }

    private fun getStudentDiplomaCredential (user: UserInfo) : IssuableCredential
    {
        //TODO: Must be corrected as now it fills information from the template

        val jsonDiploma = StudentCredentialsGenerator.getStudentDiplomaXml(user)
        val diplomas = jsonDiploma?.getJSONObject("duomenys")?.getJSONObject("pazymejimas")

        var europassDiploma : Europass =VcTemplateManager.loadTemplate("Europass") as Europass

        var europassSubject = europassDiploma.credentialSubject

        europassSubject?.dateOfBirth = jsonDiploma?.getJSONObject("duomenys")?.getJSONObject("pazymejimas")?.getJSONObject("asmuo")?.getString("gimimoData")
        europassSubject?.familyName = diplomas?.getJSONObject("asmuo")?.getString("pavarde")
        europassSubject?.givenNames = diplomas?.getJSONObject("asmuo")?.getString("vardas")
        europassSubject?.identifier =  Europass.EuropassSubject.Identifier(
            diplomas?.getJSONObject("isdavusiInstitucija")!!.getString("institucijosPavadinimas"),
            diplomas?.getJSONObject("isdavusiInstitucija")!!.getString("institucijosPavadinimas")
        )
        var learningAchievement = Europass.EuropassSubject.LearningAchievement(
            diplomas?.getJSONObject("priedas").getString("numeris").toString(),
            diplomas?.getJSONObject("priedas").getString("tipas"),
            diplomas?.getJSONObject("priedas").getString("studijuProgramosReikalavimai"),
            listOf(
                diplomas?.getJSONObject("priedas").getString("kvalifikacijosGalimybes"),
                diplomas?.getJSONObject("priedas").getString("studijuProgramosReikalavimai")
            )

        )
        europassSubject?.learningAchievement = learningAchievement

        var awardingOpportunity =  Europass.EuropassSubject.AwardingOpportunity(
            diplomas?.getInt("blankoKodas").toString(),
            diplomas!!.getString("programosPavadinimas"),
            Europass.EuropassSubject.AwardingOpportunity.AwardingBody(
                id = diplomas?.getJSONObject("isdavusiInstitucija").getInt("institucijosKodas").toString(),
                eidasLegalIdentifier = "",
                registration = diplomas?.getJSONObject("isdavusiInstitucija").getString("institucijosPavadinimas"),
                preferredName = diplomas?.getJSONObject("isdavusiInstitucija").getString("institucijosVadovas")
            )
        )
        europassSubject?.awardingOpportunity = awardingOpportunity

        val grades = diplomas?.getJSONArray("dalykas")!!.toString()
        europassSubject?.learningSpecification =  Europass.EuropassSubject.LearningSpecification(
            "gg",
            listOf(grades),
            5,
            2,
            listOf("berebe")
        )

        europassDiploma.credentialSubject = europassSubject

        return IssuableCredential(
            europassDiploma!!.credentialSchema!!.id,
            europassDiploma.type.last(),
            mapOf(
                Pair(
                    "credentialSubject",
                    (europassDiploma as AbstractVerifiableCredential<out CredentialSubject>).credentialSubject!!
                )
            )
        )
    }

}