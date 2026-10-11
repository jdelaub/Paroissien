// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MekuriTurnTest {
    private val config = MekuriConfiguration.Default

    @Test
    fun `zones split on the configured ratio`() {
        assertEquals(MekuriZone.Leading, MekuriZone.resolve(10f, 400f, config))
        assertEquals(MekuriZone.Center, MekuriZone.resolve(200f, 400f, config))
        assertEquals(MekuriZone.Trailing, MekuriZone.resolve(390f, 400f, config))
    }

    @Test
    fun `boundary points fall in the centre`() {
        assertEquals(MekuriZone.Center, MekuriZone.resolve(100f, 400f, config))
        assertEquals(MekuriZone.Center, MekuriZone.resolve(300f, 400f, config))
    }

    @Test
    fun `trailing advances left to right and retreats right to left`() {
        assertEquals(MekuriTurn.Forward, MekuriTurn.from(MekuriZone.Trailing, MekuriDirection.LeftToRight))
        assertEquals(MekuriTurn.Backward, MekuriTurn.from(MekuriZone.Trailing, MekuriDirection.RightToLeft))
        assertEquals(MekuriTurn.Forward, MekuriTurn.from(MekuriZone.Leading, MekuriDirection.RightToLeft))
        assertEquals(MekuriTurn.Backward, MekuriTurn.from(MekuriZone.Leading, MekuriDirection.LeftToRight))
        assertNull(MekuriTurn.from(MekuriZone.Center, MekuriDirection.LeftToRight))
        assertNull(MekuriTurn.from(MekuriZone.Center, MekuriDirection.RightToLeft))
    }

    @Test
    fun `release commits past threshold or velocity`() {
        assertEquals(MekuriTurnDecision.Commit, MekuriTurnDecision.resolve(0.5f, 0f, config))
        assertEquals(MekuriTurnDecision.Revert, MekuriTurnDecision.resolve(0.1f, 0f, config))
        assertEquals(MekuriTurnDecision.Commit, MekuriTurnDecision.resolve(0.1f, 900f, config))
    }

    @Test
    fun `a fling against the turn never commits`() {
        assertEquals(MekuriTurnDecision.Revert, MekuriTurnDecision.resolve(0.1f, -900f, config))
    }

    @Test
    fun `turns stop at the ends`() {
        assertEquals(1, MekuriTurn.Forward.targetIndex(0, 3))
        assertNull(MekuriTurn.Forward.targetIndex(2, 3))
        assertNull(MekuriTurn.Backward.targetIndex(0, 3))
        assertEquals(1, MekuriTurn.Backward.targetIndex(2, 3))
    }
}
