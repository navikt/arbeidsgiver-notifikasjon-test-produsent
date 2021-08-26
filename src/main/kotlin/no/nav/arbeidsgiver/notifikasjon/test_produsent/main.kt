package no.nav.arbeidsgiver.notifikasjon.test_produsent
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory


fun main() {
    val log = LoggerFactory.getLogger("main")
    log.info("hello world")

    embeddedServer(Netty, port = 8080) {
        routing {
            get("ok") {
                call.respondText("i'm ok")
            }

            get("doStuff") {
                log.info("doing stuff")
                call.respondText("stuff done")
            }
        }
    }.start(wait = true)
}
