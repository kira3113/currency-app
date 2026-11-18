package com.lamahuissn.currency.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lamahuissn.currency.data.CurrencyCatalog
import com.lamahuissn.currency.data.Point
import com.lamahuissn.currency.data.Prefs
import com.lamahuissn.currency.data.RatesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

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

/** The Chart tab: one pair, one period, and the series the screen draws. */
class ChartViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = Prefs(application)
    private val client = RatesClient()

    private val _chart = MutableStateFlow(
        ChartUi(base = prefs.chartBase, quote = prefs.chartQuote, period = prefs.chartPeriod)
    )
    val chart: StateFlow<ChartUi> = _chart.asStateFlow()

    init {
        loadChart()
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
}
