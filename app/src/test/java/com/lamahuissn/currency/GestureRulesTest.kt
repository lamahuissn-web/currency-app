package com.lamahuissn.currency

import com.lamahuissn.currency.data.GestureAxis
import com.lamahuissn.currency.data.GestureRules
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The swipe-versus-scroll rule. This is the bug that made the list feel stuck: a short vertical drag
 * on a currency row was claimed by the row's swipe handler, so the LazyColumn never got the gesture.
 */
class GestureRulesTest {

    private val slop = 8f

    @Test
    fun `nothing is claimed before the finger has moved`() {
        assertEquals(GestureAxis.Undecided, GestureRules.axis(0f, 0f, slop))
        assertEquals(GestureAxis.Undecided, GestureRules.axis(4f, 5f, slop))
        assertEquals(GestureAxis.Undecided, GestureRules.axis(-8f, 8f, slop))
    }

    @Test
    fun `a vertical drag belongs to the list`() {
        assertEquals(GestureAxis.Scroll, GestureRules.axis(2f, 30f, slop))
        assertEquals(GestureAxis.Scroll, GestureRules.axis(-3f, -40f, slop))
        // Scrolling at a slight angle must still scroll, not rip the row sideways.
        assertEquals(GestureAxis.Scroll, GestureRules.axis(9f, 60f, slop))
    }

    @Test
    fun `a horizontal drag belongs to the row`() {
        assertEquals(GestureAxis.Swipe, GestureRules.axis(40f, 3f, slop))
        assertEquals(GestureAxis.Swipe, GestureRules.axis(-120f, -9f, slop))
        assertEquals(GestureAxis.Swipe, GestureRules.axis(60f, 9f, slop))
    }

    @Test
    fun `a diagonal with no clear winner scrolls`() {
        assertEquals(GestureAxis.Scroll, GestureRules.axis(30f, 30f, slop))
        assertEquals(GestureAxis.Scroll, GestureRules.axis(-30f, 30f, slop))
    }

    @Test
    fun `the slop applies to the larger axis, not to each one`() {
        assertEquals(GestureAxis.Undecided, GestureRules.axis(7f, 7f, slop))
        assertEquals(GestureAxis.Swipe, GestureRules.axis(9f, 1f, slop))
        assertEquals(GestureAxis.Scroll, GestureRules.axis(1f, 9f, slop))
    }
}
