package com.lamahuissn.currency.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * The Count tab's on-disk shape, kept free of Android so it can be tested: a bag you cannot restore
 * after a restart is worse than no bag at all, and that is exactly the kind of bug unit tests catch.
 */
object CountCodec {

    /** code → (denomination → quantity) as a JSON object of objects. */
    fun encodeStacks(stacks: Map<String, Map<Long, Int>>): String {
        val root = JSONObject()
        for ((code, stack) in stacks) {
            val live = stack.filterValues { it > 0 }
            if (live.isEmpty()) continue
            val inner = JSONObject()
            for ((denomination, quantity) in live) inner.put(denomination.toString(), quantity)
            root.put(code, inner)
        }
        return root.toString()
    }

    fun decodeStacks(raw: String?): Map<String, Map<Long, Int>> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            root.keys().asSequence().mapNotNull { code ->
                val inner = root.optJSONObject(code) ?: return@mapNotNull null
                val stack = inner.keys().asSequence()
                    .mapNotNull { key ->
                        val denomination = key.toLongOrNull() ?: return@mapNotNull null
                        val quantity = inner.optInt(key, 0)
                        if (quantity > 0) denomination to quantity else null
                    }
                    .toMap()
                if (stack.isEmpty()) null else code to stack
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    fun encodeRates(rates: Map<String, Double>): String {
        val root = JSONObject()
        for ((code, rate) in rates) root.put(code, rate)
        return root.toString()
    }

    fun decodeRates(raw: String?): Map<String, Double> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            root.keys().asSequence().associateWith { root.optDouble(it, 0.0) }
                .filterValues { it > 0 }
        }.getOrDefault(emptyMap())
    }

    fun encodeHistory(history: List<SavedCount>): String {
        val array = JSONArray()
        for (entry in history) {
            val parts = JSONObject()
            for ((code, total) in entry.parts) parts.put(code, total)
            array.put(
                JSONObject()
                    .put("at", entry.at)
                    .put("totalUsd", entry.totalUsd)
                    .put("notes", entry.notes)
                    .put("expected", entry.expected)
                    .put("parts", parts)
                    .put("rates", encodeRates(entry.rates))
                    .put("stacks", encodeStacks(entry.stacks))
            )
        }
        return array.toString()
    }

    fun decodeHistory(raw: String?): List<SavedCount> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val partsObj = obj.optJSONObject("parts") ?: JSONObject()
                SavedCount(
                    at = obj.optLong("at"),
                    totalUsd = obj.optDouble("totalUsd", 0.0),
                    notes = obj.optInt("notes", 0),
                    expected = obj.optDouble("expected", 0.0),
                    parts = partsObj.keys().asSequence().associateWith { partsObj.optDouble(it, 0.0) },
                    // Older entries stored the rate as a JSON object; both shapes decode the same way.
                    rates = decodeRates(obj.opt("rates")?.toString()),
                    stacks = decodeStacks(obj.opt("stacks")?.toString()),
                )
            }
        }.getOrDefault(emptyList())
    }
}
