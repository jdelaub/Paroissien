// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MekuriFoldPassTest {

    @Test
    fun `a left to right pass folds unmirrored`() {
        val pass = MekuriFoldPass(MekuriDirection.LeftToRight, 0.4f)
        assertFalse(pass.isMirrored)
        assertEquals(1f, pass.mirrorScale, TOLERANCE)
        assertEquals(0.4f, pass.shaderProgress, TOLERANCE)
    }

    @Test
    fun `a right to left pass mirrors rather than negating progress`() {
        val pass = MekuriFoldPass(MekuriDirection.RightToLeft, 0.4f)
        assertTrue(pass.isMirrored)
        assertEquals(-1f, pass.mirrorScale, TOLERANCE)
        assertEquals(0.4f, pass.shaderProgress, TOLERANCE)
    }

    @Test
    fun `the mirror depends on the direction alone`() {
        assertEquals(1f, MekuriFoldPass.mirrorScale(MekuriDirection.LeftToRight), TOLERANCE)
        assertEquals(-1f, MekuriFoldPass.mirrorScale(MekuriDirection.RightToLeft), TOLERANCE)
    }

    @Test
    fun `a bottom lift flips the layer vertically in either direction`() {
        for (direction in MekuriDirection.entries) {
            val pass = MekuriFoldPass(direction, 0.4f, liftsFromBottom = true)
            assertEquals(-1f, pass.verticalScale, TOLERANCE)
            assertEquals(MekuriFoldPass.mirrorScale(direction), pass.mirrorScale, TOLERANCE)
            assertEquals(0.4f, pass.shaderProgress, TOLERANCE)
        }
        assertEquals(1f, MekuriFoldPass(MekuriDirection.LeftToRight, 0.4f).verticalScale, TOLERANCE)
        assertEquals(1f, MekuriFoldPass.verticalScale(liftsFromBottom = false), TOLERANCE)
        assertEquals(-1f, MekuriFoldPass.verticalScale(liftsFromBottom = true), TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1e-3f
    }
}
