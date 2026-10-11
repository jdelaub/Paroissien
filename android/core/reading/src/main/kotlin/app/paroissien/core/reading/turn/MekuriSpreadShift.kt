// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

/**
 * Shift of the spread stack at rest and at the end of a turn; the two are equal
 * while nothing turns.
 */
internal data class MekuriShiftSpan(
    val start: Float,
    val end: Float,
) {
    /** Progress clamps to 0..1; overshoot never slides the book. */
    fun value(progress: Float): Float =
        this.start + (this.end - this.start) * progress.coerceIn(0f, 1f)

    internal companion object {
        internal val Zero: MekuriShiftSpan = MekuriShiftSpan(0f, 0f)
    }
}

/**
 * Horizontal shift of the whole spread stack. A spread holding one page is
 * shifted to centre that page in the container; a full spread is not shifted.
 * During a turn the shift runs from the departing spread's to the landing
 * spread's with the turn's progress.
 */
internal object MekuriSpreadShift {
    fun atRest(
        leading: Int?,
        trailing: Int?,
        pageWidth: Float,
        direction: MekuriDirection,
    ): Float {
        val reading = if (direction == MekuriDirection.LeftToRight) 1f else -1f
        return when {
            leading == null && trailing != null -> -reading * pageWidth / 2f
            leading != null && trailing == null -> reading * pageWidth / 2f
            else -> 0f
        }
    }

    fun atRest(
        page: Int,
        layout: MekuriSpreadLayout,
        pageWidth: Float,
        direction: MekuriDirection,
    ): Float {
        val slots = layout.pages(layout.spreadIndex(page))
        return this.atRest(slots.leading, slots.trailing, pageWidth, direction)
    }

    /** A blocked turn lands where it began. */
    fun span(
        turn: MekuriTurnState,
        layout: MekuriSpreadLayout,
        pageWidth: Float,
        direction: MekuriDirection,
    ): MekuriShiftSpan = MekuriShiftSpan(
        start = this.atRest(turn.fromIndex, layout, pageWidth, direction),
        end = this.atRest(turn.targetIndex ?: turn.fromIndex, layout, pageWidth, direction),
    )
}
