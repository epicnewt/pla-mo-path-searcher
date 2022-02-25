package com.epicnewt.pla.rng.model

import com.epicnewt.pla.rng.model.pokemon.Pokemon
import kotlinx.serialization.Serializable

@Serializable
data class ReseedSet(val groupSeed: ULong, val pokemon: List<Pokemon>)