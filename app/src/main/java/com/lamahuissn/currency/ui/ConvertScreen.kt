package com.lamahuissn.currency.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lamahuissn.currency.BuildConfig
import com.lamahuissn.currency.data.CurrencyCatalog
import com.lamahuissn.currency.data.Metals
import com.lamahuissn.currency.data.badgeText
import com.lamahuissn.currency.data.visibleTargets
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertScreen(
    state: ConvertUi,
    onRowTap: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onPin: (String) -> Unit,
    onMove: (from: String, to: String) -> Unit,
    onSort: (byValue: Boolean) -> Unit,
    onReset: () -> Unit,
    onRefresh: () -> Unit,
    onToggleSound: () -> Unit,
    onSelectTab: (NavTab) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var metalFor by remember { mutableStateOf<String?>(null) }

    // A refresh that finished gets acknowledged: a green pill says so for a moment, so the owner
    // sees that the tap did something even when the numbers moved by a cent.
    var flash by remember { mutableStateOf(false) }
    var seenUpdate by remember { mutableStateOf(state.updatedAt) }
    LaunchedEffect(state.updatedAt) {
        if (state.updatedAt > 0L && state.updatedAt != seenUpdate) {
            seenUpdate = state.updatedAt
            flash = true
            Sfx.click()
            delay(2200)
            flash = false
        }
    }
    var reorderMode by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val base = CurrencyCatalog.of(state.base)

    // A row is 64dp plus its hairline; the drag maths needs that exact pitch.
    val rowHeightPx = with(LocalDensity.current) { 65.dp.toPx() }

    // In reorder mode the list shows the STORED order, so what the owner drags is what gets saved.
    // Pins are applied again once the mode ends (they float to the top by design).
    val rows = if (reorderMode) visibleTargets(state.base, state.targets) else state.rows
    val rowsNow by rememberUpdatedState(rows)

    Box(Modifier.fillMaxSize().background(Ink.Page)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Currency",
                    style = MaterialTheme.typography.displaySmall,
                    color = Ink.Text,
                    modifier = Modifier.weight(1f),
                )
                if (reorderMode) {
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Ink.Green)
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    Sfx.click()
                                    dragging = null
                                    dragOffset = 0f
                                    reorderMode = false
                                }
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Done", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    HeaderActions(onAdd = onAdd, onMenu = { menuOpen = !menuOpen })
                }
            }

            Text(
                text = if (reorderMode) {
                    "Drag a row up or down to move it."
                } else {
                    statusLine(state)
                },
                color = if (state.error != null && !reorderMode) Ink.Red else Ink.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            )

            if (state.loading && state.usdTo.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(2.dp),
                    color = Ink.Green,
                    trackColor = Ink.Surface,
                )
            }

            if (reorderMode) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    items(rows, key = { it }) { code ->
                        val currency = CurrencyCatalog.of(code)
                        val isDragging = dragging == code
                        Box(
                            Modifier
                                .animateItem()
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                                .pointerInput(code) {
                                    detectDragGestures(
                                        onDragStart = {
                                            Sfx.click()
                                            dragging = code
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                            val list = rowsNow
                                            val from = list.indexOf(code)
                                            if (from >= 0) {
                                                val steps = (dragOffset / rowHeightPx).toInt()
                                                if (steps != 0) {
                                                    val to = (from + steps).coerceIn(0, list.lastIndex)
                                                    if (to != from) {
                                                        onMove(code, list[to])
                                                        // Keep the dragged row under the finger.
                                                        dragOffset -= steps * rowHeightPx
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = { dragging = null; dragOffset = 0f },
                                        onDragCancel = { dragging = null; dragOffset = 0f },
                                    )
                                },
                        ) {
                            CurrencyRow(
                                name = currency.name,
                                code = currency.code,
                                flag = currency.flag,
                                badge = null,
                                solid = false,
                                handle = true,
                                onClick = { },
                            )
                            HairLine()
                        }
                    }

                    item(key = "reorder-hint") {
                        Text(
                            text = "Pinned currencies return to the top when you tap Done.",
                            color = Ink.Muted.copy(alpha = 0.55f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
                }
            } else {
                // Pulling the list down re-reads the rates; the spinner is driven by the real
                // request, not by a flag that is cleared before the answer arrives.
                PullToRefreshBox(
                    isRefreshing = state.loading,
                    onRefresh = onRefresh,
                    modifier = Modifier.weight(1f),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp),
                    ) {
                        item(key = "base") {
                            CurrencyRow(
                                name = base.name,
                                code = base.code,
                                flag = base.flag,
                                badge = badgeText(base.code, state.amount),
                                solid = true,
                                onClick = { onRowTap(base.code) },
                            )
                            HairLine()
                        }

                        items(rows, key = { it }) { code ->
                            val currency = CurrencyCatalog.of(code)
                            val converted = state.converted(code)
                            val pinned = state.pinned.contains(code)
                            val badge = when {
                                // A metal is quoted by the ounce, so the row shows its price rather
                                // than "1 USD = 0.000248 gold".
                                Metals.isMetal(code) -> Metals.rowBadge(state.usdTo[code] ?: 0.0)
                                converted != null -> badgeText(code, converted)
                                state.loading -> null      // shimmer while the rates are on their way
                                else -> "—"
                            }
                            SwipeRow(
                                onSwipeRight = { onPin(code) },
                                onSwipeLeft = { onRemove(code) },
                                pinned = pinned,
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    if (Metals.isMetal(code)) metalFor = code else onRowTap(code)
                                },
                            ) {
                                CurrencyRow(
                                    name = currency.name,
                                    code = currency.code,
                                    flag = currency.flag,
                                    badge = badge,
                                    solid = false,
                                    pinned = pinned,
                                    // The tap belongs to the swipe container, so a drag never opens it.
                                    onClick = { },
                                )
                            }
                            HairLine()
                        }

                        if (rows.isEmpty()) {
                            item(key = "empty") {
                                Text(
                                    text = "No currencies yet. Tap + to add one.",
                                    color = Ink.Muted,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }

                        item(key = "hint") {
                            Text(
                                text = "Swipe a row right to pin it, left to delete it. Pull down to refresh.",
                                color = Ink.Muted.copy(alpha = 0.55f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            )
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            FloatingNav(selected = NavTab.Convert, onSelect = onSelectTab)
        }

        if (menuOpen) {
            OptionsSheet(
                onDismiss = { menuOpen = false },
                options = listOf<Pair<String, () -> Unit>>(
                    "Reorder currencies" to { menuOpen = false; reorderMode = true },
                    if (state.soundOn) {
                        "Sound: on" to { menuOpen = false; onToggleSound() }
                    } else {
                        "Sound: off" to { menuOpen = false; onToggleSound() }
                    },
                    "Sort A → Z" to { menuOpen = false; onSort(false) },
                    "Sort by value" to { menuOpen = false; onSort(true) },
                    "Refresh rates" to { menuOpen = false; onRefresh() },
                    "Restore default list" to { menuOpen = false; onReset() },
                ),
            )
        }

        AnimatedVisibility(
            visible = flash,
            enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { it / 2 },
            exit = fadeOut(tween(220)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp),
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Ink.GreenDeep)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✓", color = Ink.Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Prices updated · " +
                        SimpleDateFormat("HH:mm", Locale.US).format(Date(state.updatedAt)),
                    color = Ink.Green,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        metalFor?.let { code ->
            MetalsSheet(
                code = code,
                rates = state.usdTo,
                updatedAt = state.updatedAt,
                onDismiss = { metalFor = null },
            )
        }
    }
}

private fun statusLine(state: ConvertUi): String = when {
    state.loading && state.usdTo.isEmpty() -> "Loading rates…"
    state.error != null -> "Offline — showing the last saved rates"
    state.updatedAt > 0L -> {
        val time = SimpleDateFormat("HH:mm", Locale.US).format(Date(state.updatedAt))
        "Updated $time · ${state.usdTo.size} currencies · v${BuildConfig.VERSION_NAME}"
    }
    else -> "Rates load from open.er-api.com and CoinGecko"
}

/** A small scrim sheet for the list commands, so the header stays one pill wide. */
@Composable
fun OptionsSheet(onDismiss: () -> Unit, options: List<Pair<String, () -> Unit>>) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
                .clip(RoundedCornerShape(20.dp))
                .background(Ink.Surface)
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            options.forEach { (label, action) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .pointerInput(label) {
                            detectTapGestures {
                                Sfx.click()
                                action()
                            }
                        }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, color = Ink.Text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                if (label != options.last().first) {
                    Spacer(Modifier.height(1.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Ink.Divider))
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
