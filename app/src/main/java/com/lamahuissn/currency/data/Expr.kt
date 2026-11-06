package com.lamahuissn.currency.data

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * A small arithmetic entry for the amount sheet: the owner collects bills all day and does sums in
 * his head ("3 months × 45,000"). Typing the sum into the field is faster and leaves no rounding on
 * the table.
 *
 * Rules, deliberately the ones a phone calculator has:
 *  - × and ÷ bind tighter than + and −, evaluated left to right inside the same precedence;
 *  - a trailing operator is not an error, it just means "still typing" — everything before it is
 *    evaluated so the running total stays on screen;
 *  - anything unparsable returns null, and the caller converts nothing rather than a guess.
 */
object Expr {

    private val operators = setOf('+', '−', '-', '×', '*', '÷', '/')

    /** Renders the operator the pad produced; the stored form uses these four. */
    fun normalize(op: Char): Char = when (op) {
        '-' -> '−'
        '*' -> '×'
        '/' -> '÷'
        else -> op
    }

    fun isOperator(c: Char): Boolean = operators.contains(c)

    /** How many operators the entry holds — the amount sheet shows its expression line only then. */
    fun operatorCount(entry: String): Int = entry.count { isOperator(it) }

    fun evaluate(entry: String): Double? {
        val trimmed = entry.trim().trimEnd { isOperator(it) || it == ' ' }
        if (trimmed.isEmpty()) return null

        val tokens = tokenize(trimmed) ?: return null
        if (tokens.isEmpty()) return null

        // Pass 1: × and ÷
        val folded = ArrayList<Any>(tokens.size)
        var i = 0
        var current: Double? = null
        while (i < tokens.size) {
            when (val token = tokens[i]) {
                is Double -> current = token
                is Char -> {
                    val left = current ?: return null
                    val right = tokens.getOrNull(i + 1) as? Double ?: return null
                    val value = when (token) {
                        '×' -> left * right
                        '÷' -> if (right == 0.0) return null else left / right
                        else -> {
                            // + or − ends this multiplication chain.
                            folded.add(left)
                            folded.add(token)
                            current = right
                            i += 2
                            continue
                        }
                    }
                    current = value
                    i += 2
                    continue
                }
            }
            i++
        }
        current?.let { folded.add(it) }
        if (folded.isEmpty()) return null

        // Pass 2: + and −
        var total = folded.firstOrNull() as? Double ?: return null
        var j = 1
        while (j < folded.size) {
            val op = folded[j] as? Char ?: return null
            val value = folded.getOrNull(j + 1) as? Double ?: return null
            total = if (op == '+') total + value else total - value
            j += 2
        }
        return total
    }

    private fun tokenize(text: String): List<Any>? {
        val tokens = ArrayList<Any>()
        val number = StringBuilder()
        for (c in text) {
            when {
                c.isDigit() || c == '.' -> number.append(c)
                isOperator(c) -> {
                    if (number.isEmpty()) return null
                    tokens.add(number.toString().toDoubleOrNull() ?: return null)
                    number.clear()
                    tokens.add(normalize(c))
                }
                c == ' ' || c == ',' -> Unit
                else -> return null
            }
        }
        if (number.isNotEmpty()) tokens.add(number.toString().toDoubleOrNull() ?: return null)
        return tokens
    }

    /** "1500×3" → "1,500 × 3"; numbers grouped, operators spaced, so the sum stays readable. */
    fun display(entry: String): String {
        val out = StringBuilder()
        val number = StringBuilder()
        fun flush() {
            if (number.isEmpty()) return
            val raw = number.toString()
            if (raw.endsWith(".")) {
                out.append(groupDigits(raw.dropLast(1))).append('.')
            } else {
                out.append(groupDigits(raw))
            }
            number.clear()
        }
        for (c in entry) {
            when {
                isOperator(c) -> {
                    flush()
                    if (out.isNotEmpty()) out.append(' ')
                    out.append(normalize(c)).append(' ')
                }
                c == '.' -> number.append(c)
                c.isDigit() -> number.append(c)
                else -> number.append(c)
            }
        }
        flush()
        return out.toString().trim()
    }

    /** A result without noise: two decimals at most, no trailing zeros, grouped thousands. */
    fun resultText(value: Double): String {
        val rounded = (value * 100.0).roundToLong() / 100.0
        val shown = if (abs(rounded - rounded.roundToLong()) < 0.000001) {
            rounded.roundToLong().toString()
        } else {
            String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
        }
        val parts = shown.split('.')
        val whole = groupDigits(parts[0])
        return if (parts.size > 1) "$whole.${parts[1]}" else whole
    }

    /**
     * Groups the digits the way the entry line does, but tolerates a bare "-" and a trailing dot,
     * which [groupDigits] (built for typing) does not have to.
     */
    private fun groupDigits(raw: String): String {
        if (raw.isEmpty()) return raw
        val negative = raw.startsWith("-")
        val body = if (negative) raw.drop(1) else raw
        val dot = body.indexOf('.')
        val whole = if (dot >= 0) body.substring(0, dot) else body
        val fraction = if (dot >= 0) body.substring(dot) else ""
        val grouped = if (whole.isEmpty()) whole
            else whole.reversed().chunked(3).joinToString(",").reversed()
        return (if (negative) "-" else "") + grouped + fraction
    }
}
