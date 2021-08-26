package no.nav.arbeidsgiver.notifikasjon.test_produsent
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
                log.info("doing stuff")

                val accessToken = client.submitForm<String>(
                    url = tokenEndpoint,
                    formParameters = Parameters.build {
                        append("tenant", tenantId)
                        append("client_id", clientId)
                        append("scope", "api://dev-gcp.fager.notifikasjon-produsent-api/.default\n")
                        append("client_secret", clientSecret)
                        append("grant_type", "client_credentials")
                    }
                ) {
                    method = HttpMethod.Post
                }

                call.respondText(accessToken)
            }
        }
    }.start(wait = true)
}
