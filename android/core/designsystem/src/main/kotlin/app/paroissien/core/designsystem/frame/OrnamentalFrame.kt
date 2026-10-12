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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
// vectoriels répétés (frises, pièces centrales, coins, filets, baguettes, ornements extérieurs), décrits une fois
// dans tools/cadres/cadres/<nom>.py et exportés par tools/cadres/generate.py.

/**
 * Un tracé de module : chemin SVG rempli, ou tracé au trait si [stroke] > 0 (unités du module), aux bouts ronds si
 * [round]. [ink] nomme une encre du cadre ; « paper » est le fond de page du thème.
 */
@Immutable
class Op(val ink: String, val pathData: String, val stroke: Float = 0f, val round: Boolean = false)

/** Tracés en texte compact, une ligne par tracé : encre, épaisseur du trait (0 = rempli), « r » si bouts ronds, chemin. */
fun ops(vararg chunks: String): List<Op> = chunks.joinToString("").lineSequence().filter { it.isNotBlank() }.map { line ->
    val a = line.indexOf(' ')
    val b = line.indexOf(' ', a + 1)
    val c = line.indexOf(' ', b + 1)
    Op(line.substring(0, a), line.substring(c + 1), line.substring(a + 1, b).toFloat(), line.substring(b + 1, c) == "r")
}.toList()

/**
 * Frise : élément répété le long d'une bande. Repère local : u le long de la bande, v de 0 (bord extérieur) à 100
 * (côté texte). Toutes les instances dessinent [base], puis toutes dessinent [over] (dessus d'entrelacs).
 * [outset] et [inset] : débord permis au-delà du bord extérieur et du bord intérieur, en unités de profondeur.
 */
@Immutable
class EdgeModule(val period: Float, val phase: Float, val base: List<Op>, val over: List<Op>, val outset: Float = 0f, val inset: Float = 0f)

/**
 * Motif dans une boîte [width] × [height] : coin (dessiné pour le coin haut gauche, les autres en miroir), complément
 * de coin, ornement extérieur ou terminaison (en unités de baguette). [outset] : débord permis vers l'extérieur.
 */
@Immutable
class BoxModule(val ops: List<Op>, val width: Float = 100f, val height: Float = 100f, val outset: Float = 0f)

/** Pièce au milieu d'une bande (médaillon, losange) : u de -[width]/2 à +[width]/2, v comme la frise. */
@Immutable
class CenterModule(val width: Float, val ops: List<Op>, val outset: Float = 0f)

/** Filet tout autour du cadre, à la profondeur [at] (0 = bord extérieur, 100 = côté texte), sous ou sur les frises. */
@Immutable
class Rule(val ink: String, val at: Float, val width: Float, val over: Boolean = true)

/** Baguettes : rectangle extérieur et baguettes intérieures croisées qui débordent de [overhang], terminées par [terminal]. */
@Immutable
class Bars(val layers: List<Pair<String, Float>>, val overhang: Float, val terminal: BoxModule?, val cornerTerminals: Boolean)

/**
 * Un cadre : frises haute, latérales et du pied, pièces centrales, coins, filets, baguettes et ornements extérieurs.
 * Les épaisseurs sont en dp ; les filets, baguettes et ornements extérieurs en unités (100 = la bande la plus fine).
 */
@Immutable
class FrameSpec(
    val name: String,
    val title: String,
    val top: EdgeModule,
    val side: EdgeModule,
    /** Bande du pied, souvent plus haute (convention des manuscrits). */
    val bottom: EdgeModule,
    val topCenter: CenterModule?,
    val sideCenter: CenterModule?,
    val bottomCenter: CenterModule?,
    val corner: BoxModule?,
    val bottomCorner: BoxModule?,
    /** Comble les coins allongés du pied, pointe vers l'extérieur. */
    val cornerFill: BoxModule?,
    val outerCorner: BoxModule?,
    val outerBottomCorner: BoxModule?,
    val outerMid: BoxModule?,
    val outerBottomMid: BoxModule?,
    val outerSideMid: BoxModule?,
    val bars: Bars?,
    val rules: List<Rule>,
    /** Encre du fond des bandes, ou aucun fond. */
    val ground: String?,
    val margin: Dp,
    val topDepth: Dp,
    val sideDepth: Dp,
    val bottomDepth: Dp,
    val textPadding: Dp,
) {
    /** Marges que le contenu doit laisser libres : cadre, baguette intérieure et respiration. */
    fun contentPadding(header: Dp = 0.dp): PaddingValues {
        val barHalf = bars?.let { (it.layers.first().second / 2 * minOf(topDepth, sideDepth).value / 100f).dp } ?: 0.dp
        val horizontal = margin + sideDepth + barHalf + textPadding
        return PaddingValues(
            start = horizontal,
            end = horizontal,
            top = margin + header + topDepth + barHalf + textPadding,
            bottom = margin + bottomDepth + barHalf + textPadding,
        )
    }
}

/** Un cadre du registre : son nom, son titre dans le paroissien d'origine, sa forme et ses encres. */
@Immutable
class OrnamentalFrameDef(val name: String, val title: String, val spec: FrameSpec, val style: FrameStyle)

/** Couleurs d'un cadre : celles du livre, remplacées en partie selon la couleur liturgique. */
@Immutable
class FrameStyle(
    val base: Map<String, Color>,
    val palettes: Map<LiturgicalColor, Map<String, Color>>,
    /** Encres rapprochées du fond de page dans les thèmes sombres ([ornamentDim]). */
    val dimmed: Set<String>,
) {
    fun inks(liturgicalColor: LiturgicalColor?, paper: PaperColors): Map<String, Color> {
        val inks = base.toMutableMap()
        liturgicalColor?.let { palettes[it]?.let(inks::putAll) }
        val dim = ornamentDim(paper)
        if (dim > 0f) {
            for (ink in dimmed) inks[ink]?.let { inks[ink] = mix(it, paper.background, dim) }
        }
        inks["paper"] = paper.background
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
    Box(modifier.fillMaxSize().paperBackground()) {
        OrnamentalFrameLayer(spec, style, liturgicalColor, headerHeight)
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = spec.margin, start = spec.margin, end = spec.margin)
                .height(headerHeight),
        ) { header() }
        content(spec.contentPadding(headerHeight))
    }
}

/**
 * Le cadre seul, dessiné dans sa propre couche sur toute la place disponible, sans fond : pour l'insérer sous une
 * page déjà mise en forme (le lecteur, la doublure). [bottomInset] réserve une bande sous le cadre (les rubans).
 */
@Composable
fun OrnamentalFrameLayer(
    spec: FrameSpec,
    style: FrameStyle,
    liturgicalColor: LiturgicalColor? = null,
    headerHeight: Dp = 0.dp,
    bottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val paper = LocalPaperColors.current
    val prepared = remember(spec) { PreparedFrame(spec) }
    val inks = remember(style, liturgicalColor, paper) { style.inks(liturgicalColor, paper) }
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer()
            .drawWithCache {
                val geometry = FrameGeometry(spec, size.width, size.height - bottomInset.toPx(), headerHeight.toPx(), this)
                onDrawBehind { drawFrame(prepared, spec, inks, geometry) }
            },
    )
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

private class PreparedOp(val ink: String, val path: Path, val stroke: Stroke?)

private fun List<Op>.prepare(): List<PreparedOp> = map {
    val stroke = when {
        it.stroke <= 0f -> null
        it.round -> Stroke(width = it.stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        else -> Stroke(width = it.stroke)
    }
    PreparedOp(it.ink, PathParser().parsePathString(it.pathData).toPath(), stroke)
}

private class PreparedEdge(m: EdgeModule) {
    val period = m.period
    val phase = m.phase
    val outset = m.outset
    val inset = m.inset
    val base = m.base.prepare()
    val over = m.over.prepare()
}

private class PreparedBox(m: BoxModule) {
    val width = m.width
    val height = m.height
    val outset = m.outset
    val ops = m.ops.prepare()
}

private class PreparedCenter(m: CenterModule) {
    val width = m.width
    val outset = m.outset
    val ops = m.ops.prepare()
}

private class PreparedFrame(spec: FrameSpec) {
    private val edges = HashMap<EdgeModule, PreparedEdge>()
    private val boxes = HashMap<BoxModule, PreparedBox>()
    private fun edge(m: EdgeModule) = edges.getOrPut(m) { PreparedEdge(m) }
    private fun box(m: BoxModule?) = m?.let { boxes.getOrPut(it) { PreparedBox(it) } }

    val top = edge(spec.top)
    val side = edge(spec.side)
    val bottom = edge(spec.bottom)
    val topCenter = spec.topCenter?.let(::PreparedCenter)
    val sideCenter = spec.sideCenter?.let(::PreparedCenter)
    val bottomCenter = spec.bottomCenter?.let(::PreparedCenter)
    val corner = box(spec.corner)
    val bottomCorner = box(spec.bottomCorner)
    val cornerFill = box(spec.cornerFill)
    val outerCorner = box(spec.outerCorner)
    val outerBottomCorner = box(spec.outerBottomCorner)
    val outerMid = box(spec.outerMid)
    val outerBottomMid = box(spec.outerBottomMid)
    val outerSideMid = box(spec.outerSideMid)
    val terminal = box(spec.bars?.terminal)
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
    val ring: Path
    val bars: Path?
    val ext: Float

    init {
        with(density) {
            val margin = spec.margin.toPx()
            ox = margin
            oy = margin + headerPx
            ow = width - 2 * margin
            oh = height - oy - margin
            t = spec.topDepth.toPx()
            s = spec.sideDepth.toPx()
            b = spec.bottomDepth.toPx()
        }
        u = min(t, s) / 100f
        ring = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(ox, oy, ox + ow, oy + oh))
            addRect(Rect(ox + s, oy + t, ox + ow - s, oy + oh - b))
        }
        ext = (spec.bars?.overhang ?: 0f) * u
        // rectangle extérieur, puis baguettes intérieures croisées qui débordent jusqu'aux terminaisons
        bars = spec.bars?.let {
            Path().apply {
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
}

// ---------------------------------------------------------------------------------------------------------------
// Dessin
// ---------------------------------------------------------------------------------------------------------------

private fun DrawScope.drawOps(ops: List<PreparedOp>, inks: Map<String, Color>) {
    for (op in ops) {
        val color = inks[op.ink] ?: continue
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

private inline fun DrawScope.clipped(x0: Float, y0: Float, x1: Float, y1: Float, block: DrawScope.() -> Unit) =
    clipRect(min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1), block = block)

/** Une bande : origine au bord extérieur (x0, y0), direction (ax, ay), de l'extérieur vers le texte (dx, dy). */
private class Band(val x0: Float, val y0: Float, val ax: Float, val ay: Float, val dx: Float, val dy: Float, val len: Float, val depth: Float)

/**
 * Une portion de bande, de u = [from] à [from] + [len] : n éléments, n = arrondi(longueur / pas nominal), puis le pas
 * est ajusté pour tomber juste (comme border-image-repeat: round) ; un NinePatch étirerait le motif au lieu de le
 * répéter. [back] la parcourt depuis sa fin, en miroir.
 */
private fun DrawScope.drawRun(m: PreparedEdge, inks: Map<String, Color>, mtx: Matrix, band: Band, from: Float, len: Float, back: Boolean) {
    if (len <= 0f) return
    val k = band.depth / 100f
    val n = max(1, (len / (m.period * k)).roundToInt())
    val step = len / n
    val ka = step / m.period
    val sg = if (back) -1f else 1f
    val start = if (back) from + len else from
    val x = band.x0 + band.ax * start
    val y = band.y0 + band.ay * start
    val o = m.outset * k
    val i = m.inset * k
    // Un pixel de recouvrement avec la pièce voisine : deux découpes lissées bord à bord laissent un liseré.
    val e = 1f
    clipped(
        band.x0 + band.ax * (from - e) - band.dx * o, band.y0 + band.ay * (from - e) - band.dy * o,
        band.x0 + band.ax * (from + len + e) + band.dx * (band.depth + i), band.y0 + band.ay * (from + len + e) + band.dy * (band.depth + i),
    ) {
        for (pass in listOf(m.base, m.over)) {
            for (j in -1..n + 1) {
                val u0 = (j + m.phase) * step
                withAffine(mtx, sg * band.ax * ka, sg * band.ay * ka, band.dx * k, band.dy * k, x + sg * band.ax * u0, y + sg * band.ay * u0) {
                    drawOps(pass, inks)
                }
            }
        }
    }
}

/** Une bande entière ; avec une pièce centrale, deux demi-bandes symétriques de part et d'autre. */
private fun DrawScope.drawBand(m: PreparedEdge, center: PreparedCenter?, inks: Map<String, Color>, mtx: Matrix, band: Band) {
    if (center == null) return drawRun(m, inks, mtx, band, 0f, band.len, back = false)
    val k = band.depth / 100f
    val cw = center.width * k
    val half = (band.len - cw) / 2
    drawRun(m, inks, mtx, band, 0f, half, back = false)
    drawRun(m, inks, mtx, band, band.len - half, half, back = true)
    val mid = band.len / 2
    val o = center.outset * k
    clipped(
        band.x0 + band.ax * (mid - cw / 2 - 1f) - band.dx * o, band.y0 + band.ay * (mid - cw / 2 - 1f) - band.dy * o,
        band.x0 + band.ax * (mid + cw / 2 + 1f) + band.dx * band.depth, band.y0 + band.ay * (mid + cw / 2 + 1f) + band.dy * band.depth,
    ) {
        withAffine(mtx, band.ax * k, band.ay * k, band.dx * k, band.dy * k, band.x0 + band.ax * mid, band.y0 + band.ay * mid) {
            drawOps(center.ops, inks)
        }
    }
}

/**
 * Coin : le module, dessiné pour le coin haut gauche, est posé contre l'angle intérieur de la case et mis à l'échelle
 * pour y tenir ; les autres coins sont des miroirs ([fx], [fy]). Si la case est plus haute (pied), le complément
 * comble le reste, pointe vers l'extérieur.
 */
private fun DrawScope.drawCorner(
    m: PreparedBox?, fill: PreparedBox?, inks: Map<String, Color>, mtx: Matrix,
    x: Float, y: Float, w: Float, h: Float, fx: Float, fy: Float,
) {
    if (m == null) return
    val sc = min(w / m.width, h / m.height)
    val o = m.outset * sc
    val ix = if (fx > 0) x + w else x
    val iy = if (fy > 0) y + h else y
    clipped(if (fx > 0) x - o else x + w + o, if (fy > 0) y - o else y + h + o, ix, iy) {
        withAffine(mtx, fx * sc, 0f, 0f, fy * sc, ix - fx * m.width * sc, iy - fy * m.height * sc) { drawOps(m.ops, inks) }
        val rest = h - m.height * sc
        if (fill != null && rest > 0.25f * w) {
            val f = min(w, rest) / 100f
            val left = x + w / 2 - 50f * f
            val outerY = if (fy > 0) y else y + h
            withAffine(mtx, f, 0f, 0f, fy * f, left, outerY) { drawOps(fill.ops, inks) }
        }
    }
}

private fun DrawScope.drawFrame(f: PreparedFrame, spec: FrameSpec, inks: Map<String, Color>, g: FrameGeometry) {
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
    spec.ground?.let { ink -> inks[ink]?.let { drawPath(g.ring, it) } }

    fun rules(over: Boolean) {
        for (r in spec.rules) {
            if (r.over != over) continue
            val color = inks[r.ink] ?: continue
            val k = r.at / 100f
            drawRect(color, Offset(ox + k * s, oy + k * t), Size(ow - 2 * k * s, oh - k * t - k * b), style = Stroke(width = r.width * u, join = StrokeJoin.Miter))
        }
    }
    rules(over = false)

    // 2. frises : haut, gauche, droite, pied
    val hLen = ow - 2 * s
    val vLen = oh - t - b
    drawBand(f.top, f.topCenter, inks, mtx, Band(ox + s, oy, 1f, 0f, 0f, 1f, hLen, t))
    drawBand(f.side, f.sideCenter, inks, mtx, Band(ox, oy + t, 0f, 1f, 1f, 0f, vLen, s))
    drawBand(f.side, f.sideCenter, inks, mtx, Band(ox + ow, oy + t, 0f, 1f, -1f, 0f, vLen, s))
    drawBand(f.bottom, f.bottomCenter, inks, mtx, Band(ox + s, oy + oh, 1f, 0f, 0f, -1f, hLen, b))

    // 3. coins
    drawCorner(f.corner, f.cornerFill, inks, mtx, ox, oy, s, t, 1f, 1f)
    drawCorner(f.corner, f.cornerFill, inks, mtx, ox + ow - s, oy, s, t, -1f, 1f)
    drawCorner(f.bottomCorner, f.cornerFill, inks, mtx, ox, oy + oh - b, s, b, 1f, -1f)
    drawCorner(f.bottomCorner, f.cornerFill, inks, mtx, ox + ow - s, oy + oh - b, s, b, -1f, -1f)

    rules(over = true)

    // 4. baguettes : chaque couche (contour, couleur, filet clair) ; les croisements se fondent. Puis terminaisons.
    val bars = spec.bars
    val barPath = g.bars
    if (bars != null && barPath != null) {
        for ((ink, width) in bars.layers) {
            val color = inks[ink] ?: continue
            drawPath(barPath, color, style = Stroke(width = width * u, cap = StrokeCap.Butt, join = StrokeJoin.Miter))
        }
        f.terminal?.let { term ->
            fun terminal(x: Float, y: Float, degrees: Float, push: Float = 0f) {
                val a = degrees * PI.toFloat() / 180f
                val c = cos(a)
                val sn = sin(a)
                withAffine(mtx, c * u, sn * u, -sn * u, c * u, x + c * push * u, y + sn * push * u) { drawOps(term.ops, inks) }
            }
            for (y in floatArrayOf(oy + t, oy + oh - b)) {
                terminal(ox - g.ext, y, 180f)
                terminal(ox + ow + g.ext, y, 0f)
            }
            for (x in floatArrayOf(ox + s, ox + ow - s)) {
                terminal(x, oy - g.ext, 270f)
                terminal(x, oy + oh + g.ext, 90f)
            }
            if (bars.cornerTerminals) {
                terminal(ox, oy, 225f, 4f)
                terminal(ox + ow, oy, 315f, 4f)
                terminal(ox + ow, oy + oh, 45f, 4f)
                terminal(ox, oy + oh, 135f, 4f)
            }
        }
    }

    // 5. ornements extérieurs : aux quatre coins (miroirs), au milieu de chaque côté
    fun place(m: PreparedBox?, a: Float, bb: Float, c: Float, d: Float, e: Float, ff: Float) {
        if (m == null) return
        withAffine(mtx, a * u, bb * u, c * u, d * u, e, ff) { drawOps(m.ops, inks) }
    }
    place(f.outerCorner, 1f, 0f, 0f, 1f, ox, oy)
    place(f.outerCorner, -1f, 0f, 0f, 1f, ox + ow, oy)
    place(f.outerBottomCorner, 1f, 0f, 0f, -1f, ox, oy + oh)
    place(f.outerBottomCorner, -1f, 0f, 0f, -1f, ox + ow, oy + oh)
    place(f.outerMid, 1f, 0f, 0f, 1f, ox + ow / 2, oy)
    place(f.outerBottomMid, 1f, 0f, 0f, -1f, ox + ow / 2, oy + oh)
    // au milieu de la bande latérale (et de sa pièce centrale), pas du cadre : le pied est souvent plus haut
    val my = oy + t + (oh - t - b) / 2
    place(f.outerSideMid, 0f, -1f, 1f, 0f, ox, my)
    place(f.outerSideMid, 0f, 1f, -1f, 0f, ox + ow, my)
}
