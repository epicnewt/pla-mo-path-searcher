package com.epicnewt.pla.rng.model.pokemon

import kotlinx.serialization.Serializable

@Serializable
data class IVs(val hp: Int, val att: Int, val def: Int, val spAtt: Int, val spDef: Int, val speed: Int) {
    companion object {
        val EVs = arrayOf(
            0..19 to 0,
            20..25 to 1,
            26..30 to 2,
            31..31 to 3
        ).flatMap { (n, ev) ->
            n.map { ev }
        }

        fun from(ivs: List<Int>): IVs {
            var i = 0
            return IVs(ivs[i++], ivs[i++], ivs[i++], ivs[i++], ivs[i++], ivs[i])
        }
    }

    val evs = arrayOf(hp, att, def, spAtt, spDef, speed).map { EVs[it] }.joinToString("")

    override fun toString(): String {
        return evs
    }
}