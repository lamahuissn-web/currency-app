package com.lamahuissn.currency.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamahuissn.currency.data.CurrencyCatalog
import com.lamahuissn.currency.data.groupDigits

/**
 * The amount sheet. It is the one warm surface in the app: the burgundy says "you are typing a
 * number", the black screens say "you are reading rates".
 */
@Composable
fun KeypadSheet(
    code: String,
    initialAmount: Double,
    onDismiss: () -> Unit,
    onConvert: (Double) -> Unit,
) {
    val currency = CurrencyCatalog.of(code)
    var entry by remember(code, initialAmount) {
        mutableStateOf(trimNumber(initialAmount))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Ink.SheetTop, Ink.SheetMid, Ink.SheetBottom)
                )
            )
            .statusBarsPadding()
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).navigationBarsPadding()) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlagDot(flag = currency.flag, code = currency.code, size = 34.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(currency.name, color = Ink.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(currency.code, color = Color(0xFFB9A7A8), fontSize = 12.sp, letterSpacing = 0.6.sp)
                }
                RoundButton(
                    size = 34.dp,
                    background = Color(0xFF2A1F20),
                    border = Color(0x33FFFFFF),
                    onClick = onDismiss,
                ) {
                    Text("✕", color = Ink.Text, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            // What was typed is what is shown: 100 stays 100, no forced decimals.
            Text(
                text = if (entry.isEmpty()) "Enter Amount" else groupDigits(entry),
                color = if (entry.isEmpty()) Color(0xFF8B7576) else Ink.Text,
                fontSize = if (entry.length > 9) 30.sp else 40.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "${currency.symbol.ifEmpty { currency.code }} ${currency.code}",
                color = Color(0xFF8B7576),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.weight(1f))

            Keypad(
                onDigit = { digit ->
                    entry = when {
                        digit == "." && entry.contains(".") -> entry
                        digit == "." && entry.isEmpty() -> "0."
                        entry == "0" && digit != "." -> digit
                        entry.length >= 12 -> entry
                        else -> entry + digit
                    }
                },
                // One tap wipes the whole amount, as asked: the key is a clear, not a digit eraser.
                onBackspace = { entry = "" },
                onQuick = { amount -> entry = trimNumber(amount) },
            )

            Spacer(Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (entry.isEmpty()) Color(0xFF1E7A2E) else Ink.Green)
                    .pointerInput(entry) {
                        detectTapGestures {
                            val value = entry.toDoubleOrNull()
                            if (value != null && value > 0.0) onConvert(value)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Convert", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onQuick: (Double) -> Unit,
) {
    val rows = listOf(
        listOf("7", "8", "9") to ("1K" to 1000.0),
        listOf("4", "5", "6") to ("100" to 100.0),
        listOf("1", "2", "3") to ("10" to 10.0),
        listOf("0", ".", "⌫") to ("1" to 1.0),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { (digits, quick) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                digits.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(
                                if (key == "⌫") Color(0xFF4A3536) else Color(0x22FFFFFF)
                            )
                            .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                            .pointerInput(key) {
                                detectTapGestures {
                                    when (key) {
                                        "⌫" -> onBackspace()
                                        else -> onDigit(key)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(key, color = Ink.Text, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(Color(0x33000000))
                        .pointerInput(quick.first) { detectTapGestures { onQuick(quick.second) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(quick.first, color = Ink.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** A starting amount without a trailing ".0" — 1.0 reads as 1 in the entry line. */
private fun trimNumber(value: Double): String {
    val rounded = kotlin.math.round(value * 100.0) / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}
