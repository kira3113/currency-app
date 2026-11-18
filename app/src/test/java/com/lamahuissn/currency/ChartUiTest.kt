package com.lamahuissn.currency

import com.lamahuissn.currency.data.Point
import com.lamahuissn.currency.ui.ChartUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The Chart tab's state, after the AppViewModel split: first/last still read the series ends. */
class ChartUiTest {

    private val points = listOf(
        Point(at = 1_752_000_000_000L, value = 1.08),
        Point(at = 1_752_086_400_000L, value = 1.10),
        Point(at = 1_752_172_800_000L, value = 1.09),
    )

    @Test
    fun `first and last read the ends of the series`() {
        val state = ChartUi(base = "EUR", quote = "USD", period = "3M", points = points)
        assertEquals(1.08, state.first!!, 0.0)
        assertEquals(1.09, state.last!!, 0.0)
    }

    @Test
    fun `first and last are null for an empty series`() {
        val state = ChartUi()
        assertNull(state.first)
        assertNull(state.last)
    }
}
