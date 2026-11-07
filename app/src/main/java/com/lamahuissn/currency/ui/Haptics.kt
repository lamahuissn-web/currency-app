package com.lamahuissn.currency.ui

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Touch feedback the owner can feel. Counting cash is a physical act, and a phone that answers with a
 * tick under the thumb is easier to trust than one that only changes a number.
 *
 * Two strengths are enough: [tick] for a step that can be undone by another step, [bump] for a
 * deliberate gesture (a long press, a drag, a save) that should feel like a decision.
 */
fun HapticFeedback.tick() {
    performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

fun HapticFeedback.bump() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}
