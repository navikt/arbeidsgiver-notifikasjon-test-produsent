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
import java.util.*

val objectMapper = jacksonObjectMapper()

val httpClient = HttpClient(Apache)

val log = LoggerFactory.getLogger("main")

suspend fun ApplicationCall.respondHtml(html: String) {
    this.respondText(
        contentType = ContentType.Text.Html,
        text = html,
    )
}
fun Route.handleForm(path: String, body: suspend (Parameters) -> String) {
    post(path){
        try {
            val formParameters = call.receiveParameters()
            val result = body(formParameters)
            call.respondHtml(okPage(result))
        } catch (e: Exception) {
            log.error("unexpected exception", e)
            call.respondHtml(errorPage)
        }
    }
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

            handleForm("/opprett_notifikasjon_altinn") { form ->
                sendNotifikasjon(
                    type = form["type"].toString(),
                    variables = mapOf(
                        "vnr" to form["vnr"].toString(),
                        "tekst" to form["tekst"].toString(),
                        "url" to form["url"].toString(),
                        "serviceCode" to form["scode"].toString(),
                        "serviceEdition" to form["sedit"].toString(),
                        "sms" to form["sms"]!!.ifBlank { null },
                        "epost" to form["epost"]!!.ifBlank { null },
                        "frist" to form["frist"]!!.ifBlank { null },
                    ).filterValues { it != null },
                    mottaker = """
                        altinn: {
                            serviceCode: ${'$'}serviceCode
                            serviceEdition: ${'$'}serviceEdition
                        }
                    """,
                )
            }
            handleForm("/opprett_notifikasjon_digisyfo") { form ->
                sendNotifikasjon(
                    type = form["type"].toString(), mottaker = """
                            naermesteLeder: {
                                naermesteLederFnr: ${'$'}fnrLeder
                                ansattFnr: ${'$'}fnrSykmeldt
                            }
                        """, variables = mapOf(
                        "vnr" to form["vnr"].toString(),
                        "tekst" to form["tekst"].toString(),
                        "url" to form["url"].toString(),
                        "fnrLeder" to form["fnrleder"].toString(),
                        "fnrSykmeldt" to form["fnrsyk"].toString()
                    )
                )
            }
            handleForm("/opprett_notifikasjon_altinn_rolle") { form ->
                sendNotifikasjon(
                    type = form["type"].toString(),
                    variables = mapOf(
                        "vnr" to form["vnr"].toString(),
                        "tekst" to form["tekst"].toString(),
                        "url" to form["url"].toString(),
                        "roleDefinitionCode" to form["rcode"].toString(),
                    ),
                    mottaker = """
                            altinnRolle: {
                                roleDefinitionCode: ${'$'}roleDefinitionCode
                            }
                        """,
                )
            }
            handleForm("/opprett_notifikasjon_altinn_reportee") { form ->
                sendNotifikasjon(
                    type = form["type"].toString(),
                    variables = mapOf(
                        "vnr" to form["vnr"].toString(),
                        "fnr" to form["fnr"].toString(),
                        "tekst" to form["tekst"].toString(),
                        "url" to form["url"].toString(),
                    ),
                    mottaker = """
                            altinnReportee: {
                                fnr: ${'$'}fnr
                            }
                        """,
                )
            }
            handleForm("/oppgave_utfoert") { form ->
                executeGraphql(
                    """
                    mutation OppgaveUtfoert(${'$'}id: ID!) {
                      oppgaveUtfoert(id: ${'$'}id) {
                        __typename
                        ... on Error {
                          feilmelding
                        }
                        ... on OppgaveUtfoertVellykket {
                          id
                        }
                      }
                    }
                    """.trimIndent(),
                    mapOf("id" to form["id"].toString())
                )
            }
            handleForm("/oppgave_utgaatt") { form ->
                executeGraphql(
                    """
                    mutation OppgaveUtgaatt(${'$'}id: ID!) {
                      oppgaveUtgaatt(id: ${'$'}id) {
                        __typename
                        ... on Error {
                          feilmelding
                        }
                        ... on OppgaveUtgaattVellykket {
                          id
                        }
                      }
                    }
                    """.trimIndent(),
                    mapOf("id" to form["id"].toString())
                )
            }


            handleForm("/opprett_sak_servicecode") { form ->
                opprettNySak(
                    variables = mapOf(
                        "vnr" to form["vnr"].toString(),
                        "tittel" to form["tittel"].toString(),
                        "url" to form["url"].toString(),
                        "serviceCode" to form["scode"].toString(),
                        "serviceEdition" to form["sedit"].toString(),
                        "hardDelete" to form["hardDelete"]?.let { if (it.isNotBlank()) mapOf("om" to it) else null },
                    ),
                    mottaker = """
                            altinn: {
                                serviceCode: ${'$'}serviceCode
                                serviceEdition: ${'$'}serviceEdition
                            }
                        """,
                )
            }
            handleForm("/opprett_sak_rolle") { form ->
                opprettNySak(
                    variables = mapOf(
                        "vnr" to form["vnr"].toString(),
                        "tittel" to form["tittel"].toString(),
                        "url" to form["url"].toString(),
                        "roleDefinitionCode" to form["rcode"].toString(),
                    ),
                    mottaker = """
                            altinnRolle: {
                                roleDefinitionCode: ${'$'}roleDefinitionCode
                            }
                        """,
                )
            }
            handleForm("/opprett_sak_reportee") { form ->
                opprettNySak(
                    variables = mapOf(
                        "vnr" to form["vnr"].toString(),
                        "fnr" to form["fnr"].toString(),
                        "tittel" to form["tittel"].toString(),
                        "url" to form["url"].toString(),
                    ),
                    mottaker = """
                            altinnReportee: {
                                fnr: ${'$'}fnr
                            }
                        """,
                )
            }
            handleForm("/opprett_sak_digisyfo") { form ->
                opprettNySak(
                    variables = mapOf(
                        "vnr" to form["vnr"].toString(),
                        "tittel" to form["tittel"].toString(),
                        "url" to form["url"].toString(),
                        "fnrLeder" to form["fnrleder"].toString(),
                        "fnrSykmeldt" to form["fnrsyk"].toString()
                    ),
                    mottaker = """
                            naermesteLeder: {
                                naermesteLederFnr: ${'$'}fnrLeder
                                ansattFnr: ${'$'}fnrSykmeldt
                            }
                        """,
                )
            }
            handleForm("/oppdater_sak"){ form->
                oppdaterStatusTilSak(
                    id = form["id"].toString(),
                    nyLenkeTilSak = form["nyLenkeTilSak"].toString(),
                    nyStatus = form["nyStatus"].toString(),
                    nyTekst = form["nyTekst"].toString()
                )
            }
            handleForm("/hard_delete_notifikasjon") { form ->
                executeGraphql(
                    hardDeleteNotifikasjon(),
                    mapOf("id" to form["id"].toString())
                )
            }
            handleForm("/hard_delete_sak") { form ->
                executeGraphql(
                    hardDeleteSak(),
                    mapOf("id" to form["id"].toString())
                )
            }
        }
    }.start(wait = true)
}

suspend fun opprettNySak(variables: Map<String, Any?>, mottaker: String): String {
    return executeGraphql(nySak(variables.keys.toList(), mottaker), variables)
}

suspend fun oppdaterStatusTilSak(id: String, nyLenkeTilSak: String, nyTekst: String, nyStatus: String): String{

    return executeGraphql(
        """
            mutation OppdaterSak(
                ${'$'}id: ID!, 
                ${'$'}nyStatus: SaksStatus!, 
                ${'$'}nyTekst: String!, 
                ${'$'}nyLenkeTilSak: String!
            ){
                nyStatusSak(
                    id: ${'$'}id
                    nyStatus: ${'$'}nyStatus
                    overstyrStatustekstMed: ${'$'}nyTekst
                    nyLenkeTilSak: ${'$'}nyLenkeTilSak
                ){
                    __typename
                    ... on Error{
                        feilmelding
                    }
                    ... on NyStatusSakVellykket{
                      id
                    }
                }
            }
        """, mapOf("id" to id, "nyStatus" to nyStatus, "nyTekst" to nyTekst, "nyLenkeTilSak" to nyLenkeTilSak)
    )

}

suspend fun sendNotifikasjon(type: String, mottaker: String, variables: Map<String, Any?>): String {
    return when (type) {
        "beskjed" -> executeGraphql(nyBeskjed(variables.keys.toList(), mottaker), variables)
        "oppgave" -> executeGraphql(nyOppgave(variables.keys.toList(), mottaker), variables)
        else -> "ukjent type '$type' :("
    }
}

suspend fun executeGraphql(query: String, variables: Map<String, Any?>): String {
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
// language=HTML
fun inputs(name: String, inpLabel: String, inpValue: String = ""): String {
    val id: String = UUID.randomUUID().toString()
    return """
    <div class='nes-field'>
        <label for="${id}">${inpLabel}:</label>
        <input class='nes-input' id="${id}" name="${name}" type="text" value="${inpValue}" >
    </div>
    """
}


// language=HTML
fun notifikasjonstypevalg( ):String{
    val beskjedId = UUID.randomUUID().toString()
    val oppgaveId = UUID.randomUUID().toString()
    return """
        <section class="nes-container with-title">
            <h3 class='title'>Notifikasjonstype</h3>
            <label for="${beskjedId}">
                <input type="radio" class='nes-radio' id="${beskjedId}" name="type" value="beskjed" checked> 
                <span>beskjed</span>
            </label>
            <label for="${oppgaveId}">
                <input type="radio" class='nes-radio' id="${oppgaveId}" name="type" value="oppgave">
                <span>oppgave</span>
            </label>
        </section>
        """
}

// language=HTML
fun inputSection(
    tittel: String,
    action: String,
    knapp:String = "send",
    buttonType: String = "is-primary",
    body: () -> String
):String{
    return """
    <section class="nes-container with-title">
        <h2 class='title'>${tittel}</h2>
        <form method="post" action="${action}">
            ${body()}
            <button class='nes-btn ${buttonType}'>${knapp}</button>
        </form>
    </section>
    """
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
val sendPage: String =
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
                     ${ inputSection("Mottakere: altinn-tjeneste", "/opprett_notifikasjon_altinn") {
                        """
                        ${ inputs("vnr", "Virksomhetsnummer", "910825526") }
                        ${ inputs("scode", "Service code", "4936") }
                        ${ inputs("sedit", "Service edition", "1") }
                        ${ inputs("tekst", "Tekst", "Dette er en test-melding") }
                        ${ inputs("url", "url", "https://dev.nav.no") }
                        ${ inputs("frist", "frist", "") }
                        ${ inputs("epost", "varsle epost", "") }
                        ${ inputs("sms", "varsle sms", "") }
                        ${ notifikasjonstypevalg() }
                        """ }}
                    ${ inputSection( "Mottakere: naermeste leder", "/opprett_notifikasjon_digisyfo") {
                        """
                        ${inputs("vnr", "Virksomhetsnummer", "910825526")}
                        ${inputs("fnrleder", "Fnr leder")}
                        ${inputs("fnrsyk", "Fnr sykmeldt")}
                        ${inputs("tekst", "Tekst","Dette er en test-melding")}
                        ${inputs("url", "url", "https://dev.nav.no")}
                        ${notifikasjonstypevalg()}
                        """ }}
                    ${ inputSection("Mottakere: altinn rolle", "/opprett_notifikasjon_altinn_rolle"){
                        """
                            ${inputs("vnr", "Virksomhetsnummer", "910825526")}
                            ${inputs("altinn_rcode", "altinn rollekode","DAGL")}
                            ${inputs("tekst", "Tekst","Dette er en test-melding")}
                            ${inputs("url", "url", "https://dev.nav.no")} 
                            ${notifikasjonstypevalg()}
                        """}}
                    ${ inputSection("Mottakere: altinn reportee", "/opprett_notifikasjon_altinn_reportee"){
                    """
                        ${inputs("vnr", "Virksomhetsnummer", "910825526")}
                        ${inputs("fnr", "altinn reportee","16120101181")}
                        ${inputs("tekst", "Tekst","Dette er en test-melding")}
                        ${inputs("url", "url", "https://dev.nav.no")}
                        ${notifikasjonstypevalg()}
                    """}}
                    ${ inputSection("Hard Delete notifikasjon","/hard_delete_notifikasjon", "slett", "is-error" ){
                        inputs("id", "id")
                    }}
                    ${ inputSection("Oppgave utført","/oppgave_utfoert", "sett utført", ){
                        inputs("id", "id")
                    }}
                    ${ inputSection("Oppgave utgått","/oppgave_utgaatt", "sett utgått", ){
                        inputs("id", "id")
                    }}
                </section>
                <section class="nes-container with-title" style='overflow: scroll; width: 50vw'>
                    <h1>Opprett sak</h1>
                    ${inputSection("Mottakere: altinn tjeneste","/opprett_sak_servicecode"){
                        """
                        ${inputs( "vnr","Virksomhetsnummer", "910825526")}
                        ${inputs("scode", "Service code", "4936")}
                        ${inputs("sedit", "Service edition", "1")}
                        ${inputs("tittel", "Tekst", "Dette er en test-melding")}
                        ${inputs("url", "url", "https://dev.nav.no")}
                        ${inputs("hardDelete", "hardDelete", "")}
                        """
                    }}
                    ${ inputSection("Mottakere: altinn rolle", "/opprett_sak_rolle" ){
                        """
                            ${inputs("vnr", "Virksomhetsnummer", "910825526")}
                            ${inputs("rcode", "altinn rollekode","DAGL")}
                            ${inputs("tittel", "Tittel","Dette er en test-melding")}
                            ${inputs("url", "url", "https://dev.nav.no")}
                        """ }}
                    ${ inputSection("Mottakere: altinn reportee", "/opprett_sak_reportee"){
                        """
                            ${inputs("vnr", "Virksomhetsnummer", "910825526")}
                            ${inputs("fnr", "altinn reportee","16120101181")}
                            ${inputs("tittel", "Tittel","Dette er en test-melding")}
                            ${inputs("url", "url", "https://dev.nav.no")}
                        """ }}
                    ${inputSection("Mottakere: nærmeste leder", "/opprett_sak_digisyfo"){
                        """
                            ${inputs( "altinn_vnr","Virksomhetsnummer", "910825526")}
                            ${inputs("fnrleder", "Fnr leder")}
                            ${inputs("fnrsyk", "Fnr sykmeldt")}
                            ${inputs("tittel", "Tekst", "Dette er en test-melding")}
                            ${inputs("url", "url", "https://dev.nav.no")} 
                        """ }}     
                    ${inputSection("Oppdater sak", "/oppdater_sak"){
                        """
                            ${inputs("id", "id")}
                            ${inputs("nyLenkeTilSak", "nyLenkeTilSak")}
                            ${inputs("nyStatus", "nyStatus", "UNDER_BEHANDLING")}
                            ${inputs("nyTekst", "nyTekst")}
                        """}}
                    ${ inputSection("Hard Delete sak","/hard_delete_sak", "slett", "is-error" ){
                            inputs( "id", "id")
                    }}
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
        mutation NyOppgave(${vars.graphQLParameters(mapOf("frist" to "ISO8601Date"))}) {
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
                    ${ if ("frist" in vars) "frist: ${'$'}frist" else "" }
                    ${vars.eksterneVarsler()}
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

// language=HTML
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
        mutation NyBeskjed(${vars.graphQLParameters()}) {
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
                    ${vars.eksterneVarsler()}
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
        mutation NySak(${vars.graphQLParameters(mapOf("hardDelete" to "FutureTemporalInput"))}) {
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
                initiellStatus: MOTTATT
                hardDelete: ${'$'}hardDelete
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

private fun List<String>.graphQLParameters(typeOverrides: Map<String, String> = mapOf()) =
    joinToString(" ") { "${'$'}$it: ${typeOverrides.getOrDefault(it, "String!")}" }


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


private fun List<String>.eksterneVarsler(): String {
    val harSms = contains("sms")
    val harEpost = contains("epost")
    if (!harSms && !harEpost) {
        return ""
    }

    val smsPart = """
        {
            sms: {
                mottaker: {
                  kontaktinfo: {
                    tlf: ${'$'}sms
                  }
                }
                smsTekst: "Test sms fra test-produsent"
                sendetidspunkt: {
                  sendevindu: LOEPENDE
                }
            }
        }
    """
    val epostPart = """
        {
          epost: {
            mottaker: {
              kontaktinfo: {
                epostadresse: ${'$'}epost
              }
            }
            epostTittel: "Test epost fra test-produsent"
            epostHtmlBody: "Dette er en test"
            sendetidspunkt: {
              sendevindu: LOEPENDE
            }
          }
        }
    """
    return """eksterneVarsler: [
        ${if(harSms) smsPart else ""}
        ${if(harEpost) epostPart else ""}
    ]"""
}
