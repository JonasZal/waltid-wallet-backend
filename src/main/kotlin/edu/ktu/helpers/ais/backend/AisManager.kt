package edu.ktu.helpers.ais.backend

import id.walt.webwallet.backend.auth.UserData

object AisManager {

    fun getKtuModulesList(): Collection<KtuModule>{

        //TODO: Should take from file instead of this place

        var aKtuModule = KtuModule()

        aKtuModule.code = "A00A000"
        aKtuModule.description = "Description of a test module"

        return listOf<KtuModule>().plus(aKtuModule)
    }

    fun createNewUser(userId : String, userPassword : String, userName : String, userFamilyName : String): String {

        return try {
            AisUserManager.addUserData( UserData(userId, userPassword, "ktudiplomas.xml", userName, userFamilyName) )
            "User created"
        } catch (e: Exception) {
            "Can't create user. Error: " + e.message
        }
    }

}