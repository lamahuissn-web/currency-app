package com.lamahuissn.currency.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lamahuissn.currency.data.CurrencyCatalog
import com.lamahuissn.currency.data.Point
import com.lamahuissn.currency.data.Metals
import com.lamahuissn.currency.data.Prefs
import com.lamahuissn.currency.data.RatesClient
import com.lamahuissn.currency.data.moveItem
import com.lamahuissn.currency.data.orderRows
import com.lamahuissn.currency.data.togglePin as togglePinCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class ConvertUi(
    val base: String = Prefs.DEFAULT_BASE,
    val amount: Double = 1.0,
    val targets: List<String> = Prefs.DEFAULT_TARGETS,
    val pinned: List<String> = emptyList(),
    val soundOn: Boolean = true,
    /** Units of each currency per 1 US dollar — the one table every figure is derived from. */
    val usdTo: Map<String, Double> = emptyMap(),
    val updatedAt: Long = 0L,
    /** When the metals were last read — they move intraday, the fiat table does not. */
    val metalsAt: Long = 0L,
    val loading: Boolean = false,
    val error: String? = null,
) {
    fun rate(from: String, to: String): Double? {
        if (from == to) return 1.0
        val a = usdTo[from] ?: return null
        val b = usdTo[to] ?: return null
        if (a == 0.0) return null
        // 1 unit of `from` is (1/a) dollars, and each dollar is b units of `to`.
        return b / a
    }

    fun converted(code: String): Double? = rate(base, code)?.times(amount)

    /** What the list draws: pinned first, never the base itself, never the same code twice. */
    val rows: List<String> get() = orderRows(base, targets, pinned)
}

data class ChartUi(
    val base: String = "EUR",
    val quote: String = "USD",
    val period: String = "3M",
    val points: List<Point> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
) {
    val first: Double? get() = points.firstOrNull()?.value
    val last: Double? get() = points.lastOrNull()?.value
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = Prefs(application)
    private val client = RatesClient()

    private var metalsJob: Job? = null
    private var metalsInFlight = false

    private val _convert = MutableStateFlow(
        ConvertUi(
            base = prefs.base,
            amount = prefs.amount,
            targets = prefs.targets,
            pinned = prefs.pinned,
            soundOn = prefs.soundOn,
            usdTo = prefs.cachedRates,
            updatedAt = prefs.updatedAt,
        )
    )
    val convert: StateFlow<ConvertUi> = _convert.asStateFlow()

    private val _chart = MutableStateFlow(
        ChartUi(base = prefs.chartBase, quote = prefs.chartQuote, period = prefs.chartPeriod)
    )
    val chart: StateFlow<ChartUi> = _chart.asStateFlow()

    init {
        // Gold and silver arrive as rows without the owner hunting for them, exactly once.
        if (!prefs.metalsSeeded) {
            val next = (prefs.targets + Metals.codes).distinct()
            prefs.targets = next
            prefs.metalsSeeded = true
            _convert.update { it.copy(targets = next) }
        }
        refresh()
        loadChart()
    }

    fun refresh() {
        if (_convert.value.loading) return
        _convert.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    // The fiat table is what the list is built on; a crypto failure must not blank it.
                    val fiat = client.fiatPerUsd()
                    val crypto = runCatching { client.cryptoPerUsd() }.getOrDefault(emptyMap())
                    // Metals ride along: if their feed is down the currency list still fills.
                    val metals = runCatching { client.metalsPerUsd() }.getOrDefault(emptyMap())
                    fiat + crypto + metals
                }
            }
            result.onSuccess { table ->
                val now = System.currentTimeMillis()
                prefs.cachedRates = table
                prefs.updatedAt = now
                _convert.update {
                    it.copy(
                        usdTo = table,
                        updatedAt = now,
                        metalsAt = if (Metals.codes.any(table::containsKey)) now else it.metalsAt,
                        loading = false,
                        error = null,
                    )
                }
            }.onFailure { failure ->
                _convert.update {
                    it.copy(
                        loading = false,
                        error = if (it.usdTo.isEmpty()) failure.message ?: "No connection" else null,
                    )
                }
            }
        }
    }

    /**
     * Re-read gold and silver only — two small requests. A refresh less than [METALS_INTERVAL_MS] old
     * is skipped unless [force] is set, so overlapping calls cannot stack up on the feed.
     */
    fun refreshMetals(force: Boolean = false) {
        // A full refresh already carries the metals; do not race it.
        if (_convert.value.loading) return
        val now = System.currentTimeMillis()
        if (!force && now - _convert.value.metalsAt < METALS_INTERVAL_MS) return
        // A slow network must not stack requests on the feed: one metal call at a time.
        if (metalsInFlight) return
        metalsInFlight = true
        viewModelScope.launch {
            val metals = try {
                withContext(Dispatchers.IO) {
                    runCatching { client.metalsPerUsd() }.getOrDefault(emptyMap())
                }
            } finally {
                metalsInFlight = false
            }
            if (metals.isEmpty()) {
                // The feed did not answer: keep the last prices and leave their timestamp alone, so
                // the sheet keeps telling the truth about how old the number is.
                _convert.update { it.copy(metalsAt = _convert.value.metalsAt) }
                return@launch
            }
            val at = System.currentTimeMillis()
            val merged = _convert.value.usdTo + metals
            prefs.cachedRates = merged
            _convert.update { it.copy(usdTo = merged, metalsAt = at) }
        }
    }

    /**
     * The live feed, while the app is on screen: gold and silver re-read every [METALS_INTERVAL_MS]
     * because those are the numbers that move during the day. Started on resume and cancelled on
     * pause, so nothing is polled while the phone is in a pocket — and nothing keeps running after
     * the screen goes away.
     */
    fun startMetalsTicker() {
        if (metalsJob?.isActive == true) return
        metalsJob = viewModelScope.launch {
            while (isActive) {
                refreshMetals(force = true)
                delay(METALS_INTERVAL_MS)
            }
        }
    }

    fun stopMetalsTicker() {
        metalsJob?.cancel()
        metalsJob = null
    }

    fun setBaseAndAmount(code: String, amount: Double) {
        prefs.base = code
        prefs.amount = amount
        _convert.update { it.copy(base = code, amount = amount) }
    }

    fun setBase(code: String) {
        prefs.base = code
        _convert.update { it.copy(base = code) }
    }

    fun setAmount(amount: Double) {
        prefs.amount = amount
        _convert.update { it.copy(amount = amount) }
    }

    fun addTarget(code: String) {
        _convert.update { state ->
            if (state.targets.contains(code)) state
            else {
                val next = state.targets + code
                prefs.targets = next
                state.copy(targets = next)
            }
        }
    }

    fun removeTarget(code: String) {
        _convert.update { state ->
            val next = state.targets.filterNot { it == code }
            prefs.targets = next
            state.copy(targets = next)
        }
    }

    fun toggleSound() {
        val next = !_convert.value.soundOn
        prefs.soundOn = next
        _convert.update { it.copy(soundOn = next) }
    }

    /**
     * Moving a row while reordering. The pair is given by code, not by index, because the row the
     * owner drags is filtered (the base is never listed) while the stored list is not — indices in
     * the two lists do not line up.
     */
    fun moveTarget(fromCode: String, toCode: String) {
        val list = moveItem(_convert.value.targets, fromCode, toCode)
        prefs.targets = list
        _convert.update { it.copy(targets = list) }
    }

    fun togglePin(code: String) {
        val next = togglePinCode(_convert.value.pinned, code)
        prefs.pinned = next
        _convert.update { it.copy(pinned = next) }
    }

    fun resetTargets() {
        val next = Prefs.DEFAULT_TARGETS
        prefs.targets = next
        _convert.update { it.copy(targets = next) }
    }

    fun sortTargets(byValue: Boolean) {
        val state = _convert.value
        val next = if (!byValue) {
            state.targets.sorted()
        } else {
            state.targets.sortedByDescending { state.converted(it) ?: 0.0 }
        }
        prefs.targets = next
        _convert.update { it.copy(targets = next) }
    }

    fun setChartPair(base: String, quote: String) {
        prefs.chartBase = base
        prefs.chartQuote = quote
        _chart.update { it.copy(base = base, quote = quote) }
        loadChart()
    }

    fun swapChartPair() {
        val state = _chart.value
        setChartPair(state.quote, state.base)
    }

    fun setChartPeriod(period: String) {
        prefs.chartPeriod = period
        _chart.update { it.copy(period = period) }
        loadChart()
    }

    fun loadChart() {
        val state = _chart.value
        _chart.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val points = withContext(Dispatchers.IO) {
                runCatching { fetchSeries(state.base, state.quote, state.period) }
            }
            val fresh = _chart.value
            // A slower answer for a pair the owner already moved away from must not overwrite it.
            if (fresh.base != state.base || fresh.quote != state.quote || fresh.period != state.period) {
                return@launch
            }
            points.onSuccess { series ->
                _chart.update {
                    it.copy(
                        points = series,
                        loading = false,
                        error = if (series.isEmpty()) "No published series for this pair" else null,
                    )
                }
            }.onFailure { failure ->
                _chart.update { it.copy(points = emptyList(), loading = false, error = failure.message ?: "No connection") }
            }
        }
    }

    private fun fetchSeries(base: String, quote: String, period: String): List<Point> {
        val days = daysFor(period)
        if (CurrencyCatalog.cryptoIds.containsKey(base)) {
            return client.cryptoHistory(base, quote, days)
        }
        val calendar = Calendar.getInstance()
        val end = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val start = calendar.time
        val iso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return client.fiatHistory(base, quote, iso.format(start), iso.format(end))
    }

    private fun daysFor(period: String): Int = when (period) {
        "1D" -> 7      // the ECB publishes one point per working day
        "1W" -> 7
        "1M" -> 30
        "3M" -> 90
        "6M" -> 180
        "1Y" -> 365
        else -> 730
    }

    companion object {
        /** Fifteen seconds: fast enough to watch, slow enough not to lean on a free feed. */
        const val METALS_INTERVAL_MS = 15_000L
    }
}
