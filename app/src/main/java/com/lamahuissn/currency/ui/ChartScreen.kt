package com.lamahuissn.currency.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamahuissn.currency.data.CurrencyCatalog
import com.lamahuissn.currency.data.formatValue
import com.lamahuissn.currency.data.percentChange

private val PERIODS = listOf("1D", "1W", "1M", "3M", "6M", "1Y", "2Y")

@Composable
fun ChartScreen(
    state: ChartUi,
    onPeriod: (String) -> Unit,
    onSwap: () -> Unit,
    onPickBase: () -> Unit,
    onPickQuote: () -> Unit,
    onSelectTab: (NavTab) -> Unit,
) {
    val base = CurrencyCatalog.of(state.base)
    val quote = CurrencyCatalog.of(state.quote)
    val first = state.first
    val last = state.last
    val change = if (first != null && last != null) percentChange(first, last) else null
    val rising = (change ?: 0.0) >= 0.0

    Box(Modifier.fillMaxSize().background(Ink.Page)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${quote.code} per 1 ${base.code}",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    color = Ink.Text,
                    modifier = Modifier.weight(1f),
                )
                RoundButton(size = 36.dp, onClick = onSwap) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = "Swap the pair",
                        tint = Ink.Text,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = last?.let { "$" + formatValue(it) } ?: "—",
                    color = Ink.Text,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light,
                )
                if (change != null) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = (if (rising) "↑ " else "↓ ") + formatValue(kotlin.math.abs(change)) + "%",
                        color = if (rising) Ink.Green else Ink.Red,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.loading -> Text("Loading…", color = Ink.Muted, fontSize = 13.sp)
                    state.points.size < 2 -> Text(
                        state.error ?: "No series for this pair",
                        color = Ink.Muted,
                        fontSize = 13.sp,
                    )
                    else -> TrendChart(
                        values = state.points.map { it.value },
                        color = if (rising) Ink.Green else Ink.Red,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PERIODS.forEach { period ->
                    val selected = period == state.period
                    val chipBackground by androidx.compose.animation.animateColorAsState(
                        targetValue = if (selected) Ink.SurfaceHigh else Color.Transparent,
                        animationSpec = tween(200),
                        label = "chip",
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(chipBackground)
                            .pointerInput(period) {
                                detectTapGestures {
                                    Sfx.click()
                                    onPeriod(period)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            period,
                            color = if (selected) Ink.Text else Ink.Muted,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PairButton(base = true, flag = base.flag, code = base.code, Modifier.weight(1f), onPickBase)
                PairButton(base = false, flag = quote.flag, code = quote.code, Modifier.weight(1f), onPickQuote)
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Tap a currency to change the pair.",
                color = Ink.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            FloatingNav(selected = NavTab.Chart, onSelect = onSelectTab)
        }
    }
}

@Composable
private fun PairButton(
    base: Boolean,
    flag: String,
    code: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Ink.Surface)
            .pointerInput(code, base) { detectTapGestures { onClick() } }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        FlagDot(flag = flag, code = code, size = 28.dp)
        Spacer(Modifier.width(10.dp))
        Text(code, color = Ink.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** The line and its fading area, drawn directly: axes and gridlines would be noise at this size. */
@Composable
private fun TrendChart(values: List<Double>, color: Color) {
    // The series draws itself left to right when a new pair or period arrives.
    var progress by remember(values) { mutableFloatStateOf(0f) }
    LaunchedEffect(values) {
        animate(0f, 1f, animationSpec = tween(650)) { value, _ -> progress = value }
    }

    Canvas(Modifier.fillMaxSize()) {
        if (values.size < 2) return@Canvas
        val minValue = values.minOrNull() ?: return@Canvas
        val maxValue = values.maxOrNull() ?: return@Canvas
        val range = (maxValue - minValue).let { if (it <= 0.0) maxValue * 0.01 + 1e-9 else it }
        val width = size.width
        val height = size.height
        val top = height * 0.06f
        val usable = height * 0.88f

        fun xAt(index: Int): Float = index.toFloat() / (values.size - 1) * width
        fun yAt(value: Double): Float = (top + usable - ((value - minValue) / range * usable)).toFloat()

        val lastShown = ((values.size - 1) * progress).toInt().coerceIn(1, values.size - 1)
        val line = Path()
        for (index in 0..lastShown) {
            val x = xAt(index)
            val y = yAt(values[index])
            if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }

        val area = Path().apply {
            addPath(line)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.02f)),
                startY = 0f,
                endY = height,
            ),
        )
        drawPath(
            path = line,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawCircle(
            color = color,
            radius = 3.dp.toPx(),
            center = Offset(xAt(lastShown), yAt(values[lastShown])),
        )
    }
}
