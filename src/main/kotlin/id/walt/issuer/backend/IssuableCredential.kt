package id.walt.issuer.backend

import com.nimbusds.oauth2.sdk.id.Identifier
import id.walt.model.oidc.CredentialClaim
import id.walt.vclib.model.AbstractVerifiableCredential
import id.walt.vclib.model.CredentialSubject
import id.walt.vclib.registry.VcTypeRegistry
import id.walt.vclib.templates.VcTemplateManager
import id.walt.webwallet.backend.auth.UserInfo
import edu.ktu.helpers.StudentCredentialsGenerator
import id.walt.vclib.credentials.Europass
import id.walt.vclib.credentials.VerifiableDiploma
import id.walt.vclib.credentials.VerifiableId

/*
//walt.id original class
data class IssuableCredential (
  val schemaId: String,
  val type: String,
  val credentialData: Map<String, Any>? = null
) {
  companion object {
    fun fromTemplateId(templateId: String): IssuableCredential {
      val tmpl = VcTemplateManager.loadTemplate(templateId)
      return IssuableCredential(
        tmpl!!.credentialSchema!!.id,
        tmpl.type.last(),
        mapOf(Pair("credentialSubject", (tmpl as AbstractVerifiableCredential<out CredentialSubject>).credentialSubject!!)))
    }
  }
}
*/

data class IssuableCredential (
  val schemaId: String,
  val type: String,
  val credentialData: Map<String, Any>? = null
) {
  companion object {
    fun fromTemplateId(templateId: String, user : UserInfo): IssuableCredential {
      var crd = VcTemplateManager.loadTemplate(templateId)

      println("template id:")
      println(templateId)
      println("template")

      if (templateId == "VerifiableId") {
        crd = crd as VerifiableId
        println("credential subject:")
        println(crd.credentialSubject)

        crd.credentialSubject = generateStudentId(crd)

        println("credential subject po pakeitimo :")
        println(crd.credentialSubject)
      }
      else if (templateId == "VerifiableDiploma"){
        crd = crd as VerifiableDiploma
        println("credential subject:")
        println(crd.credentialSubject)
        crd.credentialSubject
        crd.credentialSubject = generateDiploma(crd,user)

        println("credential subject po pakeitimo :")
        println(crd.credentialSubject)
      }
      else if (templateId == " Europass"){
        crd = crd as Europass
        println("europass subject:")
        println("credential subject:")
        println(crd.credentialSubject)
        crd.credentialSubject
        crd.credentialSubject = generateDiplomaEuropass(crd,user)

        println("credential subject po pakeitimo :")
        println(crd.credentialSubject)
      }
      return IssuableCredential(
        crd!!.credentialSchema!!.id,
        crd.type.last(),
        mapOf(
          Pair(
            "credentialSubject",
            (crd as AbstractVerifiableCredential<out CredentialSubject>).credentialSubject!!
          )
        )
      )
    }
    fun generateStudentId(cred: VerifiableId): VerifiableId.VerifiableIdSubject? {

      var changedCred = cred.credentialSubject
      changedCred?.currentAddress = listOf("SomeStreet 28 g.")
      changedCred?.dateOfBirth = "1987-04-05"
      changedCred?.gender = "Male"
      changedCred?.firstName = "fdhgfghgfh"
      changedCred?.familyName = "Jonas"
      changedCred?.placeOfBirth = "Lietuva"
      changedCred?.nameAndFamilyNameAtBirth = "Litva"

      return changedCred

    }
    fun generateDiplomaEuropass(cred: Europass, user : UserInfo) : Europass.EuropassSubject?{
      val jsonDiploma = StudentCredentialsGenerator.getStudentDiplomaXml(user)
      val diplomas = jsonDiploma?.getJSONObject("duomenys")?.getJSONObject("pazymejimas")

      var changedCred = cred.credentialSubject


      changedCred?.dateOfBirth = jsonDiploma?.getJSONObject("duomenys")?.getJSONObject("pazymejimas")?.getJSONObject("asmuo")?.getString("gimimoData")
      changedCred?.familyName = diplomas?.getJSONObject("asmuo")?.getString("pavarde")
      changedCred?.givenNames = diplomas?.getJSONObject("asmuo")?.getString("vardas")
      changedCred?.identifier =  Europass.EuropassSubject.Identifier(
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
      changedCred?.learningAchievement = learningAchievement

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
      changedCred?.awardingOpportunity = awardingOpportunity

      val grades = diplomas?.getJSONArray("dalykas")!!.toString()
      changedCred?.learningSpecification =  Europass.EuropassSubject.LearningSpecification(
        "gg",
        listOf(grades),
        5,
        2,
        listOf("berebe")
      )
      return changedCred

    }
    fun generateDiploma(cred: VerifiableDiploma, user : UserInfo): VerifiableDiploma.VerifiableDiplomaSubject? {

      println("userInfo")
      println(user)
      println("diplomaFile")

      val jsonDiploma = StudentCredentialsGenerator.getStudentDiplomaXml(user)
      val diplomas = jsonDiploma?.getJSONObject("duomenys")?.getJSONObject("pazymejimas")

      var changedCred = cred.credentialSubject

      changedCred?.dateOfBirth = jsonDiploma?.getJSONObject("duomenys")?.getJSONObject("pazymejimas")?.getJSONObject("asmuo")?.getString("gimimoData")
      changedCred?.familyName = diplomas?.getJSONObject("asmuo")?.getString("pavarde")
      changedCred?.givenNames = diplomas?.getJSONObject("asmuo")?.getString("vardas")
      changedCred?.identifier = diplomas?.getJSONObject("isdavusiInstitucija")?.getString("institucijosPavadinimas")
      val grades = diplomas?.getJSONArray("dalykas")!!.toString()
      var gradingScheme = VerifiableDiploma.VerifiableDiplomaSubject.GradingScheme(
        id = diplomas?.getJSONArray("dalykas")?.getJSONObject(0)!!.getInt("kodas").toString(),
        title = diplomas?.getJSONArray("dalykas")?.getJSONObject(0)!!.getString("pavadinimas"),
        description = grades
      )
      changedCred?.gradingScheme = gradingScheme

      var learningAchievement = VerifiableDiploma.VerifiableDiplomaSubject.LearningAchievement(
        diplomas?.getJSONObject("priedas").getString("numeris").toString(),
        diplomas?.getJSONObject("priedas").getString("tipas"),
        diplomas?.getJSONObject("priedas").getString("studijuProgramosReikalavimai"),
        listOf(
          diplomas?.getJSONObject("priedas").getString("kvalifikacijosGalimybes"),
          diplomas?.getJSONObject("priedas").getString("studijuProgramosReikalavimai")
        )

      )
      changedCred?.learningAchievement = learningAchievement

      var awardingOpportunity =  VerifiableDiploma.VerifiableDiplomaSubject.AwardingOpportunity(
        diplomas?.getInt("blankoKodas").toString(),
        diplomas?.getString("programosPavadinimas"),
        VerifiableDiploma.VerifiableDiplomaSubject.AwardingOpportunity.AwardingBody(
          id = diplomas?.getJSONObject("isdavusiInstitucija").getInt("institucijosKodas").toString(),
          eidasLegalIdentifier = null,
          registration = diplomas?.getJSONObject("isdavusiInstitucija").getString("institucijosPavadinimas"),
          preferredName = diplomas?.getJSONObject("isdavusiInstitucija").getString("institucijosVadovas")
        )
      )
      changedCred?.awardingOpportunity = awardingOpportunity
      return changedCred

    }
  }
}

/*
//walt.id original class
data class Issuables (
  val credentials: List<IssuableCredential>
    )
{
  val credentialsByType
    get() = credentials.associateBy { it.type }
  val credentialsBySchemaId
    get() = credentials.associateBy { it.schemaId }

  companion object {
    fun fromCredentialClaims(credentialClaims: List<CredentialClaim>): Issuables {
      return Issuables(
        credentials = credentialClaims.flatMap { claim -> VcTypeRegistry.getTypesWithTemplate().values
          .map { it.metadata.template!!() }
          .filter { it.credentialSchema != null }
          .filter { (isSchema(claim.type!!) && it.credentialSchema!!.id == claim.type) ||
                    (!isSchema(claim.type!!) && it.type.last() == claim.type)
          }
          .map { it.type.last() }
        } .map { IssuableCredential.fromTemplateId(it) }
      )
    }
  }
}
*/

data class Issuables(
  val credentials: List<IssuableCredential>
) {
  val credentialsByType
    get() = credentials.associateBy { it.type }
  val credentialsBySchemaId
    get() = credentials.associateBy { it.schemaId }

  companion object {
    fun fromCredentialClaims(credentialClaims: List<CredentialClaim>, user : UserInfo): Issuables {
      return Issuables(
        credentials = credentialClaims.flatMap { claim ->
          VcTypeRegistry.getTypesWithTemplate().values
            .map { it.metadata.template!!() }
            .filter { it.credentialSchema != null }
            .filter {
              (isSchema(claim.type!!) && it.credentialSchema!!.id == claim.type) ||
                      (!isSchema(claim.type!!) && it.type.last() == claim.type)
            }
            .map { it.type.last() }
        }.map { IssuableCredential.fromTemplateId(it,user) }
      )
    }
  }
}

data class NonceResponse(
  val p_nonce: String,
  val expires_in: String? = null
)
