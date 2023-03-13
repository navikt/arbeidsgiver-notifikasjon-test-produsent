package no.nav.arbeidsgiver.notifikasjon.test_produsent

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.application.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
    post(path) {
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

fun notifikasjonFelles(
    form: Parameters,
    vararg custom: Pair<String, String?>,
): Map<String, String?> {
    val variables = mapOf(
        "vnr" to form["vnr"].toString(),
        "tekst" to form["tekst"].toString(),
        "url" to form["url"].toString(),
        "sms" to form["sms"]!!.ifBlank { null },
        "epost" to form["epost"]!!.ifBlank { null },
        "altinntjenesteServiceCode" to form["altinntjenesteServiceCode"]!!.ifBlank { null },
        "altinntjenesteServiceEdition" to form["altinntjenesteServiceEdition"]!!.ifBlank { null },
        "grupperingsid" to form["grupperingsid"]!!.ifBlank { null },
    ) + custom.toMap()
    return variables.filterValues { it != null }
}

fun oppgaveFelles(form: Parameters, vararg custom: Pair<String, String?>): Map<String, String?> {
    return notifikasjonFelles(
        form,
        "frist" to form["frist"]!!.ifBlank { null },
        "paaminnelse_konkret" to form["paaminnelse_konkret"]!!.ifBlank { null },
        "paaminnelse_etter_opprettelse" to form["paaminnelse_etter_opprettelse"]!!.ifBlank { null },
        "paaminnelse_for_frist" to form["paaminnelse_for_frist"]!!.ifBlank { null },
        *custom,
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

            handleForm("/opprett_beskjed_altinn") { form ->
                val variables = notifikasjonFelles(
                    form,
                    "serviceCode" to form["scode"].toString(),
                    "serviceEdition" to form["sedit"].toString(),
                )
                executeGraphql(
                    nyBeskjed(
                        variables.keys.toList(),
                        mottaker = """
                            altinn: {
                                serviceCode: ${'$'}serviceCode
                                serviceEdition: ${'$'}serviceEdition
                            }
                         """
                    ),
                    variables = variables
                )
            }
            handleForm("/opprett_beskjed_digisyfo") { form ->
                val variables = notifikasjonFelles(
                    form,
                    "fnrLeder" to form["fnrleder"].toString(),
                    "fnrSykmeldt" to form["fnrsyk"].toString(),
                )
                executeGraphql(
                    nyBeskjed(
                        variables.keys.toList(),
                        mottaker = """
                                   naermesteLeder: {
                                       naermesteLederFnr: ${'$'}fnrLeder
                                       ansattFnr: ${'$'}fnrSykmeldt
                                   }
                                   """
                    ),
                    variables = variables
                )
            }
            handleForm("/opprett_oppgave_altinn") { form ->
                val variables = oppgaveFelles(
                    form,
                    "serviceCode" to form["scode"].toString(),
                    "serviceEdition" to form["sedit"].toString(),
                )
                executeGraphql(
                    nyOppgave(
                        variables.keys.toList(),
                        mottaker = """
                                                altinn: {
                                                    serviceCode: ${'$'}serviceCode
                                                    serviceEdition: ${'$'}serviceEdition
                                                }
                                            """
                    ),
                    variables = variables
                )
            }
            handleForm("/opprett_oppgave_digisyfo") { form ->
                val variables = oppgaveFelles(
                    form,
                    "fnrLeder" to form["fnrleder"].toString(),
                    "fnrSykmeldt" to form["fnrsyk"].toString(),
                )
                executeGraphql(
                    nyOppgave(
                        variables.keys.toList(),
                        mottaker = """
                                                    naermesteLeder: {
                                                        naermesteLederFnr: ${'$'}fnrLeder
                                                        ansattFnr: ${'$'}fnrSykmeldt
                                                    }
                                                """
                    ),
                    variables = variables
                )
            }
            handleForm("/oppgave_utfoert") { form ->
                executeGraphql(
                    """
                    mutation OppgaveUtfoert(${'$'}id: ID!, ${'$'}nyLenke: String) {
                      oppgaveUtfoert(id: ${'$'}id, nyLenke: ${'$'}nyLenke) {
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
                    mapOf(
                        "id" to form["id"].toString(),
                        "nyLenke" to form["nyLenke"]!!.ifBlank { null }
                    )
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
                        "grupperingsid" to form["grupperingsid"].toString(),
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
            handleForm("/opprett_sak_digisyfo") { form ->
                opprettNySak(
                    variables = mapOf(
                        "grupperingsid" to form["grupperingsid"].toString(),
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
            handleForm("/oppdater_sak") { form ->
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

suspend fun oppdaterStatusTilSak(id: String, nyLenkeTilSak: String, nyTekst: String, nyStatus: String): String {

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

val graphQLEndpoint = if (System.getenv("NAIS_CLUSTER_NAME") == "dev-gcp")
    "http://notifikasjon-produsent-api/api/graphql"
else
    "https://notifikasjon-fake-produsent-api.labs.nais.io/"

suspend fun executeGraphql(query: String, variables: Map<String, Any?>): String {
    log.info("Ville ha sendt: {}, {}", query, variables)
    val requestBody = objectMapper.writeValueAsString(
        mapOf(
            "query" to query,
            "variables" to variables,
        )
    )
    val accessToken = getAccessToken()
    val response: HttpResponse = httpClient.post(graphQLEndpoint) {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
        header(HttpHeaders.ContentType, "application/json")
        header(HttpHeaders.Accept, "application/json")
        setBody(requestBody)
    }

    return objectMapper.writeValueAsString(
        mapOf(
            "status" to response.status,
            "body" to response.bodyAsText(),
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

// language=HTML
fun inputSection(
    tittel: String,
    action: String,
    knapp: String = "send",
    buttonType: String = "is-primary",
    body: () -> String
): String {
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
    if (System.getenv("NAIS_CLUSTER_NAME") != "dev-gcp") {
        return ""
    }
    val tokenEndpoint = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")!!
    val tenantId = System.getenv("AZURE_APP_TENANT_ID")!!
    val clientId = System.getenv("AZURE_APP_CLIENT_ID")!!
    val clientSecret = System.getenv("AZURE_APP_CLIENT_SECRET")!!

    val accessTokenResponse = httpClient.submitForm(
        url = tokenEndpoint,
        formParameters = Parameters.build {
            set("tenant", tenantId)
            set("client_id", clientId)
            set("scope", "api://dev-gcp.fager.notifikasjon-produsent-api/.default")
            set("client_secret", clientSecret)
            set("grant_type", "client_credentials")
        }
    )
    val map: Map<String, Any> = objectMapper.readValue(accessTokenResponse.bodyAsText())
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
                  
                  input:checked + label.nes-btn {
                    color: #fff;
                    background-color: #92cc41;
                  }
                  
                  #beskjed_tab, #oppgave_tab, #felles_tab, #sak_tab {
                    display: none;
                  }
                  
                  #beskjed:checked ~ #beskjed_tab,
                  #oppgave:checked ~ #oppgave_tab,
                  #felles:checked ~ #felles_tab,
                  #sak:checked ~ #sak_tab {
                        display: block;
                  }
                  
                </style>
            </head>
            <body>
                <input type="radio" id="beskjed" name="tab" checked style='display: none'>
                <label class="nes-btn" for="beskjed">Beskjed</label>
                
                <input type="radio" id="oppgave" name="tab" style='display: none'>
                <label class="nes-btn" for="oppgave">Oppgave</label>
                
                <input type="radio" id="felles" name="tab" style='display: none'>
                <label class="nes-btn" for="felles">Felles</label>
                
                <input type="radio" id="sak" name="tab" style='display: none'>
                <label class="nes-btn" for="sak">Sak</label>
            
                <section id="beskjed_tab" class="nes-container" style='overflow: scroll'>
                    <h1>Opprett beskjed</h1>
                     ${inputSection("Mottakere: altinn-tjeneste", "/opprett_beskjed_altinn") {
        """
                        ${inputs("vnr", "Virksomhetsnummer", "910825526")}
                        ${inputs("scode", "Service code", "4936")}
                        ${inputs("sedit", "Service edition", "1")}
                        ${inputs("tekst", "Tekst", "Dette er en test-melding")}
                        ${inputs("url", "url", "https://dev.nav.no")}
                        ${inputs("grupperingsid", "grupperingsid", "${UUID.randomUUID()}")}
                        ${inputs("epost", "varsle epost", "")}
                        ${inputs("sms", "varsle sms", "")}
                        ${inputs("altinntjenesteServiceCode", "varsle servicecode", "")}
                        ${inputs("altinntjenesteServiceEdition", "varsle serviceedition", "")}
                        """
    }
    }
                    ${
        inputSection("Mottakere: naermeste leder", "/opprett_beskjed_digisyfo") {
            """
                        ${inputs("vnr", "Virksomhetsnummer", "910825526")}
                        ${inputs("fnrleder", "Fnr leder")}
                        ${inputs("fnrsyk", "Fnr sykmeldt")}
                        ${inputs("tekst", "Tekst", "Dette er en test-melding")}
                        ${inputs("url", "url", "https://dev.nav.no")}
                        ${inputs("grupperingsid", "grupperingsid", "${UUID.randomUUID()}")}
                        """
        }
    }
    
                </section>
                <section id="oppgave_tab" class="nes-container" style='overflow: scroll'>
                    <h1>Opprett oppgave</h1>
                     ${
        inputSection("Mottakere: altinn-tjeneste", "/opprett_oppgave_altinn") {
            """
                        ${inputs("vnr", "Virksomhetsnummer", "910825526")}
                        ${inputs("scode", "Service code", "4936")}
                        ${inputs("sedit", "Service edition", "1")}
                        ${inputs("tekst", "Tekst", "Dette er en test-melding")}
                        ${inputs("url", "url", "https://dev.nav.no")}
                        ${inputs("frist", "frist", "")}
                        ${inputs("paaminnelse_konkret", "Påminnelse YYYY-MM-DDTHH:MM:SS", "")}
                        ${inputs("paaminnelse_etter_opprettelse", "Påminnelse etter opprettelse", "")}
                        ${inputs("paaminnelse_for_frist", "Påminnelse før frist", "")}
                        ${inputs("grupperingsid", "grupperingsid", "${UUID.randomUUID()}")}
                        ${inputs("epost", "varsle epost", "")}
                        ${inputs("sms", "varsle sms", "")}
                        ${inputs("altinntjenesteServiceCode", "varsle servicecode", "")}
                        ${inputs("altinntjenesteServiceEdition", "varsle serviceedition", "")}
                        """
        }
    }
                    ${
        inputSection("Mottakere: naermeste leder", "/opprett_oppgave_digisyfo") {
            """
                        ${inputs("vnr", "Virksomhetsnummer", "910825526")}
                        ${inputs("fnrleder", "Fnr leder")}
                        ${inputs("fnrsyk", "Fnr sykmeldt")}
                        ${inputs("tekst", "Tekst", "Dette er en test-melding")}
                        ${inputs("url", "url", "https://dev.nav.no")}
                        ${inputs("frist", "frist", "")}
                        ${inputs("grupperingsid", "grupperingsid", "${UUID.randomUUID()}")}
                        """
        }
    }
                    ${
        inputSection("Oppgave utført", "/oppgave_utfoert", "sett utført") {
            """
                ${inputs("id", "id")}
                ${inputs("nyLenke", "nyLenke")}
            """
        }
    }
                    ${
        inputSection("Oppgave utgått", "/oppgave_utgaatt", "sett utgått") {
            inputs("id", "id")
        }
    }
                </section>
                <section id="felles_tab" class="nes-container with-title" style='overflow: scroll;'>
                    <h1>Felles</h1>
                    ${
        inputSection("Hard Delete notifikasjon", "/hard_delete_notifikasjon", "slett", "is-error") {
            inputs("id", "id")
        }
    }
                </section>
                <section id="sak_tab" class="nes-container with-title" style='overflow: scroll; '>
                    <h1>Opprett sak</h1>
                    ${
        inputSection("Mottakere: altinn tjeneste", "/opprett_sak_servicecode") {
            """
                        ${inputs("grupperingsid", "Grupperingsid", "${UUID.randomUUID()}")}
                        ${inputs("vnr", "Virksomhetsnummer", "910825526")}
                        ${inputs("scode", "Service code", "4936")}
                        ${inputs("sedit", "Service edition", "1")}
                        ${inputs("tittel", "Tekst", "Dette er en test-melding")}
                        ${inputs("url", "url", "https://dev.nav.no")}
                        ${inputs("hardDelete", "hardDelete", "")}
                        """
        }
    }
                    ${
        inputSection("Mottakere: nærmeste leder", "/opprett_sak_digisyfo") {
            """
                            ${inputs("grupperingsid", "Grupperingsid", "${UUID.randomUUID()}")}
                            ${inputs("vnr", "Virksomhetsnummer", "910825526")}
                            ${inputs("fnrleder", "Fnr leder")}
                            ${inputs("fnrsyk", "Fnr sykmeldt")}
                            ${inputs("tittel", "Tekst", "Dette er en test-melding")}
                            ${inputs("url", "url", "https://dev.nav.no")} 
                        """
        }
    }     
                    ${
        inputSection("Oppdater sak", "/oppdater_sak") {
            """
                            ${inputs("id", "id")}
                            ${inputs("nyLenkeTilSak", "nyLenkeTilSak")}
                            ${inputs("nyStatus", "nyStatus", "UNDER_BEHANDLING")}
                            ${inputs("nyTekst", "nyTekst")}
                        """
        }
    }
                    ${
        inputSection("Hard Delete sak", "/hard_delete_sak", "slett", "is-error") {
            inputs("id", "id")
        }
    }
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
        mutation NyOppgave(${vars.graphQLParameters(mapOf(
        "frist" to "ISO8601Date",
        "paaminnelse_konkret" to "ISO8601LocalDateTime",
        "paaminnelse_etter_opprettelse" to "ISO8601Duration",
        "paaminnelse_for_frist" to "ISO8601Duration",
    ))}) {
            nyOppgave(
                nyOppgave: {
                    metadata: {
                        eksternId: "${UUID.randomUUID()}"
                        virksomhetsnummer: ${'$'}vnr
                        ${if ("grupperingsid" in vars) "grupperingsid: ${'$'}grupperingsid" else ""}
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
                    ${if ("frist" in vars) "frist: ${'$'}frist" else ""}
                    ${vars.eksterneVarsler()}
                    ${ if ("paaminnelse_konkret" in vars)
                        """
                            paaminnelse: {
                                tidspunkt: {
                                    konkret: ${'$'}paaminnelse_konkret
                                }
                            }
                        """
                        else ""
                    }
                    ${ if ("paaminnelse_for_frist" in vars)
        """
                            paaminnelse: {
                                tidspunkt: {
                                    foerFrist: ${'$'}paaminnelse_for_frist
                                }
                            }
                        """
    else ""
    }
                    ${ if ("paaminnelse_etter_opprettelse" in vars)
        """
                            paaminnelse: {
                                tidspunkt: {
                                    etterOpprettelse: ${'$'}paaminnelse_etter_opprettelse
                                }
                            }
                        """
    else ""
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
                        ${if ("grupperingsid" in vars) "grupperingsid: ${'$'}grupperingsid" else ""}
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
                grupperingsid: ${'$'}grupperingsid
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
    val harAltinntjeneste = contains("altinntjenesteServiceCode") && contains("altinntjenesteServiceEdition")
    if (!harSms && !harEpost && !harAltinntjeneste) {
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
    val altinntjenestePart = """
        {
          altinntjeneste: {
            mottaker: {
              serviceCode: ${'$'}altinntjenesteServiceCode
              serviceEdition: ${'$'}altinntjenesteServiceEdition
            }
            tittel: "Test varsel fra test-produsent"
            innhold: "Dette er en test"
            sendetidspunkt: {
              sendevindu: NKS_AAPNINGSTID
            }
          }
        }
    """
    return """eksterneVarsler: [
        ${if (harSms) smsPart else ""}
        ${if (harEpost) epostPart else ""}
        ${if (harAltinntjeneste) altinntjenestePart else ""}
    ]"""
}
