package no.nav.arbeidsgiver.notifikasjon.test_produsent

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory
import java.util.*

fun <T> basedOnEnv(prod: T, other: T): T =
    when (System.getenv("NAIS_CLUSTER_NAME")) {
        "prod-gcp" -> prod
        else -> other
    }

val objectMapper = jacksonObjectMapper()
fun main() {
    val log = LoggerFactory.getLogger("main")
    log.info("hello world")

    val client = HttpClient(Apache)
    val tokenEndpoint = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")!!
    val tenantId = System.getenv("AZURE_APP_TENANT_ID")!!
    val clientId = System.getenv("AZURE_APP_CLIENT_ID")!!
    val clientSecret = System.getenv("AZURE_APP_CLIENT_SECRET")!!


    embeddedServer(Netty, port = 8080) {
        routing {
            get("ok") {
                call.respondText("i'm ok")
            }

            get("doStuff") {
                try {
                    log.info("doing stuff")

                    val accessTokenResponse = client.submitForm<String>(
                        url = tokenEndpoint,
                        formParameters = Parameters.build {
                            append("tenant", tenantId)
                            append("client_id", clientId)
                            append(
                                "scope", basedOnEnv(
                                    prod = "api://prod-gcp.fager.notifikasjon-produsent-api/.default",
                                    other = "api://dev-gcp.fager.notifikasjon-produsent-api/.default"
                                )
                            )
                            append("client_secret", clientSecret)
                            append("grant_type", "client_credentials")
                        }
                    ) {
                        method = HttpMethod.Post
                    }
                    val map: Map<String, Any> = objectMapper.readValue(accessTokenResponse)
                    val accessToken = map["access_token"]
                    val response: String = client.post("http://notifikasjon-produsent-api/api/graphql") {
                        headers {
                            append(HttpHeaders.Authorization, "Bearer $accessToken")
                            append(HttpHeaders.ContentType, "application/json")
                            append(HttpHeaders.Accept, "application/json")
                        }
                        body = objectMapper.writeValueAsString(
                            mapOf(
                                "query" to
        """
            mutation {
                softDeleteNotifikasjon(id: "dacc5fad-0aaa-4602-8360-4fdcd58f78fd") {
                    __typename
                    ... on Error {
                        feilmelding
                    }
                }
            }
        """
                            )
                        )
                    }

                    call.respond(response)
                } catch (e: RuntimeException) {
                    log.error(":'(", e)
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }
    }.start(wait = true)
}
