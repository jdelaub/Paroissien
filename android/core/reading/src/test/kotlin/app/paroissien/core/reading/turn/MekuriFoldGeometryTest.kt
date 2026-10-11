// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MekuriFoldGeometryTest {
    private val pageSize = Size(400f, 800f)
    private val geometry = MekuriFoldGeometry(
        pageWidth = pageSize.width,
        configuration = MekuriConfiguration.Default,
    )

    @Test
    fun `axis starts at the trailing edge and ends at the leading edge`() {
        assertEquals(400f, geometry.foldAxisOffset(0f), TOLERANCE)
        assertEquals(0f, geometry.foldAxisOffset(1f), TOLERANCE)
    }

    @Test
    fun `axis is monotonic`() {
        val samples = (0..10).map { geometry.foldAxisOffset(it / 10f) }
        assertTrue(samples.zipWithNext().all { (a, b) -> a >= b })
    }

    @Test
    fun `bowed axis is monotonic in progress at every row`() {
        for (row in 0..8) {
            val y = row * pageSize.height / 8
            val samples = (0..20).map {
                geometry.foldAxisOffset(it / 20f, y = y, pageHeight = pageSize.height)
            }
            assertTrue(
                "row y=$y",
                samples.zipWithNext().all { (a, b) -> a > b },
            )
        }
    }

    @Test
    fun `bow offsets the crease by bow lead times held squared`() {
        // 400 x (1 - 0.25) + 0.10 x (y - 400) - 0.35 x 400 x 0.25 x 0.75 x (1 - y / 800)^2
        assertEquals(265.234375f, geometry.foldAxisOffset(0.25f, y = 200f, pageHeight = 800f), TOLERANCE)
        assertEquals(318.359375f, geometry.foldAxisOffset(0.25f, y = 600f, pageHeight = 800f), TOLERANCE)
    }

    @Test
    fun `a straight crease differs from a bowed one by the bow term alone`() {
        val straight = MekuriFoldGeometry(pageSize.width, MekuriConfiguration(creaseBow = 0f))
        assertEquals(280f, straight.foldAxisOffset(0.25f, y = 200f, pageHeight = 800f), TOLERANCE)
        assertEquals(320f, straight.foldAxisOffset(0.25f, y = 600f, pageHeight = 800f), TOLERANCE)
    }

    @Test
    fun `progress clamps outside zero to one`() {
        assertEquals(geometry.foldAxisOffset(0f), geometry.foldAxisOffset(-2f), TOLERANCE)
        assertEquals(geometry.foldAxisOffset(1f), geometry.foldAxisOffset(3f), TOLERANCE)
        assertEquals(0f, geometry.bowLead(-2f), TOLERANCE)
        assertEquals(0f, geometry.bowLead(3f), TOLERANCE)
    }

    @Test
    fun `shear and bow place the crease across the rows`() {
        assertEquals(125f, geometry.foldAxisOffset(0.5f, y = 0f, pageHeight = 800f), TOLERANCE)
        assertEquals(191.25f, geometry.foldAxisOffset(0.5f, y = 400f, pageHeight = 800f), TOLERANCE)
        assertEquals(240f, geometry.foldAxisOffset(0.5f, y = 800f, pageHeight = 800f), TOLERANCE)
    }

    @Test
    fun `bow lead peaks at half progress and vanishes at both ends`() {
        assertEquals(0f, geometry.bowLead(0f), TOLERANCE)
        assertEquals(35f, geometry.bowLead(0.5f), TOLERANCE)
        assertEquals(0f, geometry.bowLead(1f), TOLERANCE)
    }

    @Test
    fun `radius opens along the fold from the held end`() {
        assertEquals(100f, geometry.heldRadius, TOLERANCE)
        assertEquals(0.0625f, geometry.radiusSlope, TOLERANCE)
        assertEquals(100f, geometry.radius(foldDistance = 0f), TOLERANCE)
        assertEquals(150f, geometry.radius(foldDistance = 800f), TOLERANCE)
    }

    @Test
    fun `landing scale holds at one until the last thirty percent and floors at the end`() {
        assertEquals(1f, geometry.landingRadiusScale(0f), TOLERANCE)
        assertEquals(1f, geometry.landingRadiusScale(0.5f), TOLERANCE)
        assertEquals(0.5f, geometry.landingRadiusScale(0.85f), TOLERANCE)
        assertEquals(MekuriFoldGeometry.LandingFloor, geometry.landingRadiusScale(1f), TOLERANCE)
    }

    @Test
    fun `crease shadow is a full height band centred on the straight axis`() {
        val band = geometry.creaseShadowRect(0.5f, pageSize)
        assertEquals(180f, band.left, TOLERANCE)
        assertEquals(0f, band.top, TOLERANCE)
        assertEquals(220f, band.right, TOLERANCE)
        assertEquals(800f, band.bottom, TOLERANCE)
    }

    @Test
    fun `only non positive progress is flat`() {
        assertTrue(geometry.isFlat(0f))
        assertTrue(geometry.isFlat(-0.5f))
        assertFalse(geometry.isFlat(0.0001f))
    }

    private companion object {
        const val TOLERANCE = 1e-4f
    }
}
