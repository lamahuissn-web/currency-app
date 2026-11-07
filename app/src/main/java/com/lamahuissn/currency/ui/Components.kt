package com.lamahuissn.currency.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamahuissn.currency.data.Metals
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** A circular flag. Flags are the phone's own emoji, clipped so every row starts the same width. */
@Composable
fun FlagDot(flag: String, code: String, size: Dp = 36.dp) {
    // Gold and silver have no flag: they get an ingot drawn in their own metal instead.
    if (Metals.isMetal(code)) {
        MetalDot(code = code, size = size)
        return
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Ink.SurfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (flag.isNotEmpty()) {
            Text(
                text = flag,
                fontSize = (size.value * 0.72f).sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = code.take(1),
                color = Ink.Muted,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}

/**
 * The figure pill on the right of a row. Solid green carries the base, deep green the rest, and a
 * figure that changes rolls to its new value instead of snapping: the numbers are the app.
 */
@Composable
fun ValueBadge(text: String, solid: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (solid) Ink.Green else Ink.GreenDeep)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                (slideInVertically(tween(180)) { it / 2 } + fadeIn(tween(180))) togetherWith
                    (slideOutVertically(tween(140)) { -it / 2 } + fadeOut(tween(140)))
            },
            label = "value",
        ) { shown ->
            Text(
                text = shown,
                color = if (solid) Color.White else Ink.Green,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
            )
        }
    }
}

/** A pill that breathes while the first rates are still on their way. */
@Composable
fun ShimmerBadge() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(
        Modifier
            .width(72.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Ink.Green.copy(alpha = alpha * 0.35f))
    )
}

@Composable
fun HairLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Ink.Divider)
    )
}

/**
 * One currency line: flag, name over its code, and the figure at the far end. A long press on a row
 * in the watch list removes it — the gesture the list needs and the one the screen has room for.
 */
@Composable
fun CurrencyRow(
    name: String,
    code: String,
    flag: String,
    badge: String?,
    solid: Boolean,
    modifier: Modifier = Modifier,
    pinned: Boolean = false,
    handle: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .pointerInput(code, onLongClick) {
                detectTapGestures(
                    onTap = {
                        Sfx.click()
                        onClick()
                    },
                    onLongPress = { onLongClick?.invoke() },
                )
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlagDot(flag = flag, code = code)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = Ink.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (pinned) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = "Pinned",
                        tint = Ink.Green,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(code.uppercase(), color = Ink.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)
        }
        Spacer(Modifier.width(8.dp))
        if (handle) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Drag to reorder",
                tint = Ink.Muted,
                modifier = Modifier.size(24.dp),
            )
        } else if (badge == null) {
            ShimmerBadge()
        } else {
            ValueBadge(text = badge, solid = solid)
        }
    }
}

@Composable
fun FloatingNav(selected: NavTab, onSelect: (NavTab) -> Unit) {
    Row(
        modifier = Modifier
            .width(296.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Ink.Surface)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem("Convert", Icons.Rounded.AttachMoney, selected == NavTab.Convert, Modifier.weight(1f)) {
            onSelect(NavTab.Convert)
        }
        NavItem("Chart", Icons.Rounded.ShowChart, selected == NavTab.Chart, Modifier.weight(1f)) {
            onSelect(NavTab.Chart)
        }
        NavItem("Count", Icons.Rounded.Payments, selected == NavTab.Count, Modifier.weight(1f)) {
            onSelect(NavTab.Count)
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (selected) Ink.SurfaceHigh else Color.Transparent,
        animationSpec = tween(220),
        label = "navBackground",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) Ink.Green else Ink.Text,
        animationSpec = tween(220),
        label = "navTint",
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(26.dp))
            .background(background)
            .pointerInput(selected) {
                detectTapGestures {
                    Sfx.click()
                    onClick()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                color = tint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

enum class NavTab { Convert, Chart, Count }

/** The pill on the right of the Convert header: add a currency, or the list's small menu. */
@Composable
fun HeaderActions(onAdd: () -> Unit, onMenu: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Ink.Surface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier.size(24.dp).pointerInput(Unit) {
                detectTapGestures {
                    Sfx.click()
                    onAdd()
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = Ink.Text, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
        Box(
            Modifier.size(24.dp).pointerInput(Unit) {
                detectTapGestures {
                    Sfx.click()
                    onMenu()
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Text("···", color = Ink.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** A round icon button on a dark surface — the swap button, the close button, the confirm tick. */
@Composable
fun RoundButton(
    size: Dp = 36.dp,
    background: Color = Ink.Surface,
    border: Color? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(if (border != null) Modifier.border(1.dp, border, CircleShape) else Modifier)
            .pointerInput(Unit) {
                detectTapGestures {
                    Sfx.click()
                    onClick()
                }
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}


/**
 * The mark for gold and silver: a bullion bar drawn in its own metal, so the row reads as bullion at
 * a glance instead of showing a blank flag. Gold is warm yellow, silver is cool grey, both with a
 * top-face highlight — small enough that the shape carries the meaning, not the detail.
 */
@Composable
fun MetalDot(code: String, size: Dp = 36.dp) {
    val gold = code == "XAU"
    val top = if (gold) Color(0xFFFFE08A) else Color(0xFFF7F7FA)
    val body = if (gold) Color(0xFFE0AE2B) else Color(0xFFC9C9CF)
    val deep = if (gold) Color(0xFF8A5E00) else Color(0xFF6E6E75)

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Ink.SurfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.68f)) {
            val w = this.size.width
            val h = this.size.height

            // The bar, seen from the front: a trapezoid, wider at the base.
            val bar = Path().apply {
                moveTo(w * 0.06f, h * 0.78f)
                lineTo(w * 0.19f, h * 0.40f)
                lineTo(w * 0.81f, h * 0.40f)
                lineTo(w * 0.94f, h * 0.78f)
                close()
            }
            drawPath(bar, Brush.verticalGradient(listOf(body, deep)))

            // The lit top face, so it reads as metal rather than a grey wedge.
            val face = Path().apply {
                moveTo(w * 0.19f, h * 0.40f)
                lineTo(w * 0.30f, h * 0.20f)
                lineTo(w * 0.70f, h * 0.20f)
                lineTo(w * 0.81f, h * 0.40f)
                close()
            }
            drawPath(face, Brush.verticalGradient(listOf(top, body)))
        }
    }
}

/**
 * A row you move sideways: right pins, left deletes. The gesture is handled in one place so a swipe
 * can never also count as a tap that opens the keypad, and a row that is not dragged springs back.
 */
@Composable
fun SwipeRow(
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    pinned: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val maxSwipe = with(density) { 132.dp.toPx() }
    val threshold = with(density) { 92.dp.toPx() }
    val currentClick by rememberUpdatedState(onClick)
    val currentRight by rememberUpdatedState(onSwipeRight)
    val currentLeft by rememberUpdatedState(onSwipeLeft)

    Box(modifier.fillMaxWidth().clipToBounds()) {
        // The layer the row uncovers. Which side it paints is decided by the direction of the drag.
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(if (offset.value >= 0f) Ink.GreenDeep else Color(0xFF3A0F12))
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (offset.value >= 0f) Arrangement.Start else Arrangement.End,
        ) {
            val colour = if (offset.value >= 0f) Ink.Green else Ink.Red
            Icon(
                imageVector = if (offset.value >= 0f) Icons.Rounded.PushPin else Icons.Rounded.Delete,
                contentDescription = null,
                tint = colour,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (offset.value >= 0f) {
                    if (pinned) "Unpin" else "Pin"
                } else {
                    "Delete"
                },
                color = colour,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(Ink.Page)
                .pointerInput(onClick) {
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragging = false
                        var lastX = down.position.x
                        val startX = down.position.x
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!dragging) {
                                    currentClick()
                                } else {
                                    val settled = offset.value
                                    if (settled > threshold || settled < -threshold) Sfx.click()
                                    when {
                                        settled > threshold -> currentRight()
                                        settled < -threshold -> currentLeft()
                                    }
                                    scope.launch { offset.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = 420f)) }
                                }
                                break
                            }
                            val dx = change.position.x - lastX
                            lastX = change.position.x
                            if (!dragging && abs(change.position.x - startX) > slop) dragging = true
                            if (dragging) {
                                change.consume()
                                val next = (offset.value + dx).coerceIn(-maxSwipe, maxSwipe)
                                scope.launch { offset.snapTo(next) }
                            }
                        }
                    }
                },
        ) {
            content()
        }
    }
}
