package com.lamahuissn.currency.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The first second of the app: a gold bar and a silver bar settle into the frame over a market line,
 * then the name. It is deliberately short — a splash that makes the owner wait is a splash that gets
 * on his nerves — and it plays once per launch, not on every resume.
 */
private object SplashOnce {
    var shown = false
}

@Composable
fun SplashOverlay(version: String) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }
    val p = t.value

    Box(
        Modifier.fillMaxSize().background(Ink.Page),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ---- the mark ----
            Canvas(Modifier.size(180.dp)) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f

                // A quiet glow so the bars sit in light rather than on a flat black.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x2E00D632), Color(0x00000000)),
                        center = Offset(cx, cy),
                        radius = w * 0.48f,
                    ),
                    radius = w * 0.48f,
                    center = Offset(cx, cy),
                )

                // The market line under the bars, drawn on as the bars arrive.
                val lineY = cy + h * 0.26f
                val pts = listOf(
                    Offset(w * 0.10f, lineY + h * 0.04f),
                    Offset(w * 0.26f, lineY - h * 0.02f),
                    Offset(w * 0.40f, lineY + h * 0.01f),
                    Offset(w * 0.54f, lineY - h * 0.06f),
                    Offset(w * 0.70f, lineY - h * 0.02f),
                    Offset(w * 0.90f, lineY - h * 0.11f),
                )
                val line = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                }
                val progress = (p / 0.55f).coerceIn(0f, 1f)
                if (progress > 0f) {
                    rotate(0f) {
                        drawPath(
                            path = line,
                            color = Ink.Green,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                            alpha = 1f,
                        )
                    }
                    // Mask the tail so the line looks like it is being drawn, not just appearing.
                    drawRect(
                        color = Ink.Page,
                        topLeft = Offset(w * (0.10f + 0.80f * progress), 0f),
                        size = Size(w, h),
                    )
                }

                // ---- gold bar, sliding in from the left ----
                val slide = (1f - ((p / 0.75f).coerceIn(0f, 1f))) * 54f
                rotate(degrees = -7f, pivot = Offset(cx - w * 0.16f, cy)) {
                    drawBar(
                        left = w * 0.16f - slide,
                        top = cy - h * 0.16f,
                        width = w * 0.34f,
                        height = h * 0.20f,
                        topFace = Color(0xFFFFE08A),
                        body = Color(0xFFE0AE2B),
                        deep = Color(0xFF8A5E00),
                        alpha = p,
                    )
                }

                // ---- silver bar, sliding in from the right ----
                rotate(degrees = 7f, pivot = Offset(cx + w * 0.18f, cy + h * 0.02f)) {
                    drawBar(
                        left = w * 0.50f + slide,
                        top = cy + h * 0.02f,
                        width = w * 0.34f,
                        height = h * 0.20f,
                        topFace = Color(0xFFF7F7FA),
                        body = Color(0xFFC9C9CF),
                        deep = Color(0xFF6E6E75),
                        alpha = p,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Currency",
                color = Ink.Text.copy(alpha = ((p - 0.20f) / 0.80f).coerceIn(0f, 1f)),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "RATES · GOLD · COUNT",
                color = Ink.Muted.copy(alpha = ((p - 0.35f) / 0.65f).coerceIn(0f, 1f)),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
            )

            Spacer(Modifier.height(34.dp))

            // A hairline that fills as the app gets ready underneath.
            Box(
                Modifier
                    .width(120.dp)
                    .height(2.dp)
                    .background(Ink.SurfaceHigh),
            ) {
                Box(
                    Modifier
                        .width(120.dp * p)
                        .height(2.dp)
                        .background(Ink.Green),
                )
            }
        }

        Text(
            text = "v$version",
            color = Ink.Muted.copy(alpha = 0.5f),
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp),
        )
    }
}

/** One ingot: a trapezoid body with a lit top face, the same shape the row marks use. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBar(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    topFace: Color,
    body: Color,
    deep: Color,
    alpha: Float,
) {
    val bodyPath = Path().apply {
        moveTo(left, top + height)
        lineTo(left + width * 0.14f, top + height * 0.34f)
        lineTo(left + width * 0.86f, top + height * 0.34f)
        lineTo(left + width, top + height)
        close()
    }
    drawPath(bodyPath, Brush.verticalGradient(listOf(body, deep)), alpha = alpha)

    val facePath = Path().apply {
        moveTo(left + width * 0.14f, top + height * 0.34f)
        lineTo(left + width * 0.26f, top)
        lineTo(left + width * 0.74f, top)
        lineTo(left + width * 0.86f, top + height * 0.34f)
        close()
    }
    drawPath(facePath, Brush.verticalGradient(listOf(topFace, body)), alpha = alpha)
}

/** True on the first composition of the process only, so a resume never replays the splash. */
@Composable
fun rememberSplashVisible(): Boolean = !SplashOnce.shown

fun markSplashSeen() {
    SplashOnce.shown = true
}
