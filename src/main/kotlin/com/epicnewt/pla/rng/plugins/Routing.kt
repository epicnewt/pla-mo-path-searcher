package com.epicnewt.pla.rng.plugins

import com.epicnewt.pla.rng.holisticSearch
import com.epicnewt.pla.rng.model.pokemon.Pokemon
import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import kotlinx.serialization.Serializable

@KtorExperimentalLocationsAPI
fun Application.configureRouting() {
    install(Locations) {
    }

    routing {
        static("assets") {
            resources("assets")
        }

        post("/holistic-search") {
            val (seed, spawns, rolls, genderRatio, gameVersion, agro, fixedGender) = call.receive<HolisticSearch>()
            val matcher: (Pokemon) -> Boolean = { it.shiny && it.alpha }
            val holisticSearch = holisticSearch(
                seed.toULong(16),
                spawns,
                rolls,
                avoidTown = (gameVersion != "1.0.2"),
                matchCount = 1,
                isAggressive = false,
                spawnLimit = if (spawns > 11) spawns - 4 else spawns,
                isGenderless = fixedGender,
                matcher = matcher
            ).let {
                val noneMatchExtra = it.isEmpty() || it.none() { sr ->
                    sr.advances.sumOf { a ->
                        a.reseeds.dropLast(1).sumOf { rs ->
                            rs.pokemon.count(matcher)
                        }
                    } >= 1
                }
                if (noneMatchExtra && spawns > 11) {
                    holisticSearch(
                        seed.toULong(16),
                        spawns,
                        rolls,
                        avoidTown = (gameVersion != "1.0.2"),
                        matchCount = 1,
                        isAggressive = agro,
                        isGenderless = fixedGender
                    )
                } else {
                    it
                }
            }
            val response = mapOf("results" to holisticSearch)
            call.respond(response)
        }

        get("/") {
            val dev = call.application.developmentMode;
            call.respondHtml {
                head {
                    title {
                        +"Pokémon Legends Arceus Path Searcher"
                    }
                    script(src = "https://unpkg.com/react@17/umd/react.${if (dev) "development" else "production.min"}.js") {
                        attributes["crossorigin"] = ""
                    }
                    script(src = "https://unpkg.com/react-dom@17/umd/react-dom.${if (dev) "development" else "production.min"}.js") {
                        attributes["crossorigin"] = ""
                    }
                    script(src = "https://unpkg.com/babel-standalone@6/babel.min.js") {
                        attributes["crossorigin"] = ""
                    }
                    script(src = "https://unpkg.com/react-bootstrap@next/dist/react-bootstrap.min.js") {
                        attributes["crossorigin"] = ""
                    }
                    script(src = "https://unpkg.com/@material-ui/core@5.0.0/umd/material-ui.production.min.js") {
                        attributes["crossorigin"] = ""
                    }
                    script(src = "https://unpkg.com/@mui/material@latest/umd/material-ui.${if (dev) "development" else "production.min"}.js") {
                        attributes["crossorigin"] = ""
                    }
                    styleLink("https://fonts.googleapis.com/css?family=Roboto:300,400,500,700&display=swap")
                    styleLink("https://fonts.googleapis.com/icon?family=Material+Icons")
                    styleLink("/assets/css/index.css")
                    script("text/babel", "/assets/jsx/app.jsx") { }
                }
                body {
                    div {
                        id = "root"
                    }
                }
            }
        }
    }
}

@Serializable
data class HolisticSearch(
    val seed: String,
    val spawns: Int,
    val rolls: Int,
    val genderRatio: List<Int>,
    val gameVersion: String,
    val agro: Boolean,
    val fixedGender: Boolean
)
