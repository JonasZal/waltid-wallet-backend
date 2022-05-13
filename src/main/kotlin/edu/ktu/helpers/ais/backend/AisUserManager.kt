package edu.ktu.helpers.ais.backend

import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonJson
import id.walt.WALTID_DATA_ROOT
import id.walt.webwallet.backend.auth.UserData
import id.walt.webwallet.backend.auth.UserInfo
import java.io.File

object AisUserManager {

    private val studentLoginFile: String = "$WALTID_DATA_ROOT/data/Logins/logins.json"

    fun getUserData(userEmail: String) : UserData?
    {
        val studentDataString: String = File(studentLoginFile).readText(Charsets.UTF_8)

        if (studentDataString.isEmpty()) return null

        val studentLoginList = Klaxon().parseArray<UserData>(studentDataString)
        return studentLoginList?.firstOrNull { it.email == userEmail }
    }

    fun authenticate(userData: UserInfo) : Boolean
    {
        val storedUser = getUserData(userData.email ?: return false) ?: return false
        if(userData.password == storedUser.password) return true

        return false
    }

    fun addUserData(user: UserData)
    {
        var studentLoginList: MutableList<UserData> = mutableListOf()

        if (File(studentLoginFile).exists() ){
            val studentDataString: String = File(studentLoginFile).readText(Charsets.UTF_8)

            if(studentDataString.isNotEmpty())
                studentLoginList = Klaxon().parseArray<UserData>(studentDataString)?.toMutableList() ?: mutableListOf()

            var userIdx = studentLoginList.indexOfFirst { u -> u.email == user.email }

            if(userIdx < 0)
                studentLoginList.add(user)
            else {
                studentLoginList[userIdx].password = user.password
                studentLoginList[userIdx].enrolledCourse = ""
            }
        }
        else{
            studentLoginList = mutableListOf(user)
        }

        File(studentLoginFile).writeText(Klaxon().toJsonString(studentLoginList), Charsets.UTF_8)
    }

    fun updateUserData(user: UserData)
    {
        var studentLoginList: MutableList<UserData> = mutableListOf()

        if (File(studentLoginFile).exists() ){
            val studentDataString: String = File(studentLoginFile).readText(Charsets.UTF_8)

            if(studentDataString.isNotEmpty())
                studentLoginList = Klaxon().parseArray<UserData>(studentDataString)?.toMutableList() ?: mutableListOf()

            var userIdx = studentLoginList.indexOfFirst { u -> u.email == user.email }

            if(userIdx < 0)
                throw Exception("User not found")
            else {
                studentLoginList[userIdx].enrolledCourse = user.enrolledCourse
                studentLoginList[userIdx].diplomaName = user.diplomaName
            }
        }
        else{
            throw Exception("User data file does not exists")
        }

        File(studentLoginFile).writeText(Klaxon().toJsonString(studentLoginList), Charsets.UTF_8)
    }

    fun getEnrolledModules(userInfo: UserInfo): Collection<String>{
        return listOf(getUserData(userInfo.email!!)!!.enrolledCourse ?: "")
    }

    fun enrolUserToCourse(userInfo: UserInfo, courseId: String): String {
        var studentData = getUserData(userInfo.email!!) ?: throw Exception("User not found")

        studentData.enrolledCourse = courseId

        updateUserData(studentData)

        return "User enrolled to {$courseId}"
    }
}