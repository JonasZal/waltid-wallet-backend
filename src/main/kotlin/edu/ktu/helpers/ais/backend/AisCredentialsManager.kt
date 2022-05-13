package edu.ktu.helpers.ais.backend

import edu.ktu.helpers.StudentCredentialsGenerator
import id.walt.issuer.backend.IssuableCredential
import id.walt.issuer.backend.Issuables
import id.walt.issuer.backend.IssuerConfig
import id.walt.vclib.credentials.Europass
import id.walt.vclib.credentials.VerifiableId
import id.walt.vclib.model.AbstractVerifiableCredential
import id.walt.vclib.model.CredentialSubject
import id.walt.vclib.templates.VcTemplateManager
import id.walt.webwallet.backend.auth.UserData
import id.walt.webwallet.backend.auth.UserInfo
import id.walt.webwallet.backend.config.WalletConfig
import netscape.javascript.JSObject
import java.util.*
import org.json.JSONObject



object AisCredentialsManager {

    fun listIssuableCredentialsFor(user: UserInfo): Issuables {
        //val cred = listOf(getStudentIdCredential(user), getStudentDiplomaCredential(user), getEuropassDiploma(user))
        val cred = listOf(getStudentIdCredential(user), getEuropassDiploma(user))
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
            dateOfBirth = "",
            placeOfBirth = "",
            identifier = listOf(VerifiableId.VerifiableIdSubject.Identifier("http://ktu.edu/student-identification-number", "KTUSTUD" + studInfo!!.name.uppercase())),
            currentAddress = listOf(""),
            gender = ""
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

    private fun getEuropassDiploma(user: UserInfo)  : IssuableCredential {
        val studInfo = AisUserManager.getUserData(user.email!!)

        var europassDiploma: Europass.EuropassSubject = Europass.EuropassSubject()

        europassDiploma.id = generateId("credential")
        europassDiploma.identifier = Europass.EuropassSubject.Identifier("http://ktu.edu/student-identification-number", "KTUSTUD" + studInfo!!.name.uppercase())
        europassDiploma.achieved = getAchievements(user, studInfo)

        var europass = Europass(
            context = listOf("https://www.w3.org/2018/credentials/v1"),
            credentialSubject = europassDiploma
        )

        return IssuableCredential(
            schemaId="https://raw.githubusercontent.com/walt-id/waltid-ssikit-vclib/master/src/test/resources/schemas/Europass.json",
            type="Europass",
            credentialData =
            mapOf(
                Pair(
                    "credentialSubject",
                    (europass as AbstractVerifiableCredential<out CredentialSubject>).credentialSubject!!
                )
            )
        )

    }

    private fun getAchievements(user: UserInfo, studData: UserData):  List<Europass.EuropassSubject.Achieved>{
        val issuer = IssuerConfig.config.issuerDid ?: return emptyList()

        val jsonDiploma = StudentCredentialsGenerator.getStudentDiplomaXml(user)

        //TODO: values below should be populated from data file

        val title = "Transition of a City Towards Circular Economy"

        var achievement = Europass.EuropassSubject.Achieved(
            id = generateId("learningAchievement"),
            title = title,
            additionalNote = null,
            identifier = null,
            wasDerivedFrom = getAssessment(jsonDiploma!!),
            wasInfluencedBy = getLearningActivities(),
            wasAwardedBy = Europass.EuropassSubject.Achieved.WasAwardedBy(generateId("awardingProcess"), listOf(issuer), null, null),
            hasPart = null,
            entitlesTo = null,
            specifiedBy = getLearningSpecification(),
        )


        return listOf(achievement)
    }

    private fun generateId(type: String): String {
        return "urn:epass:" + type + ":" + UUID.randomUUID().toString()
    }

    private fun getAssessment(jsonDiploma: JSONObject): List<Europass.EuropassSubject.Achieved.WasDerivedFrom>{
        //TODO: values below should be populated from data file

        val title = "Overall grade"
        val grade: Double = 9.00
        val subAssessment = getSubAssessment(jsonDiploma)

        return listOf(Europass.EuropassSubject.Achieved.WasDerivedFrom(
            id = generateId("assessment"),
            title = title,
            grade = grade.toString(),
            assessedBy = emptyList(),
            hasPart = subAssessment,
            specifiedBy = getSpecification()
        ))
    }

    private fun getSubAssessment(jsonDiploma: JSONObject): List<Europass.EuropassSubject.Achieved.WasDerivedFrom>{
        //TODO: values below should be populated from data file

        val title = "Individual assignment1"
        val grade = 9.00

        return listOf(Europass.EuropassSubject.Achieved.WasDerivedFrom(
            id = generateId("assessment"),
            title = title,
            grade = grade.toString(),
            assessedBy = emptyList(),
            hasPart = emptyList(),
            specifiedBy = Europass.EuropassSubject.Achieved.WasDerivedFrom.SpecifiedBy(id = generateId("assessmentSpecification"), title = title, null)
        ))
    }

    private fun getSpecification(): Europass.EuropassSubject.Achieved.WasDerivedFrom.SpecifiedBy{
        val title = "Overall grade"
        return Europass.EuropassSubject.Achieved.WasDerivedFrom.SpecifiedBy(
            id = generateId("assessmentSpecification"),
            title = title,
            gradingScheme =
                Europass.EuropassSubject.Achieved.WasDerivedFrom.SpecifiedBy.GradingScheme(
                    id = generateId("gradingScheme"),
                    title = "ECTS"
                ))
    }

    private fun getLearningSpecification(): List<Europass.EuropassSubject.Achieved.SpecifiedBy>{
        //TODO: Should be populated from file or at least should match issued VC activity description
        val title = "Transition of a City Towards Circular Economy"

        return listOf(Europass.EuropassSubject.Achieved.SpecifiedBy(
            id = generateId("learningSpecification"),
            title = title,
            eCTSCreditPoints = 6,
            learningOutcome = listOf(
                Europass.EuropassSubject.Achieved.SpecifiedBy.LearningOutcome(
                    id = generateId("learningOutcome"),
                    name = "Basics of Data and software business",
                    definition = "Student understands the basic principles of data and software business, and the special characteristics of software industry",
                    relatedESCOSkill = listOf(
                        "http://data.europa.eu/esco/skill/943d07ec-fb75-4bb5-bc07-20451a2b66e5",
                        "http://data.europa.eu/esco/skill/7dd94ad3-13d6-43fe-8b94-51fcbf67ced10",
                        "http://data.europa.eu/esco/skill/fcb2b5f4-3a64-42f2-987a-073bea986105"
                    )
                )
            ),
            learningOpportunityType = listOf("http://data.europa.eu/snb/learning-opportunity/05053c1cbe"),
            learningSetting = "http://data.europa.eu/snb/learning-setting/6fd4685715"
        ))
    }

    private fun getLearningActivities(): List<Europass.EuropassSubject.Achieved.WasInfluencedBy>{

        //TODO: Should be populated from file or at least should match issued VC activity description

        return listOf(
            Europass.EuropassSubject.Achieved.WasInfluencedBy(
                id = generateId("learningActivity"),
                title = "Individual exercise",
                definition = "The individual exercise is a written task that has two parts: 1. Choose 1 main lecture topic to focus on 2. Choose 2 guest lectures to focus on-Access the documentation in Moodle and answer the according questions in your own time (by the 5th of October) -Each task requires some self-learning, such as finding suitable examples for typical revenue streams and so on. -Return your answers in one document and upload to Moodle (note. Plagiarism tested automatically). The individual assignment accounts for 40% of the final grade.",
                specifiedBy = Europass.EuropassSubject.Achieved.WasInfluencedBy.SpecifiedBy(
                    id = generateId("learningActivitySpecification"),
                    title = "Individual exercise",
                    learningActivityType = listOf("http://data.europa.eu/snb/learning-activity/bf2e3a7bae"),
                    mode = listOf("http://data.europa.eu/snb/learning-assessment/920fbb3cbe")
                )
            ),
            Europass.EuropassSubject.Achieved.WasInfluencedBy(
                id = generateId("learningActivity"),
                title = "Course exercise",
                definition = "Minimum viable product (MVP) –Concept to explore product-market fit. Students will Identify the topic area of their MVP (Data-, platform-, product-Or service business), sketch their MVP (Use lean business model canvas), present and then report on their MVP. 3-5 people, 4 weeks remote collaboration, pitching of MVPs in a final event together with Business Tampere and Start-up centre Tribe. The course exercise accounts for 60% of the final grade.",
                specifiedBy = Europass.EuropassSubject.Achieved.WasInfluencedBy.SpecifiedBy(
                    id = generateId("learningActivitySpecification"),
                    title = "Course exercise",
                    learningActivityType = listOf("http://data.europa.eu/snb/learning-activity/bf2e3a7bae"),
                    mode = listOf("http://data.europa.eu/snb/learning-assessment/920fbb3cbe")
                )
            ),
            Europass.EuropassSubject.Achieved.WasInfluencedBy(
                id = generateId("learningActivity"),
                title = "Zoom lectures",
                definition = "includes both basic lectures on data and software business and guest lectures by companies.",
                specifiedBy = Europass.EuropassSubject.Achieved.WasInfluencedBy.SpecifiedBy(
                    id = generateId("learningActivitySpecification"),
                    title = "Zoom lectures",
                    learningActivityType = listOf("http://data.europa.eu/snb/learning-activity/bf2e3a7bae"),
                    mode = listOf("http://data.europa.eu/snb/learning-assessment/920fbb3cbe")
                )
            ),
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