package com.lamahuissn.currency.data

/**
 * Which banknotes exist, per currency. Counting is a physical act: only notes that are actually in
 * circulation belong in the ladder, and only currencies we have a real ladder for are offered in the
 * Count tab — a guessed ladder would let the owner count money that does not exist.
 */
object Banknotes {

    private val ladders: Map<String, List<Long>> = mapOf(
        "USD" to listOf(100L, 50L, 20L, 10L, 5L, 1L),
        "EUR" to listOf(200L, 100L, 50L, 20L, 10L, 5L),
        "GBP" to listOf(50L, 20L, 10L, 5L),
        "LBP" to listOf(100_000L, 50_000L, 20_000L, 10_000L, 5_000L, 1_000L),
        "TRY" to listOf(200L, 100L, 50L, 20L, 10L, 5L),
        "EGP" to listOf(200L, 100L, 50L, 20L, 10L, 5L),
        "AED" to listOf(500L, 200L, 100L, 50L, 20L, 10L, 5L),
        "SAR" to listOf(500L, 200L, 100L, 50L, 20L, 10L, 5L),
        "QAR" to listOf(500L, 200L, 100L, 50L, 10L, 5L),
        "SYP" to listOf(5_000L, 2_000L, 1_000L, 500L, 200L, 100L),
        "JOD" to listOf(50L, 20L, 10L, 5L, 1L),
        "IQD" to listOf(25_000L, 10_000L, 5_000L, 1_000L, 500L, 250L),
        "CAD" to listOf(100L, 50L, 20L, 10L, 5L),
        "AUD" to listOf(100L, 50L, 20L, 10L, 5L),
        "CHF" to listOf(1_000L, 200L, 100L, 50L, 20L, 10L),
        "INR" to listOf(2_000L, 500L, 200L, 100L, 50L, 10L),
        "RUB" to listOf(5_000L, 2_000L, 1_000L, 500L, 100L, 50L),
        "MAD" to listOf(200L, 100L, 50L, 20L),
        "DZD" to listOf(2_000L, 1_000L, 500L, 200L, 100L),
        "YER" to listOf(1_000L, 500L, 250L, 100L, 50L),
    )

    /** Codes the Count tab can offer, biggest ladder first is irrelevant — the picker sorts by name. */
    val supported: List<String> get() = ladders.keys.sorted()

    fun has(code: String): Boolean = ladders.containsKey(code)

    /** Top of the ladder downwards; empty when we have no ladder for this currency. */
    fun ladder(code: String): List<Long> = ladders[code] ?: emptyList()

    /** The bag starts with the currencies this owner actually handles. */
    val defaultBag: List<String> = listOf("USD", "LBP")
}
