package com.epicnewt.pla.rng.util

fun ULong.toHex() = this.toString(16).uppercase()

fun Int.pow(i: Int): ULong {
    if (i < 2)
        return 1u;
    return (0.toULong() until i.toULong()).reduce { acc, _ ->
        if (acc == 0.toULong()) this@pow.toULong() else acc * this@pow.toULong()
    }
}