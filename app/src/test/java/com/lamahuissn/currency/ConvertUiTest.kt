package com.lamahuissn.currency

import com.lamahuissn.currency.ui.ConvertUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The Convert tab's state, after the AppViewModel split: the rate table is still the one table
 * every figure is derived from, and the list still draws pinned first, never the base, never a
 * code twice. Pure Kotlin — no Android, no network.
 */
class ConvertUiTest {

    private val rates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.9,
        "LBP" to 89_550.0,
        "GBP" to 0.75,
    )

    private fun state() = ConvertUi(
        base = "USD",
        amount = 100.0,
        targets = listOf("LBP", "EUR", "USD", "GBP", "LBP"),
        pinned = listOf("GBP", "LBP"),
        usdTo = rates,
    )

    @Test
    fun `rate is 1 for the same currency`() {
        assertEquals(1.0, state().rate("EUR", "EUR")!!, 0.0)
    }

    @Test
    fun `rate crosses through the dollar table`() {
        // 1 EUR is (1 / 0.9) dollars; each dollar is 89,550 LBP.
        assertEquals(89_550.0 / 0.9, state().rate("EUR", "LBP")!!, 0.001)
        assertEquals(0.9, state().rate("USD", "EUR")!!, 0.0)
    }

    @Test
    fun `rate is null for an unknown currency`() {
        assertNull(state().rate("USD", "XXX"))
        assertNull(state().rate("XXX", "USD"))
    }

    @Test
    fun `rate is null when the source rate is zero`() {
        val zero = state().copy(usdTo = rates + ("EUR" to 0.0))
        assertNull(zero.rate("EUR", "LBP"))
    }

    @Test
    fun `converted multiplies the rate by the amount`() {
        assertEquals(90.0, state().converted("EUR")!!, 0.0)
    }

    @Test
    fun `rows put pinned first, drop the base, and never repeat a code`() {
        assertEquals(listOf("GBP", "LBP", "EUR"), state().rows)
    }
}
