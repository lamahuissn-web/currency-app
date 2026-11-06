package com.lamahuissn.currency.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamahuissn.currency.data.Currency

/** Choosing the two currencies a chart is drawn between. */
@Composable
fun PairPickerScreen(
    title: String,
    options: List<Currency>,
    selected: String,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Ink.Page)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.size(36.dp))
                Text(
                    title,
                    color = Ink.Text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                RoundButton(size = 36.dp, onClick = onClose) {
                    Text("✕", color = Ink.Text, fontSize = 14.sp)
                }
            }

            LazyColumn(Modifier.weight(1f).navigationBarsPadding()) {
                items(options, key = { it.code }) { currency ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .pointerInput(currency.code) { detectTapGestures { onPick(currency.code) } }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FlagDot(flag = currency.flag, code = currency.code, size = 32.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(currency.name, color = Ink.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(currency.code, color = Ink.Muted, fontSize = 11.sp, letterSpacing = 0.6.sp)
                        }
                        if (currency.code == selected) {
                            Text("✓", color = Ink.Green, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    HairLine()
                }
            }
        }
    }
}
