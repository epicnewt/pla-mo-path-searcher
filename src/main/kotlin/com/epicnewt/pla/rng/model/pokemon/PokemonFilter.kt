package com.epicnewt.pla.rng.model.pokemon

data class PokemonFilter(
    val ivs: List<Int>?,
    val gender: Int?,
    val nature: Int,
    val isShiny: Boolean,
    val isAlpha: Boolean
)