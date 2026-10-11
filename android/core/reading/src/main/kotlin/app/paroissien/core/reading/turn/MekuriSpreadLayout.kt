// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

/** The two slots of one spread; either is null when no page falls in it. */
internal data class MekuriSpreadPages(
    val leading: Int?,
    val trailing: Int?,
)

/**
 * Pairs pages into spreads. With [coverStandsAlone], spread 0 holds only page 0
 * in its trailing slot; otherwise pages pair from page 0. Page and spread
 * indices are in reading order, and neither this type nor [MekuriLeaf] knows
 * left from right.
 */
internal data class MekuriSpreadLayout(
    val pageCount: Int,
    val coverStandsAlone: Boolean,
) {
    private val slotOffset: Int get() = if (this.coverStandsAlone) 1 else 0

    val spreadCount: Int get() = (this.pageCount + this.slotOffset + 1) / 2

    fun spreadIndex(page: Int): Int = (page + this.slotOffset) / 2

    fun pages(spreadIndex: Int): MekuriSpreadPages {
        val leading = 2 * spreadIndex - this.slotOffset
        return MekuriSpreadPages(this.page(leading), this.page(leading + 1))
    }

    /**
     * Null when the turn would leave `0 until spreadCount` or the departing slot
     * is absent. The guard is on the target spread, not on the arriving slot.
     */
    fun leaf(turn: MekuriTurn, spreadIndex: Int): MekuriLeaf? {
        val target = this.targetSpread(turn, spreadIndex) ?: return null
        val current = this.pages(spreadIndex)
        val next = this.pages(target)
        return when (turn) {
            MekuriTurn.Forward -> current.trailing?.let { front ->
                MekuriLeaf(front = front, back = next.leading, revealed = next.trailing, landsIn = MekuriSlot.Leading)
            }
            MekuriTurn.Backward -> current.leading?.let { front ->
                MekuriLeaf(front = front, back = next.trailing, revealed = next.leading, landsIn = MekuriSlot.Trailing)
            }
        }
    }

    /**
     * Page to select once the turn lands: the leading page of the new spread, or
     * its trailing page when the leading slot is absent.
     */
    fun selection(turn: MekuriTurn, spreadIndex: Int): Int? {
        if (this.leaf(turn, spreadIndex) == null) return null
        val target = this.targetSpread(turn, spreadIndex) ?: return null
        val pages = this.pages(target)
        return pages.leading ?: pages.trailing
    }

    private fun targetSpread(turn: MekuriTurn, spreadIndex: Int): Int? {
        val target = when (turn) {
            MekuriTurn.Forward -> spreadIndex + 1
            MekuriTurn.Backward -> spreadIndex - 1
        }
        return if (target in 0 until this.spreadCount) target else null
    }

    private fun page(index: Int): Int? = if (index in 0 until this.pageCount) index else null
}
