package id.walt.webwallet.backend.auth

import edu.ktu.helpers.ais.backend.AisUserManager
import id.walt.model.DidMethod
import id.walt.services.context.ContextManager
import id.walt.services.did.DidService
import id.walt.webwallet.backend.context.WalletContextManager
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented
import okhttp3.internal.toImmutableList
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.file.Paths

object AuthController {
    val routes
        get() = path("auth") {
            path("login") {
                post(documented(document().operation {
                    it.summary("Login")
                        .operationId("login")
                        .addTagsItem("Authentication")
                }
                    .body<UserInfo> { it.description("Login info") }
                    .json<UserInfo>("200"),
                    AuthController::login), UserRole.UNAUTHORIZED)
            }
            path("userInfo") {
                get(
                    documented(document().operation {
                        it.summary("Get current user info")
                            .operationId("userInfo")
                            .addTagsItem("Authentication")
                    }
                        .json<UserInfo>("200"),
                        AuthController::userInfo), UserRole.AUTHORIZED)
            }
        }

    fun login(ctx: Context) {
        val userInfo = ctx.bodyAsClass(UserInfo::class.java)

        if(AisUserManager.authenticate(userInfo))
        {
            println("User ${userInfo.id} authenticated")

            ContextManager.runWith(WalletContextManager.getUserContext(userInfo)) {
                if(DidService.listDids().isEmpty()) {
                    DidService.create(DidMethod.key)
                }
            }

            ctx.json(UserInfo(userInfo.id).apply {
                token = JWTService.toJWT(userInfo)

            }   )

            return
        }

        ctx.status(401).result("Unauthorized")

        /*
        val path = Paths.get("").toAbsolutePath().toString() + "/data/Logins/logins.txt"
        var userDataList = readUserData(path)
        // TODO: verify login credentials!!
        var authentificated = false
        for(user in userDataList){
            if(user.email == userInfo.email && user.password == userInfo.password){
//                userInfo.diplomaFile = user.diplomaName
                authentificated = true
                println("duomenys prisijungimo geri")
                ContextManager.runWith(WalletContextManager.getUserContext(userInfo)) {
                    if(DidService.listDids().isEmpty()) {
                        DidService.create(DidMethod.key)
                    }
                }

                ctx.json(UserInfo(userInfo.id).apply {
                    token = JWTService.toJWT(userInfo)

                }   )


                //var jsonString = "{${UserInfo(userInfo.id).apply { token = JWTService.toJWT(userInfo) } }, diplomaFile: ${user.diplomaName}}"
//                var jsonString = "{ token : \"${JWTService.toJWT(userInfo)}\", diplomaFile: \"${user.diplomaName}\" }"
//
//                var jsonObject = JSONObject(jsonString)
//                println(jsonString)
//                ctx.json(jsonObject)
                return

            }
        }
//        ContextManager.runWith(WalletContextManager.getUserContext(userInfo)) {
//            if(DidService.listDids().isEmpty()) {
//                DidService.create(DidMethod.key)
//            }
//        }
//        ctx.json(UserInfo(userInfo.id).apply {
//            token = JWTService.toJWT(userInfo)
//        })
        ctx.status(401).result("Unauthorized")*/
    }

    /*
    fun readUserData(path : String) : List<UserData>{

        var userDataList = mutableListOf<UserData>()

        val xmlIdData = File(path).readText(charset = Charsets.UTF_8)

        val file = File(path)
        try {
            BufferedReader(FileReader(file)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    println(line)
                    var lines = line!!.split(" ")
                    println("lines:")
                    println(lines)
                    var user = UserData(lines[0],lines[1],lines[2])
                    println("user:")
                    println(user)
                    userDataList.add(user)

                }
                println("userDataList")
                println(userDataList)

            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return userDataList.toImmutableList()
    }

     */
    fun userInfo(ctx: Context) {
        println("ctx: ")
        println(ctx.body())
        println("ctx json: ")
        println(ctx.body())
        ctx.json(JWTService.getUserInfo(ctx)!!)
    }
}
