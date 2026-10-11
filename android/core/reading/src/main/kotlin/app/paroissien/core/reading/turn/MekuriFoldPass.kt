// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import kotlin.math.abs

/**
 * What one fold render needs for a direction, progress and lifting corner. The
 * shader only folds a flap entering from the right with its free corner at the
 * top, so a right-to-left turn mirrors the page horizontally around it and a
 * bottom lift mirrors it vertically; [shaderProgress] is never negative.
 */
internal data class MekuriFoldPass(
    val isMirrored: Boolean,
    val shaderProgress: Float,
    val liftsFromBottom: Boolean = false,
) {
    constructor(direction: MekuriDirection, progress: Float, liftsFromBottom: Boolean = false) : this(
        isMirrored = direction == MekuriDirection.RightToLeft,
        shaderProgress = abs(direction.sweep(progress)),
        liftsFromBottom = liftsFromBottom,
    )

    /** Horizontal scale applied on both sides of the fold effect. */
    val mirrorScale: Float
        get() = if (isMirrored) -1f else 1f

    /** Vertical scale applied on both sides of the fold effect. */
    val verticalScale: Float
        get() = if (liftsFromBottom) -1f else 1f

    internal companion object {
        /** The mirror depends on the direction alone, not on progress. */
        fun mirrorScale(direction: MekuriDirection): Float =
            MekuriFoldPass(direction, 0f).mirrorScale

        /** The vertical mirror depends on the lifting corner alone. */
        fun verticalScale(liftsFromBottom: Boolean): Float =
            MekuriFoldPass(MekuriDirection.LeftToRight, 0f, liftsFromBottom).verticalScale
    }
}
