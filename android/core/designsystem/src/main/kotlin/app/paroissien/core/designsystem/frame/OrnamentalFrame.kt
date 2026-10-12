package app.paroissien.core.designsystem.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.PaperColors
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.paperBackground
import app.paroissien.core.designsystem.paperColors
import app.paroissien.core.liturgy.LiturgicalColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Cadres ornementaux d'après les paroissiens chromolithographiés du XIXᵉ siècle. Un cadre est fait de modules
// vectoriels répétés (frises, coins, terminaisons), décrits une fois dans tools/cadres/generate.py.

/** Encres d'un cadre : chaque tracé en référence une, la couleur est résolue à l'exécution. */
enum class FrameInk { GROUND, ACCENT, LEAF, GOLD, STRAND, BAR, BAR_LIGHT, OUTLINE }

/** Un tracé de module : chemin SVG rempli, ou tracé au trait si [stroke] > 0 (unités du module). */
@Immutable
class Op(val ink: FrameInk, val pathData: String, val stroke: Float = 0f)

/**
 * Frise : élément répété le long d'une bande. Repère local : u le long de la bande, v de 0 (bord extérieur)
 * à 100 (côté texte). Toutes les instances dessinent [base], puis toutes dessinent [over] (dessus d'entrelacs).
 */
@Immutable
class EdgeModule(val period: Float, val phase: Float, val base: List<Op>, val over: List<Op>)

/** Motif dans une boîte 100 × 100 (coin), ou pointé vers +x depuis l'origine (terminaison). */
@Immutable
class BoxModule(val ops: List<Op>)

@Immutable
class FrameSpec(
    val edge: EdgeModule,
    /** Bande du pied, plus haute (convention des manuscrits). */
    val bottomEdge: EdgeModule,
    val corner: BoxModule,
    /** Comble les coins allongés du pied, pointe vers l'extérieur. */
    val cornerFill: BoxModule,
    /** Bout des baguettes croisées. */
    val terminal: BoxModule,
    val margin: Dp,
    val top: Dp,
    val side: Dp,
    val bottom: Dp,
    /** Baguettes, en unités de module (100 = épaisseur de la bande la plus fine). */
    val bar: Float,
    val barOutline: Float,
    val barLight: Float,
    val barOverhang: Float,
    val textPadding: Dp,
) {
    /** Marges que le contenu doit laisser libres : cadre, baguette intérieure et respiration. */
    fun contentPadding(header: Dp = 0.dp): PaddingValues {
        val barHalf = ((bar / 2 + barOutline) * minOf(top, side).value / 100f).dp
        val horizontal = margin + side + barHalf + textPadding
        return PaddingValues(
            start = horizontal,
            end = horizontal,
            top = margin + header + top + barHalf + textPadding,
            bottom = margin + bottom + barHalf + textPadding,
        )
    }
}

/** Couleurs d'un cadre : celles du livre, remplacées en partie selon la couleur liturgique. */
@Immutable
class FrameStyle(
    val base: Map<FrameInk, Color>,
    val palettes: Map<LiturgicalColor, Map<FrameInk, Color>>,
    /** Encres rapprochées du fond de page dans les thèmes sombres ([ornamentDim]). */
    val dimmed: Set<FrameInk>,
) {
    fun inks(liturgicalColor: LiturgicalColor?, paper: PaperColors): Map<FrameInk, Color> {
        val inks = base.toMutableMap()
        liturgicalColor?.let { palettes[it]?.let(inks::putAll) }
        val dim = ornamentDim(paper)
        if (dim > 0f) {
            for (ink in dimmed) inks[ink] = mix(inks.getValue(ink), paper.background, dim)
        }
        return inks
    }

    /** Part dont le cadre se rapproche du fond pour ne pas éblouir : rien en clair et en sépia, 0,28 en sombre, 0,6 en nuit. */
    fun ornamentDim(paper: PaperColors): Float = when {
        !paper.isDark -> 0f
        paper == nightPaper -> 0.6f
        else -> 0.28f
    }

    private fun mix(a: Color, b: Color, t: Float) = Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
    )

    private companion object {
        val nightPaper = paperColors(PaperTheme.NUIT)
    }
}

/**
 * Page encadrée sur le papier du thème. Le cadre est dessiné dans sa propre couche : le texte qui défile ne le
 * fait pas redessiner, seuls la taille, le thème ou la couleur liturgique le font. [content] reçoit les marges
 * à respecter pour rester à l'intérieur ; [header] occupe la bande au-dessus du cadre (titre courant, folio).
 */
@Composable
fun OrnamentalFrame(
    spec: FrameSpec,
    style: FrameStyle,
    modifier: Modifier = Modifier,
    liturgicalColor: LiturgicalColor? = null,
    headerHeight: Dp = 0.dp,
    header: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val paper = LocalPaperColors.current
    val prepared = remember(spec) { PreparedFrame(spec) }
    val inks = remember(style, liturgicalColor, paper) { style.inks(liturgicalColor, paper) }
    Box(modifier.fillMaxSize().paperBackground()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer()
                .drawWithCache {
                    val geometry = FrameGeometry(spec, size.width, size.height, headerHeight.toPx(), this)
                    onDrawBehind { drawFrame(prepared, spec, inks, geometry) }
                },
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = spec.margin, start = spec.margin, end = spec.margin)
                .height(headerHeight),
        ) { header() }
        content(spec.contentPadding(headerHeight))
    }
}

/** Fondu en haut et en bas d'une zone qui défile, pour que le texte ne soit pas coupé net par le cadre. */
fun Modifier.fadingEdges(top: Dp = 10.dp, bottom: Dp = 22.dp): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val t = (top.toPx() / size.height).coerceIn(0f, 0.5f)
        val b = (bottom.toPx() / size.height).coerceIn(0f, 0.5f)
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, t to Color.Black, 1f - b to Color.Black, 1f to Color.Transparent),
            blendMode = BlendMode.DstIn,
        )
    }

// ---------------------------------------------------------------------------------------------------------------
// Préparation : chemins analysés une fois par cadre, géométrie une fois par taille
// ---------------------------------------------------------------------------------------------------------------

private class PreparedOp(val ink: FrameInk, val path: Path, val stroke: Stroke?)

private fun List<Op>.prepare(): List<PreparedOp> = map {
    PreparedOp(it.ink, PathParser().parsePathString(it.pathData).toPath(), if (it.stroke > 0f) Stroke(width = it.stroke) else null)
}

private class PreparedEdge(m: EdgeModule) {
    val period = m.period
    val phase = m.phase
    val base = m.base.prepare()
    val over = m.over.prepare()
}

private class PreparedFrame(spec: FrameSpec) {
    val edge = PreparedEdge(spec.edge)
    val bottomEdge = PreparedEdge(spec.bottomEdge)
    val corner = spec.corner.ops.prepare()
    val cornerFill = spec.cornerFill.ops.prepare()
    val terminal = spec.terminal.ops.prepare()
}

private class FrameGeometry(spec: FrameSpec, width: Float, height: Float, headerPx: Float, density: Density) {
    val ox: Float
    val oy: Float
    val ow: Float
    val oh: Float
    val t: Float
    val s: Float
    val b: Float
    val u: Float
    val ext: Float
    val ring: Path
    val bars: Path

    init {
        with(density) {
            val margin = spec.margin.toPx()
            ox = margin
            oy = margin + headerPx
            ow = width - 2 * margin
            oh = height - oy - margin
            t = spec.top.toPx()
            s = spec.side.toPx()
            b = spec.bottom.toPx()
        }
        u = min(t, s) / 100f
        ext = spec.barOverhang * u
        ring = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(ox, oy, ox + ow, oy + oh))
            addRect(Rect(ox + s, oy + t, ox + ow - s, oy + oh - b))
        }
        // rectangle extérieur, puis baguettes intérieures croisées qui débordent jusqu'aux terminaisons
        bars = Path().apply {
            addRect(Rect(ox, oy, ox + ow, oy + oh))
            for (y in floatArrayOf(oy + t, oy + oh - b)) {
                moveTo(ox - ext, y)
                lineTo(ox + ow + ext, y)
            }
            for (x in floatArrayOf(ox + s, ox + ow - s)) {
                moveTo(x, oy - ext)
                lineTo(x, oy + oh + ext)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Dessin
// ---------------------------------------------------------------------------------------------------------------

private fun DrawScope.drawOps(ops: List<PreparedOp>, inks: Map<FrameInk, Color>) {
    for (op in ops) {
        val color = inks.getValue(op.ink)
        if (op.stroke != null) drawPath(op.path, color, style = op.stroke) else drawPath(op.path, color)
    }
}

/** Matrice affine (a, b, c, d, e, f) : x' = a·x + c·y + e, y' = b·x + d·y + f. */
private fun Matrix.setAffine(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float): Matrix {
    reset()
    values[Matrix.ScaleX] = a
    values[Matrix.SkewY] = b
    values[Matrix.SkewX] = c
    values[Matrix.ScaleY] = d
    values[Matrix.TranslateX] = e
    values[Matrix.TranslateY] = f
    return this
}

private inline fun DrawScope.withAffine(
    m: Matrix, a: Float, b: Float, c: Float, d: Float, e: Float, f: Float,
    block: DrawScope.() -> Unit,
) = withTransform({ transform(m.setAffine(a, b, c, d, e, f)) }, block)

/**
 * Une bande : n éléments, n = arrondi(longueur / pas nominal), puis le pas est ajusté pour que la bande tombe juste
 * entre les coins (comme border-image-repeat: round). Un NinePatch étirerait le motif au lieu de le répéter.
 * (ax, ay) = direction de la bande, (dx, dy) = direction extérieur → texte.
 */
private fun DrawScope.drawBand(
    m: PreparedEdge, inks: Map<FrameInk, Color>, mtx: Matrix,
    x0: Float, y0: Float, ax: Float, ay: Float, dx: Float, dy: Float,
    len: Float, depth: Float, clip: Rect,
) {
    val k = depth / 100f
    val n = max(1, (len / (m.period * k)).roundToInt())
    val step = len / n
    val ka = step / m.period
    clipRect(clip.left, clip.top, clip.right, clip.bottom) {
        for (pass in listOf(m.base, m.over)) {
            for (i in -1..n + 1) {
                val u0 = (i + m.phase) * step
                withAffine(mtx, ax * ka, ay * ka, dx * k, dy * k, x0 + ax * u0, y0 + ay * u0) { drawOps(pass, inks) }
            }
        }
    }
}

private fun DrawScope.drawCorner(
    f: PreparedFrame, inks: Map<FrameInk, Color>, mtx: Matrix,
    x: Float, y: Float, w: Float, h: Float, outerAtBottom: Boolean,
) {
    val q = min(w, h)
    clipRect(x, y, x + w, y + h) {
        // rosace dans le carré côté texte
        val rosY = if (outerAtBottom) y else y + h - q
        withAffine(mtx, q / 100f, 0f, 0f, q / 100f, x + (w - q) / 2, rosY) { drawOps(f.corner, inks) }
        // coin allongé (pied) : une fleur de lys comble le reste, pointe vers l'extérieur
        val rest = h - q
        if (rest > 0.25f * w) {
            val sc = min(w, rest) / 100f
            val fx = x + w / 2 - 50f * sc
            if (outerAtBottom) {
                withAffine(mtx, sc, 0f, 0f, -sc, fx, y + h) { drawOps(f.cornerFill, inks) }
            } else {
                withAffine(mtx, sc, 0f, 0f, sc, fx, y) { drawOps(f.cornerFill, inks) }
            }
        }
    }
}

private fun DrawScope.drawFrame(f: PreparedFrame, spec: FrameSpec, inks: Map<FrameInk, Color>, g: FrameGeometry) {
    val mtx = Matrix()
    val ox = g.ox
    val oy = g.oy
    val ow = g.ow
    val oh = g.oh
    val t = g.t
    val s = g.s
    val b = g.b
    val u = g.u

    // 1. fond des bandes
    drawPath(g.ring, inks.getValue(FrameInk.GROUND))

    // 2. frises : haut, gauche, droite, pied
    val hLen = ow - 2 * s
    val vLen = oh - t - b
    drawBand(f.edge, inks, mtx, ox + s, oy, 1f, 0f, 0f, 1f, hLen, t, Rect(ox + s, oy, ox + ow - s, oy + t))
    drawBand(f.edge, inks, mtx, ox, oy + t, 0f, 1f, 1f, 0f, vLen, s, Rect(ox, oy + t, ox + s, oy + oh - b))
    drawBand(f.edge, inks, mtx, ox + ow, oy + t, 0f, 1f, -1f, 0f, vLen, s, Rect(ox + ow - s, oy + t, ox + ow, oy + oh - b))
    drawBand(f.bottomEdge, inks, mtx, ox + s, oy + oh, 1f, 0f, 0f, -1f, hLen, b, Rect(ox + s, oy + oh - b, ox + ow - s, oy + oh))

    // 3. coins
    drawCorner(f, inks, mtx, ox, oy, s, t, outerAtBottom = false)
    drawCorner(f, inks, mtx, ox + ow - s, oy, s, t, outerAtBottom = false)
    drawCorner(f, inks, mtx, ox, oy + oh - b, s, b, outerAtBottom = true)
    drawCorner(f, inks, mtx, ox + ow - s, oy + oh - b, s, b, outerAtBottom = true)

    // 4. baguettes : contour, rose, puis filet clair ; les croisements se fondent
    for ((ink, width) in listOf(
        FrameInk.OUTLINE to spec.bar + 2 * spec.barOutline,
        FrameInk.BAR to spec.bar,
        FrameInk.BAR_LIGHT to spec.barLight,
    )) {
        drawPath(g.bars, inks.getValue(ink), style = Stroke(width = width * u, cap = StrokeCap.Butt, join = StrokeJoin.Miter))
    }

    // 5. terminaisons : bout des baguettes intérieures, puis diagonales aux angles
    fun terminal(x: Float, y: Float, degrees: Float, push: Float = 0f) {
        val a = degrees * PI.toFloat() / 180f
        val c = cos(a)
        val sn = sin(a)
        withAffine(mtx, c * u, sn * u, -sn * u, c * u, x + c * push * u, y + sn * push * u) { drawOps(f.terminal, inks) }
    }
    for (y in floatArrayOf(oy + t, oy + oh - b)) {
        terminal(ox - g.ext, y, 180f)
        terminal(ox + ow + g.ext, y, 0f)
    }
    for (x in floatArrayOf(ox + s, ox + ow - s)) {
        terminal(x, oy - g.ext, 270f)
        terminal(x, oy + oh + g.ext, 90f)
    }
    terminal(ox, oy, 225f, 4f)
    terminal(ox + ow, oy, 315f, 4f)
    terminal(ox + ow, oy + oh, 45f, 4f)
    terminal(ox, oy + oh, 135f, 4f)
}
