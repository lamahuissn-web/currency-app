package com.lamahuissn.currency.data

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * How a converted figure is written. The screenshots this app is built from show two decimals for
 * large figures (89,550.64) and three for the rest (0.742), so: two decimals from 100 up, three
 * between 1 and 100, and enough digits to keep a small figure from collapsing to 0.00.
 */
fun formatValue(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    val pattern = when {
        value == 0.0 -> "#,##0.00"
        abs(value) >= 100.0 -> "#,##0.00"
        abs(value) >= 1.0 -> "#,##0.000"
        abs(value) >= 0.01 -> "#,##0.0000"
        else -> "#,##0.00000000"
    }
    val text = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US)).format(value)
    return trimToTwoDecimals(text)
}

/**
 * The figures in the screens this app copies are written with their trailing zeros dropped but
 * never fewer than two decimals (0.89, 0.742, 6.65, 1.00), so the pattern above over-specifies and
 * this trims back.
 */
private fun trimToTwoDecimals(text: String): String {
    val dot = text.indexOf('.')
    if (dot < 0) return text
    var end = text.length
    while (end - dot > 3 && text[end - 1] == '0') end--
    return text.substring(0, end)
}

/** The badge text for a row: the currency's own symbol, then the figure. */
fun badgeText(code: String, value: Double): String {
    val currency = CurrencyCatalog.of(code)
    val symbol = currency.symbol
    val figure = formatValue(value)
    if (symbol.isEmpty()) return "$figure $code"
    val last = symbol.last()
    val needsSpace = last.isLetter() || symbol.length > 2
    return if (needsSpace) "$symbol $figure" else "$symbol$figure"
}

/** The headline figure on the chart screen, with the same rules as the list. */
fun headlineText(code: String, value: Double): String = badgeText(code, value)

/** Percent change between the first and the last point of a series. */
fun percentChange(first: Double, last: Double): Double {
    if (first == 0.0) return 0.0
    return (last - first) / first * 100.0
}
