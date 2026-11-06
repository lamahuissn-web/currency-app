package com.lamahuissn.currency.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamahuissn.currency.data.Currency
import com.lamahuissn.currency.data.CurrencyCatalog
import kotlinx.coroutines.launch
import java.util.Currency as JavaCurrency
import java.util.Locale

private enum class CatalogTab(val label: String) { All("All"), Crypto("Crypto"), Metal("Metal") }

/**
 * The add-currency sheet: a segmented filter, the owner's own country first, then the alphabet with
 * a scrubber on the right edge and a search box pinned to the bottom. Tapping a row adds or removes
 * it immediately; the tick in the corner closes the sheet.
 */
@Composable
fun AddCurrencyScreen(
    selected: List<String>,
    baseCode: String,
    onToggle: (String) -> Unit,
    onClose: () -> Unit,
) {
    var tab by remember { mutableStateOf(CatalogTab.All) }
    var query by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val source = when (tab) {
        CatalogTab.All -> CurrencyCatalog.fiat + CurrencyCatalog.crypto
        CatalogTab.Crypto -> CurrencyCatalog.crypto
        CatalogTab.Metal -> CurrencyCatalog.metals
    }
    val filtered = source.filter { it.matches(query) }.sortedBy { it.name.lowercase() }

    // The currency of the phone's own country, offered above the alphabet.
    val local = remember {
        runCatching {
            val code = JavaCurrency.getInstance(Locale.getDefault()).currencyCode
            CurrencyCatalog.fiat.firstOrNull { it.code == code }
        }.getOrNull()
    }
    val localShown = local != null && tab != CatalogTab.Metal && query.isBlank()

    // Everything already chosen sits in one block at the top, in the order it was picked.
    val chosen = selected.distinct().map { CurrencyCatalog.of(it) }

    // A currency already in that block must not appear again under its letter (nor a second time
    // under "My Location"), which is the duplication the owner complained about.
    val listed = filtered.filter { currency ->
        currency.code !in chosen.map { it.code } && !(localShown && local != null && currency.code == local.code)
    }
    val sections = listed.groupBy { it.initial }.toSortedMap()

    // Item index of each section header inside the list, so the scrubber can jump to it exactly.
    var cursor = (if (localShown) 2 else 0) + (if (chosen.isNotEmpty()) 1 + chosen.size else 0)
    val sectionIndex = LinkedHashMap<Char, Int>()
    sections.forEach { (letter, rows) ->
        sectionIndex[letter] = cursor
        cursor += 1 + rows.size
    }

    Box(Modifier.fillMaxSize().background(Ink.Page)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.size(36.dp))
                Text(
                    text = "Add Currency",
                    color = Ink.Text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                RoundButton(size = 36.dp, background = Ink.Green, onClick = onClose) {
                    Icon(Icons.Rounded.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Ink.Surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CatalogTab.entries.forEach { entry ->
                    val active = entry == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) Ink.SurfaceHigh else Color.Transparent)
                            .pointerInput(entry) { detectTapGestures { tab = entry } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            entry.label,
                            color = if (active) Ink.Text else Ink.Muted,
                            fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    if (localShown && local != null) {
                        item(key = "location-header") {
                            SectionHeader("My Location — ${local.country}")
                        }
                        item(key = "location-row") {
                            CurrencyPickRow(
                                currency = local,
                                selected = local.code in chosen.map { it.code },
                                isBase = local.code == baseCode,
                                onToggle = onToggle,
                            )
                            HairLine()
                        }
                    }

                    if (chosen.isNotEmpty()) {
                        item(key = "chosen-header") {
                            SectionHeader("Selected · ${chosen.size}")
                        }
                        chosen.forEach { currency ->
                            item(key = "chosen-${currency.code}") {
                                CurrencyPickRow(
                                    currency = currency,
                                    selected = true,
                                    isBase = currency.code == baseCode,
                                    onToggle = onToggle,
                                )
                                HairLine()
                            }
                        }
                    }

                    sections.forEach { (letter, rows) ->
                        item(key = "header-$letter") { SectionHeader(letter.toString()) }
                        rows.forEach { currency ->
                            item(key = currency.code) {
                                Box(Modifier.animateItem()) {
                                    CurrencyPickRow(currency, false, false, onToggle)
                                }
                                HairLine()
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        item { EmptyNote(tab, query) }
                    }
                }

                if (sections.size > 2) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp)
                            .fillMaxHeight(0.62f),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        sections.keys.forEach { letter ->
                            Text(
                                text = letter.toString(),
                                color = Ink.Green,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .pointerInput(letter) {
                                        detectTapGestures {
                                            val target = sectionIndex[letter] ?: return@detectTapGestures
                                            scope.launch { listState.scrollToItem(target) }
                                        }
                                    }
                                    .padding(vertical = 1.dp, horizontal = 4.dp),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Ink.Surface)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⌕", color = Ink.Muted, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Currency, Country, Region, or Code",
                            color = Ink.Muted,
                            fontSize = 13.sp,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Ink.Text, fontSize = 13.sp),
                        cursorBrush = SolidColor(Ink.Green),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    Text(
                        "✕",
                        color = Ink.Muted,
                        fontSize = 14.sp,
                        modifier = Modifier.pointerInput(Unit) { detectTapGestures { query = "" } },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Ink.Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun CurrencyPickRow(
    currency: Currency,
    selected: Boolean,
    isBase: Boolean = false,
    onToggle: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .pointerInput(currency.code, selected) { detectTapGestures { onToggle(currency.code) } }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlagDot(flag = currency.flag, code = currency.code, size = 34.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(currency.name, color = Ink.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(currency.code, color = Ink.Muted, fontSize = 12.sp, letterSpacing = 0.6.sp)
        }
        Spacer(Modifier.width(8.dp))
        if (isBase) {
            // The input line of the Convert screen; it cannot also be one of its own targets.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Ink.GreenDeep)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text("base", color = Ink.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) Ink.Green else Color.Transparent)
                .then(if (selected) Modifier else Modifier.border(1.5.dp, Color(0xFF3A3A3C), CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun EmptyNote(tab: CatalogTab, query: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Text(
            text = when {
                query.isNotBlank() -> "Nothing matches \"$query\"."
                tab == CatalogTab.Metal -> "Metals need a paid price feed (gold, silver, platinum), so this build does not carry them. Fiat and crypto are live."
                else -> "Nothing here yet."
            },
            color = Ink.Muted,
            fontSize = 13.sp,
        )
    }
}
