package app.paroissien.core.imaging

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Réglage d'une image numérisée : niveaux (point noir, point blanc), exposition en IL, contraste. Le tout tient dans
 * une seule matrice de couleur 4 × 5, appliquée d'un coup par Android (`ColorMatrix`) : l'aperçu reste instantané.
 */
data class Tone(val black: Int = 0, val white: Int = 255, val exposure: Float = 0f, val contrast: Float = 1f) {

    /** Matrice ARGB au format de `android.graphics.ColorMatrix` (20 valeurs, décalages sur 0-255). */
    fun colorMatrix(): FloatArray {
        val levels = 255f / (white - black).coerceAtLeast(1)
        val gain = 2f.pow(exposure)
        val scale = levels * gain * contrast
        val offset = (-black * levels * gain - 128f) * contrast + 128f
        return floatArrayOf(
            scale, 0f, 0f, 0f, offset,
            0f, scale, 0f, 0f, offset,
            0f, 0f, scale, 0f, offset,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    companion object {
        /**
         * Niveaux automatiques : on étire la luminance entre ses centiles 0,5 et 99,5, sans jamais resserrer à moins de
         * 96 niveaux (une carte ancienne et pâle ne doit pas devenir criarde). Un papier jauni le reste un peu.
         */
        fun auto(image: RgbImage): Tone {
            val hist = IntArray(256)
            image.pixels.forEach { p ->
                val l = (0.2126 * ((p shr 16) and 0xFF) + 0.7152 * ((p shr 8) and 0xFF) + 0.0722 * (p and 0xFF)).roundToInt()
                hist[l.coerceIn(0, 255)]++
            }
            val n = image.pixels.size
            fun percentile(q: Double): Int {
                var acc = 0
                for (v in 0 until 256) { acc += hist[v]; if (acc >= n * q) return v }
                return 255
            }
            var black = percentile(0.005)
            var white = percentile(0.995)
            if (white - black < 96) {
                val mid = (black + white) / 2
                black = (mid - 48).coerceAtLeast(0)
                white = (black + 96).coerceAtMost(255)
            }
            return Tone(black = black, white = white)
        }
    }
}

/** Taille de sortie d'une carte redressée : les côtés moyens du quadrilatère, ramenés à [maxSide] au plus. */
fun outputSize(quad: Quad, width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
    fun dist(a: Point, b: Point) = kotlin.math.hypot(((a.x - b.x) * width).toDouble(), ((a.y - b.y) * height).toDouble())
    val w = (dist(quad.topLeft, quad.topRight) + dist(quad.bottomLeft, quad.bottomRight)) / 2
    val h = (dist(quad.topLeft, quad.bottomLeft) + dist(quad.topRight, quad.bottomRight)) / 2
    val scale = minOf(1.0, maxSide / maxOf(w, h))
    return (w * scale).roundToInt().coerceAtLeast(1) to (h * scale).roundToInt().coerceAtLeast(1)
}
