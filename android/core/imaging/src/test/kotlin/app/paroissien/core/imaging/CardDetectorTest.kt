package app.paroissien.core.imaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class CardDetectorTest {

    private fun argb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    /** Une carte claire tournée de [degrees] sur une table sombre et bruitée, avec une image et du texte dessus. */
    private fun photo(w: Int, h: Int, degrees: Double, cardW: Double, cardH: Double): Pair<RgbImage, List<Point>> {
        val random = Random(7)
        val cx = w / 2.0; val cy = h / 2.0
        val a = Math.toRadians(degrees)
        val pixels = IntArray(w * h) { i ->
            val x = i % w - cx; val y = i / w - cy
            // Coordonnées dans le repère de la carte.
            val u = x * cos(a) + y * sin(a)
            val v = -x * sin(a) + y * cos(a)
            val inCard = kotlin.math.abs(u) <= cardW / 2 && kotlin.math.abs(v) <= cardH / 2
            val n = random.nextInt(-12, 12)
            when {
                !inCard -> argb(90 + n, 70 + n, 55 + n) // bois
                kotlin.math.abs(u) < cardW * 0.35 && v < cardH * 0.15 && v > -cardH * 0.4 -> argb(150 + n, 60 + n, 70 + n) // l'image
                (v.toInt() / 6) % 2 == 0 && v > cardH * 0.2 -> argb(60 + n, 50 + n, 45 + n) // lignes de texte
                else -> argb(235 + n, 225 + n, 200 + n) // papier
            }
        }
        val corners = listOf(-1 to -1, 1 to -1, 1 to 1, -1 to 1).map { (su, sv) ->
            val u = su * cardW / 2; val v = sv * cardH / 2
            Point(((cx + u * cos(a) - v * sin(a)) / (w - 1)).toFloat(), ((cy + u * sin(a) + v * cos(a)) / (h - 1)).toFloat())
        }
        return RgbImage(w, h, pixels) to corners
    }

    private fun assertNear(expected: List<Point>, actual: Quad, tolerance: Float) {
        actual.points.forEach { p ->
            val nearest = expected.minOf { e -> kotlin.math.hypot((e.x - p.x).toDouble(), (e.y - p.y).toDouble()) }
            assertTrue("coin $p trop loin des coins attendus $expected ($nearest)", nearest < tolerance)
        }
    }

    @Test
    fun findsAStraightCard() {
        val (image, corners) = photo(400, 300, 0.0, 160.0, 240.0)
        assertNear(corners, CardDetector.detect(image)!!, 0.03f)
    }

    @Test
    fun findsARotatedCard() {
        val (image, corners) = photo(480, 360, 17.0, 150.0, 230.0)
        val quad = CardDetector.detect(image)
        assertNotNull(quad)
        assertNear(corners, quad!!, 0.035f)
        // Ordre : haut gauche, haut droite, bas droite, bas gauche.
        assertTrue(quad.topLeft.y < quad.bottomLeft.y && quad.topLeft.x < quad.topRight.x)
    }

    @Test
    fun uniformPhotoHasNoCard() {
        assertNull(CardDetector.detect(RgbImage(200, 200, IntArray(200 * 200) { argb(128, 128, 128) })))
    }

    @Test
    fun orderingIsClockwiseFromTopLeft() {
        val q = Quad.ordered(listOf(Point(0.9f, 0.9f), Point(0.1f, 0.1f), Point(0.1f, 0.9f), Point(0.9f, 0.1f)))
        assertEquals(Quad(Point(0.1f, 0.1f), Point(0.9f, 0.1f), Point(0.9f, 0.9f), Point(0.1f, 0.9f)), q)
    }

    @Test
    fun autoLevelsStretchAPaleScan() {
        // Une carte pâle : tout entre 90 et 200.
        val pixels = IntArray(10_000) { val v = 90 + it % 111; argb(v, v, v) }
        val tone = Tone.auto(RgbImage(100, 100, pixels))
        assertTrue(tone.black in 89..92 && tone.white in 198..201)
        val m = tone.colorMatrix()
        assertEquals(0f, m[0] * tone.black + m[4], 0.5f)
        assertEquals(255f, m[0] * tone.white + m[4], 0.5f)
    }

    @Test
    fun outputSizeKeepsTheCardProportions() {
        val (w, h) = outputSize(Quad(Point(0f, 0f), Point(0.5f, 0f), Point(0.5f, 1f), Point(0f, 1f)), 2000, 3000, 1440)
        // 1 000 × 3 000 px dans la photo, ramenés à 1 440 px de haut.
        assertEquals(480, w)
        assertEquals(1440, h)
    }
}
