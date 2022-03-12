package com.epicnewt.pla.rng

import com.epicnewt.pla.rng.model.SearchResult
import com.epicnewt.pla.rng.model.pokemon.Pokemon
import java.lang.Integer.min
import kotlin.math.absoluteValue
import kotlin.math.sign

fun deepHolisticSearch(initialSeed: ULong, species: Int, totalSpawns: Int, rolls: Int, isAggressive: Boolean, matcher: (Pokemon) -> Boolean): Collection<SearchResult> {
    //sugar
    fun search(
        spawns: Int = totalSpawns, aggressive: Boolean = isAggressive, ignoreFirstMatch: Boolean = false, seed: ULong = initialSeed, spawnInitial: Boolean = true,
    ) = holisticSearch(
        seed, species, spawns, rolls, aggressive,
        matchCount = if (ignoreFirstMatch) 2 else 1,
        spawnInitial = spawnInitial,
        matcher = matcher
    )

    val firstSearch = search(min(10, totalSpawns), false)
    return firstSearch.mapNotNull { result ->
        val lastAdvance = result.advances.last()
        val remainingSpawns = totalSpawns - result.path.dropLast(1).dropLastWhile { it < 0 }.sumOf { it.absoluteValue }
        val extendedSearch: Collection<SearchResult> = search( // spawns initial
            remainingSpawns,
            aggressive = false, //faster & best chance
            ignoreFirstMatch = true,
            seed = lastAdvance.reseeds.first().groupSeed.toULong(16)
        )
        // if last spawn group has only one we can save a spawn by going back to town
        // ... unless I optimise the search to do that by default for passive mon actions
        // a trailing integer on a passive path can be reduced by one and replaced with a trip to town

        extendedSearch.mergeWith(result).ifEmpty { null }
    }.flatten().ifEmpty {
        if (isAggressive) search().plus(firstSearch).toSet() else firstSearch
    }
}

private fun Collection<SearchResult>.mergeWith(result: SearchResult): Collection<SearchResult> = map { it.mergeWith(result) }


private fun SearchResult.mergeWith(first: SearchResult): SearchResult = SearchResult(
    mergePaths(first.path, path),
    first.advances.dropLast(1).plus(advances),
    nextSeed
)

fun mergePaths(first: List<Int>, last: List<Int>): List<Int> {
    return first.dropLast(1).dropLastWhile { it < 0 }.plus(last)
}

fun main() {
    deepHolisticSearch("764FAD6B9D19C9AD".toULong(16), 133, 12, 27, true) { it.shiny && it.alpha }.forEach {
        it.run {
            println(pathDescription)
        }
    }
}