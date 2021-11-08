package no.nav.arbeidsgiver.notifikasjon.test_produsent

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

val objectMapper = jacksonObjectMapper()

val httpClient = HttpClient(Apache)


suspend fun ApplicationCall.respondHtml(html: String) {
    this.respondText(
        contentType = ContentType.Text.Html,
        text = html,
    )
}

fun main() {
    val log = LoggerFactory.getLogger("main")

    embeddedServer(Netty, port = 8080) {
        routing {
            get("ok") {
                call.respond("ok")
            }

            get {
                log.info("hello, stranger")

                call.respondHtml(sendPage)
            }

            post("/submit") {
                try {
                    val formParameters = call.receiveParameters()
                    val vnr = formParameters["vnr"].toString()
                    val tekst = formParameters["tekst"].toString()
                    val url = formParameters["url"].toString()
                    val type = formParameters["type"].toString()

                    log.info("trying to send test notification: '$vnr' '$tekst' '$url' '$type'")
                    val utfall = sendNotifikasjon(vnr, tekst, url, type)

                    call.respondHtml(okPage(utfall))
                } catch (e: Exception) {
                    log.error("unexpected exception", e)
                    call.respondHtml(errorPage)
                }
            }
        }
    }.start(wait = true)
}

suspend fun sendNotifikasjon(vnr: String, tekst: String, url: String, type: String): String {
    val variables = mapOf(
        "vnr" to vnr,
        "tekst" to tekst,
        "url" to url,
    )
    return when (type) {
        "beskjed" -> executeGraphql(nyBeskjed, variables)
        "oppgave" -> executeGraphql(nyOppgave, variables)
        else -> "ukjent type '$type' :("
    }
}

suspend fun executeGraphql(query: String, variables: Map<String, String>): String {
    val accessToken = getAccessToken()
    val response: HttpResponse =  httpClient.post("http://notifikasjon-produsent-api/api/graphql") {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
        header(HttpHeaders.ContentType, "application/json")
        header(HttpHeaders.Accept, "application/json")
        body = objectMapper.writeValueAsString(
            mapOf(
                "query" to query,
                "variables" to variables,
            )
        )
    }

    return objectMapper.writeValueAsString(
        mapOf(
            "status" to response.status,
            "body" to response.readText(),
        )
    )
}

suspend fun getAccessToken(): String {
    val tokenEndpoint = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")!!
    val tenantId = System.getenv("AZURE_APP_TENANT_ID")!!
    val clientId = System.getenv("AZURE_APP_CLIENT_ID")!!
    val clientSecret = System.getenv("AZURE_APP_CLIENT_SECRET")!!

    val accessTokenResponse = httpClient.submitForm<String>(
        url = tokenEndpoint,
        formParameters = Parameters.build {
            set("tenant", tenantId)
            set("client_id", clientId)
            set( "scope" , "api://dev-gcp.fager.notifikasjon-produsent-api/.default")
            set("client_secret", clientSecret)
            set("grant_type", "client_credentials")
        }
    ) {
        method = HttpMethod.Post
    }
    val map: Map<String, Any> = objectMapper.readValue(accessTokenResponse)
    return map["access_token"] as String
}

// language=HTML
const val sendPage: String =
    """
        <html>
            <head>
                <title>Test produsent</title>
            </head>
            <body>
                <form method="post" action="/submit">
                    <label for="vnr">Virksomhetsnummer:</label>
                    <input id="vnr" name="vnr" type="text" value="910825631"><br>
                    
                    <label for="tekst">Tekst:</label>
                    <input id="tekst" name="tekst" type="text" value="Dette er en test-melding"><br>
                    
                    <label for="url">url:</label>
                    <input id="url" name="url" type="text" value="https://dev.nav.no"><br>
                    
                    
                    Notifikasjonstype:<br>
                    <input type="radio" id="beskjed" name="type" value="beskjed" checked>
                    <label for="beskjed">beskjed</label><br>
                    <input type="radio" id="oppgave" name="type" value="oppgave">
                    <label for="oppgave">oppgave</label><br>
                    <input type="submit" value="send">
                </form>
            </body>
        </html>
    """

fun okPage(utfall: String): String =
    // language=HTML
    """
        <html>
            <head>
                <title>Notifikasjon forsøkt sendt</title>
            </head>
            <body>
                <h1>Svar fra produsent-api:</h1>
                $utfall
                <br>
                <a href="/">lag ny notifikasjon</a>
            </body>
        </html>
        
    """

val nyOppgave: String =
    // language=GraphQL
    """
        mutation NyOppgave(${'$'}vnr: String! ${'$'}tekst: String! ${'$'}url: String) {
            nyOppgave(
                nyOppgave: {
                    metadata: {
                        eksternId: "${java.util.UUID.randomUUID()}"
                    }
                    mottaker: {
                        altinn: {
                            serviceCode: "4936"
                            serviceEdition: "1"
                            virksomhetsnummer: ${'$'}vnr
                        }
                    }
                    notifikasjon: {
                        merkelapp: "fager"
                        tekst: ${'$'}tekst
                        lenke: ${'$'}url
                    } 
                }
            ) {
                __typename
                ... on NyOppgaveVellykket {
                    id
                }
                ... on Error {
                    feilmelding
                }
            }
        }
    """

const val errorPage: String =
    """
        <html>
            <head>
                <title> error </title>
            </head>
            <body>
                error :( se log
                <a href="/">hovedside</a>
            </body>
        </html>
    """


val nyBeskjed: String =
    // language=GraphQL
    """
        mutation NyBeskjed(${'$'}vnr: String! ${'$'}tekst: String! ${'$'}url: String) {
            nyBeskjed(
                nyBeskjed: {
                    metadata: {
                        eksternId: "${java.util.UUID.randomUUID()}"
                    }
                    mottaker: {
                        altinn: {
                            serviceCode: "4936"
                            serviceEdition: "1"
                            virksomhetsnummer: ${'$'}vnr
                        }
                    }
                    notifikasjon: {
                        merkelapp: "fager"
                        tekst: ${'$'}tekst
                        lenke: ${'$'}url
                    } 
                }
            ) {
                __typename
                ... on NyBeskjedVellykket {
                    id
                }
                ... on Error {
                    feilmelding
                }
            }
        }
    """

