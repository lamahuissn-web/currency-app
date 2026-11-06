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
 *
 * The live table is the same one the Convert tab uses: **units of that currency per 1 USD**
 * (LBP 89,551 · GBP 0.742 · EUR 0.8905), which is why a stack is divided into dollars.
 */
class CountMathTest {

    private val live = mapOf(
        "USD" to 1.0,
        "GBP" to 0.742,
        "EUR" to 0.8905,
        "LBP" to 89_550.64,
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
        assertTrue(CountMath.step(emptyMap(), 20L, -1).isEmpty())
    }

    @Test
    fun `typing zero clears the denomination`() {
        assertTrue(CountMath.set(mapOf(50L to 4), 50L, 0).isEmpty())
        assertEquals(mapOf(50L to 25), CountMath.set(mapOf(50L to 4), 50L, 25))
    }

    @Test
    fun `a stack of lebanese pounds is worth cents, not billions`() {
        // The exact case that shipped broken: 50,000 LBP counted as $4,475,000,000.00 because the
        // rate was multiplied instead of divided.
        val usd = CountMath.toUsd(50_000.0, "LBP", live, emptyMap())
        assertEquals(0.5583, usd, 0.0001)
        assertEquals("$0.56", CountMath.usd(usd))
        assertTrue("50,000 LBP can never be worth more than a dollar", usd < 1.0)
    }

    @Test
    fun `mixed bag totals in usd`() {
        val counts = mapOf(
            "USD" to mapOf(100L to 7),                 // 700.00
            "GBP" to mapOf(50L to 3, 20L to 2),        // 190.00 -> 256.06
            "LBP" to mapOf(100_000L to 40),            // 4,000,000 -> 44.67
        )
        val grand = CountMath.grandTotal(listOf("USD", "GBP", "LBP"), counts, live, emptyMap())
        assertEquals(1000.7326, grand.usd, 0.01)
        assertEquals(52, grand.notes)
        assertEquals("$1,000.73", CountMath.usd(grand.usd))
    }

    @Test
    fun `one euro is worth more than one dollar`() {
        // The table says 0.8905 EUR per USD, so 100 EUR is about 112 USD — the other direction of
        // the same trap, and the one a European stack would expose.
        assertEquals(112.30, CountMath.toUsd(100.0, "EUR", live, emptyMap()), 0.01)
    }

    @Test
    fun `a currency left out of the bag contributes nothing`() {
        val counts = mapOf("GBP" to mapOf(50L to 3))   // 150 GBP
        assertEquals(0.0, CountMath.grandTotal(listOf("USD"), counts, live, emptyMap()).usd, 0.0)
        assertEquals(150.0 / 0.742, CountMath.grandTotal(listOf("GBP"), counts, live, emptyMap()).usd, 0.0001)
    }

    @Test
    fun `a manual rate wins over the market`() {
        // LBP at the street rate the owner actually counts at: 89,000 to the dollar.
        val manual = mapOf("LBP" to 89_000.0)
        val counts = mapOf("LBP" to mapOf(100_000L to 10))  // 1,000,000 LBP
        assertEquals(
            1_000_000.0 / 89_000,
            CountMath.grandTotal(listOf("LBP"), counts, live, manual).usd,
            0.0001,
        )
        assertEquals(89_000.0, CountMath.rateFor("LBP", live, manual), 0.0)
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
    fun `a zero rate is treated as missing, never as a divide by zero`() {
        assertEquals(0.0, CountMath.toUsd(50_000.0, "LBP", live, mapOf("LBP" to 0.0)), 0.0)
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
    }

    @Test
    fun `a rate prints the way the convert tab prints it`() {
        assertEquals("89,551", CountMath.rate(89_550.64))
        assertEquals("0.7420", CountMath.rate(0.742))
        assertEquals("0", CountMath.rate(0.0))
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

    @Test
    fun `restoring a saved stack reproduces its total`() {
        val stacks = mapOf(
            "USD" to mapOf(100L to 7),
            "GBP" to mapOf(50L to 3, 20L to 2),
        )
        val restored = CountMath.grandTotal(listOf("USD", "GBP"), stacks, live, emptyMap())
        // 700 + 190 / 0.742 = 956.06
        assertEquals(956.0647, restored.usd, 0.01)
        assertEquals(
            "the restored fingerprint matches what was saved",
            CountState.fingerprintOf(stacks),
            CountState(counts = stacks).fingerprint(),
        )
    }
}
