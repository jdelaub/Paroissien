// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

/** Where a turn is in its life: under the finger, released, or settling. */
internal sealed interface MekuriTurnPhase {
    data object Dragging : MekuriTurnPhase

    data class Armed(val decision: MekuriTurnDecision) : MekuriTurnPhase

    data class Settling(val decision: MekuriTurnDecision) : MekuriTurnPhase
}

/**
 * One turn in flight, as what is on screen: two live base slots and the leaf
 * turning over them. [progress] runs 0 (nothing turned) to 1 (turned) in the
 * turn's own direction; the fold renderer reads [foldProgress]. Single-page mode
 * has no leading slot and no [leafBack]; its leaf is the trailing page across
 * the whole container, with its own front mirrored on the reverse.
 */
internal data class MekuriTurnState(
    val id: Int,
    val turn: MekuriTurn,
    /** Page selected when the turn began. */
    val fromIndex: Int,
    /** Page selected once the turn lands; null when the turn is blocked. */
    val targetIndex: Int?,
    /** Live page in the leading slot; null when absent. */
    val leading: Int?,
    /** Live page in the trailing slot; null when absent. */
    val trailing: Int?,
    /** Page on the face that lies in the trailing slot at fold progress 0. */
    val leafFront: Int?,
    /** Page on the face that lands in the leading slot at fold progress 1. */
    val leafBack: Int?,
    val progress: Float = 0f,
    /** Progress the current drag started from; non-zero after a takeover. */
    val startProgress: Float = 0f,
    val phase: MekuriTurnPhase = MekuriTurnPhase.Dragging,
    /**
     * The bottom corner lifts instead of the top. Decided when the turn begins;
     * a takeover keeps it.
     */
    val liftsFromBottom: Boolean = false,
    /**
     * Container height, in dp, a finger grabbed the turn at; null for a turn no
     * finger started, which tracks the page's midline. A takeover keeps it.
     */
    val grabY: Float? = null,
    /**
     * A finger or an accessibility action started or took over the turn; a turn
     * driven by the selection stays silent.
     */
    val playsHaptics: Boolean = false,
) {
    val isBlocked: Boolean get() = this.targetIndex == null

    /** A turn with no face to draw has nothing to lift. */
    val hasLeaf: Boolean get() = this.leafFront != null || this.leafBack != null

    val isSettling: Boolean get() = this.phase != MekuriTurnPhase.Dragging

    /** Single-page mode: the page drawn live underneath the leaf. */
    val baseIndex: Int? get() = this.trailing

    /** Single-page mode: the page drawn folded above the base. */
    val turningIndex: Int? get() = this.leafFront

    /**
     * Progress handed to the fold renderer. A backward turn starts fully folded
     * and unfolds into place.
     */
    val foldProgress: Float
        get() = this.turn.fold(this.progress)

    internal companion object {
        /**
         * Single-page mode. A forward turn lifts the current page off the next;
         * a backward turn unfolds the previous page over the current.
         */
        internal fun begin(id: Int, turn: MekuriTurn, from: Int, pageCount: Int): MekuriTurnState {
            val target = turn.targetIndex(from, pageCount)
            val trailing = if (turn == MekuriTurn.Forward) target else from
            val leafFront = if (turn == MekuriTurn.Forward) from else target
            return MekuriTurnState(
                id = id,
                turn = turn,
                fromIndex = from,
                targetIndex = target,
                leading = null,
                trailing = trailing,
                leafFront = leafFront,
                leafBack = null,
            )
        }

        /**
         * Spread mode. A backward turn is the previous spread's forward leaf
         * unfolding, so its faces reach the slots swapped. A blocked turn lifts
         * the departing page over nothing.
         */
        internal fun begin(id: Int, turn: MekuriTurn, from: Int, layout: MekuriSpreadLayout): MekuriTurnState {
            val spreadIndex = layout.spreadIndex(from)
            val current = layout.pages(spreadIndex)
            val leaf = layout.leaf(turn, spreadIndex)
            val faces = when {
                turn == MekuriTurn.Forward && leaf != null ->
                    Faces(current.leading, leaf.revealed, leaf.front, leaf.back)
                turn == MekuriTurn.Backward && leaf != null ->
                    Faces(leaf.revealed, current.trailing, leaf.back, leaf.front)
                turn == MekuriTurn.Forward ->
                    Faces(current.leading, null, current.trailing, null)
                else ->
                    Faces(null, current.trailing, null, current.leading)
            }
            return MekuriTurnState(
                id = id,
                turn = turn,
                fromIndex = from,
                targetIndex = layout.selection(turn, spreadIndex),
                leading = faces.leading,
                trailing = faces.trailing,
                leafFront = faces.leafFront,
                leafBack = faces.leafBack,
            )
        }

        private data class Faces(
            val leading: Int?,
            val trailing: Int?,
            val leafFront: Int?,
            val leafBack: Int?,
        )
    }
}
