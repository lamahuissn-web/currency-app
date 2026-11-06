package com.lamahuissn.currency.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** One point of a historical series: when it was sampled, and the quote per 1 base. */
data class Point(val at: Long, val value: Double)

/**
 * The three public data sources this app reads, none of which needs an API key:
 *
 * - `open.er-api.com` — fiat rates against the US dollar, refreshed daily.
 * - `api.frankfurter.dev` — the ECB's own daily reference series, for charts of the currencies it
 *   publishes (about thirty). A pair it does not publish is reported as unavailable rather than
 *   drawn from a guess.
 * - `api.coingecko.com` — crypto prices, both spot and historical.
 *
 * Parsing lives in the companion so it can be tested against captured bodies without a network.
 */
class RatesClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Currency/1.0 (Android)")
            .header("Accept", "application/json")
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return body ?: error("empty response")
        }
    }

    /** Units of every fiat currency per 1 US dollar. */
    fun fiatPerUsd(): Map<String, Double> =
        parseFiat(get("https://open.er-api.com/v6/latest/USD"))

    /** Crypto prices expressed the same way: units of coin per 1 US dollar. */
    fun cryptoPerUsd(): Map<String, Double> {
        val ids = CurrencyCatalog.cryptoIds.values.joinToString(",")
        return parseCrypto(get("https://api.coingecko.com/api/v3/simple/price?ids=$ids&vs_currencies=usd"))
    }

    /** Daily series of [quote] per 1 [base], from the ECB reference rates. Empty when unpublished. */
    fun fiatHistory(base: String, quote: String, startIso: String, endIso: String): List<Point> {
        val url = "https://api.frankfurter.dev/v1/$startIso..$endIso?base=$base&symbols=$quote"
        val body = try {
            get(url)
        } catch (e: Exception) {
            return emptyList()
        }
        return parseHistory(body, quote)
    }

    /** Price of one coin in [quote], for the last [days] days. */
    fun cryptoHistory(code: String, quote: String, days: Int): List<Point> {
        val id = CurrencyCatalog.cryptoIds[code] ?: return emptyList()
        val url = "https://api.coingecko.com/api/v3/coins/$id/market_chart?vs_currency=${quote.lowercase()}&days=$days"
        val body = try {
            get(url)
        } catch (e: Exception) {
            return emptyList()
        }
        return parseMarketChart(body)
    }

    companion object {

        fun parseFiat(body: String): Map<String, Double> {
            val root = JSONObject(body)
            val rates = root.optJSONObject("rates") ?: error("no rates in response")
            val out = HashMap<String, Double>(rates.length())
            for (code in rates.keys()) {
                val value = rates.optDouble(code, Double.NaN)
                if (!value.isNaN() && value > 0.0) out[code] = value
            }
            if (out.isEmpty()) error("empty rate table")
            return out
        }

        fun parseCrypto(body: String): Map<String, Double> {
            val root = JSONObject(body)
            val out = HashMap<String, Double>()
            for ((code, id) in CurrencyCatalog.cryptoIds) {
                // A coin worth N dollars is 1/N of a dollar's worth of coin.
                val price = root.optJSONObject(id)?.optDouble("usd", 0.0) ?: 0.0
                if (price > 0.0) out[code] = 1.0 / price
            }
            return out
        }

        fun parseHistory(body: String, quote: String): List<Point> {
            val root = JSONObject(body)
            val rates = root.optJSONObject("rates") ?: return emptyList()
            val out = ArrayList<Point>()
            for (day in rates.keys()) {
                val value = rates.optJSONObject(day)?.optDouble(quote, Double.NaN) ?: Double.NaN
                if (!value.isNaN() && value > 0.0) out.add(Point(at = dayToMillis(day), value = value))
            }
            out.sortBy { it.at }
            return out
        }

        fun parseMarketChart(body: String): List<Point> {
            val prices = JSONObject(body).optJSONArray("prices") ?: return emptyList()
            val out = ArrayList<Point>(prices.length())
            for (i in 0 until prices.length()) {
                val pair = prices.optJSONArray(i) ?: continue
                val at = pair.optLong(0)
                val value = pair.optDouble(1, Double.NaN)
                if (!value.isNaN() && value > 0.0) out.add(Point(at, value))
            }
            return out
        }

        private fun dayToMillis(iso: String): Long = try {
            java.time.LocalDate.parse(iso).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }
}
