// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MekuriTurnStateTest {
    private val coverAlone = MekuriSpreadLayout(pageCount = 6, coverStandsAlone = true)

    @Test
    fun `a forward turn curls the current page over the next`() {
        val state = MekuriTurnState.begin(1, MekuriTurn.Forward, from = 3, pageCount = 6)
            .copy(progress = 0.4f)
        assertEquals(4, state.baseIndex)
        assertEquals(3, state.turningIndex)
        assertEquals(0.4f, state.foldProgress, TOLERANCE)
        assertFalse(state.isBlocked)
    }

    @Test
    fun `a backward turn unfolds the previous page over the current`() {
        val state = MekuriTurnState.begin(1, MekuriTurn.Backward, from = 3, pageCount = 6)
            .copy(progress = 0.4f)
        assertEquals(3, state.baseIndex)
        assertEquals(2, state.turningIndex)
        assertEquals(0.6f, state.foldProgress, TOLERANCE)
    }

    @Test
    fun `blocked single turns have nothing to reveal or to curl`() {
        val last = MekuriTurnState.begin(1, MekuriTurn.Forward, from = 5, pageCount = 6)
        assertTrue(last.isBlocked)
        assertNull(last.baseIndex)
        assertEquals(5, last.turningIndex)
        assertTrue(last.hasLeaf)

        val first = MekuriTurnState.begin(2, MekuriTurn.Backward, from = 0, pageCount = 6)
        assertTrue(first.isBlocked)
        assertEquals(0, first.baseIndex)
        assertNull(first.turningIndex)
        assertFalse(first.hasLeaf)
    }

    @Test
    fun `a forward spread turn lifts the trailing page and lands the next one leading`() {
        val state = MekuriTurnState.begin(1, MekuriTurn.Forward, from = 2, layout = coverAlone)
            .copy(progress = 0.4f)
        assertEquals(1, state.leading)
        assertEquals(4, state.trailing)
        assertEquals(2, state.leafFront)
        assertEquals(3, state.leafBack)
        assertEquals(3, state.targetIndex)
        assertEquals(0.4f, state.foldProgress, TOLERANCE)
        assertTrue(state.hasLeaf)
    }

    @Test
    fun `a backward spread turn unfolds the previous leaf over the current spread`() {
        val state = MekuriTurnState.begin(1, MekuriTurn.Backward, from = 4, layout = coverAlone)
            .copy(progress = 0.4f)
        assertEquals(1, state.leading)
        assertEquals(4, state.trailing)
        assertEquals(2, state.leafFront)
        assertEquals(3, state.leafBack)
        assertEquals(1, state.targetIndex)
        assertEquals(0.6f, state.foldProgress, TOLERANCE)
    }

    @Test
    fun `turning from the cover opens the book and selects the leading page`() {
        val state = MekuriTurnState.begin(1, MekuriTurn.Forward, from = 0, layout = coverAlone)
        assertNull(state.leading)
        assertEquals(2, state.trailing)
        assertEquals(0, state.leafFront)
        assertEquals(1, state.leafBack)
        assertEquals(1, state.targetIndex)

        val back = MekuriTurnState.begin(2, MekuriTurn.Backward, from = 2, layout = coverAlone)
        assertNull(back.leading)
        assertEquals(2, back.trailing)
        assertEquals(0, back.leafFront)
        assertEquals(1, back.leafBack)
        assertEquals(0, back.targetIndex)
    }

    @Test
    fun `blocked spread turns lift the departing page over nothing`() {
        val last = MekuriTurnState.begin(1, MekuriTurn.Forward, from = 5, layout = coverAlone)
        assertTrue(last.isBlocked)
        assertEquals(5, last.leading)
        assertFalse(last.hasLeaf)

        val paired = MekuriSpreadLayout(pageCount = 6, coverStandsAlone = false)
        val lastPaired = MekuriTurnState.begin(2, MekuriTurn.Forward, from = 4, layout = paired)
        assertTrue(lastPaired.isBlocked)
        assertEquals(4, lastPaired.leading)
        assertNull(lastPaired.trailing)
        assertEquals(5, lastPaired.leafFront)
        assertNull(lastPaired.leafBack)

        val first = MekuriTurnState.begin(3, MekuriTurn.Backward, from = 0, layout = coverAlone)
        assertTrue(first.isBlocked)
        assertFalse(first.hasLeaf)
    }

    @Test
    fun `a settling phase is not a dragging one`() {
        val state = MekuriTurnState.begin(1, MekuriTurn.Forward, from = 0, pageCount = 6)
        assertFalse(state.isSettling)
        assertTrue(state.copy(phase = MekuriTurnPhase.Armed(MekuriTurnDecision.Commit)).isSettling)
        assertTrue(state.copy(phase = MekuriTurnPhase.Settling(MekuriTurnDecision.Revert)).isSettling)
    }

    private companion object {
        const val TOLERANCE = 1e-3f
    }
}
