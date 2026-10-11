// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import org.junit.Assert.assertEquals
import org.junit.Test

class MekuriDirectionTest {
    @Test
    fun `direction sweeps a signed progress`() {
        assertEquals(0.25f, MekuriDirection.LeftToRight.sweep(0.25f), 0f)
        assertEquals(-0.25f, MekuriDirection.RightToLeft.sweep(0.25f), 0f)
    }
}
