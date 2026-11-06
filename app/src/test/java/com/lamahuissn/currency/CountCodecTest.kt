package com.lamahuissn.currency

import com.lamahuissn.currency.data.CountCodec
import com.lamahuissn.currency.data.SavedCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Count tab's storage. A bag counted by hand and lost on restart is the worst failure this
 * screen can have, so the encode/decode pair is tested rather than trusted.
 */
class CountCodecTest {

    @Test
    fun `stacks survive a round trip`() {
        val stacks = mapOf(
            "USD" to mapOf(100L to 7, 20L to 3),
            "LBP" to mapOf(100_000L to 40),
        )
        assertEquals(stacks, CountCodec.decodeStacks(CountCodec.encodeStacks(stacks)))
    }

    @Test
    fun `zero rows are dropped rather than stored`() {
        val stacks = mapOf("USD" to mapOf(100L to 2, 50L to 0))
        val decoded = CountCodec.decodeStacks(CountCodec.encodeStacks(stacks))
        assertEquals(mapOf("USD" to mapOf(100L to 2)), decoded)
    }

    @Test
    fun `an empty or broken store reads as empty instead of crashing`() {
        assertTrue(CountCodec.decodeStacks(null).isEmpty())
        assertTrue(CountCodec.decodeStacks("").isEmpty())
        assertTrue(CountCodec.decodeStacks("{not json").isEmpty())
        assertTrue(CountCodec.decodeRates("[]").isEmpty())
        assertTrue(CountCodec.decodeHistory("nonsense").isEmpty())
    }

    @Test
    fun `a saved count keeps its rates, parts and notes`() {
        val entry = SavedCount(
            at = 1_760_000_000_000,
            totalUsd = 1000.7309,
            notes = 52,
            expected = 1100.0,
            parts = mapOf("USD" to 700.0, "GBP" to 190.0, "LBP" to 4_000_000.0),
            rates = mapOf("USD" to 1.0, "GBP" to 1.3477, "LBP" to 1.0 / 89550.64),
            stacks = mapOf("USD" to mapOf(100L to 7), "GBP" to mapOf(50L to 3, 20L to 2)),
        )
        val decoded = CountCodec.decodeHistory(CountCodec.encodeHistory(listOf(entry))).single()

        assertEquals(entry.at, decoded.at)
        assertEquals(entry.totalUsd, decoded.totalUsd, 1e-9)
        assertEquals(entry.notes, decoded.notes)
        assertEquals(entry.expected, decoded.expected, 1e-9)
        assertEquals(entry.parts, decoded.parts)
        assertEquals(entry.rates.size, decoded.rates.size)
        assertEquals(entry.rates["GBP"]!!, decoded.rates["GBP"]!!, 1e-12)
        assertEquals(entry.stacks, decoded.stacks)
    }

    @Test
    fun `history keeps its order, newest first`() {
        val older = SavedCount(1_000, 10.0, 1, 0.0, mapOf("USD" to 10.0), emptyMap(), mapOf("USD" to mapOf(10L to 1)))
        val newer = SavedCount(2_000, 20.0, 2, 0.0, mapOf("USD" to 20.0), emptyMap(), mapOf("USD" to mapOf(20L to 1)))
        val decoded = CountCodec.decodeHistory(CountCodec.encodeHistory(listOf(newer, older)))
        assertEquals(listOf(2_000L, 1_000L), decoded.map { it.at })
    }

    @Test
    fun `manual rates survive a round trip`() {
        val rates = mapOf("LBP" to 1.0 / 89_000, "USD" to 1.0)
        val decoded = CountCodec.decodeRates(CountCodec.encodeRates(rates))
        assertEquals(rates.keys, decoded.keys)
        assertEquals(rates["LBP"]!!, decoded["LBP"]!!, 1e-15)
    }
}
