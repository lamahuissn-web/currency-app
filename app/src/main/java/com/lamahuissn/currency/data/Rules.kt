package com.lamahuissn.currency.data

/**
 * The rows the Convert list may draw.
 *
 * The base currency is the input line and must never also appear as one of its own targets — the
 * list showed a base of GBP twice (once solid, once as a converted row) because GBP was also in the
 * watch list. The stored list is deliberately left untouched, so choosing a different base later
 * brings the old row back instead of silently deleting it.
 */
fun visibleTargets(base: String, targets: List<String>): List<String> =
    targets.asSequence()
        .filter { it.isNotBlank() && it != base }
        .distinct()
        .toList()

/**
 * What the screen draws, in order: the pinned rows first (in the order they were pinned), then the
 * rest of the watch list. A pin on a currency that has since been removed or made the base is
 * ignored rather than resurrecting a row.
 */
fun orderRows(base: String, targets: List<String>, pinned: List<String>): List<String> {
    val visible = visibleTargets(base, targets)
    val head = pinned.filter { visible.contains(it) }.distinct()
    val tail = visible.filterNot { head.contains(it) }
    return head + tail
}

/** Pinning is a toggle: the same gesture takes the row back down. */
fun togglePin(pinned: List<String>, code: String): List<String> =
    if (pinned.contains(code)) pinned - code else pinned + code

/**
 * The amount line while it is being typed: no forced decimals (`100` stays `100`), with thousands
 * separators so a long figure is still readable. A trailing dot is kept so `1.` can become `1.5`.
 */
fun groupDigits(entry: String): String {
    if (entry.isEmpty()) return entry
    val dot = entry.indexOf('.')
    val whole = if (dot >= 0) entry.substring(0, dot) else entry
    val rest = if (dot >= 0) entry.substring(dot) else ""
    val grouped = whole.reversed().chunked(3).joinToString(",").reversed()
    return grouped + rest
}
