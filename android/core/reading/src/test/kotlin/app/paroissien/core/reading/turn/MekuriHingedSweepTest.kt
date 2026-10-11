// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MekuriHingedSweepTest {
    private val pageWidth = 400f
    private val layerWidth = 800f
    private val pageHeight = 800f
    private val configuration = MekuriConfiguration()

    @Test
    fun `the sweep stops at the spine`() {
        val sweep = MekuriHingedSweep.of(1f, configuration.creaseBow, layerWidth / 2, layerWidth)
        assertEquals(0.5f, sweep.shaderProgress, TOLERANCE)
        assertEquals(0f, sweep.creaseBow, TOLERANCE)
    }

    @Test
    fun `progress clamps outside zero to one`() {
        assertEquals(0f, MekuriHingedSweep.of(-1f, 0.35f, 400f, 800f).shaderProgress, TOLERANCE)
        assertEquals(0.5f, MekuriHingedSweep.of(4f, 0.35f, 400f, 800f).shaderProgress, TOLERANCE)
    }

    @Test
    fun `the hinged crease sits one page past the page's own crease`() {
        val page = MekuriFoldGeometry(pageWidth, configuration)
        for (progress in listOf(0.1f, 0.25f, 0.5f, 0.75f, 0.9f, 1f)) {
            val sweep = MekuriHingedSweep.of(progress, configuration.creaseBow, layerWidth / 2, layerWidth)
            val layer = MekuriFoldGeometry(
                pageWidth = layerWidth,
                configuration = MekuriConfiguration(creaseBow = sweep.creaseBow),
            )
            for (y in listOf(0f, 200f, 400f, 600f, 800f)) {
                assertEquals(
                    "progress=$progress y=$y",
                    pageWidth + page.foldAxisOffset(progress, y, pageHeight),
                    layer.foldAxisOffset(sweep.shaderProgress, y, pageHeight),
                    TOLERANCE,
                )
            }
        }
    }

    @Test
    fun `the bow rescale keeps the crease inside the layer`() {
        for (step in 0..20) {
            val progress = step / 20f
            val sweep = MekuriHingedSweep.of(progress, configuration.creaseBow, layerWidth / 2, layerWidth)
            assertTrue("progress=$progress", sweep.shaderProgress in 0f..0.5f)
            assertTrue("progress=$progress", sweep.creaseBow in 0f..configuration.creaseBow)
        }
    }

    private companion object {
        const val TOLERANCE = 1e-3f
    }
}
