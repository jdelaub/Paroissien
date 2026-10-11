// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

/**
 * A page turn in reading order. [Forward] is always index plus one; the
 * direction only decides which screen edge advances the story.
 */
internal enum class MekuriTurn {
    Forward,
    Backward,

    ;

    /**
     * Progress handed to the fold renderer. A backward turn starts fully folded
     * and unfolds into place.
     */
    internal fun fold(progress: Float): Float = when (this) {
        Forward -> progress
        Backward -> 1f - progress
    }

    /** Null when the turn would leave `0 until pageCount`. */
    internal fun targetIndex(from: Int, pageCount: Int): Int? {
        val target = when (this) {
            Forward -> from + 1
            Backward -> from - 1
        }
        return if (target in 0 until pageCount) target else null
    }

    internal companion object {
        /** The centre zone never turns a page. */
        internal fun from(zone: MekuriZone, direction: MekuriDirection): MekuriTurn? = when (zone) {
            MekuriZone.Center -> null
            MekuriZone.Trailing -> when (direction) {
                MekuriDirection.LeftToRight -> Forward
                MekuriDirection.RightToLeft -> Backward
            }
            MekuriZone.Leading -> when (direction) {
                MekuriDirection.LeftToRight -> Backward
                MekuriDirection.RightToLeft -> Forward
            }
        }
    }
}
