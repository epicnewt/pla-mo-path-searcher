package com.epicnewt.pla.rng.plugins

import com.epicnewt.pla.rng.holisticSearch
import io.ktor.application.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*

@KtorExperimentalLocationsAPI
fun Application.configureRouting() {
    install(Locations) {
    }

    routing {
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
    }
}

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
