package com.epicnewt.pla.rng.model.pokemon

import com.epicnewt.pla.rng.XOROSHIRO
import com.epicnewt.pla.rng.util.toHex
import kotlinx.serialization.Serializable

const val MALE_ONLY = 0
const val FEMALE_ONLY = ((32 * 8) - 1)
const val NO_GENDER = -1

enum class Gender {
    MALE, FEMALE, NONE
}

@Serializable
data class Pokemon(
    val seed: String,
    val sidtid: ULong,
    val pid: ULong,
    val ivs: IVs,
    val nature: Nature,
    val gender: Gender,
    val shiny: Boolean,
    val alpha: Boolean
) {
    companion object {
        val rng = XOROSHIRO(0u)
        fun fromSeed(
            seed: ULong,
            species: Int,
            rolls: Int,
            isAlpha: Boolean,
            perfectIVs: Int = if (isAlpha) 3 else 0,
        ): Pokemon {
            rng.reseed(seed)
            val encryption_constant = rng.rand(0xFFFFFFFFu)
            val sidtid = rng.rand(0xFFFFFFFFu)
            var pid: ULong = 0u;
            var shiny: Boolean = false;
            for (i in 0 until rolls) {
                pid = rng.rand(0xFFFFFFFFu)
//                shiny = ((pid >> 16) ^ (sidtid >> 16) ^ (pid & 0xFFFF) ^ (sidtid & 0xFFFF)) < 0x10
                shiny = (pid.shr(16)).xor(sidtid.shr(16)).xor(pid.and(0xFFFFu)).xor(sidtid.and(0xFFFFu)) < 0x10u
                if (shiny)
                    break;
            }

            val blank = 50
            val ivs: MutableList<Int> = listOf(blank, blank, blank, blank, blank, blank).toMutableList()
            for (i in 0 until perfectIVs) {
                var index = rng.rand(6u).and(0xFFFFu).toInt()
                while (ivs[index] != blank)
                    index = rng.rand(6u).and(0xFFFFu).toInt()
                ivs[index] = 31
            }
            for (i in 0 until 6) {
                if (ivs[i] == blank)
                    ivs[i] = rng.rand(32u).toInt()
            }
            val ability = rng.rand(2u).toUInt()

            val gender = when (val ratio = genderRatios[species] ?: -1) {
                MALE_ONLY -> Gender.MALE
                FEMALE_ONLY -> Gender.FEMALE
                NO_GENDER -> Gender.NONE
                else -> if (rng.rand(252u).toInt() + 1 < ratio) Gender.FEMALE else Gender.MALE
            }

            val nature: Int = rng.rand(25u).toInt()
            return Pokemon(
                seed.toHex(),
                sidtid,
                pid,
                IVs.from(ivs),
                Nature.get(nature),
                gender,
                shiny,
                isAlpha
            )
        }
    }
}