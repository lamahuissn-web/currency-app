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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamahuissn.currency.data.CurrencyCatalog
import com.lamahuissn.currency.data.Expr

/**
 * The amount sheet. It is the one warm surface in the app: the burgundy says "you are typing a
 * number", the black screens say "you are reading rates".
 *
 * The entry accepts arithmetic — `1500×3`, `200+350` — because collecting money is mostly sums, and
 * doing them here leaves no hand-rounding on the table. The running total shows above the pad as the
 * expression is built, so the number is never a surprise.
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

    val hasOperator = Expr.operatorCount(entry) > 0
    val value = Expr.evaluate(entry)
    val display = when {
        entry.isEmpty() -> "Enter Amount"
        hasOperator && value != null -> Expr.resultText(value)
        else -> Expr.display(entry)
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

            // The sum being built, small and out of the way — empty for a plain amount.
            Text(
                text = if (hasOperator) Expr.display(entry) else " ",
                color = Color(0xFFB9A7A8),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            // What was typed is what is shown: 100 stays 100, no forced decimals.
            Text(
                text = display,
                color = if (entry.isEmpty()) Color(0xFF8B7576) else Ink.Text,
                fontSize = if (display.length > 9) 30.sp else 40.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(fontFeatureSettings = "tnum"),
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

            OperatorRow(
                onOperator = { op ->
                    entry = when {
                        entry.isEmpty() -> ""                       // nothing to operate on yet
                        Expr.isOperator(entry.last()) ->
                            entry.dropLast(1) + op                  // replace, never double up
                        else -> (entry + op).take(24)
                    }
                },
            )

            Spacer(Modifier.height(12.dp))

            Keypad(
                onDigit = { digit ->
                    val operand = entry.split('+', '−', '×', '÷').last()
                    entry = when {
                        digit == "." && operand.contains(".") -> entry
                        digit == "." && (entry.isEmpty() || Expr.isOperator(entry.last())) -> entry + "0."
                        Expr.isOperator(entry.lastOrNull() ?: ' ') -> entry + digit
                        entry == "0" && digit != "." -> digit
                        entry.length >= 24 -> entry
                        else -> entry + digit
                    }
                },
                // One character at a time; C wipes the lot.
                onBackspace = { entry = entry.dropLast(1) },
                onClear = { entry = "" },
                onQuick = { amount -> entry = trimNumber(amount) },
            )

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (value == null || value <= 0.0) Color(0xFF1E7A2E) else Ink.Green)
                    .pointerInput(entry) {
                        detectTapGestures {
                            val computed = Expr.evaluate(entry)
                            if (computed != null && computed > 0.0) onConvert(computed)
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

/** ÷ × − + across the sheet: the four operations a till actually needs. */
@Composable
private fun OperatorRow(onOperator: (Char) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf('÷', '×', '−', '+').forEach { op ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x2EFFFFFF))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
                    .pointerInput(op) {
                        detectTapGestures {
                            Sfx.click()
                            onOperator(op)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(op.toString(), color = Ink.Text, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onQuick: (Double) -> Unit,
) {
    // The fourth column is the quick-amount column; C takes the place of the least useful chip.
    val rows = listOf(
        listOf("7", "8", "9") to "1K",
        listOf("4", "5", "6") to "100",
        listOf("1", "2", "3") to "10",
        listOf("0", ".", "⌫") to "C",
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { (digits, quick) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                    Sfx.click()
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
                        .pointerInput(quick) {
                            detectTapGestures {
                                Sfx.click()
                                when (quick) {
                                    "C" -> onClear()
                                    "1K" -> onQuick(1000.0)
                                    "100" -> onQuick(100.0)
                                    else -> onQuick(10.0)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = quick,
                        color = if (quick == "C") Color(0xFFFFD9D9) else Ink.Text,
                        fontSize = if (quick == "C") 20.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
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
