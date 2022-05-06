package edu.ktu.helpers.ais.backend

import id.walt.webwallet.backend.auth.UserData
import id.walt.webwallet.backend.auth.UserInfo

object AisManager {

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