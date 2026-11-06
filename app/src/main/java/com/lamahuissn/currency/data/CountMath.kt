package com.lamahuissn.currency.data

import java.util.Locale
import kotlin.math.abs

/**
 * The money math behind the Count tab, kept out of Compose so it can be tested without a device.
 *
 * Everything is USD-based, exactly like the rate table: one number per currency, and every total is
 * `local face value × units of USD per unit of that currency`.
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
     * USD per one unit of [code]: the owner's manual rate wins over the live table, because in
     * Lebanon the street rate is often not the published one and his total must match his reality.
     * A currency with no rate at all contributes nothing rather than a made-up number.
     */
    fun rateFor(code: String, live: Map<String, Double>, manual: Map<String, Double>): Double =
        manual[code] ?: live[code] ?: 0.0

    fun hasRate(code: String, live: Map<String, Double>, manual: Map<String, Double>): Boolean =
        manual.containsKey(code) || live.containsKey(code)

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
            usd += localTotal(stack) * rateFor(code, live, manual)
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

    fun group(value: Double): String =
        String.format(Locale.US, "%,.0f", value)

    /** "$1,000.73" — the grand total, always two decimals so the cents read as cents. */
    fun usd(value: Double): String =
        (if (value < 0) "-$" else "$") + String.format(Locale.US, "%,.2f", abs(value))

    /** "£190" — a stack in its own currency, whole notes, no cents. */
    fun local(value: Double, symbol: String): String = symbol + group(value)

    /** "1 GBP = $1.3477" — small enough to read, precise enough not to lose a cent on a big stack. */
    fun rate(units: Double): String = when {
        units == 0.0 -> "$0"
        units >= 100 -> String.format(Locale.US, "$%,.0f", units)
        units >= 1 -> String.format(Locale.US, "$%.4f", units)
        units >= 0.01 -> String.format(Locale.US, "$%.4f", units)
        else -> String.format(Locale.US, "$%.8f", units)
    }
}
