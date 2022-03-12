package com.epicnewt.pla.rng

import com.epicnewt.pla.rng.model.Advance
import com.epicnewt.pla.rng.model.ReseedSet
import com.epicnewt.pla.rng.model.SearchResult
import com.epicnewt.pla.rng.model.pokemon.Pokemon
import com.epicnewt.pla.rng.util.pow
import com.epicnewt.pla.rng.util.toHex
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

const val MULTI_BATTLE_MAX = 3;
const val SPAWN_OFFSET = 4;

fun isDigitSumOverLimit(n: ULong, radix: Int, depth: Int, limit: Int, offset: Int): Boolean {
    tailrec fun sumUnsignedDigits(n: ULong, radix: ULong, limit: ULong, sum: ULong): Boolean {
        if (sum > limit)
            return true
        if (n == ULong.MIN_VALUE)
            return false

        return sumUnsignedDigits(n / radix, radix, limit, sum + n % radix)
    }

    tailrec fun sumSignedDigits(n: ULong, radix: ULong, length: Int, limit: Int, sum: Int): Boolean {
//        println("n: $n, radix: $radix, length: $length, limit: $limit, sum: $sum")
        if (sum > limit)
            return true
        if (length == 0)
            return false

        val add = if (n == ULong.MIN_VALUE) offset else ((n % radix).toInt() - offset).absoluteValue

        return sumSignedDigits(n / radix, radix, length - 1, limit, sum + add)
    }

    return if (offset == 0) {
        sumUnsignedDigits(n, radix.toULong(), limit.toULong(), 0u)
    } else {
        sumSignedDigits(n, radix.toULong(), depth, limit, 0)
    }
}

fun holisticSearch(
    seed: ULong,
    species: Int,
    totalSpawns: Int,
    rolls: Int,
    isAggressive: Boolean = true,
    matchCount: Int = 1,
    spawnInitial: Boolean = true,
    multiBattleMax: Int = MULTI_BATTLE_MAX,
    depth: Int = 1,
    maxDepth: Int = 10,
    matcher: (Pokemon) -> Boolean
): Collection<SearchResult> {
    val multiBattleShift = if (isAggressive) multiBattleMax else 0
    val uMultiBattleShift = multiBattleShift.toULong()
    val base = totalSpawns + multiBattleShift - SPAWN_OFFSET
    val uBase = base.toULong()
    val despawns = totalSpawns - SPAWN_OFFSET

    val matchesFound = ArrayList<SearchResult>()

    val start: ULong = if (matchCount == 1) 0u else 10u
    val lastPathN = ((base - 1).toUInt() * base.pow(depth))
    for (i in (start..lastPathN)) {
        val isRedundant = ((i % uBase)) > uMultiBattleShift
        if (
            depth != maxDepth && isRedundant
            || isDigitSumOverLimit(i, base, depth, despawns, multiBattleShift)
        ) continue

        val pathStr = i.toString(base).padStart(depth, '0') // optimise this next
        var path = pathStr.toCharArray().map { c -> c.digitToInt(totalSpawns) - multiBattleShift } // optimise this next
        val advance = lastAdvanceOfHolisticAggressivePath(seed, species, rolls, path, spawnInitial) // much faster
        var isMatch = when (advance.reseeds.size) {
            1 -> advance.reseeds.first().pokemon.any(matcher) // is always an advance of 4
            else -> advance.reseeds.last().pokemon.last().let(matcher)
        }

        val (advances, nextSeed) = if (isMatch) {
            if (isRedundant)
                path = path.dropLast(1).plus(path.last() - 1).plus(0)

            followHolisticAggressivePath(seed, species, rolls, path, spawnInitial) //full seed
        } else {
            listOf<Advance>() to ULong.MIN_VALUE
        }

        if (isMatch && matchCount > 1) {
            val additionalMatches = advances.drop(1).sumOf { advance ->
                advance.reseeds.dropLast(1).sumOf {
                    it.pokemon.count(matcher)
                }
            } + 1

            isMatch = additionalMatches >= matchCount
        }

        if (isMatch) {
            matchesFound.add(SearchResult(path, advances, nextSeed))
        }
    }

    if (matchesFound.isNotEmpty())
        return matchesFound.toSet()

    if (depth == maxDepth)
        return emptyList()

    return holisticSearch(seed, species, totalSpawns, rolls, isAggressive, matchCount, spawnInitial, multiBattleMax, depth + 1, maxDepth, matcher)
}

private fun lastAdvanceOfHolisticAggressivePath(seed: ULong, species: Int, rolls: Int, path: List<Int>, spawnInitial: Boolean): Advance {
    val mainRng = XOROSHIRO(seed)
    var latestSeed = seed

    fun spawn(n: Int) {
        repeat(n * 2) { mainRng.next() }
        latestSeed = mainRng.next()
        mainRng.reseed(latestSeed)
    }

    var backToTown = spawnInitial
    val advances = path.dropLast(1).dropLastWhile { it < 0 }

    advances.forEach { action ->
        if (backToTown) {
            backToTown = false
            spawn(4)
        }
        when {
            action < 0 -> spawn(action.absoluteValue)
            action == 0 -> backToTown = true
            else -> {
                repeat(action) { spawn(1) }
                backToTown = true
            }
        }
    }

    val (finalAdvance, _) = followHolisticAggressivePath(latestSeed, species, rolls, path.drop(advances.size), spawnInitial)
    return finalAdvance.first() // there should only be one if we did this right
}

private fun followHolisticAggressivePath(seed: ULong, species: Int, rolls: Int, path: List<Int>, spawnInitial: Boolean): Pair<ArrayList<Advance>, ULong> {
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
        if (spawnGroups.isNotEmpty()) {
            advances.add(Advance(actions.toIntArray(), ArrayList(spawnGroups)))
            actions.clear()
            spawnGroups.clear()
        }
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
        spawnedPokemon.add(Pokemon.fromSeed(spawnerRng.next(), species, rolls, alpha))
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
            isDirty = advances.isEmpty()
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

    return advances to nextSeed(advances.last());
}

private fun nextSeed(advances: Advance): ULong {
    val (groupSeed, pokemon) = advances.reseeds.last()

    val mainRng = XOROSHIRO(groupSeed.toULong(16))

    for (p in pokemon) {
        mainRng.next()
        mainRng.next()
    }

    return mainRng.next()
}

@OptIn(ExperimentalTime::class)
fun benchmark(block: () -> Unit): Duration {
    repeat(100000) { block() }

    return measureTime {
        repeat(1000000) { block() }
    }
}

fun main() {
    val seed = "3C45448D30518872".toULong(16)

    val radix = 11;

    val tests = listOf(
        "1111",
        "7181",
        "456",
        "47",
        "1",
        "0"
    ).map { it.toInt(radix) }.map {
        it.toString(radix).padStart(4, '0').toCharArray().map { (it.digitToInt(radix) - 3).absoluteValue } to it
    }

    tests.forEach { (expectedSum, num) ->
        val sum = expectedSum.sum()
        println("Num: ${num.toString(radix).padStart(4, '0')}, Sum: ${expectedSum.joinToString("+")} = $sum")
        print("expectedFalse(${sum + 1}): ${isDigitSumOverLimit(num.toULong(), radix, 4, sum + 1, 3)}, ")
        print("expectedFalse($sum): ${isDigitSumOverLimit(num.toULong(), radix, 4, sum, 3)}, ")
        println("expectedTrue(${sum - 1}): ${isDigitSumOverLimit(num.toULong(), radix, 4, sum - 1, 3)}")
    }

}