// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MekuriDragTest {
    private val config = MekuriConfiguration.Default

    @Test
    fun `a drag toward the spine turns forward in both directions`() {
        assertEquals(MekuriTurn.Forward, MekuriDrag.turn(-40f, MekuriDirection.LeftToRight))
        assertEquals(MekuriTurn.Backward, MekuriDrag.turn(40f, MekuriDirection.LeftToRight))
        assertEquals(MekuriTurn.Forward, MekuriDrag.turn(40f, MekuriDirection.RightToLeft))
        assertEquals(MekuriTurn.Backward, MekuriDrag.turn(-40f, MekuriDirection.RightToLeft))
        assertNull(MekuriDrag.turn(0f, MekuriDirection.LeftToRight))
    }

    @Test
    fun `a flick against the turn projects negative and reverts`() {
        val axis = MekuriDrag.axis(MekuriTurn.Forward, MekuriDirection.LeftToRight)
        assertEquals(-1f, axis, TOLERANCE)
        val projected = MekuriDrag.projectedVelocity(900f, axis)
        assertEquals(-900f, projected, TOLERANCE)
        assertEquals(MekuriTurnDecision.Revert, MekuriTurnDecision.resolve(0.1f, projected, config))
        assertEquals(900f, MekuriDrag.projectedVelocity(-900f, axis), TOLERANCE)
    }

    @Test
    fun `a flick against the turn projects negative and reverts right to left`() {
        val axis = MekuriDrag.axis(MekuriTurn.Forward, MekuriDirection.RightToLeft)
        assertEquals(1f, axis, TOLERANCE)
        val projected = MekuriDrag.projectedVelocity(-900f, axis)
        assertEquals(-900f, projected, TOLERANCE)
        assertEquals(MekuriTurnDecision.Revert, MekuriTurnDecision.resolve(0.1f, projected, config))
        assertEquals(900f, MekuriDrag.projectedVelocity(900f, axis), TOLERANCE)
        assertEquals(MekuriTurnDecision.Commit, MekuriTurnDecision.resolve(0.1f, 900f, config))
    }

    @Test
    fun `a drag locks a turn only when horizontally dominant`() {
        assertEquals(MekuriTurn.Forward, MekuriDrag.turn(Offset(-40f, 12f), MekuriDirection.LeftToRight))
        assertEquals(MekuriTurn.Forward, MekuriDrag.turn(Offset(40f, 12f), MekuriDirection.RightToLeft))
        assertNull(MekuriDrag.turn(Offset(-12f, 40f), MekuriDirection.LeftToRight))
        assertNull(MekuriDrag.turn(Offset(12f, -40f), MekuriDirection.RightToLeft))
        assertNull(MekuriDrag.turn(Offset(-30f, 30f), MekuriDirection.LeftToRight))
        assertNull(MekuriDrag.turn(Offset.Zero, MekuriDirection.LeftToRight))
    }

    @Test
    fun `a tie is not dominant and the check reads either sign`() {
        assertEquals(false, MekuriDrag.isHorizontallyDominant(Offset(10f, 10f)))
        assertEquals(true, MekuriDrag.isHorizontallyDominant(Offset(11f, 10f)))
        assertEquals(true, MekuriDrag.isHorizontallyDominant(Offset(-11f, 10f)))
        assertEquals(true, MekuriDrag.isHorizontallyDominant(Offset(-11f, -10f)))
    }

    private val track = MekuriEdgeTrack(
        turn = MekuriTurn.Forward,
        geometry = MekuriFoldGeometry(400f, MekuriConfiguration.Default),
        pageHeight = 800f,
        row = 400f,
        reach = 400f,
    )

    @Test
    fun `a blocked drag keeps a third of its travel`() {
        val free = MekuriDrag.progress(0f, -300f, -1f, isBlocked = false, track = this.track)
        val blocked = MekuriDrag.progress(0f, -300f, -1f, isBlocked = true, track = this.track)
        assertEquals(300f, this.track.travel(free), 0.05f)
        assertEquals(100f, this.track.travel(blocked), 0.05f)
    }

    @Test
    fun `drag progress clamps and resumes from a takeover`() {
        assertEquals(1f, MekuriDrag.progress(0f, -2000f, -1f, isBlocked = false, track = this.track), TOLERANCE)
        assertEquals(0f, MekuriDrag.progress(0.6f, 2000f, -1f, isBlocked = false, track = this.track), TOLERANCE)
        val resumed = MekuriDrag.progress(0.5f, -100f, -1f, isBlocked = false, track = this.track)
        assertEquals(this.track.travel(0.5f) + 100f, this.track.travel(resumed), 0.05f)
    }

    @Test
    fun `a sliding stack still lands the edge under the finger`() {
        val sliding = this.track.copy(reach = 800f, stackTravel = -200f)
        for (distance in listOf(10f, 140f, 500f)) {
            assertEquals(distance, sliding.travel(sliding.progress(distance)), 0.05f)
        }
    }

    @Test
    fun `the free edge stays under the finger`() {
        for (distance in listOf(10f, 60f, 140f, 300f, 600f)) {
            assertEquals(distance, this.track.travel(this.track.progress(distance)), 0.05f)
        }
    }

    private companion object {
        const val TOLERANCE = 1e-3f
    }
}
