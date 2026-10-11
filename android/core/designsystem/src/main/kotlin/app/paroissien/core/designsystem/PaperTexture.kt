package app.paroissien.core.designsystem

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.abs
import kotlin.random.Random

/**
 * Fond de page : couleur du thème et grain de papier procédural (rapport 07, §2.3), ±1,5 % de luminance
 * en moyenne. La tuile est calculée une fois ; le contraste du texte reste au-dessus de l'AA.
 */
@Composable
fun Modifier.paperBackground(): Modifier {
    val paper = LocalPaperColors.current
    val brush = remember { ShaderBrush(ImageShader(paperGrain, TileMode.Repeated, TileMode.Repeated)) }
    return drawBehind {
        drawRect(paper.background)
        drawRect(brush)
    }
}

private const val TILE = 256

/** Bruit de valeur à deux octaves (fibres lâches) plus un grain fin, codé en noir ou blanc translucide. */
private val paperGrain: ImageBitmap by lazy {
    val random = Random(1517) // graine fixe : le papier est le même à chaque ouverture
    val coarse = FloatArray(17 * 17) { random.nextFloat() * 2f - 1f }
    fun coarseAt(x: Float, y: Float): Float {
        val gx = x / TILE * 16f
        val gy = y / TILE * 16f
        val x0 = gx.toInt().coerceAtMost(15)
        val y0 = gy.toInt().coerceAtMost(15)
        val fx = gx - x0
        val fy = gy - y0
        fun v(i: Int, j: Int) = coarse[(j % 16) * 17 + (i % 16)] // indices modulo 16 : la tuile se raccorde
        val top = v(x0, y0) * (1 - fx) + v(x0 + 1, y0) * fx
        val bottom = v(x0, y0 + 1) * (1 - fx) + v(x0 + 1, y0 + 1) * fx
        return top * (1 - fy) + bottom * fy
    }
    val pixels = IntArray(TILE * TILE) { index ->
        val x = (index % TILE).toFloat()
        val y = (index / TILE).toFloat()
        val n = 0.55f * coarseAt(x, y) + 0.45f * (random.nextFloat() * 2f - 1f)
        val alpha = (abs(n) * 0.035f * 255f).toInt().coerceIn(0, 255)
        val rgb = if (n > 0f) 0xFFFFFF else 0x000000
        (alpha shl 24) or rgb
    }
    Bitmap.createBitmap(pixels, TILE, TILE, Bitmap.Config.ARGB_8888).asImageBitmap()
}
