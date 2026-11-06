package com.lamahuissn.currency.data

import android.content.Context

/**
 * One saved count: the USD value is frozen at the moment of saving, together with the rates that
 * produced it — a ledger line must not re-price itself when tomorrow's rate moves.
 */
data class SavedCount(
    val at: Long,
    val totalUsd: Double,
    val notes: Int,
    val expected: Double,
    val parts: Map<String, Double>,
    val rates: Map<String, Double>,
    /** code → (denomination → quantity), so restoring puts the actual notes back on screen. */
    val stacks: Map<String, Map<Long, Int>> = emptyMap(),
)

/** Everything the Count tab remembers between launches. */
data class CountState(
    val bag: List<String> = Banknotes.defaultBag,
    /** code → (denomination → how many notes) */
    val counts: Map<String, Map<Long, Int>> = emptyMap(),
    /** code → USD per unit, when the owner typed his own rate. */
    val manualRates: Map<String, Double> = emptyMap(),
    val expected: Double = 0.0,
    val history: List<SavedCount> = emptyList(),
    val collapsed: Set<String> = emptySet(),
    /** The counted content as it was when the owner last pressed Save. */
    val savedFingerprint: String = "",
) {
    fun stack(code: String): Map<Long, Int> = counts[code].orEmpty()

    /**
     * The counted money only — not the bag, not the rates, not the target. Nothing counted is the
     * empty string, which is what a fresh state holds, so an untouched screen never reads as unsaved.
     */
    fun fingerprint(): String = fingerprintOf(counts)

    companion object {
        /** The same fingerprint for a stack map that is not (yet) a [CountState] — used on restore. */
        fun fingerprintOf(counts: Map<String, Map<Long, Int>>): String = counts.entries
            .mapNotNull { (code, stack) ->
                val counted = stack.filterValues { it > 0 }
                if (counted.isEmpty()) null else code + ":" + counted.entries
                    .sortedByDescending { it.key }
                    .joinToString(",") { "${it.key}x${it.value}" }
            }
            .sorted()
            .joinToString("|")
    }
}

/**
 * Storage for the Count tab. Counts are a draft: every tap is written, because losing a bag you just
 * counted by hand is the one failure this screen must never have. History is separate and only grows
 * when the owner presses Save.
 */
class CountPrefs(context: Context) {

    private val sp = context.getSharedPreferences("currency_count", Context.MODE_PRIVATE)

    fun load(): CountState {
        val bag = sp.getString("bag", null)
            ?.split(',')
            ?.filter { it.isNotBlank() && Banknotes.has(it) }
            ?.ifEmpty { Banknotes.defaultBag }
            ?: Banknotes.defaultBag

        return CountState(
            bag = bag,
            counts = CountCodec.decodeStacks(sp.getString("counts", null)),
            manualRates = CountCodec.decodeRates(sp.getString("manualRates", null)),
            expected = sp.getFloat("expected", 0f).toDouble(),
            history = CountCodec.decodeHistory(sp.getString("history", null)),
            collapsed = sp.getString("collapsed", null)
                ?.split(',')?.filter { it.isNotBlank() }?.toSet()
                ?: emptySet(),
            savedFingerprint = sp.getString("savedFingerprint", "") ?: "",
        )
    }

    fun save(state: CountState) {
        sp.edit()
            .putString("bag", state.bag.joinToString(","))
            .putString("counts", CountCodec.encodeStacks(state.counts))
            .putString("manualRates", CountCodec.encodeRates(state.manualRates))
            .putFloat("expected", state.expected.toFloat())
            .putString("history", CountCodec.encodeHistory(state.history))
            .putString("collapsed", state.collapsed.joinToString(","))
            .putString("savedFingerprint", state.savedFingerprint)
            .apply()
    }

    companion object {
        /** A ledger you cannot read past forty lines is not a ledger you keep. */
        const val HISTORY_LIMIT = 40
    }
}
