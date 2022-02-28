package com.epicnewt.pla.rng.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(val path: List<Int>, val advances: List<Advance>, val nextSeed: String, val remainingSpawns: Int) {
    val pathDescription = path.joinToString("§")
        .replace(Regex("-(\\d)§?"), "-$1,")
        .replace(Regex("(-\\d(,-\\d)*),?"), "[$1]")
        .replace("-", "")
        .replace("§", "|")
}