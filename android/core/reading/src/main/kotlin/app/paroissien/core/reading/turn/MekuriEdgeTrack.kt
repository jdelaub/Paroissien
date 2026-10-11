// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

/**
 * Maps a drag's travel onto turn progress so the sheet's free edge stays under
 * the finger along the grabbed row. [row] is in the shader's frame, free corner
 * at 0; [reach] is the finger's full range, against which a release is judged;
 * [stackTravel] is how far the spread stack's own slide carries the edge along
 * the drag across the whole turn.
 */
internal data class MekuriEdgeTrack(
    val turn: MekuriTurn,
    val geometry: MekuriFoldGeometry,
    val pageHeight: Float,
    val row: Float,
    val reach: Float,
    val stackTravel: Float = 0f,
) {
    /** Distance the free edge has moved along the drag from where the turn starts. */
    fun travel(progress: Float): Float {
        val clamped = progress.coerceIn(0f, 1f)
        val sheet = when (this.turn) {
            MekuriTurn.Forward -> this.edge(0f) - this.edge(clamped)
            MekuriTurn.Backward -> this.edge(1f - clamped) - this.edge(1f)
        }
        return sheet + this.stackTravel * clamped
    }

    /**
     * Progress at which the free edge has moved [distance]; clamps to 0..1. A
     * stack sliding against the drag can only pull early travel below zero, so
     * travel rises through every positive value exactly once.
     */
    fun progress(distance: Float): Float {
        if (distance <= 0f) return 0f
        if (distance >= this.travel(1f)) return 1f
        var low = 0f
        var high = 1f
        repeat(SearchSteps) {
            val mid = (low + high) / 2
            if (this.travel(mid) < distance) low = mid else high = mid
        }
        return (low + high) / 2
    }

    /** Progress as a release is judged: the share of [reach] the edge has moved. */
    fun releaseProgress(progress: Float): Float =
        if (this.reach > 0f) this.travel(progress) / this.reach else 0f

    private fun edge(progress: Float): Float = this.geometry.freeEdge(progress, this.row, this.pageHeight)

    internal companion object {
        /** Bisection steps when inverting [travel]; fixed so every platform lands on the same value. */
        const val SearchSteps = 32
    }
}
