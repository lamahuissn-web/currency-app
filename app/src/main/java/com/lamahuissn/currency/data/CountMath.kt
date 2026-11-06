package com.lamahuissn.currency.data

import java.util.Locale
import kotlin.math.abs

/**
 * The money math behind the Count tab, kept out of Compose so it can be tested without a device.
 *
 * ONE CONVENTION, and it is the rate table's own: every rate is **units of that currency per 1 USD**
 * — the same numbers the Convert tab prints (LBP 89,550 · GBP 0.742). A count therefore becomes
 * dollars by dividing, never by multiplying. Getting that backwards once turned 50,000 LBP into
 * 4.4 billion dollars, so the direction is asserted in the tests.
 */
object CountMath {

    /** How near counts as a match, in USD — a cent is the smallest unit the total can show. */
    const val MATCH_EPSILON = 0.005

    data class Grand(val usd: Double, val notes: Int)

    enum class Verdict { None, Match, Short, Over }

    data class Match(val verdict: Verdict, val difference: Double)

    /** Face value of one currency's stack, and how many notes are in it. */
    fun localTotal(counts: Map<Long, Int>): Double =
        counts.entries.sumOf { (denomination, quantity) -> denomination.toDouble() * quantity }

    fun noteCount(counts: Map<Long, Int>): Int =
        counts.values.sumOf { it }.coerceAtLeast(0)

    /**
     * Units of [code] per 1 USD: the owner's manual rate wins over the live table, because in Lebanon
     * the street rate is often not the published one and his total must match his reality. A currency
     * with no rate at all reads as 0, which [toUsd] refuses to price rather than inventing a number.
     */
    fun rateFor(code: String, live: Map<String, Double>, manual: Map<String, Double>): Double =
        manual[code] ?: live[code] ?: 0.0

    fun hasRate(code: String, live: Map<String, Double>, manual: Map<String, Double>): Boolean =
        manual.containsKey(code) || live.containsKey(code)

    /** A stack in its own currency, priced in USD. Unknown rate → 0, never a guessed figure. */
    fun toUsd(
        local: Double,
        code: String,
        live: Map<String, Double>,
        manual: Map<String, Double>,
    ): Double {
        val unitsPerUsd = rateFor(code, live, manual)
        return if (unitsPerUsd > 0.0) local / unitsPerUsd else 0.0
    }

    fun grandTotal(
        bag: List<String>,
        counts: Map<String, Map<Long, Int>>,
        live: Map<String, Double>,
        manual: Map<String, Double>,
    ): Grand {
        var usd = 0.0
        var notes = 0
        for (code in bag) {
            val stack = counts[code].orEmpty()
            usd += toUsd(localTotal(stack), code, live, manual)
            notes += noteCount(stack)
        }
        return Grand(usd, notes)
    }

    fun verdict(totalUsd: Double, expected: Double): Match {
        if (expected <= 0.0) return Match(Verdict.None, 0.0)
        val diff = totalUsd - expected
        return when {
            abs(diff) < MATCH_EPSILON -> Match(Verdict.Match, 0.0)
            diff < 0 -> Match(Verdict.Short, -diff)
            else -> Match(Verdict.Over, diff)
        }
    }

    /** Bump one denomination, never below zero. */
    fun step(counts: Map<Long, Int>, denomination: Long, delta: Int): Map<Long, Int> {
        val next = (counts[denomination] ?: 0) + delta
        return if (next <= 0) counts - denomination else counts + (denomination to next)
    }

    /** Typed-in quantity for one denomination; zero removes the row from the map entirely. */
    fun set(counts: Map<Long, Int>, denomination: Long, quantity: Int): Map<Long, Int> =
        if (quantity <= 0) counts - denomination else counts + (denomination to quantity)

    // ---------- formatting ----------

    fun group(value: Double): String = String.format(Locale.US, "%,.0f", value)

    /** "$1,000.73" — the grand total, always two decimals so the cents read as cents. */
    fun usd(value: Double): String =
        (if (value < 0) "-$" else "$") + String.format(Locale.US, "%,.2f", abs(value))

    /** "£190" — a stack in its own currency, whole notes, no cents. */
    fun local(value: Double, symbol: String): String = symbol + group(value)

    /**
     * A rate exactly as the Convert tab would print it: whole units for the big ones (89,550 LBP),
     * four decimals for the small ones (0.7420 GBP). The currency code and the "= 1 USD" wording are
     * added by the caller.
     */
    fun rate(unitsPerUsd: Double): String = when {
        unitsPerUsd == 0.0 -> "0"
        unitsPerUsd >= 100 -> String.format(Locale.US, "%,.0f", unitsPerUsd)
        else -> String.format(Locale.US, "%.4f", unitsPerUsd)
    }
}
