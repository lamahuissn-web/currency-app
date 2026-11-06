package com.lamahuissn.currency.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamahuissn.currency.data.Banknotes
import com.lamahuissn.currency.data.CountMath
import com.lamahuissn.currency.data.CurrencyCatalog
import com.lamahuissn.currency.data.SavedCount

/** The dim behind every Count sheet: tapping it closes, tapping the card does not. */
@Composable
fun SheetScrim(onDismiss: () -> Unit, card: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.66f))
            .combinedClickable(onClick = onDismiss, onLongClick = onDismiss),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            card()
        }
    }
}

@Composable
private fun SheetCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 620.dp)
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Ink.Surface)
            .padding(top = 10.dp, bottom = 22.dp),
    ) { content() }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = Ink.Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 8.dp),
    )
}

/**
 * Numeric entry for one thing at a time: how many notes, the expected total, or a manual rate.
 * The first key press replaces the prefilled value, like a phone dialer, so retyping a count is one
 * tap instead of a backspace marathon.
 */
@Composable
fun CountKeypadSheet(
    title: String,
    initial: String,
    decimals: Boolean,
    onDone: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var buffer by remember { mutableStateOf(initial) }
    var replacing by remember { mutableStateOf(initial.isNotEmpty()) }

    fun press(key: String) {
        when (key) {
            "back" -> {
                buffer = buffer.dropLast(1)
                replacing = false
            }
            "." -> {
                if (decimals && !buffer.contains('.')) {
                    buffer = (if (buffer.isEmpty()) "0" else buffer) + "."
                    replacing = false
                }
            }
            else -> {
                buffer = if (replacing) key else {
                    if (buffer == "0") key else buffer + key
                }.take(12)
                replacing = false
            }
        }
    }

    SheetScrim(onDismiss) {
        SheetCard {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(title, color = Ink.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(
                    text = buffer.ifEmpty { "0" },
                    color = Ink.Text,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            SheetKeypad(
                decimals = decimals,
                onKey = { key ->
                    Sfx.click()
                    if (key == "ok") {
                        onDone(buffer.toDoubleOrNull() ?: 0.0)
                    } else {
                        press(key)
                    }
                },
            )
        }
    }
}

@Composable
private fun SheetKeypad(decimals: Boolean, onKey: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(if (decimals) "." else "", "0", "back"),
        )
        for (row in rows) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (key in row) {
                    if (key.isEmpty()) {
                        Spacer(Modifier.weight(1f).height(56.dp))
                        continue
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Ink.SurfaceHigh)
                            .combinedClickable(onClick = { onKey(key) }, onLongClick = { onKey(key) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (key == "back") "⌫" else key,
                            color = Ink.Text,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Ink.Green)
                .combinedClickable(onClick = { onKey("ok") }, onLongClick = { onKey("ok") }),
            contentAlignment = Alignment.Center,
        ) {
            Text("Done", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** Add or remove a currency from the bag. Only currencies with a real banknote ladder are listed. */
@Composable
fun CountPickerSheet(
    bag: List<String>,
    rates: Map<String, Double>,
    manual: Map<String, Double>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    SheetScrim(onDismiss) {
        SheetCard {
            SheetTitle("Add a currency to the bag")
            LazyColumn(Modifier.fillMaxWidth()) {
                items(Banknotes.supported) { code ->
                    val currency = CurrencyCatalog.of(code)
                    val inBag = bag.contains(code)
                    val rate = CountMath.rateFor(code, rates, manual)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = { onToggle(code) }, onLongClick = { onToggle(code) })
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FlagDot(currency.flag, currency.code, 30.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(currency.name, color = Ink.Text, fontSize = 16.sp)
                            Text(
                                "$code · 1 USD = ${CountMath.rate(rate)} $code",
                                color = Ink.Muted,
                                fontSize = 11.5.sp,
                            )
                        }
                        Text(
                            text = if (inBag) "added" else "+ add",
                            color = if (inBag) Ink.Muted else Ink.Green,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/** The rates actually in use, in the order of the bag, with the way to override each one. */
@Composable
fun CountRatesSheet(
    bag: List<String>,
    rates: Map<String, Double>,
    manual: Map<String, Double>,
    live: Boolean,
    updatedAt: Long,
    onEdit: (String) -> Unit,
    onReset: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    SheetScrim(onDismiss) {
        SheetCard {
            SheetTitle(
                if (live) "Rates live" else "Cached rates",
            )
            Text(
                text = if (live) {
                    (if (updatedAt > 0) "Updated ${CountScreenTime.format(updatedAt)}. " else "") +
                        "Rates are in the same direction as the Convert tab: how many units of that currency 1 USD buys. Tap one to type your own."
                } else {
                    "No connection, so these are the last rates saved on this phone. Tap one to type your own."
                },
                color = Ink.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            for (code in bag) {
                val currency = CurrencyCatalog.of(code)
                val isManual = manual.containsKey(code)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = { onEdit(code) }, onLongClick = { onReset(code) })
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlagDot(currency.flag, currency.code, 28.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("1 USD = ${CountMath.rate(CountMath.rateFor(code, rates, manual))} $code",
                             color = Ink.Text, fontSize = 15.sp)
                        Text(
                            text = if (isManual) "manual rate" else "market rate",
                            color = if (isManual) Ink.Amber else Ink.Muted,
                            fontSize = 11.5.sp,
                        )
                    }
                    Text(
                        text = if (isManual) "reset" else "edit",
                        color = if (isManual) Ink.Amber else Ink.Green,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** Past counts. Tap restores one into the counter, long-press deletes it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CountHistorySheet(
    history: List<SavedCount>,
    onRestore: (SavedCount) -> Unit,
    onDelete: (SavedCount) -> Unit,
    onDismiss: () -> Unit,
) {
    SheetScrim(onDismiss) {
        SheetCard {
            SheetTitle("Saved counts")
            if (history.isEmpty()) {
                Text(
                    "No saved counts yet. Count some notes and tap Save.",
                    color = Ink.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
                )
            } else {
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(history) { entry ->
                        val match = CountMath.verdict(entry.totalUsd, entry.expected)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onRestore(entry) },
                                    onLongClick = { onDelete(entry) },
                                )
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    CountMath.usd(entry.totalUsd),
                                    color = Ink.Text,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${CountScreenTime.format(entry.at)} · ${entry.notes} notes",
                                    color = Ink.Muted,
                                    fontSize = 11.sp,
                                )
                                Text(
                                    entry.parts.entries.joinToString(" · ") { (code, total) ->
                                        "$code ${CountMath.local(total, CurrencyCatalog.of(code).symbol)}"
                                    },
                                    color = Ink.Muted,
                                    fontSize = 11.sp,
                                )
                            }
                            MatchChip(match)
                        }
                    }
                }
            }
        }
    }
}
