package com.lamahuissn.currency.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The live metals feed: gold and silver re-read while the app is on screen, because those are
 * the numbers that move during the day. The ticker itself lives here; the prices it fetches are
 * merged into the Convert tab's table by [ConvertViewModel.refreshMetals], so this class owns no
 * price state of its own.
 *
 * The [ConvertViewModel] is passed to [startMetalsTicker] rather than held as a field, so the
 * ticker's only reference to it lives as long as the polling job — and nothing keeps running
 * after [stopMetalsTicker] cancels it.
 */
class MetalsViewModel : ViewModel() {

    private var metalsJob: Job? = null

    /**
     * The live feed, while the app is on screen: gold and silver re-read every
     * [ConvertViewModel.METALS_INTERVAL_MS] because those are the numbers that move during the
     * day. Started on resume and cancelled on pause, so nothing is polled while the phone is in
     * a pocket — and nothing keeps running after the screen goes away.
     */
    fun startMetalsTicker(convert: ConvertViewModel) {
        if (metalsJob?.isActive == true) return
        metalsJob = viewModelScope.launch {
            while (isActive) {
                convert.refreshMetals(force = true)
                delay(ConvertViewModel.METALS_INTERVAL_MS)
            }
        }
    }

    fun stopMetalsTicker() {
        metalsJob?.cancel()
        metalsJob = null
    }
}
