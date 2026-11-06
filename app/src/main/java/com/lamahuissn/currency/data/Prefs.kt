package com.lamahuissn.currency.data

import android.content.Context
import org.json.JSONObject

/**
 * What the owner chose, kept across launches: the base currency and amount, the rows he watches,
 * the last rate table (so the list is readable with no network), and the chart's pair.
 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("currency", Context.MODE_PRIVATE)

    var base: String
        get() = sp.getString("base", DEFAULT_BASE) ?: DEFAULT_BASE
        set(value) = sp.edit().putString("base", value).apply()

    var amount: Double
        get() = sp.getFloat("amount", 1f).toDouble()
        set(value) = sp.edit().putFloat("amount", value.toFloat()).apply()

    var targets: List<String>
        get() {
            val raw = sp.getString("targets", null) ?: return DEFAULT_TARGETS
            return raw.split(',').filter { it.isNotBlank() }
        }
        set(value) = sp.edit().putString("targets", value.joinToString(",")).apply()

    /** Codes pinned to the top of the list, in the order they were pinned. */
    var pinned: List<String>
        get() {
            val raw = sp.getString("pinned", null) ?: return emptyList()
            return raw.split(',').filter { it.isNotBlank() }
        }
        set(value) = sp.edit().putString("pinned", value.joinToString(",")).apply()

    /** Touch sound on/off, from the list menu. Default on. */
    var soundOn: Boolean
        get() = sp.getBoolean("soundOn", true)
        set(value) = sp.edit().putBoolean("soundOn", value).apply()

    var updatedAt: Long
        get() = sp.getLong("updatedAt", 0L)
        set(value) = sp.edit().putLong("updatedAt", value).apply()

    var cachedRates: Map<String, Double>
        get() {
            val raw = sp.getString("rates", null) ?: return emptyMap()
            val out = HashMap<String, Double>()
            try {
                val json = JSONObject(raw)
                for (code in json.keys()) out[code] = json.getDouble(code)
            } catch (e: Exception) {
                return emptyMap()
            }
            return out
        }
        set(value) {
            val json = JSONObject()
            for ((code, rate) in value) json.put(code, rate)
            sp.edit().putString("rates", json.toString()).apply()
        }

    var chartBase: String
        get() = sp.getString("chartBase", "EUR") ?: "EUR"
        set(value) = sp.edit().putString("chartBase", value).apply()

    var chartQuote: String
        get() = sp.getString("chartQuote", "USD") ?: "USD"
        set(value) = sp.edit().putString("chartQuote", value).apply()

    var chartPeriod: String
        get() = sp.getString("chartPeriod", "3M") ?: "3M"
        set(value) = sp.edit().putString("chartPeriod", value).apply()

    companion object {
        const val DEFAULT_BASE = "USD"
        val DEFAULT_TARGETS = listOf("LBP", "EGP", "TRY", "EUR", "INR", "GBP", "DKK", "BTC", "SYP", "NGN", "SAR")
    }
}
