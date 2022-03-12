package com.epicnewt.pla.rng.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult constructor(val path: List<Int>, val advances: List<Advance>, val nextSeed: ULong) {
    val pathDescription = path.joinToString("§")
        .replace(Regex("-(\\d)§?"), "-$1,")
        .replace(Regex("(-\\d(,-\\d)*),?"), "[$1]")
        .replace("-", "")
        .replace("§", "|")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchResult

        if (advances.last().reseeds != other.advances.last().reseeds) return false

        return true
    }

    override fun hashCode(): Int {
        return advances.last().reseeds.hashCode()
    }


}