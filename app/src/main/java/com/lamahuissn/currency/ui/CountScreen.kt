package com.lamahuissn.currency.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import com.lamahuissn.currency.data.Banknotes
import com.lamahuissn.currency.data.CountMath
import com.lamahuissn.currency.data.CountPrefs
import com.lamahuissn.currency.data.CountState
import com.lamahuissn.currency.data.CurrencyCatalog
import com.lamahuissn.currency.data.SavedCount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

internal object CountScreenTime {
    private val format = SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH)
    fun format(epochMillis: Long): String = format.format(Date(epochMillis))
}

private enum class KeypadKind { Notes, Expected, Rate }

private data class KeypadRequest(
    val kind: KeypadKind,
    val code: String? = null,
    val denomination: Long? = null,
    val initial: String = "",
)

/**
 * The Count tab: physically count banknotes in as many currencies as the bag holds, and see one
 * number — the total in USD — that can be checked against what the owner expected to find.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CountScreen(
    rates: Map<String, Double>,
    ratesLive: Boolean,
    ratesUpdatedAt: Long,
    onSelectTab: (NavTab) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val prefs = remember { CountPrefs(context) }
    var state by remember { mutableStateOf(prefs.load()) }

    var keypad by remember { mutableStateOf<KeypadRequest?>(null) }
    var picking by remember { mutableStateOf(false) }
    var ratesOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }

    fun commit(next: CountState) {
        state = next
        prefs.save(next)
    }

    // Back closes the sheet on top, in the order they were opened, before it closes the screen.
    BackHandler(enabled = keypad != null || picking || ratesOpen || historyOpen) {
        when {
            keypad != null -> keypad = null
            picking -> picking = false
            historyOpen -> historyOpen = false
            ratesOpen -> ratesOpen = false
        }
    }

    val grand = CountMath.grandTotal(state.bag, state.counts, rates, state.manualRates)
    val match = CountMath.verdict(grand.usd, state.expected)
    val unsaved = state.fingerprint() != state.savedFingerprint

    Box(Modifier.fillMaxSize().background(Ink.Page)) {
        Column(Modifier.fillMaxSize()) {
            // ---------- header ----------
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Count",
                        color = Ink.Text,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.2).sp,
                    )
                    Text(
                        text = if (state.bag.size > 1) "Mixed bag · total in USD" else "Total in USD",
                        color = Ink.Muted,
                        fontSize = 12.sp,
                    )
                }
                Row(
                    Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(Ink.Surface)
                        .combinedClickable(
                            onClick = { Sfx.click(); ratesOpen = true },
                            onLongClick = { Sfx.click(); ratesOpen = true },
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (ratesLive) "$" else "!", color = if (ratesLive) Ink.Green else Ink.Amber, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (ratesLive) "Rates live" else "Cached rates",
                        color = Ink.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("›", color = Ink.Muted, fontSize = 12.sp)
                }
            }

            // ---------- the one number ----------
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 4.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF0B2413), Color(0xFF081A0E))))
                    .border(1.dp, Color(0xFF12301C), RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "TOTAL IN USD",
                        color = Color(0xFF6FAE85),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    SmallAction(if (unsaved) "History" else "History") { historyOpen = true }
                    SmallAction("Save", dot = unsaved) {
                        val entry = SavedCount(
                            at = System.currentTimeMillis(),
                            totalUsd = grand.usd,
                            notes = grand.notes,
                            expected = state.expected,
                            parts = state.bag
                                .associateWith { CountMath.localTotal(state.stack(it)) }
                                .filterValues { it > 0 },
                            stacks = state.bag
                                .associateWith { state.stack(it) }
                                .filterValues { it.isNotEmpty() },
                            rates = state.bag.associateWith {
                                CountMath.rateFor(it, rates, state.manualRates)
                            },
                        )
                        if (entry.totalUsd > 0.004) {
                            haptics.bump()
                            commit(
                                state.copy(
                                    history = (listOf(entry) + state.history).take(CountPrefs.HISTORY_LIMIT),
                                    savedFingerprint = state.fingerprint(),
                                ),
                            )
                            historyOpen = true
                        }
                    }
                    SmallAction("Clear") {
                        commit(
                            state.copy(
                                counts = state.bag.associateWith { emptyMap<Long, Int>() },
                                savedFingerprint = "",
                            ),
                        )
                    }
                }

                val cents = Math.round(grand.usd * 100)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = CountMath.group((cents / 100).toDouble()),
                        color = Ink.Text,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.5).sp,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                    )
                    Text(
                        text = "." + String.format(Locale.US, "%02d", (cents % 100).toInt()),
                        color = Ink.Green,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.5).sp,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                    )
                }
                Text(
                    text = when {
                        grand.notes == 0 -> "Add a currency and start counting"
                        else -> "${grand.notes} note${if (grand.notes == 1) "" else "s"} · " +
                            "${state.bag.size} currenc${if (state.bag.size == 1) "y" else "ies"}" +
                            if (ratesLive) "" else " · cached rates"
                    },
                    color = Color(0xFF93BBA6),
                    fontSize = 12.sp,
                )

                if (grand.notes > 0) {
                    Spacer(Modifier.height(10.dp))
                    for (code in state.bag) {
                        val local = CountMath.localTotal(state.stack(code))
                        if (local <= 0) continue
                        val currency = CurrencyCatalog.of(code)
                        Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                            Text(code, color = Color(0xFF93BBA6), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(38.dp))
                            Text(
                                CountMath.local(local, currency.symbol),
                                color = Color(0xFFD7E7DD),
                                fontSize = 12.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f),
                            )
                            Text("→", color = Color(0xFF6FAE85), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                            Text(
                                CountMath.usd(CountMath.toUsd(local, code, rates, state.manualRates)),
                                color = Ink.Text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(84.dp),
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF14351F)))
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Expected", color = Color(0xFF93BBA6), fontSize = 12.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (state.expected > 0) CountMath.usd(state.expected) else "set amount",
                        color = if (state.expected > 0) Ink.Text else Color(0xFF6C8F7B),
                        fontSize = 15.sp,
                        fontWeight = if (state.expected > 0) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                Sfx.click()
                                keypad = KeypadRequest(
                                    kind = KeypadKind.Expected,
                                    initial = if (state.expected > 0) state.expected.toString() else "",
                                )
                            },
                            onLongClick = {},
                        ),
                    )
                    Spacer(Modifier.weight(1f))
                    MatchChip(match)
                }
            }

            // ---------- the bag ----------
            Text(
                "MY BAG",
                color = Ink.Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 2.dp),
            )

            LazyColumn(
                Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(bottom = 120.dp),
            ) {
                items(state.bag) { code ->
                    val currency = CurrencyCatalog.of(code)
                    val stack = state.stack(code)
                    val localTotal = CountMath.localTotal(stack)
                    val notes = CountMath.noteCount(stack)
                    val open = !state.collapsed.contains(code)

                    Column(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Ink.Surface),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        Sfx.click()
                                        commit(
                                            state.copy(
                                                collapsed = if (open) state.collapsed + code
                                                else state.collapsed - code,
                                            ),
                                        )
                                    },
                                    onLongClick = {},
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FlagDot(currency.flag, currency.code, 30.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(code, color = Ink.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    currency.name + if (notes > 0) " · $notes note${if (notes == 1) "" else "s"}" else "",
                                    color = Ink.Muted,
                                    fontSize = 11.sp,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (localTotal > 0) CountMath.local(localTotal, currency.symbol) else "—",
                                    color = Ink.Text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (localTotal > 0) {
                                    Text(
                                        CountMath.usd(CountMath.toUsd(localTotal, code, rates, state.manualRates)),
                                        color = Ink.Green,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                            Text(
                                if (open) "▾" else "▸",
                                color = Ink.Muted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }

                        if (open) {
                            for (denomination in Banknotes.ladder(code)) {
                                DenominationRow(
                                    symbol = currency.symbol,
                                    denomination = denomination,
                                    quantity = stack[denomination] ?: 0,
                                    onStep = { delta ->
                                        Sfx.click()
                                        haptics.tick()
                                        commit(
                                            state.copy(
                                                counts = state.counts + (code to
                                                    CountMath.step(stack, denomination, delta)),
                                            ),
                                        )
                                    },
                                    onType = {
                                        Sfx.click()
                                        haptics.bump()
                                        keypad = KeypadRequest(
                                            kind = KeypadKind.Notes,
                                            code = code,
                                            denomination = denomination,
                                            initial = (stack[denomination] ?: 0).toString(),
                                        )
                                    },
                                )
                            }
                            val manual = state.manualRates.containsKey(code)
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "1 USD = ${CountMath.rate(CountMath.rateFor(code, rates, state.manualRates))} $code",
                                    color = Ink.Muted,
                                    fontSize = 11.sp,
                                )
                                if (manual) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "manual · reset",
                                        color = Ink.Amber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.combinedClickable(
                                            onClick = {
                                                Sfx.click()
                                                commit(state.copy(manualRates = state.manualRates - code))
                                            },
                                            onLongClick = {},
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Box(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Ink.Surface)
                            .combinedClickable(
                                onClick = { Sfx.click(); picking = true },
                                onLongClick = { Sfx.click(); picking = true },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+  Add currency", color = Ink.Green, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp)) {
            FloatingNav(selected = NavTab.Count, onSelect = onSelectTab)
        }

        // ---------- sheets ----------
        keypad?.let { request ->
            val title = when (request.kind) {
                KeypadKind.Notes -> {
                    val currency = CurrencyCatalog.of(request.code ?: "USD")
                    "How many ${currency.symbol}${CountMath.group((request.denomination ?: 0L).toDouble())} notes?"
                }
                KeypadKind.Expected -> "Expected amount · USD"
                KeypadKind.Rate -> "Manual rate · 1 USD = ? ${request.code}"
            }
            CountKeypadSheet(
                title = title,
                initial = request.initial,
                decimals = request.kind != KeypadKind.Notes,
                onDone = { value ->
                    when (request.kind) {
                        KeypadKind.Notes -> {
                            val code = request.code ?: ""
                            commit(
                                state.copy(
                                    counts = state.counts + (code to CountMath.set(
                                        state.stack(code),
                                        request.denomination ?: 0L,
                                        value.toInt(),
                                    )),
                                ),
                            )
                        }
                        KeypadKind.Expected -> commit(state.copy(expected = value))
                        KeypadKind.Rate -> {
                            val code = request.code ?: ""
                            if (value > 0) {
                                commit(state.copy(manualRates = state.manualRates + (code to value)))
                            }
                        }
                    }
                    keypad = null
                },
                onDismiss = { keypad = null },
            )
        }

        if (picking) {
            CountPickerSheet(
                bag = state.bag,
                rates = rates,
                manual = state.manualRates,
                onToggle = { code ->
                    Sfx.click()
                    val next = if (state.bag.contains(code)) state.bag - code else state.bag + code
                    if (next.isNotEmpty()) commit(state.copy(bag = next))
                },
                onDismiss = { picking = false },
            )
        }

        if (ratesOpen) {
            CountRatesSheet(
                bag = state.bag,
                rates = rates,
                manual = state.manualRates,
                live = ratesLive,
                updatedAt = ratesUpdatedAt,
                onEdit = { code ->
                    ratesOpen = false
                    keypad = KeypadRequest(
                        kind = KeypadKind.Rate,
                        code = code,
                        initial = CountMath.rateFor(code, rates, state.manualRates).toString(),
                    )
                },
                onReset = { code -> commit(state.copy(manualRates = state.manualRates - code)) },
                onDismiss = { ratesOpen = false },
            )
        }

        if (historyOpen) {
            CountHistorySheet(
                history = state.history,
                onRestore = { entry ->
                    Sfx.click()
                    // Put the notes back exactly as they were saved, with the rates that priced them,
                    // so the restored screen reproduces the saved total rather than today's.
                    val bag = (entry.stacks.keys + entry.parts.keys + state.bag)
                        .distinct()
                        .filter { Banknotes.has(it) }
                    commit(
                        state.copy(
                            bag = bag,
                            counts = entry.stacks,
                            manualRates = state.manualRates + entry.rates,
                            expected = if (entry.expected > 0) entry.expected else state.expected,
                            savedFingerprint = CountState.fingerprintOf(entry.stacks),
                        ),
                    )
                    historyOpen = false
                },
                onDelete = { entry ->
                    Sfx.click()
                    commit(state.copy(history = state.history - entry))
                },
                onDismiss = { historyOpen = false },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DenominationRow(
    symbol: String,
    denomination: Long,
    quantity: Int,
    onStep: (Int) -> Unit,
    onType: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(62.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(78.dp)) {
            Text(
                symbol + CountMath.group(denomination.toDouble()),
                color = Ink.Text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
            Text("banknote", color = Ink.Muted, fontSize = 10.sp, letterSpacing = 0.5.sp)
        }
        Row(
            Modifier.clip(RoundedCornerShape(22.dp)).background(Color(0xFF141416)).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton(
                label = "−",
                enabled = quantity > 0,
                background = Color(0xFF3A3A3E),
                tint = Color(0xFFF2F2F7),
            ) { onStep(-1) }
            Text(
                text = quantity.toString(),
                color = Ink.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = TextStyle(fontFeatureSettings = "tnum"),
                modifier = Modifier
                    .width(32.dp)
                    .combinedClickable(onClick = onType, onLongClick = onType),
            )
            StepButton(
                label = "+",
                enabled = true,
                background = Ink.Green,
                tint = Color.White,
                onLongPress = onType,
            ) { onStep(1) }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = if (quantity == 0) "—" else CountMath.local(denomination.toDouble() * quantity, symbol),
            color = if (quantity == 0) Color(0xFF56565C) else Ink.Green,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(84.dp),
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StepButton(
    label: String,
    enabled: Boolean,
    background: Color,
    tint: Color,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (enabled) background else background.copy(alpha = 0.28f))
            .combinedClickable(
                onClick = { if (enabled) onClick() },
                onLongClick = { if (enabled) (onLongPress ?: onClick).invoke() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = tint, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SmallAction(label: String, dot: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = { Sfx.click(); onClick() }, onLongClick = { Sfx.click(); onClick() })
            .padding(horizontal = 7.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color(0xFFA9CDBA), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            if (dot) {
                Spacer(Modifier.width(3.dp))
                Box(Modifier.size(5.dp).clip(CircleShape).background(Ink.Green))
            }
        }
    }
}

/** The expected-vs-counted verdict: the one line that answers "did I count what I should have?". */
@Composable
fun MatchChip(match: CountMath.Match) {
    val (label, colour) = when (match.verdict) {
        CountMath.Verdict.None -> "—" to Ink.Muted
        CountMath.Verdict.Match -> "✓ Match" to Ink.Green
        CountMath.Verdict.Short -> "Short ${CountMath.usd(match.difference)}" to Ink.Red
        CountMath.Verdict.Over -> "Over ${CountMath.usd(match.difference)}" to Ink.Amber
    }
    Box(
        Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(colour.copy(alpha = if (match.verdict == CountMath.Verdict.None) 0.16f else 0.16f))
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = colour, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    }
}
