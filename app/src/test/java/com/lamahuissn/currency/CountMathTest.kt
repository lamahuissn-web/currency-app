package com.lamahuissn.currency

import com.lamahuissn.currency.data.Banknotes
import com.lamahuissn.currency.data.CountMath
import com.lamahuissn.currency.data.CountState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Count tab's money math. These are the numbers the owner checks against the cash in his hand,
 * so the tests are written as sums he would do himself, not as round-trip property checks.
 */
class CountMathTest {

    private val live = mapOf(
        "USD" to 1.0,
        "GBP" to 1.3477,
        "LBP" to 1.0 / 89550.64,
        "EUR" to 1.1223,
    )

    @Test
    fun `a stack is face value times quantity`() {
        val stack = mapOf(100L to 7, 50L to 3, 20L to 4, 10L to 1, 5L to 2, 1L to 1)
        assertEquals(951.0, CountMath.localTotal(stack), 0.0001)
        assertEquals(18, CountMath.noteCount(stack))
    }

    @Test
    fun `an empty stack is worth nothing and holds no notes`() {
        assertEquals(0.0, CountMath.localTotal(emptyMap()), 0.0)
        assertEquals(0, CountMath.noteCount(emptyMap()))
    }

    @Test
    fun `stepping a denomination up then down returns to nothing`() {
        var stack = CountMath.step(emptyMap(), 100L, 1)
        assertEquals(mapOf(100L to 1), stack)
        stack = CountMath.step(stack, 100L, 1)
        assertEquals(mapOf(100L to 2), stack)
        stack = CountMath.step(stack, 100L, -1)
        assertEquals(mapOf(100L to 1), stack)
        stack = CountMath.step(stack, 100L, -1)
        assertTrue("a zeroed denomination leaves no empty row behind", stack.isEmpty())
    }

    @Test
    fun `minus never goes below zero`() {
        val stack = CountMath.step(emptyMap(), 20L, -1)
        assertTrue(stack.isEmpty())
    }

    @Test
    fun `typing zero clears the denomination`() {
        val stack = CountMath.set(mapOf(50L to 4), 50L, 0)
        assertTrue(stack.isEmpty())
        assertEquals(mapOf(50L to 25), CountMath.set(mapOf(50L to 4), 50L, 25))
    }

    @Test
    fun `mixed bag totals in usd`() {
        val counts = mapOf(
            "USD" to mapOf(100L to 7),                       // 700.00
            "GBP" to mapOf(50L to 3, 20L to 2),              // 190.00 -> 256.06
            "LBP" to mapOf(100_000L to 40),                  // 4,000,000 -> 44.67
        )
        val grand = CountMath.grandTotal(listOf("USD", "GBP", "LBP"), counts, live, emptyMap())
        assertEquals(1000.7309, grand.usd, 0.001)
        assertEquals(52, grand.notes)
        assertEquals("$1,000.73", CountMath.usd(grand.usd))
    }

    @Test
    fun `a currency left out of the bag contributes nothing`() {
        val counts = mapOf("GBP" to mapOf(50L to 3))   // 150 GBP
        val without = CountMath.grandTotal(listOf("USD"), counts, live, emptyMap())
        assertEquals(0.0, without.usd, 0.0)
        val with = CountMath.grandTotal(listOf("GBP"), counts, live, emptyMap())
        assertEquals(150 * 1.3477, with.usd, 0.0001)
    }

    @Test
    fun `a manual rate wins over the market`() {
        // LBP at the street rate the owner actually counts at: 89,000 to the dollar.
        val manual = mapOf("LBP" to 1.0 / 89_000)
        val counts = mapOf("LBP" to mapOf(100_000L to 10))  // 1,000,000 LBP
        val grand = CountMath.grandTotal(listOf("LBP"), counts, live, manual)
        assertEquals(1_000_000.0 / 89_000, grand.usd, 0.0001)
        assertEquals(1.0 / 89_000, CountMath.rateFor("LBP", live, manual), 1e-12)
    }

    @Test
    fun `a currency with no rate anywhere is not silently priced`() {
        val counts = mapOf("SYP" to mapOf(5_000L to 2))
        val grand = CountMath.grandTotal(listOf("SYP"), counts, live, emptyMap())
        assertEquals("no rate means no invented total", 0.0, grand.usd, 0.0)
        assertEquals(2, grand.notes)
        assertFalse(CountMath.hasRate("SYP", live, emptyMap()))
        assertTrue(CountMath.hasRate("USD", live, emptyMap()))
    }

    @Test
    fun `the verdict compares the converted total with what was expected`() {
        assertEquals(CountMath.Verdict.None, CountMath.verdict(951.0, 0.0).verdict)
        assertEquals(CountMath.Verdict.Match, CountMath.verdict(1000.0, 1000.0).verdict)

        val short = CountMath.verdict(951.0, 1000.0)
        assertEquals(CountMath.Verdict.Short, short.verdict)
        assertEquals(49.0, short.difference, 0.0001)
        assertEquals("Short $49.00", "Short " + CountMath.usd(short.difference))

        val over = CountMath.verdict(1100.0, 1000.0)
        assertEquals(CountMath.Verdict.Over, over.verdict)
        assertEquals(100.0, over.difference, 0.0001)
    }

    @Test
    fun `a cent of drift still reads as a match`() {
        assertEquals(CountMath.Verdict.Match, CountMath.verdict(1000.004, 1000.0).verdict)
        assertEquals(CountMath.Verdict.Short, CountMath.verdict(999.99, 1000.0).verdict)
    }

    @Test
    fun `money reads the way a person writes it`() {
        assertEquals("$1,000.73", CountMath.usd(1000.7309))
        assertEquals("$0.00", CountMath.usd(0.0))
        assertEquals("$49.00", CountMath.usd(49.0))
        assertEquals("4,000,000", CountMath.group(4_000_000.0))
        assertEquals("ل.ل 4,000,000", CountMath.local(4_000_000.0, "ل.ل "))
        assertEquals("$1.3477", CountMath.rate(1.3477))
        assertEquals("$89,551", CountMath.rate(89550.64))
        assertEquals("$0.00001117", CountMath.rate(1.0 / 89550.64))
    }

    @Test
    fun `the bag only offers currencies that have real banknotes`() {
        assertTrue(Banknotes.has("USD"))
        assertTrue(Banknotes.has("LBP"))
        assertFalse(Banknotes.has("BTC"))
        assertFalse(Banknotes.has("XAU"))
        assertTrue("every offered currency has a ladder", Banknotes.supported.all { Banknotes.ladder(it).isNotEmpty() })
        assertEquals(listOf(100L, 50L, 20L, 10L, 5L, 1L), Banknotes.ladder("USD"))
        assertEquals(listOf(100_000L, 50_000L, 20_000L, 10_000L, 5_000L, 1_000L), Banknotes.ladder("LBP"))
        assertEquals(listOf("USD", "LBP"), Banknotes.defaultBag)
    }

    @Test
    fun `unsaved changes are detectable from the counted content alone`() {
        val base = CountState(bag = listOf("USD"))
        assertTrue("an untouched bag is saved", base.fingerprint() == base.savedFingerprint)

        val counted = base.copy(counts = mapOf("USD" to mapOf(100L to 1)))
        assertTrue(counted.fingerprint() != counted.savedFingerprint)

        val sameAgain = counted.copy(counts = mapOf("USD" to mapOf(100L to 1)))
        assertEquals("the same count is the same fingerprint", counted.fingerprint(), sameAgain.fingerprint())

        val more = counted.copy(counts = mapOf("USD" to mapOf(100L to 2)))
        assertTrue(counted.fingerprint() != more.fingerprint())

        val zeroRows = counted.copy(counts = mapOf("USD" to mapOf(100L to 1, 50L to 0)))
        assertEquals("a zero row must not count as a change", counted.fingerprint(), zeroRows.fingerprint())
    }
}
