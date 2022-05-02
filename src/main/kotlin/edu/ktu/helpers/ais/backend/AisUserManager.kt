package edu.ktu.helpers.ais.backend

import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonJson
import id.walt.WALTID_DATA_ROOT
import id.walt.webwallet.backend.auth.UserData
import java.io.File

object AisUserManager {

    private val studentLoginFile: String = "$WALTID_DATA_ROOT/data/Logins/logins.json"

    fun getUserData(userEmail: String) : UserData?
    {
        val studentDataString: String = File(studentLoginFile).readText(Charsets.UTF_8)

        if (studentDataString.isEmpty()) return null

        val studentLoginList = Klaxon().parse<List<UserData>>(studentDataString)
        return studentLoginList?.firstOrNull { it.email == userEmail }
    }

    fun addUserData(user: UserData)
    {
        var studentLoginList: List<UserData>?

        if (File(studentLoginFile).exists() ){
            val studentDataString: String = File(studentLoginFile).readText(Charsets.UTF_8)

            try
            {
                studentLoginList = Klaxon().parseArray(studentDataString)

            } catch (e: Exception)
            {
                val login  = Klaxon().parse<UserData>(studentDataString) ?: throw Exception("Unable to read login data file")
                studentLoginList = listOf(login)
            }

            studentLoginList = studentLoginList?.plus(user)
        }
        else{
            studentLoginList = listOf(user)
        }

        File(studentLoginFile).writeText(Klaxon().toJsonString(studentLoginList), Charsets.UTF_8)
    }
}