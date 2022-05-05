package edu.ktu.helpers.ais.backend

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented

object AisController {
    val routes
        get() =
            path(""){
                path("getModules"){
                    get("list", documented(
                        document().operation {
                            it.summary("List all available modules")
                                .addTagsItem("KTU AIS")
                                .operationId("listModules")
                        }
                            .jsonArray<KtuModule>("200"),
                        AisController::listKtuModules
                    ))
                }
                path("onboardStudent"){
                    post("request", documented(
                        document().operation {
                            it.summary("Onboard new student to the KTU AIS")
                                .addTagsItem("KTU AIS")
                                .operationId("requestOnboard")
                        }
                            .formParam<String>("userId")
                            .formParam<String>("userPassword")
                            .formParam<String>("userName")
                            .formParam<String>("userFamilyName")
                            .result<String>("200"),
                        AisController::requestOnboard
                    ))

                }
            }

    fun listKtuModules(ctx: Context){
        ctx.json(AisManager.getKtuModulesList())
    }

    fun requestOnboard(ctx: Context) {
        val userId = ctx.formParam("userId") ?: throw BadRequestResponse("No user id specified")
        val userPassword = ctx.formParam("userPassword") ?: throw BadRequestResponse("No user password specified")
        val userName = ctx.formParam("userName") ?: throw BadRequestResponse("No user name specified")
        val userFamilyName = ctx.formParam("userFamilyName") ?: throw BadRequestResponse("No user family name specified")

        ctx.result(
            "[ ${AisManager.createNewUser(userId, userPassword, userName, userFamilyName) } ]"
        )
    }

}