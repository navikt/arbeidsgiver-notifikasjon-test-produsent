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

val log = LoggerFactory.getLogger("main")

suspend fun ApplicationCall.respondHtml(html: String) {
    this.respondText(
        contentType = ContentType.Text.Html,
        text = html,
    )
}

fun main() {


    embeddedServer(Netty, port = 8080) {
        routing {
            get("ok") {
                call.respond("ok")
            }

            get {
                call.respondHtml(sendPage)
            }

            post("/submit_altinn") {
                try {
                    val formParameters = call.receiveParameters()
                    val vnr = formParameters["vnr"].toString()
                    val tekst = formParameters["tekst"].toString()
                    val url = formParameters["url"].toString()
                    val type = formParameters["type"].toString()
                    val serviceCode = formParameters["scode"].toString()
                    val serviceEdition = formParameters["sedit"].toString()

                    val variables = mapOf(
                        "vnr" to vnr,
                        "tekst" to tekst,
                        "url" to url,
                        "serviceCode" to serviceCode,
                        "serviceEdition" to serviceEdition,
                    )

                    val mottaker = """
                        altinn: {
                            serviceCode: ${'$'}serviceCode
                            serviceEdition: ${'$'}serviceEdition
                            virksomhetsnummer: ${'$'}vnr
                        }
                    """
                    val utfall = sendNotifikasjon(
                        type = type,
                        variables = variables,
                        mottaker = mottaker,
                    )
                    call.respondHtml(okPage(utfall))
                } catch (e: Exception) {
                    log.error("unexpected exception", e)
                    call.respondHtml(errorPage)
                }
            }

            post("/opprett_sak") {
                try {
                    val formParameters = call.receiveParameters()
                    val vnr = formParameters["vnr"].toString()
                    val tittel = formParameters["tittel"].toString()
                    val url = formParameters["url"].toString()
                    val serviceCode = formParameters["scode"].toString()
                    val serviceEdition = formParameters["sedit"].toString()

                    val variables = mapOf(
                        "vnr" to vnr,
                        "tittel" to tittel,
                        "url" to url,
                        "serviceCode" to serviceCode,
                        "serviceEdition" to serviceEdition,
                    )

                    val mottaker = """
                        altinn: {
                            serviceCode: ${'$'}serviceCode
                            serviceEdition: ${'$'}serviceEdition
                            virksomhetsnummer: ${'$'}vnr
                        }
                    """
                    val utfall = opprettNySak(
                        variables = variables,
                        mottaker = mottaker,
                    )
                    call.respondHtml(okPage(utfall))
                } catch (e: Exception) {
                    log.error("unexpected exception", e)
                    call.respondHtml(errorPage)
                }
            }

            post("/submit_digisyfo") {
                try {
                    val formParameters = call.receiveParameters()
                    val vnr = formParameters["vnr"].toString()
                    val tekst = formParameters["tekst"].toString()
                    val url = formParameters["url"].toString()
                    val type = formParameters["type"].toString()
                    val fnrLeder = formParameters["fnrleder"].toString()
                    val fnrSykmeldt = formParameters["fnrsyk"].toString()

                    val variables = mapOf(
                        "vnr" to vnr,
                        "tekst" to tekst,
                        "url" to url,
                        "fnrLeder" to fnrLeder,
                        "fnrSykmeldt" to fnrSykmeldt
                    )
                    val mottaker = """
                        naermesteLeder: {
                            naermesteLederFnr: ${'$'}fnrLeder
                            ansattFnr: ${'$'}fnrSykmeldt
                            virksomhetsnummer: ${'$'}vnr
                        }
                    """
                    val utfall = sendNotifikasjon(type = type, mottaker = mottaker, variables = variables)

                    call.respondHtml(okPage(utfall))
                } catch (e: Exception) {
                    log.error("unexpected exception", e)
                    call.respondHtml(errorPage)
                }
            }
            post("/submit_altinn_rolle") {
                try {
                    val formParameters = call.receiveParameters()
                    val vnr = formParameters["vnr"].toString()
                    val tekst = formParameters["tekst"].toString()
                    val url = formParameters["url"].toString()
                    val type = formParameters["type"].toString()
                    val roleDefinitionCode = formParameters["rcode"].toString()


                    val variables = mapOf(
                        "vnr" to vnr,
                        "tekst" to tekst,
                        "url" to url,
                        "roleDefinitionCode" to roleDefinitionCode,
                    )

                    val mottaker = """
                        altinnRolle: {
                            roleDefinitionCode: ${'$'}roleDefinitionCode
                        }
                    """
                    val utfall = sendNotifikasjon(
                        type = type,
                        variables = variables,
                        mottaker = mottaker,
                    )
                    call.respondHtml(okPage(utfall))
                } catch (e: Exception) {
                    log.error("unexpected exception", e)
                    call.respondHtml(errorPage)
                }
            }
            post("/submit_altinn_reportee") {
                try {
                    val formParameters = call.receiveParameters()
                    val vnr = formParameters["vnr"].toString()
                    val fnr = formParameters["fnr"].toString()
                    val tekst = formParameters["tekst"].toString()
                    val url = formParameters["url"].toString()
                    val type = formParameters["type"].toString()


                    val variables = mapOf(
                        "vnr" to vnr,
                        "fnr" to fnr,
                        "tekst" to tekst,
                        "url" to url,
                    )

                    val mottaker = """
                        altinnReportee: {
                            fnr: ${'$'}fnr
                        }
                    """
                    val utfall = sendNotifikasjon(
                        type = type,
                        variables = variables,
                        mottaker = mottaker,
                    )
                    call.respondHtml(okPage(utfall))
                } catch (e: Exception) {
                    log.error("unexpected exception", e)
                    call.respondHtml(errorPage)
                }
            }
        }
    }.start(wait = true)
}

suspend fun opprettNySak(variables: Map<String, String>, mottaker: String): String {
    return executeGraphql(nySak(variables.keys.toList(), mottaker), variables)
}

suspend fun sendNotifikasjon(type: String, mottaker: String, variables: Map<String, String>): String {
    return when (type) {
        "beskjed" -> executeGraphql(nyBeskjed(variables.keys.toList(), mottaker), variables)
        "oppgave" -> executeGraphql(nyOppgave(variables.keys.toList(), mottaker), variables)
        else -> "ukjent type '$type' :("
    }
}

suspend fun executeGraphql(query: String, variables: Map<String, String>): String {
    log.info("Ville ha sendt: {}, {}", query, variables)
    val requestBody = objectMapper.writeValueAsString(
            mapOf(
                "query" to query,
                "variables" to variables,
            )
        )
    val accessToken = getAccessToken()
    val response: HttpResponse =  httpClient.post("http://notifikasjon-produsent-api/api/graphql") {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
        header(HttpHeaders.ContentType, "application/json")
        header(HttpHeaders.Accept, "application/json")
        body = requestBody
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
            set("scope" , "api://dev-gcp.fager.notifikasjon-produsent-api/.default")
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
                <div style="margin: 2em">
                     Mottakere: altinn-tjenesten (Default: Inntektsmelding, service code "4936", edition "1") <br>
                     
                    <form method="post" action="/submit_altinn">
                        <label for="altinn_vnr">Virksomhetsnummer:</label>
                        <input id="altinn_vnr" name="vnr" type="text" value="910825526"><br>
                        
                        <label for="altinn_scode">Service code:</label>
                        <input id="altinn_scode" name="scode" type="text" value="4936"><br>
                        
                        <label for="altinn_sedit">Service edition:</label>
                        <input id="altinn_sedit" name="sedit" type="text" value="1"><br>
                        
                        <label for="altinn_tekst">Tekst:</label>
                        <input id="altinn_tekst" name="tekst" type="text" value="Dette er en test-melding"><br>
                        
                        <label for="altinn_url">url:</label>
                        <input id="altinn_url" name="url" type="text" value="https://dev.nav.no"><br>
                        
                        
                        Notifikasjonstype:<br>
                        <input type="radio" id="altinn_beskjed" name="type" value="beskjed" checked>
                        <label for="altinn_beskjed">beskjed</label><br>
                        <input type="radio" id="altinn_oppgave" name="type" value="oppgave">
                        <label for="altinn_oppgave">oppgave</label><br>
                        <input type="submit" value="send">
                    </form>
                </div>
                <div style="margin: 2em;">
                     Mottakere: naermeste leder<br>
                     
                    <form method="post" action="/submit_digisyfo">
                        <label for="vnr">Virksomhetsnummer:</label>
                        <input id="vnr" name="vnr" type="text" value="910825526"><br>
                        
                        <label for="fnrleder">Fnr leder:</label>
                        <input id="fnrleder" name="fnrleder" type="text" value=""><br>
                        
                        <label for="fnrsyk">Fnr sykmeldt:</label>
                        <input id="fnrsyk" name="fnrsyk" type="text" value=""><br>
                        
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
                </div>
                <div style="margin: 2em;">
                     Mottakere: altinn rolle<br>
                     
                    <form method="post" action="/submit_altinn_rolle">
                        <label for="vnr">Virksomhetsnummer:</label>
                        <input id="vnr" name="vnr" type="text" value="910825526"><br>
                        
                        <label for="altinn_rcode">altinn rollekode:</label>
                        <input id="altinn_rcode" name="rcode" type="text" value="DAGL"><br>

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
                </div>
                <div style="margin: 2em;">
                     Mottakere: altinn reportee<br>
                     
                    <form method="post" action="/submit_altinn_reportee">
                        <label for="vnr">Virksomhetsnummer:</label>
                        <input id="vnr" name="vnr" type="text" value="910825526"><br>
                        
                        <label for="fnr">altinn reportee:</label>
                        <input id="fnr" name="fnr" type="text" value="16120101181"><br>

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
                </div>
                <div style="margin: 2em">
                     Opprett ny sak <br>
                     
                    <form method="post" action="/opprett_sak">
                        <label for="altinn_vnr">Virksomhetsnummer:</label>
                        <input id="altinn_vnr" name="vnr" type="text" value="910825526"><br>
                        
                        <label for="altinn_scode">Service code:</label>
                        <input id="altinn_scode" name="scode" type="text" value="4936"><br>
                        
                        <label for="altinn_sedit">Service edition:</label>
                        <input id="altinn_sedit" name="sedit" type="text" value="1"><br>
                        
                        <label for="altinn_tekst">Tittel:</label>
                        <input id="altinn_tekst" name="tittel" type="text" value="Dette er en test-melding"><br>
                        
                        <label for="altinn_url">url:</label>
                        <input id="altinn_url" name="url" type="text" value="https://dev.nav.no"><br>
                        
                        
                        <input type="submit" value="send">
                    </form>
                </div>
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

fun nyOppgave(vars: List<String>, mottaker: String): String =
    // language=GraphQL
    """
        mutation NyOppgave(${ vars.joinToString(" ") { "$$it: String!" } }) {
            nyOppgave(
                nyOppgave: {
                    metadata: {
                        eksternId: "${java.util.UUID.randomUUID()}"
                        virksomhetsnummer: ${'$'}vnr
                    }
                    mottakere: [
                        {
                            $mottaker
                        }
                    ]
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

fun nyBeskjed(vars: List<String>, mottaker: String): String =
    // language=GraphQL
    """
        mutation NyBeskjed(${ vars.joinToString(" ") { "${'$'}$it: String!" } }) {
            nyBeskjed(
                nyBeskjed: {
                    metadata: {
                        eksternId: "${java.util.UUID.randomUUID()}"
                        virksomhetsnummer: ${'$'}vnr
                    }
                    mottakere: [
                        {
                            $mottaker
                        }
                    ]
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

fun nySak(vars: List<String>, mottaker: String): String =
    // language=GraphQL
    """
        mutation NySak(${ vars.joinToString(" ") { "${'$'}$it: String!" } }) {
            nySak(
                sak: {
                    grupperingsid: "${java.util.UUID.randomUUID()}"
                    merkelapp: "fager"
                    virksomhetsnummer: ${'$'}vnr
                    
                    mottakere: [
                        {
                            $mottaker
                        }
                    ]
                    
                    tittel: ${'$'}tittel
                    lenke: ${'$'}url

                    status: {
                     status: MOTTATT
                    }
                }
            ) {
                __typename
                ... on NySakVellykket {
                    id
                }
                ... on Error {
                    feilmelding
                }
            }
        }
    """
