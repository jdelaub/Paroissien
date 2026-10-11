// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import org.junit.Assert.assertEquals
import org.junit.Test

class MekuriTransitionTest {
    private val coverAlone = MekuriSpreadLayout(pageCount = 6, coverStandsAlone = true)

    @Test
    fun `a jump of more than one page crossfades instead of curling`() {
        assertEquals(MekuriTransition.Curl(MekuriTurn.Forward), MekuriTransition.between(3, 4))
        assertEquals(MekuriTransition.Curl(MekuriTurn.Backward), MekuriTransition.between(3, 2))
        assertEquals(MekuriTransition.Crossfade, MekuriTransition.between(3, 9))
        assertEquals(MekuriTransition.None, MekuriTransition.between(3, 3))
    }

    @Test
    fun `a crossfade is judged in spreads while spreads are on`() {
        assertEquals(MekuriTransition.Curl(MekuriTurn.Forward), MekuriTransition.between(2, 3, coverAlone))
        assertEquals(MekuriTransition.None, MekuriTransition.between(3, 4, coverAlone))
        assertEquals(MekuriTransition.Curl(MekuriTurn.Backward), MekuriTransition.between(3, 2, coverAlone))
        assertEquals(MekuriTransition.Crossfade, MekuriTransition.between(2, 5, coverAlone))
    }
}
