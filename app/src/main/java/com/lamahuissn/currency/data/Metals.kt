package com.lamahuissn.currency.data

import org.json.JSONObject
import java.util.Locale

/**
 * Gold and silver by the troy ounce, from `api.gold-api.com` — a keyless spot feed that returns one
 * number per call. The app stores every price in the same shape it stores currencies (units per 1
 * USD), so a metal is just another row: one ounce of gold is `1 / 4037.80` "gold-dollars".
 *
 * Grams and karats are derived, never fetched: a jeweller's gram is an ounce divided by 31.1034768
 * and scaled by the karat, and doing that in one tested place keeps the sheet and the row agreeing.
 */
object Metals {

    const val GRAMS_PER_TROY_OUNCE = 31.1034768

    /** The gold pound sold in Lebanon: 7.32 grams of 21k. */
    const val POUND_GRAMS = 7.32

    val codes: List<String> = listOf("XAU", "XAG")

    fun isMetal(code: String): Boolean = codes.contains(code)

    fun name(code: String): String = when (code) {
        "XAU" -> "Gold"
        "XAG" -> "Silver"
        else -> code
    }

    /** `{"price":4037.8,...}` → 4037.8. Anything else returns null, never zero. */
    fun parseOunceUsd(body: String): Double? = runCatching {
        val price = JSONObject(body).optDouble("price", 0.0)
        if (price > 0.0) price else null
    }.getOrNull()

    /** Units of metal per 1 USD — how the app's one table stores an ounce price. */
    fun perUsd(ounceUsd: Double): Double = if (ounceUsd > 0.0) 1.0 / ounceUsd else 0.0

    /** A price this fresh is worth calling live; older than a minute is just "as of HH:mm". */
    fun isLive(ageMs: Long): Boolean = ageMs in 0..60_000L

    /** The ounce price back out of the table. */
    fun ounceUsd(perUsd: Double): Double = if (perUsd > 0.0) 1.0 / perUsd else 0.0

    /** USD for one gram at [karat] (24 = fine). Karats above 24 are clamped. */
    fun gramUsd(ounceUsd: Double, karat: Int): Double =
        ounceUsd / GRAMS_PER_TROY_OUNCE * (karat.coerceIn(1, 24) / 24.0)

    /** USD for one Lebanese gold pound (7.32 g of 21k). */
    fun poundUsd(ounceUsd: Double): Double = gramUsd(ounceUsd, 21) * POUND_GRAMS

    /** "$4,037.80" — an ounce price, two decimals like any other money figure. */
    fun money(value: Double): String =
        (if (value < 0) "-$" else "$") + String.format(Locale.US, "%,.2f", kotlin.math.abs(value))

    /** What the Convert row shows for a metal: the price of one ounce, not a converted fraction. */
    fun rowBadge(perUsd: Double): String =
        if (perUsd > 0.0) "${money(ounceUsd(perUsd))} / oz" else "—"
}
