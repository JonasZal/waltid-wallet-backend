package edu.ktu.helpers.ais.backend

object AisManager {

    fun getKtuModulesList(): Collection<KtuModule>{

        //TODO: Should take from file instead of this place

        var aKtuModule = KtuModule()

        aKtuModule.code = "A00A000"
        aKtuModule.description = "Description of a test module"

        return listOf<KtuModule>().plus(aKtuModule)
    }

    fun createNewUser(userId : String, userPassword : String): String {

        //TODO: Creation of user in the Login file is needed
        return "User created"
    }

}