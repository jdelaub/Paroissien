// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.ui.geometry.Size

/**
 * What the container holds: one page across its whole width, or a spread of two
 * [Spread.pageSize] slots side by side, centred, with the spine between them.
 * Every turn and selection change is judged against it.
 */
internal sealed interface MekuriArrangement {
    data class Single(val pageCount: Int) : MekuriArrangement

    data class Spread(val layout: MekuriSpreadLayout, val pageSize: Size) : MekuriArrangement

    /**
     * The finger's full range across a turn, against which a release is judged:
     * the container for a single page, both slots for a spread.
     */
    fun turnWidth(containerWidth: Float): Float = when (this) {
        is Single -> containerWidth
        is Spread -> this.pageSize.width * 2f
    }

    /**
     * The drag mapping for a turn grabbed at [grabY] in the container. Pages are
     * centred vertically; the grab clamps to the page. [stackTravel] is the spread
     * stack's slide along the drag across the whole turn.
     */
    fun edgeTrack(
        turn: MekuriTurn,
        containerSize: Size,
        grabY: Float,
        liftsFromBottom: Boolean,
        configuration: MekuriConfiguration,
        stackTravel: Float = 0f,
    ): MekuriEdgeTrack {
        val page = when (this) {
            is Single -> containerSize
            is Spread -> this.pageSize
        }
        val y = (grabY - (containerSize.height - page.height) / 2f).coerceIn(0f, page.height)
        return MekuriEdgeTrack(
            turn = turn,
            geometry = MekuriFoldGeometry(page.width, configuration),
            pageHeight = page.height,
            row = if (liftsFromBottom) page.height - y else y,
            reach = this.turnWidth(containerSize.width),
            stackTravel = stackTravel,
        )
    }

    fun turnState(id: Int, turn: MekuriTurn, from: Int): MekuriTurnState = when (this) {
        is Single -> MekuriTurnState.begin(id, turn, from, this.pageCount)
        is Spread -> MekuriTurnState.begin(id, turn, from, this.layout)
    }

    fun transition(from: Int, to: Int): MekuriTransition = when (this) {
        is Single -> MekuriTransition.between(from, to)
        is Spread -> MekuriTransition.between(from, to, this.layout)
    }

    /** Live pages at rest. Single mode fills the trailing slot only. */
    fun slots(page: Int): MekuriSpreadPages = when (this) {
        is Single -> MekuriSpreadPages(
            leading = null,
            trailing = if (page in 0 until this.pageCount) page else null,
        )
        is Spread -> this.layout.pages(this.layout.spreadIndex(page))
    }

    /** Always zero in single mode. */
    fun shiftSpan(page: Int, turn: MekuriTurnState?, direction: MekuriDirection): MekuriShiftSpan =
        when (this) {
            is Single -> MekuriShiftSpan.Zero
            is Spread -> if (turn != null) {
                MekuriSpreadShift.span(turn, this.layout, this.pageSize.width, direction)
            } else {
                val rest = MekuriSpreadShift.atRest(page, this.layout, this.pageSize.width, direction)
                MekuriShiftSpan(rest, rest)
            }
        }

    companion object {
        fun resolve(
            containerSize: Size,
            pageCount: Int,
            spread: MekuriSpread,
            coverStandsAlone: Boolean,
            pageAspectRatio: Float,
        ): MekuriArrangement {
            if (!spread.isDouble(containerSize, pageAspectRatio)) return Single(pageCount)
            return Spread(
                layout = MekuriSpreadLayout(pageCount, coverStandsAlone),
                pageSize = MekuriSpread.pageSize(containerSize, pageAspectRatio),
            )
        }
    }
}
