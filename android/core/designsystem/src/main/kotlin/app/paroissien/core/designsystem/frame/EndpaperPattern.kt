package app.paroissien.core.designsystem.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import app.paroissien.core.designsystem.LocalPaperColors
import kotlin.math.max
import kotlin.math.roundToInt

// Gardes à motif répété (asanoha, symboles chrétiens…), décrites dans tools/cadres/gardes/<nom>.py et exportées par
// tools/cadres/generate.py. La tuile est peinte une fois dans une petite image, puis répétée par un shader.

/** Réseau de répétition d'une tuile. */
enum class Lattice {
    /** Lignes et colonnes. */
    SQUARE,

    /** Une ligne sur deux décalée d'une demi-tuile : losanges, nids d'abeilles. */
    STAGGERED,
}

/**
 * Motif de garde : tuile [width] × [height] en unités, large de [tileWidth] à l'écran, répétée selon [lattice] sur
 * le fond [ground]. Les tracés qui débordent de la tuile sont repris de l'autre côté.
 */
@Immutable
class PatternSpec(
    val name: String,
    val title: String,
    val width: Float,
    val height: Float,
    val tileWidth: Dp,
    val lattice: Lattice,
    val ground: String?,
    val ops: List<Op>,
)

/** Une garde du registre : son nom, son titre, son motif et ses encres. */
@Immutable
class PatternDef(val name: String, val title: String, val spec: PatternSpec, val style: FrameStyle)

/** La garde en pleine page ; [scale] grossit la tuile (aperçus). */
@Composable
fun PatternPaper(pattern: PatternDef, modifier: Modifier = Modifier, scale: Float = 1f) {
    val paper = LocalPaperColors.current
    val inks = remember(pattern, paper) { pattern.style.inks(null, paper) }
    val ops = remember(pattern) { pattern.spec.ops.prepare() }
    Box(
        modifier.fillMaxSize().drawWithCache {
            val spec = pattern.spec
            val rows = if (spec.lattice == Lattice.STAGGERED) 2 else 1
            val k = spec.tileWidth.toPx() * scale / spec.width
            // la tuile tombe sur des pixels entiers : le motif s'étire d'une fraction de pixel au plus
            val w = max(1, (spec.width * k).roundToInt())
            val h = max(1, (spec.height * rows * k).roundToInt())
            val tile = ImageBitmap(w, h)
            CanvasDrawScope().draw(this, layoutDirection, Canvas(tile), Size(w.toFloat(), h.toFloat())) {
                scale(w / spec.width, h / (spec.height * rows), pivot = Offset.Zero) {
                    spec.ground?.let { ink -> inks[ink]?.let { drawRect(it, Offset.Zero, Size(spec.width, spec.height * rows)) } }
                    for (r in -1..rows) {
                        for (c in -1..1) {
                            val shift = if (rows == 2 && r % 2 != 0) spec.width / 2 else 0f
                            translate(c * spec.width + shift, r * spec.height) { drawOps(ops, inks) }
                        }
                    }
                }
            }
            val brush = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
            onDrawBehind { drawRect(brush) }
        },
    )
}
