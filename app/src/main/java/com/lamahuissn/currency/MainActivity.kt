package com.lamahuissn.currency

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lamahuissn.currency.data.CurrencyCatalog
import com.lamahuissn.currency.ui.AddCurrencyScreen
import com.lamahuissn.currency.ui.AppViewModel
import com.lamahuissn.currency.ui.ChartScreen
import com.lamahuissn.currency.ui.ConvertScreen
import com.lamahuissn.currency.ui.CurrencyTheme
import com.lamahuissn.currency.ui.KeypadSheet
import com.lamahuissn.currency.ui.NavTab
import com.lamahuissn.currency.ui.PairPickerScreen
import com.lamahuissn.currency.ui.Sfx

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Sfx.init(this)
        setContent {
            CurrencyTheme {
                Root()
            }
        }
    }
}

@Composable
private fun Root(viewModel: AppViewModel = viewModel()) {
    val convert by viewModel.convert.collectAsState()
    val chart by viewModel.chart.collectAsState()

    val context = LocalContext.current
    // The phone's own Settings › Sound › Touch sounds switch wins over the app's menu.
    val systemTouchSounds = remember {
        runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1) == 1
        }.getOrDefault(true)
    }
    LaunchedEffect(convert.soundOn, systemTouchSounds) {
        Sfx.setEnabled(convert.soundOn)
        Sfx.setSystemEnabled(systemTouchSounds)
    }

    var tab by remember { mutableStateOf(NavTab.Convert) }
    var addingCurrency by remember { mutableStateOf(false) }
    var keypadFor by remember { mutableStateOf<String?>(null) }
    var pickingPair by remember { mutableStateOf<String?>(null) }

    // The sheets keep their last subject while sliding out, so an exit animation has something to
    // draw instead of an empty frame.
    var lastKeypad by remember { mutableStateOf<String?>(null) }
    var lastPicker by remember { mutableStateOf<String?>(null) }
    SideEffect {
        if (keypadFor != null) lastKeypad = keypadFor
        if (pickingPair != null) lastPicker = pickingPair
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 14 }) togetherWith fadeOut(tween(120))
            },
            label = "tab",
        ) { current ->
            if (current == NavTab.Convert) {
                ConvertScreen(
                    state = convert,
                    onRowTap = { code -> keypadFor = code },
                    onAdd = { addingCurrency = true },
                    onRemove = { code -> viewModel.removeTarget(code) },
                    onPin = { code -> viewModel.togglePin(code) },
                    onMove = { from, to -> viewModel.moveTarget(from, to) },
                    onSort = { byValue -> viewModel.sortTargets(byValue) },
                    onReset = { viewModel.resetTargets() },
                    onRefresh = { viewModel.refresh() },
                    onToggleSound = { viewModel.toggleSound() },
                    onSelectTab = { selected ->
                        tab = selected
                        if (selected == NavTab.Chart) viewModel.setChartPair(chart.base, chart.quote)
                    },
                )
            } else {
                ChartScreen(
                    state = chart,
                    onPeriod = { viewModel.setChartPeriod(it) },
                    onSwap = { viewModel.swapChartPair() },
                    onPickBase = { pickingPair = "base" },
                    onPickQuote = { pickingPair = "quote" },
                    onSelectTab = { tab = it },
                )
            }
        }

        AnimatedVisibility(
            visible = keypadFor != null,
            enter = slideInVertically(tween(260)) { it } + fadeIn(tween(160)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
        ) {
            val code = keypadFor ?: lastKeypad ?: convert.base
            KeypadSheet(
                code = code,
                initialAmount = if (code == convert.base) {
                    convert.amount
                } else {
                    convert.converted(code) ?: 1.0
                },
                onDismiss = { keypadFor = null },
                onConvert = { amount ->
                    // Entering an amount for another currency re-bases the whole list on it.
                    viewModel.setBaseAndAmount(code, amount)
                    keypadFor = null
                },
            )
        }

        AnimatedVisibility(
            visible = addingCurrency,
            enter = slideInVertically(tween(280)) { it } + fadeIn(tween(160)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
        ) {
            AddCurrencyScreen(
                // The base counts as chosen, and cannot be added to its own list.
                selected = (convert.rows + convert.base).distinct(),
                baseCode = convert.base,
                onToggle = { code ->
                    if (code == convert.base) {
                        // Nothing to do: the input line already is this currency.
                    } else if (convert.targets.contains(code)) {
                        viewModel.removeTarget(code)
                    } else {
                        viewModel.addTarget(code)
                    }
                },
                onClose = { addingCurrency = false },
            )
        }

        AnimatedVisibility(
            visible = pickingPair != null,
            enter = slideInVertically(tween(260)) { it } + fadeIn(tween(160)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
        ) {
            val which = pickingPair ?: lastPicker ?: "base"
            val baseIsPicking = which == "base"
            val options = (listOf(convert.base) + convert.rows)
                .distinct()
                .map { CurrencyCatalog.of(it) }
            PairPickerScreen(
                title = if (baseIsPicking) "Chart currency" else "Chart against",
                options = options,
                selected = if (baseIsPicking) chart.base else chart.quote,
                onPick = { code ->
                    if (baseIsPicking) viewModel.setChartPair(code, chart.quote)
                    else viewModel.setChartPair(chart.base, code)
                    pickingPair = null
                },
                onClose = { pickingPair = null },
            )
        }
    }
}
