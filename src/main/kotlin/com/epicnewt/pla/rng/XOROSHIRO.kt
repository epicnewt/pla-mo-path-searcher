package com.epicnewt.pla.rng

private fun rotl(seed: ULong, shift: Int): ULong {
    return seed.shl(shift).or(seed.shr(64 - shift))
}

class XOROSHIRO(private var s0: ULong = 0u, private var s1: ULong = 0x82A2B175229D6A5Bu) {
    fun next(): ULong {
        val result: ULong = s0 + s1
        s1 = s0.xor(s1)
        s0 = rotl(s0, 24).xor(s1).xor((s1.shl(16)))
        s1 = rotl(s1, 37)

        return result
    }

    fun reseed(seed: ULong) {
        s0 = seed;
        s1 = 0x82A2B175229D6A5Bu;
    }

    fun state(): String = (s0 + s1).toString(16).uppercase()

    private fun getMask(x: ULong): ULong {
        var mask = x - 1u;
        for (i in 0 until 6) {
            mask = mask.or(mask.shr(1.shl(i)))
        }
        return mask
    }

    fun rand(n: ULong): ULong {
        val mask = getMask(n)
        var res: ULong;
        do {
            res = next().and(mask)
        } while (res >= n)
        return res;
    }
}