// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MekuriSpreadShiftTest {
    private val coverAlone = MekuriSpreadLayout(pageCount = 6, coverStandsAlone = true)
    private val width = 556f
    private val ltr = MekuriDirection.LeftToRight
    private val rtl = MekuriDirection.RightToLeft

    @Test
    fun `a lone cover sits centred at rest in both directions`() {
        assertEquals(-278f, MekuriSpreadShift.atRest(null, 0, width, ltr), TOLERANCE)
        assertEquals(278f, MekuriSpreadShift.atRest(null, 0, width, rtl), TOLERANCE)
        assertEquals(-278f, MekuriSpreadShift.atRest(0, coverAlone, width, ltr), TOLERANCE)
    }

    @Test
    fun `a lone final page sits centred at rest`() {
        assertEquals(278f, MekuriSpreadShift.atRest(5, null, width, ltr), TOLERANCE)
        assertEquals(-278f, MekuriSpreadShift.atRest(5, null, width, rtl), TOLERANCE)
        assertEquals(278f, MekuriSpreadShift.atRest(5, coverAlone, width, ltr), TOLERANCE)

        val paired = MekuriSpreadLayout(pageCount = 5, coverStandsAlone = false)
        assertEquals(278f, MekuriSpreadShift.atRest(4, paired, width, ltr), TOLERANCE)
        assertEquals(0f, MekuriSpreadShift.atRest(3, paired, width, ltr), TOLERANCE)
    }

    @Test
    fun `a full spread and an empty one are never shifted`() {
        assertEquals(0f, MekuriSpreadShift.atRest(1, 2, width, ltr), TOLERANCE)
        assertEquals(0f, MekuriSpreadShift.atRest(null, null, width, ltr), TOLERANCE)
    }

    @Test
    fun `opening the cover slides the stack to its slots with the turn`() {
        val turn = MekuriTurnState.begin(1, MekuriTurn.Forward, from = 0, layout = coverAlone)
        val span = MekuriSpreadShift.span(turn, coverAlone, width, ltr)
        assertEquals(-278f, span.value(turn.progress), TOLERANCE)
        assertEquals(-139f, span.value(0.5f), TOLERANCE)
        assertEquals(0f, span.value(1f), TOLERANCE)
        assertEquals(0f, span.value(1.02f), TOLERANCE)
    }

    @Test
    fun `closing the cover slides the stack back to centre`() {
        val turn = MekuriTurnState.begin(1, MekuriTurn.Backward, from = 2, layout = coverAlone)
        val span = MekuriSpreadShift.span(turn, coverAlone, width, ltr)
        assertEquals(0f, span.value(turn.progress), TOLERANCE)
        assertEquals(-139f, span.value(0.5f), TOLERANCE)
        assertEquals(-278f, span.value(1f), TOLERANCE)
    }

    @Test
    fun `the lone final page centres as it lands and leaves centre`() {
        val forward = MekuriTurnState.begin(1, MekuriTurn.Forward, from = 4, layout = coverAlone)
        val forwardSpan = MekuriSpreadShift.span(forward, coverAlone, width, ltr)
        assertEquals(69.5f, forwardSpan.value(0.25f), TOLERANCE)
        assertEquals(278f, forwardSpan.value(1f), TOLERANCE)

        val backward = MekuriTurnState.begin(2, MekuriTurn.Backward, from = 5, layout = coverAlone)
        val backwardSpan = MekuriSpreadShift.span(backward, coverAlone, width, ltr)
        assertEquals(278f, backwardSpan.value(backward.progress), TOLERANCE)
        assertEquals(0f, backwardSpan.value(1f), TOLERANCE)
    }

    @Test
    fun `a blocked turn on a lone page lifts it in place`() {
        val layout = MekuriSpreadLayout(pageCount = 1, coverStandsAlone = true)
        val turn = MekuriTurnState.begin(1, MekuriTurn.Forward, from = 0, layout = layout)
        assertTrue(turn.isBlocked)
        assertTrue(turn.hasLeaf)
        val span = MekuriSpreadShift.span(turn, layout, width, ltr)
        assertEquals(-278f, span.value(0.3f), TOLERANCE)
    }

    @Test
    fun `a span runs between the two spreads and clamps`() {
        val turn = MekuriTurnState.begin(1, MekuriTurn.Forward, from = 0, layout = coverAlone)
        val span = MekuriSpreadShift.span(turn, coverAlone, width, ltr)
        assertEquals(MekuriShiftSpan(-278f, 0f), span)
        assertEquals(-278f, span.value(-0.2f), TOLERANCE)
        assertEquals(-208.5f, span.value(0.25f), TOLERANCE)
        assertEquals(0f, span.value(1.3f), TOLERANCE)
        assertEquals(0f, MekuriShiftSpan.Zero.value(0.5f), TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1e-3f
    }
}
