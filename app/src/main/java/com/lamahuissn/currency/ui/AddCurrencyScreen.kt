package com.lamahuissn.currency.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamahuissn.currency.data.Currency
import com.lamahuissn.currency.data.CurrencyCatalog
import com.lamahuissn.currency.data.Metals
import com.lamahuissn.currency.data.sectionStartIndices
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
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val source = when (tab) {
        CatalogTab.All -> CurrencyCatalog.fiat + CurrencyCatalog.crypto
        CatalogTab.Crypto -> CurrencyCatalog.crypto
        // Only the metals with a free live feed; the rest would be a price we cannot fill.
        CatalogTab.Metal -> CurrencyCatalog.metals.filter { Metals.isMetal(it.code) }
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
    // The leading count is everything drawn above the alphabet: the location block (header + row)
    // and, when present, the "Selected" header plus one row per chosen currency.
    val leadingItems = (if (localShown) 2 else 0) +
        (if (chosen.isNotEmpty()) 1 + chosen.size else 0)
    val sectionIndex = sectionStartIndices(
        letters = sections.keys.toList(),
        rowsPerLetter = sections.values.map { it.size },
        leadingItems = leadingItems,
    )

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

            Row(Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
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

                // The A–Z scrubber owns its strip: outside the list (so it can never sit on a row or
                // its tick), one equal cell per letter, and the WHOLE strip is one gesture surface —
                // a tap or a finger dragged over it jumps to that letter, which is what a scrubber
                // has to do. The letter under the finger lights up, so a touch always shows a result.
                if (sections.size > 2) {
                    val letters = sections.keys.toList()
                    var stripHeight by remember { mutableStateOf(0f) }
                    var activeLetter by remember { mutableStateOf<Char?>(null) }

                    fun jumpTo(y: Float) {
                        if (letters.isEmpty() || stripHeight <= 0f) return
                        val cell = stripHeight / letters.size
                        val slot = (y / cell).toInt().coerceIn(0, letters.size - 1)
                        val letter = letters[slot]
                        activeLetter = letter
                        val target = sectionIndex[letter] ?: return
                        scope.launch {
                            val count = listState.layoutInfo.totalItemsCount
                            val safe = if (count > 0) target.coerceIn(0, count - 1) else target
                            listState.animateScrollToItem(safe)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .width(28.dp)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp)
                            .onSizeChanged { stripHeight = it.height.toFloat() }
                            .pointerInput(letters) {
                                detectTapGestures { offset -> jumpTo(offset.y) }
                            }
                            .pointerInput(letters) {
                                detectVerticalDragGestures(
                                    onDragStart = { offset -> jumpTo(offset.y) },
                                    onDragEnd = { activeLetter = null },
                                    onDragCancel = { activeLetter = null },
                                    onVerticalDrag = { change, _ ->
                                        jumpTo(change.position.y)
                                        change.consume()
                                    },
                                )
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        letters.forEach { letter ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clip(CircleShape)
                                        .background(if (activeLetter == letter) Ink.Green else Color.Transparent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = letter.toString(),
                                        color = if (activeLetter == letter) Color.White else Ink.Green,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // The keyboard is a window inset, not a resize: with edge-to-edge on, the search
                    // bar has to lift itself above the IME or it sits under the keys while typing.
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Ink.Surface)
                    // The WHOLE capsule is the target. Before, only the one-line-tall text field
                    // itself could take a tap, so a press on the padding did nothing and the
                    // keyboard only appeared when a tap happened to land on that thin strip.
                    .pointerInput(Unit) {
                        detectTapGestures {
                            searchFocus.requestFocus()
                            keyboard?.show()
                        }
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⌕", color = Ink.Muted, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart,
                ) {
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(searchFocus),
                        // The field fills the capsule so its own touch area is the full height;
                        // the box centres the single line inside it.
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                inner()
                            }
                        },
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
                tab == CatalogTab.Metal -> "Gold and silver are priced by the troy ounce. Tap one to add it to the list."
                else -> "Nothing here yet."
            },
            color = Ink.Muted,
            fontSize = 13.sp,
        )
    }
}
