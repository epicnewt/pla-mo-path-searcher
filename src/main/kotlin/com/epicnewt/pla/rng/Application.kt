package com.epicnewt.pla.rng

import com.epicnewt.pla.rng.plugins.configureHTTP
import com.epicnewt.pla.rng.plugins.configureRouting
import com.epicnewt.pla.rng.plugins.configureSerialization
import com.epicnewt.pla.rng.plugins.configureTemplating
import io.ktor.locations.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*

@OptIn(KtorExperimentalLocationsAPI::class)
fun main() {
    embeddedServer(CIO, watchPaths = listOf("classes", "resources"), port = System.getenv("PORT")?.toInt() ?: 9090) {
        configureRouting()
        configureHTTP()
        configureSerialization()
        configureTemplating()
    }.start(wait = true)
}
