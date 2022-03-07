package com.epicnewt.pla.rng

import com.epicnewt.pla.rng.model.Advance
import com.epicnewt.pla.rng.model.ReseedSet
import com.epicnewt.pla.rng.model.SearchResult
import com.epicnewt.pla.rng.model.pokemon.Pokemon
import com.epicnewt.pla.rng.util.pow
import com.epicnewt.pla.rng.util.toHex
import java.lang.Integer.max
import kotlin.math.absoluteValue
import kotlin.math.min

const val MULTI_BATTLE_MAX = 3;
const val SPAWN_OFFSET = 5;

fun holisticSearch(
    seed: ULong,
    totalSpawns: Int,
    rolls: Int,
    depth: Int = 1,
    multiBattleMax: Int = MULTI_BATTLE_MAX,
    isGenderless: Boolean = false,
    isAggressive: Boolean = true,
    avoidTown: Boolean = false,
    matchCount: Int = 1,
    spawnLimit: Int = 15,
    spawnInitial: Boolean = true,
    matcher: (Pokemon) -> Boolean = { it.alpha && it.shiny },
): List<SearchResult> {
    val multiBattleShift = if (isAggressive) multiBattleMax + (if (avoidTown) 1 else 0) else 0
    val base = if (avoidTown) multiBattleShift else min(totalSpawns, spawnLimit) + multiBattleShift - SPAWN_OFFSET
    val townChar = multiBattleShift.digitToChar(base + (if (avoidTown) 1 else 0))
    val despawns = min(totalSpawns, spawnLimit) - SPAWN_OFFSET

    val matches = ArrayList<SearchResult>()

    val start: ULong = if (matchCount == 1) 0u else 10u
    for (i in (start until base.pow(depth + 1))) {
        val pathStr = if (avoidTown) i.toString(base) else i.toString(base).padStart(depth, '0')
        if (avoidTown && pathStr.contains(townChar))
            continue
        val path = pathStr.toCharArray().map { c -> c.digitToInt(totalSpawns) - multiBattleShift }
        if (isAggressive && path.isRedundant())
            continue
        if (path.sumOf { it.absoluteValue } > despawns)
            continue
        val (advances, nextSeed) = followHolisticAggressivePath(seed, rolls, path, isGenderless, spawnInitial)
        val reseeds = advances.last().reseeds
        var isMatch = when (reseeds.size) {
            1 -> reseeds.first().pokemon.any(matcher) // is always an advance of 4
            else -> reseeds.last().pokemon.last().let(matcher)
        }

        if (isMatch && matchCount > 1) {
            val additionalMatches = advances.sumOf { advance ->
                advance.reseeds.dropLast(1).sumOf {
                    it.pokemon.count(matcher)
                }
            } + 1

            isMatch = additionalMatches >= matchCount
        }

        if (isMatch) {
            matches.add(SearchResult(path, advances, nextSeed, (totalSpawns - 1) - path.sumOf { it.absoluteValue }))
        }
    }

    if (matches.isNotEmpty()) {
        if (spawnLimit < totalSpawns) {
            return matches.flatMap { match ->
                val despawned = match.path.sumOf { it.absoluteValue } + 1
                val furtherMatches = holisticSearch(match.nextSeed.toULong(16), totalSpawns - despawned, rolls, 1, multiBattleMax, isGenderless, isAggressive, avoidTown, matchCount, spawnLimit, spawnInitial, matcher)
                furtherMatches.flatMap { fm ->
                    val path = match.path.plus(0).plus(fm.path)
                    val advances = match.advances.plus(fm.advances)

                    SearchResult(path, advances, fm.nextSeed, fm.remainingSpawns)
                    furtherMatches
                }.ifEmpty { listOf(match) }
            }
        }

        return matches
    }

    if (depth == 10 && !avoidTown)
        return emptyList()

    if (depth == (totalSpawns - 5) && avoidTown)
        return emptyList()

    return holisticSearch(seed, totalSpawns, rolls, depth + 1, multiBattleMax, isGenderless, isAggressive, avoidTown, matchCount, spawnLimit, spawnInitial, matcher)
}

private fun <T> Iterable<T>.isRedundant(): Boolean {
    return last() == -1
}

private fun followHolisticAggressivePath(seed: ULong, rolls: Int, path: List<Int>, isGenderless: Boolean, spawnInitial: Boolean): Pair<List<Advance>, String> {
    val advances = ArrayList<Advance>()
    val spawnGroups = ArrayList<ReseedSet>()
    val actions = ArrayList<Int>()
    val spawnedPokemon = ArrayList<Pokemon>()
    var currentGroupSeed = seed
    val mainRng = XOROSHIRO(seed)
    val spawnerRng = XOROSHIRO()
    var isDirty = false

    fun goToTown() {
        isDirty = false
        advances.add(Advance(actions.toIntArray(), ArrayList(spawnGroups)))
        actions.clear()
        spawnGroups.clear()
    }

    fun reseed() {
        spawnGroups.add(ReseedSet(currentGroupSeed.toHex(), ArrayList(spawnedPokemon)))
        spawnedPokemon.clear()
        currentGroupSeed = mainRng.next()
        mainRng.reseed(currentGroupSeed)
    }

    fun generatePokemon() {
        spawnerRng.reseed(mainRng.next())
        mainRng.next() // spawner 1 seed, unused
        val alpha = spawnerRng.next() > 0xFD7720F353A4BBFFu && spawnedPokemon.none { it.alpha }
        spawnedPokemon.add(Pokemon.fromSeed(spawnerRng.next(), rolls, alpha, isGenderless))
    }

    if (spawnInitial) {
        for (initSpawn in 1..4) {
            generatePokemon()
        }

        reseed()
    }

    path.forEach { step ->
        if (step == 0) {
            goToTown()
            for (i in 1..4) { //advance to town
                generatePokemon()
            }
            reseed()
        } else if (step < 0) { //aggro multi battle
            actions.add(step)
            for (i in 1..step.absoluteValue) {
                generatePokemon()
            }
            reseed()
            isDirty = true
        } else if (step > 0) { // passive
            actions.add(step)
            for (i in 1..step) {
                generatePokemon()
                reseed()
            }
            goToTown()
            for (i in 1..4) { //advance to town
                generatePokemon()
            }
            reseed()
        }
    }

    if (isDirty) {
        goToTown()
    }

    return advances to nextSeed(advances).toHex();
}

private fun nextSeed(advances: ArrayList<Advance>): ULong {
    val (groupSeed, pokemon) = advances.last().reseeds.last()

    val mainRng = XOROSHIRO(groupSeed.toULong(16))

    for (p in pokemon) {
        mainRng.next()
        mainRng.next()
    }

    mainRng.reseed(mainRng.next())
    mainRng.next()
    mainRng.next()

    return mainRng.next()
}
