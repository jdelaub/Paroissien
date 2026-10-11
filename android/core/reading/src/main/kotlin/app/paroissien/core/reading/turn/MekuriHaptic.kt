// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import android.os.Build
import android.view.HapticFeedbackConstants

/**
 * A tactile cue in a turn: the page lifting, a drag crossing the progress at
 * which its release completes the turn, and the page landing.
 */
internal enum class MekuriHaptic {
    Lift,
    Detent,
    Land,
    ;

    /** The view feedback constant played for this cue. */
    val feedbackConstant: Int
        get() = when (this) {
            Lift -> HapticFeedbackConstants.GESTURE_START
            Detent -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.SEGMENT_TICK
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            Land -> HapticFeedbackConstants.VIRTUAL_KEY
        }

    internal companion object {
        /**
         * Whether a drag from [from] to [to] crosses [threshold] in either
         * direction, with the inclusive comparison a release resolves by.
         */
        fun crossesThreshold(from: Float, to: Float, threshold: Float): Boolean =
            (from >= threshold) != (to >= threshold)
    }
}
