package com.epicnewt.pla.rng.model.pokemon

import com.epicnewt.pla.rng.XOROSHIRO
import com.epicnewt.pla.rng.util.toHex
import kotlinx.serialization.Serializable

@Serializable
data class Pokemon(
    val seed: String,
    val sidtid: ULong,
    val pid: ULong,
    val ivs: IVs,
    val nature: Nature,
    val gender: Int?,
    val shiny: Boolean,
    val alpha: Boolean
) {
    companion object {
        val rng = XOROSHIRO(0u)
        fun fromSeed(
            seed: ULong,
            rolls: Int,
            isAlpha: Boolean,
            isGenderless: Boolean,
            perfectIVs: Int = if (isAlpha) 3 else 0
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
            val nature: Int
            val gender: UInt? = if (isGenderless) null else rng.rand(252u).toUInt() + 1u
            nature = rng.rand(25u).toInt()
            return Pokemon(
                seed.toHex(),
                sidtid,
                pid,
                IVs.from(ivs),
                Nature.get(nature),
                gender?.toInt(),
                shiny,
                isAlpha
            )
        }
    }
}