package com.epicnewt.pla.rng

import com.epicnewt.pla.rng.plugins.configureHTTP
import com.epicnewt.pla.rng.plugins.configureRouting
import com.epicnewt.pla.rng.plugins.configureSerialization
import com.epicnewt.pla.rng.plugins.configureTemplating
import io.ktor.locations.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*

@OptIn(KtorExperimentalLocationsAPI::class)
fun main() {
    embeddedServer(CIO, port = System.getenv("PORT")?.toInt() ?: 9090, host = "127.0.0.1") {
        configureRouting()
        configureHTTP()
        configureSerialization()
        configureTemplating()
    }.start(wait = true)
}
