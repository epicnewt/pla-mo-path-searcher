package com.epicnewt.pla.rng

import com.epicnewt.pla.rng.model.Advance
import com.epicnewt.pla.rng.model.ReseedSet
import com.epicnewt.pla.rng.model.SearchResult
import com.epicnewt.pla.rng.model.pokemon.Pokemon
import com.epicnewt.pla.rng.util.pow
import com.epicnewt.pla.rng.util.toHex
import kotlin.math.absoluteValue

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
    matcher: (Pokemon) -> Boolean = { it.alpha && it.shiny }
): List<SearchResult> {
    val multiBattleShift = if (isAggressive) multiBattleMax else 0
    val base = totalSpawns + multiBattleShift - SPAWN_OFFSET
    val despawns = totalSpawns - SPAWN_OFFSET
    val matches = (0.toULong() until base.pow(depth + 1))
        .asSequence()
        .map { it.toString(base).padStart(depth, '0') }
        .map { it.toCharArray() }
        .map { it.map { c -> c.digitToInt(totalSpawns) - multiBattleShift } }
        .map { it.toIntArray() }
        .filter { it.sumOf { i -> i.absoluteValue } <= despawns }
        .map { path -> SearchResult(path.formatAsPath(), followHolisticAggressivePath(seed, rolls, path, isGenderless)) }
        .filter { (_, advances) ->
            val reseeds = advances.last().reseeds
            when (reseeds.size) {
                1 -> reseeds.first().pokemon.any(matcher) // is always an advance of 4
                else -> reseeds.last().pokemon.last().let(matcher)
            }
        }
        .toList()

    if (matches.isNotEmpty()) {
        return matches
    }

    if (depth == 15)
        return emptyList()

    return holisticSearch(seed, totalSpawns, rolls, depth + 1, multiBattleMax, isGenderless, isAggressive, matcher)
}

private fun List<SearchResult>.toHTML() {
    this.forEach { (path, outbreakChain) ->
        println("Path: $path")
        outbreakChain.forEachIndexed { i, (actions, reseeds) ->
            actions.map {
                when (it) {
                    in -4 until 0 -> ""
                    else -> ""
                }
            }
            println("Advance: ${i + 1} (${actions.joinToString("|")})")
            var position = 1
            reseeds.forEachIndexed { o, (groupSeed, pokemon) ->
                println("  Group Seed: ${groupSeed.toHex()}")
                pokemon.forEach {
                    when (o) {
                        0 -> println("    Initial  #${position++}: $it")
                        else -> println("    Respawn  #${position++ - 4}: $it")
                    }
                }
            }
        }
    }
}

private fun IntArray.formatAsPath(): String {
    return this.joinToString("§")
        .replace(Regex("-(\\d)§?"), "-$1,")
        .replace(Regex("(-\\d(,-\\d)*),?"), "[$1]")
        .replace("-", "")
        .replace("§", "|")
}

private fun followHolisticAggressivePath(seed: ULong, rolls: Int, path: IntArray, isGenderless: Boolean): List<Advance> {
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
        spawnGroups.add(ReseedSet(currentGroupSeed, ArrayList(spawnedPokemon)))
        spawnedPokemon.clear()
        currentGroupSeed = mainRng.next()
        mainRng.reseed(currentGroupSeed)
    }

    fun generatePokemon(): Pokemon = run {
        spawnerRng.reseed(mainRng.next())
        mainRng.next() // spawner 1 seed, unused
        val alpha = spawnerRng.next() > 0xFD7720F353A4BBFFu
        Pokemon.fromSeed(spawnerRng.next(), rolls, alpha, isGenderless)
    }

    for (initSpawn in 1..4) {
        spawnedPokemon.add(generatePokemon())
    }

    reseed()

    path.forEach { step ->
        if (step == 0) {
            goToTown()
            for (i in 1..4) { //advance to town
                spawnedPokemon.add(generatePokemon())
            }
            reseed()
        } else if (step < 0) { //aggro multi battle
            actions.add(step)
            for (i in 1..step.absoluteValue) {
                spawnedPokemon.add(generatePokemon())
            }
            reseed()
            isDirty = true
        } else if (step > 0) { // passive
            actions.add(step)
            for (i in 1..step) {
                spawnedPokemon.add(generatePokemon())
                reseed()
            }
            goToTown()
            for (i in 1..4) { //advance to town
                spawnedPokemon.add(generatePokemon())
            }
            reseed()
        }
    }

    if (isDirty) {
        goToTown()
    }

    return advances;
}