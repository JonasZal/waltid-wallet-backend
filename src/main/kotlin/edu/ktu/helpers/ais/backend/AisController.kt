package edu.ktu.helpers.ais.backend

import id.walt.issuer.backend.Issuables
import id.walt.issuer.backend.IssuerManager
import id.walt.webwallet.backend.auth.JWTService
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.HttpCode
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented

object AisController {
    val routes
        get() =
            path(""){
                path("modules"){
                    get("list", documented(
                        document().operation {
                            it.summary("List all available modules")
                                .addTagsItem("KTU AIS")
                                .operationId("listModules")
                        }
                            .jsonArray<KtuModule>("200"),
                        AisController::listKtuModules
                    ))
                    get("listUserCourses", documented(
                        document().operation {
                            it.summary("List modules enrolled by student")
                                .addTagsItem("KTU AIS")
                                .operationId("listEnrolledModules")
                        }
                            .jsonArray<KtuModule>("200"),
                        AisController::listEnrolledModules
                    ))
                    post("enrol", documented(
                        document().operation {
                            it.summary("Enrol student to the KTU course")
                                .addTagsItem("KTU AIS")
                                .operationId("enrolToCourse")
                        }
                            .formParam<String>("courseId")
                            .result<String>("200"),
                        AisController::enrolUserToCourse
                    ))
                }
                path("student"){
                    get("getStudentData", documented(
                        document().operation {
                            it.summary("Get student information from the KTU AIS")
                                .addTagsItem("KTU AIS")
                                .operationId("getStudentData")
                        }
                            .result<String>("200"),
                        AisController::getStudentData
                    ))
                    post("onboardRequest", documented(
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

    private fun listKtuModules(ctx: Context){
        ctx.json(AisManager.getKtuModulesList())
    }

    private fun requestOnboard(ctx: Context) {
        val userId = ctx.formParam("userId") ?: throw BadRequestResponse("No user id specified")
        val userPassword = ctx.formParam("userPassword") ?: throw BadRequestResponse("No user password specified")
        val userName = ctx.formParam("userName") ?: throw BadRequestResponse("No user name specified")
        val userFamilyName = ctx.formParam("userFamilyName") ?: throw BadRequestResponse("No user family name specified")

        ctx.result(
            "[ ${AisManager.createNewUser(userId, userPassword, userName, userFamilyName) } ]"
        )
    }

    private fun listEnrolledModules(ctx: Context) {
        val userInfo = JWTService.getUserInfo(ctx)
        if(userInfo == null) {
            ctx.status(HttpCode.UNAUTHORIZED)
            return
        }

        ctx.json(AisManager.getEnrolledModules(userInfo))
    }

    private fun enrolUserToCourse(ctx: Context) {
        val userInfo = JWTService.getUserInfo(ctx)
        if(userInfo == null) {
            ctx.status(HttpCode.UNAUTHORIZED)
            return
        }

        val courseId = ctx.formParam("courseId") ?: throw BadRequestResponse("No course id specified")

        ctx.result(
            "[ ${AisManager.enrolUserToCourse(userInfo, courseId) } ]")
    }

    private fun getStudentData(ctx: Context) {
        val userInfo = JWTService.getUserInfo(ctx)
        if(userInfo == null) {
            ctx.status(HttpCode.UNAUTHORIZED)
            return
        }

        ctx.json(
            AisUserManager.getUserData(userInfo.email!!) ?: throw Exception("User not found"))
    }
}