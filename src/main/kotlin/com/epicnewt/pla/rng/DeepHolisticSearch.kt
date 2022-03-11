package com.epicnewt.pla.rng

import com.epicnewt.pla.rng.model.SearchResult
import com.epicnewt.pla.rng.model.pokemon.Pokemon
import java.lang.Integer.min
import kotlin.math.absoluteValue

fun deepHolisticSearch(initialSeed: ULong, species: Int, totalSpawns: Int, rolls: Int, isAggressive: Boolean, matcher: (Pokemon) -> Boolean): Collection<SearchResult> {
    //sugar
    fun search(
        spawns: Int = totalSpawns, aggressive: Boolean = isAggressive, ignoreFirstMatch: Boolean = false, seed: ULong = initialSeed, spawnInitial: Boolean = false,
    ) = holisticSearch(
        seed, species, spawns, rolls, aggressive,
        matchCount = if (ignoreFirstMatch) 2 else 1,
        spawnInitial = spawnInitial,
        matcher = matcher
    )

    return search(min(10, totalSpawns), false).mapNotNull { result ->
        val lastAdvance = result.advances.last()
        val remainingSpawns = totalSpawns - result.path.sumOf { it.absoluteValue }
        val extendedSearch: Collection<SearchResult> = when (lastAdvance.reseeds.size) {
            1 -> search( // spawns initial
                remainingSpawns,
                aggressive = false, //faster & best chance
                ignoreFirstMatch = true,
                seed = lastAdvance.reseeds.last().groupSeed.toULong(16)
            )
            else -> search(
                remainingSpawns,
                aggressive = false, //faster & best chance
                ignoreFirstMatch = true,
                spawnInitial = false,
                seed = lastAdvance.reseeds.last().groupSeed.toULong(16)
            ) // if last spawn group has only one we can save a spawn by going back to town
            // ... unless I optimise the search to do that by default for passive mon actions
            // a trailing integer on a passive path can be reduced by one and replaced with a trip to town
        }

        extendedSearch.ifEmpty { null }
    }.flatten().ifEmpty {
        search()
    }}