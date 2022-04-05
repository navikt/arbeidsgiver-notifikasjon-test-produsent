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
            post("/opprett_sak_servicecode") {
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
            post("/opprett_sak_rolle") {
                try {
                    val formParameters = call.receiveParameters()
                    val vnr = formParameters["vnr"].toString()
                    val tittel = formParameters["tittel"].toString()
                    val url = formParameters["url"].toString()
                    val roleDefinitionCode = formParameters["rcode"].toString()

                    val variables = mapOf(
                        "vnr" to vnr,
                        "tittel" to tittel,
                        "url" to url,
                        "roleDefinitionCode" to roleDefinitionCode,
                    )

                    val mottaker = """
                        altinnRolle: {
                            roleDefinitionCode: ${'$'}roleDefinitionCode
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
            post("/opprett_sak_reportee") {
                try {
                    val formParameters = call.receiveParameters()
                    val vnr = formParameters["vnr"].toString()
                    val fnr = formParameters["fnr"].toString()
                    val tekst = formParameters["tittel"].toString()
                    val url = formParameters["url"].toString()

                    val variables = mapOf(
                        "vnr" to vnr,
                        "fnr" to fnr,
                        "tittel" to tekst,
                        "url" to url,
                    )

                    val mottaker = """
                        altinnReportee: {
                            fnr: ${'$'}fnr
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
            post("/opprett_sak_digisyfo") {
                try {
                    val formParameters = call.receiveParameters()
                    val vnr = formParameters["vnr"].toString()
                    val tittel = formParameters["tittel"].toString()
                    val url = formParameters["url"].toString()
                    val fnrLeder = formParameters["fnrleder"].toString()
                    val fnrSykmeldt = formParameters["fnrsyk"].toString()

                    val variables = mapOf(
                        "vnr" to vnr,
                        "tittel" to tittel,
                        "url" to url,
                        "fnrLeder" to fnrLeder,
                        "fnrSykmeldt" to fnrSykmeldt
                    )
                    val mottaker = """
                        naermesteLeder: {
                            naermesteLederFnr: ${'$'}fnrLeder
                            ansattFnr: ${'$'}fnrSykmeldt
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
            post("/hard_delete_notifikasjon") {
                try {
                    val formParameters = call.receiveParameters()
                    val utfall = executeGraphql(
                        hardDeleteNotifikasjon(),
                        mapOf("id" to formParameters["id"].toString())
                    )
                    call.respondHtml(okPage(utfall))
                } catch (e: Exception) {
                    log.error("unexpected exception", e)
                    call.respondHtml(errorPage)
                }
            }
            post("/hard_delete_sak") {
                try {
                    val formParameters = call.receiveParameters()
                    val utfall = executeGraphql(
                        hardDeleteSak(),
                        mapOf("id" to formParameters["id"].toString())
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
    val response: HttpResponse = httpClient.post("http://notifikasjon-produsent-api/api/graphql") {
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
            set("scope", "api://dev-gcp.fager.notifikasjon-produsent-api/.default")
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
                <link href="https://fonts.googleapis.com/css?family=Press+Start+2P" rel="stylesheet">
                <link href="https://unpkg.com/nes.css/css/nes.css" rel="stylesheet" />
            
                <style>
                  html, body, pre, code, kbd, samp {
                      font-family: "Press Start 2P",serif;
                  }
                </style>
            </head>
            <body style='display: flex'>
                <section class="nes-container" style='overflow: scroll; width: 50vw'>
                    <h1>Opprett notifikasjon</h1>
                     
                    <section class="nes-container with-title">
                        <h2 class='title'>Mottakere: altinn-tjeneste</h2>
                     
                        <form method="post" action="/submit_altinn">
                            <div class='nes-field'>
                                <label for="altinn_vnr">Virksomhetsnummer:</label>
                                <input class='nes-input' id="altinn_vnr" name="vnr" type="text" value="910825526" >
                            </div>
                            <div class='nes-field'>
                                <label for="altinn_scode">Service code:</label>
                                <input class='nes-input' id="altinn_scode" name="scode" type="text" value="4936">
                            </div>
                            <div class='nes-field'>
                                <label for="altinn_sedit">Service edition:</label>
                                <input class='nes-input' id="altinn_sedit" name="sedit" type="text" value="1">
                            </div>
                            <div class='nes-field'>
                                <label for="altinn_tekst">Tekst:</label>
                                <input class='nes-input' id="altinn_tekst" name="tekst" type="text" value="Dette er en test-melding">
                            </div>
                            <div class='nes-field'>
                                <label for="altinn_url">url:</label>
                                <input class='nes-input' id="altinn_url" name="url" type="text" value="https://dev.nav.no">
                            </div>
                            
                            <section class="nes-container with-title">
                                <h3 class='title'>Notifikasjonstype</h3>
                                <label for="altinn_beskjed">
                                    <input type="radio" class='nes-radio' id="altinn_beskjed" name="type" value="beskjed" checked> 
                                    <span>beskjed</span>
                                </label>
                                <label for="altinn_oppgave">
                                    <input type="radio" class='nes-radio' id="altinn_oppgave" name="type" value="oppgave">
                                    <span>oppgave</span>
                                </label>
                            </section>
                            <button class='nes-btn is-primary'>send</button>
                        </form>
                    </section>
                    <section class="nes-container with-title">
                        <h2 class='title'>Mottakere: naermeste leder</h2>
                         
                        <form method="post" action="/submit_digisyfo">
                            <div class='nes-field'>
                                <label for="vnr">Virksomhetsnummer:</label>
                                <input class='nes-input' id="vnr" name="vnr" type="text" value="910825526">
                            </div>
                            <div class='nes-field'>    
                                <label for="fnrleder">Fnr leder:</label>
                                <input class='nes-input' id="fnrleder" name="fnrleder" type="text" value="">
                            </div>
                            <div class='nes-field'>
                                <label for="fnrsyk">Fnr sykmeldt:</label>
                                <input class='nes-input' id="fnrsyk" name="fnrsyk" type="text" value="">
                            </div>
                            <div class='nes-field'>
                                <label for="tekst">Tekst:</label>
                                <input class='nes-input' id="tekst" name="tekst" type="text" value="Dette er en test-melding">
                            </div>
                            <div class='nes-field'>
                                <label for="url">url:</label>
                                <input class='nes-input' id="url" name="url" type="text" value="https://dev.nav.no">
                            </div>
                            <section class="nes-container with-title">
                                <h3 class='title'>Notifikasjonstype</h3>
                                <label for="altinn_beskjed">
                                    <input type="radio" class='nes-radio' id="beskjed" name="type" value="beskjed" checked> 
                                    <span>beskjed</span>
                                </label>
                                <label for="altinn_oppgave">
                                    <input type="radio" class='nes-radio' id="oppgave" name="type" value="oppgave">
                                    <span>oppgave</span>
                                </label>
                            </section>
                            <button class='nes-btn is-primary'>send</button>
                        </form>
                    </section>
                    <section class="nes-container with-title">
                        <h2 class='title'>Mottakere: altinn rolle</h2>
                     
                        <form method="post" action="/submit_altinn_rolle"> 
                            <div class='nes-field'>
                                <label for="vnr">Virksomhetsnummer:</label>
                                <input class='nes-input' id="vnr" name="vnr" type="text" value="910825526">
                            </div>
                            <div class='nes-field'>    
                                <label for="altinn_rcode">altinn rollekode:</label>
                                <input class='nes-input' id="altinn_rcode" name="rcode" type="text" value="DAGL">
                            </div>
                            <div class='nes-field'>
                                <label for="tekst">Tekst:</label>
                                <input class='nes-input' id="tekst" name="tekst" type="text" value="Dette er en test-melding">
                            </div>
                            <div class='nes-field'>
                                <label for="url">url:</label>
                                <input class='nes-input' id="url" name="url" type="text" value="https://dev.nav.no">
                            </div>
                            
                            <section class="nes-container with-title">
                                <h3 class='title'>Notifikasjonstype</h3>
                                <label for="altinn_beskjed">
                                    <input type="radio" class='nes-radio' id="beskjed" name="type" value="beskjed" checked> 
                                    <span>beskjed</span>
                                </label>
                                <label for="altinn_oppgave">
                                    <input type="radio" class='nes-radio' id="oppgave" name="type" value="oppgave">
                                    <span>oppgave</span>
                                </label>
                            </section>
                            <button class='nes-btn is-primary'>send</button>
                        </form>
                    </section>
                    <section class="nes-container with-title">
                        <h2 class='title'>Mottakere: altinn reportee</h2>
                         
                        <form method="post" action="/submit_altinn_reportee">
                            <div class='nes-field'>
                                <label for="vnr">Virksomhetsnummer:</label>
                                <input class='nes-input' id="vnr" name="vnr" type="text" value="910825526">
                            </div>
                            <div class='nes-field'>
                                <label for="fnr">altinn reportee:</label>
                                <input class='nes-input' id="fnr" name="fnr" type="text" value="16120101181">
                            </div>
                            <div class='nes-field'>
                                <label for="tekst">Tekst:</label>
                                <input class='nes-input' id="tekst" name="tekst" type="text" value="Dette er en test-melding">
                            </div>
                            <div class='nes-field'>
                                <label for="url">url:</label>
                                <input class='nes-input' id="url" name="url" type="text" value="https://dev.nav.no">
                            </div>
                            
                            <section class="nes-container with-title">
                                <h3 class='title'>Notifikasjonstype</h3>
                                <label for="altinn_beskjed">
                                    <input type="radio" class='nes-radio' id="beskjed" name="type" value="beskjed" checked> 
                                    <span>beskjed</span>
                                </label>
                                <label for="altinn_oppgave">
                                    <input type="radio" class='nes-radio' id="oppgave" name="type" value="oppgave">
                                    <span>oppgave</span>
                                </label>
                            </section>
                            <button class='nes-btn is-primary'>send</button>
                        </form>
                    </section>
                    <section class="nes-container with-title">
                        <h2 class='title'>Hard Delete notifikasjon</h2>
                         
                        <form method="post" action="/hard_delete_notifikasjon">
                            <div class='nes-field'>
                                <label for="id">id:</label>
                                <input class='nes-input' id="id" name="id" type="text" value="">
                            </div>
                            <button class='nes-btn is-error'>slett</button>
                        </form>
                    </section>
                </section>
                <section class="nes-container with-title" style='overflow: scroll; width: 50vw'>
                    <h1>Opprett sak</h1>
                    <section class="nes-container with-title">
                        <h2 class='title'>Mottakere: altinn tjeneste</h2>
                         
                        <form method="post" action="/opprett_sak_servicecode">
                            <div class='nes-field'>
                                <label for="altinn_vnr">Virksomhetsnummer:</label>
                                <input class='nes-input' id="altinn_vnr" name="vnr" type="text" value="910825526">
                            </div>
                            <div class='nes-field'>
                                <label for="altinn_scode">Service code:</label>
                                <input class='nes-input' id="altinn_scode" name="scode" type="text" value="4936">
                            </div>
                            <div class='nes-field'>
                                <label for="altinn_sedit">Service edition:</label>
                                <input class='nes-input' id="altinn_sedit" name="sedit" type="text" value="1">
                            </div>
                            <div class='nes-field'>
                                <label for="altinn_tekst">Tittel:</label>
                                <input class='nes-input' id="altinn_tekst" name="tittel" type="text" value="Dette er en test-melding"></textarea>
                            </div>
                            <div class='nes-field'>
                                <label for="altinn_url">url:</label>
                                <input class='nes-input' id="altinn_url" name="url" type="text" value="https://dev.nav.no">
                            </div>
                            
                            <button class='nes-btn is-primary'>send</button>
                        </form>
                    </section>
                    <section class="nes-container with-title">
                        <h2 class='title'>Mottakere: altinn rolle</h2>
                     
                        <form method="post" action="/opprett_sak_rolle">
                            <div class='nes-field'>
                                <label for="altinn_vnr">Virksomhetsnummer:</label>
                                <input class='nes-input' id="altinn_vnr" name="vnr" type="text" value="910825526">
                            </div>
                            <div class='nes-field'>
                                <label for="altinn_rcode">altinn rollekode:</label>
                                <input class='nes-input' id="altinn_rcode" name="rcode" type="text" value="DAGL">
                            </div>
                            <div class='nes-field'>
                                <label for="altinn_tekst">Tittel:</label>
                                <input class='nes-input' id="altinn_tekst" name="tittel" type="text" value="Dette er en test-melding">
                            </div>
                            <div class='nes-field'>
                                <label for="altinn_url">url:</label>
                                <input class='nes-input' id="altinn_url" name="url" type="text" value="https://dev.nav.no">
                            </div>
                            
                            <button class='nes-btn is-primary'>send</button>
                        </form>
                    </section>
                    <section class="nes-container with-title">
                        <h2 class='title'>Mottakere: altinn reportee</h2>
                     
                        <form method="post" action="/opprett_sak_reportee">
                            <div class='nes-field'>
                                <label for="altinn_vnr">Virksomhetsnummer:</label>
                                <input class='nes-input' id="altinn_vnr" name="vnr" type="text" value="910825526">
                            </div>
                            <div class='nes-field'>
                                <label for="fnr">altinn reportee:</label>
                                <input class='nes-input' id="fnr" name="fnr" type="text" value="16120101181">
                            </div>
                            <div class='nes-field'>
                                <label for="altinn_tekst">Tittel:</label>
                                <input class='nes-input' id="altinn_tekst" name="tittel" type="text" value="Dette er en test-melding">
                            </div>
                            <div class='nes-field'>
                                <label for="altinn_url">url:</label>
                                <input class='nes-input' id="altinn_url" name="url" type="text" value="https://dev.nav.no">
                            </div>
                            <button class='nes-btn is-primary'>send</button>
                        </form>
                    </section>
                    <section class="nes-container with-title">
                        <h2 class='title'>Mottakere: nærmeste leder</h2>
                     
                        <form method="post" action="/opprett_sak_digisyfo">
                            <div class='nes-field'>
                                <label for="altinn_vnr">Virksomhetsnummer:</label>
                                <input class='nes-input' id="altinn_vnr" name="vnr" type="text" value="910825526">
                            </div>
                            <div class='nes-field'>
                                <label for="fnrleder">Fnr leder:</label>
                                <input class='nes-input' id="fnrleder" name="fnrleder" type="text" value="">
                            </div>
                            <div class='nes-field'>
                                <label for="fnrsyk">Fnr sykmeldt:</label>
                                <input class='nes-input' id="fnrsyk" name="fnrsyk" type="text" value="">
                            </div>
                            <div class='nes-field'>
                                <label for="altinn_tekst">Tittel:</label>
                                <input class='nes-input' id="altinn_tekst" name="tittel" type="text" value="Dette er en test-melding">
                            </div>
                            <div class='nes-field'>
                                <label for="altinn_url">url:</label>
                                <input class='nes-input' id="altinn_url" name="url" type="text" value="https://dev.nav.no">
                            </div>
                            
                            <button class='nes-btn is-primary'>send</button>
                        </form>
                    </section>
                    <section class="nes-container with-title">
                        <h2 class='title'>Hard Delete sak</h2>
                         
                        <form method="post" action="/hard_delete_sak">
                            <div class='nes-field'>
                                <label for="id">id:</label>
                                <input class='nes-input' id="id" name="id" type="text" value="">
                            </div>
                            <button class='nes-btn is-error'>slett</button>
                        </form>
                    </section>
                </section>
            </body>
        </html>
    """

fun okPage(utfall: String): String =
    // language=HTML
    """
        <html>
            <head>
                <title>Notifikasjon forsøkt sendt</title>
                <link href="https://fonts.googleapis.com/css?family=Press+Start+2P" rel="stylesheet">
                <link href="https://unpkg.com/nes.css/css/nes.css" rel="stylesheet" />
            
                <style>
                  html, body, pre, code, kbd, samp {
                      font-family: "Press Start 2P",serif;
                  }
                </style>
            </head>
            <body>
                <section class="nes-container with-title" style='overflow: scroll'>
                    <h1 class='title'>Svar fra produsent-api:</h1>
                    $utfall
                    <br/>
                    <a href="/">lag ny notifikasjon</a>
                </section>
            </body>
        </html>
        
    """

fun nyOppgave(vars: List<String>, mottaker: String): String =
    // language=GraphQL
    """
        mutation NyOppgave(${vars.joinToString(" ") { "$$it: String!" }}) {
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
                <link href="https://fonts.googleapis.com/css?family=Press+Start+2P" rel="stylesheet">
                <link href="https://unpkg.com/nes.css/css/nes.css" rel="stylesheet" />
            
                <style>
                  html, body, pre, code, kbd, samp {
                      font-family: "Press Start 2P",serif;
                  }
                </style>
            </head>
            <body>
                <section class="nes-container with-title" style='overflow: scroll'>
                    <h1 class='title'>Error</h1>
                    :( se log
                    <br/>
                    <a href="/">hovedside</a>
                </section>
            </body>
        </html>
    """

fun nyBeskjed(vars: List<String>, mottaker: String): String =
    // language=GraphQL
    """
        mutation NyBeskjed(${vars.joinToString(" ") { "${'$'}$it: String!" }}) {
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

fun hardDeleteNotifikasjon(): String =
    // language=GraphQL
    """
     mutation SlettNotifikasjon(${'$'}id: ID!){
        hardDeleteNotifikasjon(id: ${'$'}id) {
            __typename
            ... on Error {
                feilmelding
            }
        }
     }
    """.trimIndent()

fun nySak(vars: List<String>, mottaker: String): String =
    // language=GraphQL
    """
        mutation NySak(${vars.joinToString(" ") { "${'$'}$it: String!" }}) {
            nySak(
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
                initiell_status: MOTTATT
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

fun hardDeleteSak(): String =
    // language=GraphQL
    """
     mutation SlettSak(${'$'}id: ID!){
        hardDeleteSak(id: ${'$'}id) {
            __typename
            ... on Error {
                feilmelding
            }
        }
     }
    """.trimIndent()

