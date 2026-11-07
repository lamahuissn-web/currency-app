package com.lamahuissn.currency.data

import kotlin.math.abs

/**
 * Which way a finger is going. A row that can be swiped lives inside a list that scrolls, and the two
 * gestures compete for the same pointer: whoever claims it first wins, and the loser stops dead.
 *
 * The rule this encodes: nothing is claimed until the finger has actually moved past the touch slop,
 * and then the larger axis wins. Vertical is the safe answer when the two are equal — a list that
 * scrolls when the owner meant to scroll is never a bug report, a swipe that blocks the list is.
 */
enum class GestureAxis { Undecided, Swipe, Scroll }

object GestureRules {

    fun axis(dxTotal: Float, dyTotal: Float, slop: Float): GestureAxis {
        val horizontal = abs(dxTotal)
        val vertical = abs(dyTotal)
        if (horizontal <= slop && vertical <= slop) return GestureAxis.Undecided
        return if (horizontal > vertical) GestureAxis.Swipe else GestureAxis.Scroll
    }
}
