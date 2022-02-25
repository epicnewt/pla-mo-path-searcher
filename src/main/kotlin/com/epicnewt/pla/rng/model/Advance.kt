package com.epicnewt.pla.rng.model

import kotlinx.serialization.Serializable

@Serializable
data class Advance(val actions: IntArray, val reseeds: List<ReseedSet>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Advance

        if (!actions.contentEquals(other.actions)) return false
        if (reseeds != other.reseeds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = actions.contentHashCode()
        result = 31 * result + reseeds.hashCode()
        return result
    }
}