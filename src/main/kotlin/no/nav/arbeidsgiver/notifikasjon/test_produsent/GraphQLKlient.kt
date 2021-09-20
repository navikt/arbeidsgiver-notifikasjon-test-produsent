package no.nav.arbeidsgiver.notifikasjon.test_produsent

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory
import java.util.*


class GraphQLKlient(
    private val endpoint: String = "https://notifikasjon-fake-produsent-api.labs.nais.io/"
) {
    val log = LoggerFactory.getLogger(this::class.java)
    val objectMapper = jacksonObjectMapper()
    val client = HttpClient(Apache)

    suspend fun whoami() {
        val payload = mapOf(
            "query" to """
                        query {
                           whoami
                        }""".trimIndent()
        )
        execute(payload)
    }

    suspend fun opprettBeskjed() {
        execute(mapOf(
            "query" to """
                        mutation OpprettNyBeskjed(
                          ${'$'}eksternId: String!
                          ${'$'}virksomhetsnummer: String!
                          ${'$'}lenke: String!
                        ) {
                          nyBeskjed(nyBeskjed: {
                            metadata: {
                              eksternId: ${'$'}eksternId
                            }
                            mottaker: {
                              altinn: {
                                serviceCode: "1234"
                                serviceEdition: "1"
                                virksomhetsnummer: ${'$'}virksomhetsnummer
                              }
                            }
                            notifikasjon: {
                              merkelapp: "EtSakssystem"
                              tekst: "Du har fått svar på din søknad"
                              lenke: ${'$'}lenke
                            }
                          }) {
                            __typename
                            ... on NyBeskjedVellykket {
                              id
                            }
                            ... on Error {
                              feilmelding
                            }
                          }
                        }""".trimIndent(),
            "variables" to """
                {
                  "eksternId": "1234-oppdatering",
                  "virksomhetsnummer": "012345678",
                  "lenke": "https://dev.nav.no/sakssystem/?sak=1234"
                }
            """.trimIndent()
        ))
    }

    private suspend fun execute(payload: Map<String, String>): HttpResponse {
        val response: HttpResponse = client.post(endpoint) {
            headers {
                //append(HttpHeaders.Authorization, "Bearer $token")
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Accept, "application/json")
            }
            body = objectMapper.writeValueAsString(payload)
        }
        log.info("${response.status}: ${response.receive<String>()}")
        return response
    }
}

suspend fun main() {
    GraphQLKlient().whoami()
    GraphQLKlient().opprettBeskjed()
}