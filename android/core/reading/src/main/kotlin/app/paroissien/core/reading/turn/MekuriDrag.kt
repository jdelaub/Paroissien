// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/**
 * Pure arithmetic mapping a horizontal drag onto a turn. `axis` is the sign of
 * horizontal travel that advances a turn; velocities are projected onto it
 * before they reach [MekuriTurnDecision].
 */
internal object MekuriDrag {
    /**
     * Horizontal travel must exceed vertical travel times this before a drag
     * locks a turn; a tie does not lock.
     */
    const val HorizontalDominance: Float = 1f

    /** Travel fraction kept when the turn is blocked at the first or last page. */
    const val BlockedDamping: Float = 1f / 3f

    /**
     * Pages are centred vertically, so the container's midline is the page's. A
     * grab on the midline lifts the top corner.
     */
    fun liftsFromBottom(y: Float, height: Float): Boolean = y > height / 2f

    /**
     * Re-evaluated on every sample until a turn locks; never latched.
     */
    fun isHorizontallyDominant(translation: Offset): Boolean =
        abs(translation.x) > abs(translation.y) * HorizontalDominance

    fun axis(turn: MekuriTurn, direction: MekuriDirection): Float = when (direction) {
        MekuriDirection.LeftToRight -> if (turn == MekuriTurn.Forward) -1f else 1f
        MekuriDirection.RightToLeft -> if (turn == MekuriTurn.Forward) 1f else -1f
    }

    /** Null when there is no horizontal travel. */
    fun turn(translation: Float, direction: MekuriDirection): MekuriTurn? {
        if (translation == 0f) return null
        val forwardAxis = this.axis(MekuriTurn.Forward, direction)
        return if (translation * forwardAxis > 0f) MekuriTurn.Forward else MekuriTurn.Backward
    }

    /** Null unless the drag is horizontally dominant. */
    fun turn(translation: Offset, direction: MekuriDirection): MekuriTurn? {
        if (!this.isHorizontallyDominant(translation)) return null
        return this.turn(translation.x, direction)
    }

    /**
     * Progress after `translation` from a drag that took over at `start`, keeping
     * the free edge under the finger. A blocked turn keeps [BlockedDamping] of
     * its travel.
     */
    fun progress(
        start: Float,
        translation: Float,
        axis: Float,
        isBlocked: Boolean,
        track: MekuriEdgeTrack,
    ): Float {
        val damping = if (isBlocked) BlockedDamping else 1f
        return track.progress(track.travel(start) + translation * axis * damping)
    }

    /** Velocity measured along the turn, positive toward completion. */
    fun projectedVelocity(velocity: Float, axis: Float): Float = velocity * axis
}
