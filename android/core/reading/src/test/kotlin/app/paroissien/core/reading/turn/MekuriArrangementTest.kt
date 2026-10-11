// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class MekuriArrangementTest {
    private val aspect = 2f / 3f
    private val coverAlone = MekuriSpreadLayout(pageCount = 6, coverStandsAlone = true)
    private val width = 556f

    private fun resolve(size: Size) = MekuriArrangement.resolve(
        containerSize = size,
        pageCount = 6,
        spread = MekuriSpread.Automatic,
        coverStandsAlone = true,
        pageAspectRatio = this.aspect,
    )

    @Test
    fun `an arrangement is single until two pages fit`() {
        val phone = this.resolve(Size(402f, 874f))
        assertEquals(MekuriArrangement.Single(pageCount = 6), phone)
        assertEquals(402f, phone.turnWidth(402f))
        assertEquals(MekuriSpreadPages(leading = null, trailing = 3), phone.slots(3))

        val pad = this.resolve(Size(1194f, 834f))
        assertEquals(MekuriArrangement.Spread(this.coverAlone, Size(556f, 834f)), pad)
        assertEquals(1112f, pad.turnWidth(1194f))
        assertEquals(MekuriSpreadPages(leading = 3, trailing = 4), pad.slots(4))
        assertEquals(MekuriTransition.None, pad.transition(3, 4))
    }

    @Test
    fun `a page outside the book leaves the single slot empty`() {
        val phone = this.resolve(Size(402f, 874f))
        assertEquals(MekuriSpreadPages(leading = null, trailing = null), phone.slots(9))
    }

    @Test
    fun `single mode is never shifted`() {
        val single = MekuriArrangement.Single(pageCount = 6)
        val turn = MekuriTurnState.begin(id = 1, turn = MekuriTurn.Forward, from = 0, pageCount = 6)
        assertEquals(0f, single.shiftSpan(0, null, MekuriDirection.LeftToRight).value(0f))
        assertEquals(0f, single.shiftSpan(0, turn, MekuriDirection.LeftToRight).value(turn.progress))
        assertEquals(MekuriShiftSpan.Zero, single.shiftSpan(0, turn, MekuriDirection.LeftToRight))
    }

    @Test
    fun `the arrangement shifts the spread stack at rest and in flight`() {
        val arrangement = MekuriArrangement.Spread(this.coverAlone, Size(this.width, 834f))
        assertEquals(-278f, arrangement.shiftSpan(0, null, MekuriDirection.LeftToRight).value(0f))
        assertEquals(0f, arrangement.shiftSpan(3, null, MekuriDirection.LeftToRight).value(0f))
        assertEquals(-278f, arrangement.shiftSpan(5, null, MekuriDirection.RightToLeft).value(0f))
        val turn = MekuriTurnState
            .begin(id = 1, turn = MekuriTurn.Forward, from = 0, layout = this.coverAlone)
            .copy(progress = 0.5f)
        assertEquals(-139f, arrangement.shiftSpan(0, turn, MekuriDirection.LeftToRight).value(turn.progress))
    }

    @Test
    fun `a span runs between the two spreads and clamps`() {
        val arrangement = MekuriArrangement.Spread(this.coverAlone, Size(this.width, 834f))
        val turn = MekuriTurnState.begin(id = 1, turn = MekuriTurn.Forward, from = 0, layout = this.coverAlone)
        val span = MekuriSpreadShift.span(turn, this.coverAlone, this.width, MekuriDirection.LeftToRight)
        assertEquals(MekuriShiftSpan(-278f, 0f), span)
        assertEquals(-278f, span.value(-0.2f))
        assertEquals(-208.5f, span.value(0.25f))
        assertEquals(0f, span.value(1.3f))

        assertEquals(
            MekuriShiftSpan(-278f, -278f),
            arrangement.shiftSpan(0, null, MekuriDirection.LeftToRight),
        )
        assertEquals(span, arrangement.shiftSpan(0, turn, MekuriDirection.LeftToRight))
    }

    @Test
    fun `a forced arrangement ignores the container`() {
        val single = MekuriArrangement.resolve(
            containerSize = Size(1194f, 834f),
            pageCount = 6,
            spread = MekuriSpread.Single,
            coverStandsAlone = true,
            pageAspectRatio = this.aspect,
        )
        assertEquals(MekuriArrangement.Single(pageCount = 6), single)

        val double = MekuriArrangement.resolve(
            containerSize = Size(402f, 874f),
            pageCount = 6,
            spread = MekuriSpread.Double,
            coverStandsAlone = true,
            pageAspectRatio = this.aspect,
        )
        assertEquals(MekuriArrangement.Spread(this.coverAlone, Size(201f, 301.5f)), double)
    }

    @Test
    fun `the turn a spread begins is the spread's, not the page's`() {
        val pad = this.resolve(Size(1194f, 834f))
        val spreadTurn = pad.turnState(id = 1, turn = MekuriTurn.Forward, from = 2)
        assertEquals(2, spreadTurn.leafFront)
        assertEquals(3, spreadTurn.leafBack)
        assertEquals(3, spreadTurn.targetIndex)

        val phone = this.resolve(Size(402f, 874f))
        val singleTurn = phone.turnState(id = 1, turn = MekuriTurn.Forward, from = 2)
        assertEquals(2, singleTurn.leafFront)
        assertEquals(null, singleTurn.leafBack)
        assertEquals(3, singleTurn.targetIndex)
    }

    @Test
    fun `the other page of a shown spread is a change of zero`() {
        val pad = this.resolve(Size(1194f, 834f))
        assertEquals(MekuriTransition.None, pad.transition(3, 4))
        assertEquals(MekuriTransition.Curl(MekuriTurn.Forward), pad.transition(3, 5))

        val phone = this.resolve(Size(402f, 874f))
        assertEquals(MekuriTransition.Curl(MekuriTurn.Forward), phone.transition(3, 4))
        assertEquals(MekuriTransition.Crossfade, phone.transition(3, 5))
    }
}
