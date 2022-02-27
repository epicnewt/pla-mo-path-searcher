package com.epicnewt.pla.rng.plugins

import com.epicnewt.pla.rng.holisticSearch
import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.routing.post
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
        get<AggressiveSearch> { params ->
            val (seed, spawns, rolls, shiny, alpha) = params;
            call.respond(
                holisticSearch(seed.toULong(16), spawns, rolls) { p ->
                    p.shiny == shiny && p.alpha == alpha
                }
            )
        }

        get<AggressiveSearch.Genderless> { params ->
            val (seed, spawns, rolls, shiny, alpha) = params.search;
            call.respond(
                holisticSearch(seed.toULong(16), spawns, rolls, isGenderless = true) { p ->
                    p.shiny == shiny && p.alpha == alpha
                }
            )
        }

        get<PassiveSearch> { params ->
            val (seed, spawns, rolls, shiny, alpha) = params;
            call.respond(
                holisticSearch(seed.toULong(16), spawns, rolls, isAggressive = false) { p ->
                    p.shiny == shiny && p.alpha == alpha
                }
            )
        }

        get<PassiveSearch.Genderless> { params ->
            val (seed, spawns, rolls, shiny, alpha) = params.search;
            call.respond(
                holisticSearch(seed.toULong(16), spawns, rolls, isAggressive = false, isGenderless = true) { p ->
                    p.shiny == shiny && p.alpha == alpha
                }
            )
        }
        post("/holistic-search") {
            val (seed, spawns, rolls, genderRatio) = call.receive<HolisticSearch>()
            println("POST :: ${seed}, ${spawns}, ${rolls}, ${genderRatio}")
            val holisticSearch = holisticSearch(seed.toULong(16), spawns, rolls)
            val response = mapOf("results" to holisticSearch)
            println(response)
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
                    script("text/babel", "/assets/jsx/app.jsx") {  }
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
data class HolisticSearch(val seed: String, val spawns: Int, val rolls: Int, val genderRatio: List<Int>)

@OptIn(KtorExperimentalLocationsAPI::class)
@Location("/v1/aggressive/{seed}/{spawns}")
data class AggressiveSearch(val seed: String, val spawns: Int = 10, val rolls: Int = 26, val shiny: Boolean = true, val alpha: Boolean = true) {
    @Location("/genderless")
    data class Genderless(val search: AggressiveSearch)
}

@OptIn(KtorExperimentalLocationsAPI::class)
@Location("/v1/passive/{seed}/{spawns}")
data class PassiveSearch(val seed: String, val spawns: Int = 10, val rolls: Int = 26, val shiny: Boolean = true, val alpha: Boolean = true, val gender: IntRange = 0..252) {
    @Location("/genderless")
    data class Genderless(val search: PassiveSearch)
}
