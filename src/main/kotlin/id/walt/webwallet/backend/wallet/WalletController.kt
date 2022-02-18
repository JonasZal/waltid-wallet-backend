package id.walt.webwallet.backend.wallet

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import id.walt.crypto.KeyAlgorithm
import id.walt.custodian.Custodian
import id.walt.model.DidMethod
import id.walt.model.dif.DescriptorMapping
import id.walt.model.dif.PresentationSubmission
import id.walt.model.oidc.*
import id.walt.rest.custodian.CustodianController
import id.walt.services.context.ContextManager
import id.walt.services.did.DidService
import id.walt.services.essif.EssifClient
import id.walt.services.essif.didebsi.DidEbsiService
import id.walt.services.key.KeyService
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.model.toCredential
import id.walt.webwallet.backend.auth.JWTService
import id.walt.webwallet.backend.auth.UserRole
import id.walt.webwallet.backend.config.WalletConfig
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.HttpCode
import io.javalin.http.HttpResponseException
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

object WalletController {
    val routes
        get() = path("wallet") {
            path("did") {
                // list dids
                path("list") {
                    get(
                        documented(document().operation {
                            it.summary("List my DIDs").operationId("listDids").addTagsItem("Wallet")
                        }
                            .jsonArray<String>("200"),
                            WalletController::listDids
                        ), UserRole.AUTHORIZED
                    )
                }
                // create new DID
                path("create") {
                    post(
                        documented(document().operation {
                            it.summary("Create new DID")
                                .description("Creates and registers a DID. Currently the DID methos: key, web and ebsi are supported. For EBSI: a  bearer token is required.")
                                .operationId("createDid").addTagsItem("Wallet")
                        }
                            .body<DidCreationRequest>()
                            .result<String>("200"),
                            WalletController::createDid
                        ), UserRole.AUTHORIZED
                    )
                }
            }
            path("credentials") {
                get(
                    "list",
                    documented(CustodianController.listCredentialIdsDocs(), CustodianController::listCredentials),
                    UserRole.AUTHORIZED
                )
            }
            path("keys") {
                get(
                    "list",
                    documented(CustodianController.listKeysDocs(), CustodianController::listKeys),
                    UserRole.AUTHORIZED
                )
            }
            path("siopv2") {
                // called from EXTERNAL verifier
                get("initPresentation", documented(
                    document().operation {
                        it.summary("Parse siop request from URL query parameters")
                            .operationId("initPresentation")
                            .addTagsItem("siop")
                    }
                        .queryParam<String>("response_type")
                        .queryParam<String>("client_id")
                        .queryParam<String>("redirect_uri")
                        .queryParam<String>("scope")
                        .queryParam<String>("state")
                        .queryParam<String>("nonce")
                        .queryParam<String>("registration")
                        .queryParam<Long>("exp")
                        .queryParam<Long>("iat")
                        .queryParam<String>("claims")
                        .result<String>("302"),
                    WalletController::initCredentialPresentation
                ), UserRole.UNAUTHORIZED)
                // called by wallet UI
                get("continuePresentation", documented(
                    document().operation {
                        it.summary("Continue presentation requested by verifer")
                            .operationId("continuePresentation")
                            .addTagsItem("siop")
                    }
                        .queryParam<String>("sessionId")
                        .queryParam<String>("did")
                        .json<CredentialPresentationSession>("200"),
                    WalletController::continuePresentation
                ), UserRole.AUTHORIZED)
                // called by wallet UI
                post("fulfillPresentation", documented(
                    document().operation {
                        it.summary("Fullfil credentials presentation with selected credentials")
                            .operationId("fulfillPresentation")
                            .addTagsItem("siop")
                    }
                        .queryParam<String>("sessionId")
                        .body<List<PresentableCredential>>()
                        .json<PresentationResponse>("200"),
                    WalletController::fulfillPresentation
                ), UserRole.AUTHORIZED)
                // issuance // CUSTOM // called from EXTERNAL issuer
                get("initPassiveIssuance", documented(
                    document().operation {
                        it.summary("Initialize passive credential issuance through SIOP presentation flow")
                            .operationId("initPassiveIssuance")
                            .addTagsItem("siop")
                    }
                        .queryParam<String>("response_type")
                        .queryParam<String>("client_id")
                        .queryParam<String>("redirect_uri")
                        .queryParam<String>("scope")
                        .queryParam<String>("state")
                        .queryParam<String>("nonce")
                        .queryParam<String>("registration")
                        .queryParam<Long>("exp")
                        .queryParam<Long>("iat")
                        .queryParam<String>("claims")
                        .queryParam<String>("subject_did")
                        .result<String>("302"),
                    WalletController::initPassiveIssuance
                ), UserRole.UNAUTHORIZED)
                // called by wallet UI
                post("fulfillPassiveIssuance", documented(
                    document().operation {
                        it.summary("Post credentials required by issuer, to issue credentials")
                            .operationId("fulfillPassiveIssuance")
                            .addTagsItem("siop")
                    }
                        .queryParam<String>("sessionId")
                        .body<List<PresentableCredential>>()
                        .json<String>("200"), // issuance session ID
                    WalletController::fulfillPassiveIssuance
                ), UserRole.AUTHORIZED)
                // issuance // OIDC
                path("issuer") {
                    get(
                        "list", documented(
                            document().operation {
                                it.summary("List known credential issuers").addTagsItem("siop")
                                    .operationId("listIssuers")
                            },
                            WalletController::listIssuers
                        ),
                        UserRole.UNAUTHORIZED
                    )
                    get("metadata", documented(
                        document().operation {
                            it.summary("get issuer meta data").addTagsItem("siop").operationId("issuerMeta")
                        }
                            .queryParam<String>("issuerId"),
                        WalletController::issuerMeta),
                        UserRole.UNAUTHORIZED)
                }
                post("initIssuance", documented(
                    document().operation {
                        it.summary("Initialize credential issuance from selected issuer").addTagsItem("siop")
                            .operationId("initIssuance")
                    }
                        .body<CredentialIssuanceRequest>()
                        .result<String>("200"),
                    WalletController::initIssuance
                ), UserRole.AUTHORIZED)
                // called from EXTERNAL issuer / user-agent
                get("finalizeIssuance", documented(
                    document().operation {
                        it.summary("Finalize credential issuance").addTagsItem("siop").operationId("finalizeIssuance")
                    }
                        .queryParam<String>("code")
                        .queryParam<String>("state")
                        .result<String>("302"),
                    WalletController::finalizeIssuance
                ), UserRole.UNAUTHORIZED)
                // called by wallet UI
                get("issuanceSessionInfo", documented(
                    document().operation {
                        it.summary("Get issuance session info, including issued credentials").addTagsItem("siop")
                            .operationId("issuanceSessionInfo")
                    }
                        .queryParam<String>("sessionId")
                        .json<CredentialIssuanceSession>("200"),
                    WalletController::getIssuanceSessionInfo
                ), UserRole.AUTHORIZED)
            }
        }

    fun listDids(ctx: Context) {
        ctx.json(DidService.listDids())
    }

    fun createDid(ctx: Context) {
        val didCreationReq = ctx.bodyAsClass<DidCreationRequest>()

        val key = didCreationReq.keyId?.let { KeyService.getService().load(it).keyId }
            ?: KeyService.getService().listKeys()
                .firstOrNull { k -> k.algorithm == KeyAlgorithm.ECDSA_Secp256k1 }?.keyId
            ?: KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)

        when (didCreationReq.method) {
            DidMethod.ebsi -> {
                if (didCreationReq.didEbsiBearerToken.isNullOrEmpty()) {
                    ctx.status(HttpCode.BAD_REQUEST)
                        .result("ebsiBearerToken form parameter is required for EBSI DID registration.")
                    return
                }

                val did = DidService.create(didCreationReq.method, key.id)
                EssifClient.onboard(did, didCreationReq.didEbsiBearerToken)
                EssifClient.authApi(did)
                DidEbsiService.getService().registerDid(did, did)
                ctx.result(did)
            }
            DidMethod.web -> {
                val didStr = DidService.create(
                    didCreationReq.method,
                    key.id,
                    DidService.DidWebOptions(
                        domain = URI.create(didCreationReq.didWebDomain?.run {
                            when {
                                startsWith("https://", true) -> this
                                else -> "https://${this}"
                            }
                        } ?: WalletConfig.config.walletApiUrl).authority,
                        path = didCreationReq.didWebDomain?.run { didCreationReq.didWebPath ?: "" }
                            ?: "api/did-registry/${key.id}"
                    )
                )
                val didDoc = DidService.load(didStr)
                // !! Implicit USER CONTEXT is LOST after this statement !!
                ContextManager.runWith(DidWebRegistryController.didRegistryContext) {
                    DidService.storeDid(didStr, didDoc.encodePretty())
                }
                ctx.result(didStr)
            }
            DidMethod.key -> {
                ctx.result(
                    DidService.create(
                        didCreationReq.method,
                        key.id
                    )
                )
            }
        }
    }

    fun initCredentialPresentation(ctx: Context) {
        val req = SIOPv2Request.fromHttpContext(ctx)
        val session = CredentialPresentationManager.initCredentialPresentation(req, passiveIssuance = false)
        ctx.status(HttpCode.FOUND).header("Location", "${WalletConfig.config.walletUiUrl}/CredentialRequest?sessionId=${session.id}")
    }

    fun initPassiveIssuance(ctx: Context) {
        val req = SIOPv2Request.fromHttpContext(ctx)
        val session = CredentialPresentationManager.initCredentialPresentation(req, passiveIssuance = true)
        ctx.status(HttpCode.FOUND).header("Location", "${WalletConfig.config.walletUiUrl}/CredentialRequest?sessionId=${session.id}")
    }

    fun continuePresentation(ctx: Context) {
        val sessionId = ctx.queryParam("sessionId") ?: throw BadRequestResponse("sessionId not specified")
        val did = ctx.queryParam("did") ?: throw BadRequestResponse("did not specified")
        ctx.json(CredentialPresentationManager.continueCredentialPresentationFor(sessionId, did))
    }

    fun fulfillPresentation(ctx: Context) {
        val sessionId = ctx.queryParam("sessionId") ?: throw BadRequestResponse("sessionId not specified")
        val selectedCredentials = ctx.body().let { klaxon.parseArray<PresentableCredential>(it) } ?: throw BadRequestResponse("No selected credentials given")

        ctx.json(CredentialPresentationManager.fulfillPresentation(sessionId, selectedCredentials).let { PresentationResponse.fromSiopResponse(it) })
    }

    fun fulfillPassiveIssuance(ctx: Context) {
        val sessionId = ctx.queryParam("sessionId") ?: throw BadRequestResponse("sessionId not specified")
        val selectedCredentials = ctx.body().let { klaxon.parseArray<PresentableCredential>(it) } ?: throw BadRequestResponse("No selected credentials given")
        val issuanceSession = CredentialPresentationManager.fulfillPassiveIssuance(sessionId, selectedCredentials, JWTService.getUserInfo(ctx)!!)
        ctx.result(issuanceSession.id)
    }

    fun listIssuers(ctx: Context) {
        ctx.json(WalletConfig.config.issuers.values)
    }

    fun issuerMeta(ctx: Context) {
        val metadata = ctx.queryParam("issuerId")?.let { CredentialIssuanceManager.ciSvc(it) }?.metadata
        if (metadata != null)
            ctx.json(metadata.toJSONObject())
        else
            ctx.status(HttpCode.NOT_FOUND)
    }

    fun initIssuance(ctx: Context) {
        val issuance = ctx.bodyAsClass<CredentialIssuanceRequest>()
        val location = CredentialIssuanceManager.initIssuance(issuance, JWTService.getUserInfo(ctx)!!)
        if (location != null) {
            ctx.result(location.toString())
        } else {
            ctx.status(HttpCode.INTERNAL_SERVER_ERROR)
        }
    }

    fun finalizeIssuance(ctx: Context) {
        val state = ctx.queryParam("state")
        val code = ctx.queryParam("code")
        if (state.isNullOrEmpty() || code.isNullOrEmpty()) {
            ctx.status(HttpCode.BAD_REQUEST).result("No state or authorization code given")
            return
        }
        val issuance = CredentialIssuanceManager.finalizeIssuance(state, code)
        if (issuance?.credentials != null) {
            ctx.status(HttpCode.FOUND)
                .header("Location", "${WalletConfig.config.walletUiUrl}/ReceiveCredential?sessionId=${issuance.id}")
        } else {
            ctx.status(HttpCode.FOUND).header("Location", "${WalletConfig.config.walletUiUrl}/IssuanceError")
        }
    }

    fun getIssuanceSessionInfo(ctx: Context) {
        val sessionId = ctx.queryParam("sessionId")
        val issuanceSession = sessionId?.let { CredentialIssuanceManager.getSession(it) }
        if (issuanceSession == null) {
            ctx.status(HttpCode.BAD_REQUEST).result("Invalid or expired session id given")
            return
        }
        ctx.json(issuanceSession)
    }
}
