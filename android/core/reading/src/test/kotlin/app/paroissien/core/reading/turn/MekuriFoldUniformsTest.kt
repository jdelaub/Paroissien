// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class MekuriFoldUniformsTest {
    private val configuration = MekuriConfiguration()
    private val pageSize = Size(400f, 800f)
    private val leafSize = Size(800f, 800f)

    @Test
    fun `an unhinged pass reaches the shader untouched`() {
        val uniforms = MekuriFoldUniforms.of(pageSize, 0.4f, configuration, MekuriFace.Whole)
        assertEquals(0.4f, uniforms.progress, TOLERANCE)
        assertEquals(100f, uniforms.heldRadius, TOLERANCE)
        assertEquals(0.0625f, uniforms.radiusSlope, TOLERANCE)
        assertEquals(0.10f, uniforms.shear, TOLERANCE)
        assertEquals(0.35f, uniforms.bow, TOLERANCE)
        assertEquals(0.86f, uniforms.backFaceDim, TOLERANCE)
        assertEquals(40f, uniforms.shadowWidth, TOLERANCE)
        assertEquals(0.35f, uniforms.shadowOpacity, TOLERANCE)
        assertEquals(0f, uniforms.face, TOLERANCE)
    }

    @Test
    fun `each face selects its own pass`() {
        for (face in MekuriFace.entries) {
            assertEquals(
                face.raw,
                MekuriFoldUniforms.of(pageSize, 0.4f, configuration, face).face,
                TOLERANCE,
            )
        }
        assertEquals(listOf(0f, 1f, 2f, 3f), MekuriFace.entries.map { it.raw })
    }

    @Test
    fun `a hinged leaf halves its progress and takes its radius from the page`() {
        val uniforms = MekuriFoldUniforms.of(
            size = leafSize,
            progress = 0.4f,
            configuration = configuration,
            face = MekuriFace.Front,
            hinge = MekuriFoldUniforms.hinge(leafSize),
        )
        assertEquals(400f, MekuriFoldUniforms.hinge(leafSize), TOLERANCE)
        assertEquals(0.2f, uniforms.progress, TOLERANCE)
        assertEquals(100f, uniforms.heldRadius, TOLERANCE)
        assertEquals(40f, uniforms.shadowWidth, TOLERANCE)
        assertEquals(0.35f * 0.6f / 0.8f, uniforms.bow, TOLERANCE)
    }

    @Test
    fun `a hinged leaf flattens its roll and shear over the landing`() {
        fun landed(progress: Float) = MekuriFoldUniforms.of(
            leafSize,
            progress,
            configuration,
            MekuriFace.Back,
            MekuriFoldUniforms.hinge(leafSize),
        )
        assertEquals(100f, landed(0.5f).heldRadius, TOLERANCE)
        assertEquals(0.10f, landed(0.5f).shear, TOLERANCE)
        assertEquals(100f * 0.5f, landed(0.85f).heldRadius, TOLERANCE)
        assertEquals(100f * 0.01f, landed(1f).heldRadius, TOLERANCE)
        assertEquals(0.10f * 0.01f, landed(1f).shear, TOLERANCE)
    }

    @Test
    fun `a whole pass flattens its roll over the landing too`() {
        assertEquals(100f, MekuriFoldUniforms.of(pageSize, 0.5f, configuration, MekuriFace.Whole).heldRadius, TOLERANCE)
        assertEquals(1f, MekuriFoldUniforms.of(pageSize, 1f, configuration, MekuriFace.Whole).heldRadius, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1e-3f
    }
}
