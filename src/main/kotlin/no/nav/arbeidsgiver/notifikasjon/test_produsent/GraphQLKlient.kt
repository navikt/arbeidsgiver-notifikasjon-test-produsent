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
                        mutation {
                            nyBeskjed(nyBeskjed: {
                                mottaker: {
                                    altinn: {
                                        serviceCode: "4936",
                                        serviceEdition: "1"
                                        virksomhetsnummer: "922658986"
                                    } 
                                }
                                notifikasjon: {
                                    lenke: "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                                    tekst: "Epic lol 🎉"
                                    merkelapp: "fager"
                                }
                                metadata: {
                                    eksternId: "${UUID.randomUUID()}"
                                }
                            }) {
                                ... on NyBeskjedVellykket {
                                    id
                                }
                                ... on Error {
                                    feilmelding
                                }
                            }
                        }""".trimIndent()
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