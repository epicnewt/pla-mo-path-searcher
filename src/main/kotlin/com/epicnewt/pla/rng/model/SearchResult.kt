package com.epicnewt.pla.rng.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(val path: String, val advances: List<Advance>) {
}