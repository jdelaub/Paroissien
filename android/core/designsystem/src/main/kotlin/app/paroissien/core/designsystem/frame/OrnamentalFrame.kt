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
 * [mirrored] : après une pièce centrale, la frise repart en miroir (sinon elle continue dans le même sens).
 * [spill] : unités dont chaque portion mord sur ses voisines ; elle ne dessine alors que ses propres éléments.
 */
@Immutable
class EdgeModule(
    val period: Float,
    val phase: Float,
    val base: List<Op>,
    val over: List<Op>,
    val outset: Float = 0f,
    val inset: Float = 0f,
    val mirrored: Boolean = true,
    val spill: Float = 0f,
)

/**
 * Motif dans une boîte [width] × [height] : coin (dessiné pour le coin haut gauche, les autres en miroir), complément
 * de coin, pièce de bout, ornement extérieur ou terminaison (en unités de baguette). [outset] : débord permis vers
 * l'extérieur ; [inset] : vers le texte (pièces de bout). [above] : un coin dessiné après les filets et les baguettes.
 */
@Immutable
class BoxModule(
    val ops: List<Op>,
    val width: Float = 100f,
    val height: Float = 100f,
    val outset: Float = 0f,
    val inset: Float = 0f,
    val above: Boolean = false,
)

/**
 * Pièce sur une bande (médaillon, losange) : u de -[width]/2 à +[width]/2, v comme la frise, posée à la fraction [at]
 * de la longueur. [inset] : débord vers le texte ; [upright] : la pièce garde partout l'aspect qu'elle a sur la bande
 * haute ou sur la bande droite, déplacée et jamais retournée (figure debout, inscription) ; [above] : dessinée après
 * les filets et les baguettes, qu'elle interrompt.
 */
@Immutable
class CenterModule(
    val width: Float,
    val ops: List<Op>,
    val outset: Float = 0f,
    val inset: Float = 0f,
    val upright: Boolean = false,
    val above: Boolean = false,
    val at: Float = 0.5f,
)

/** D'où se mesure la profondeur d'un filet. */
enum class RuleFrom {
    /** [Rule.at] de 0 (bord extérieur) à 100 (côté texte), proportionnel à chaque bande. */
    FRACTION,

    /** [Rule.at] en unités depuis le bord extérieur : la même distance sur toutes les bandes. */
    OUTER,

    /** [Rule.at] en unités depuis le côté texte. */
    INNER,
}

/** Filet le long des bandes présentes, à la profondeur [at], sous ou sur les frises. */
@Immutable
class Rule(val ink: String, val at: Float, val width: Float, val over: Boolean = true, val from: RuleFrom = RuleFrom.FRACTION)

/**
 * Pièces posées une fois à chaque bout d'une bande (socle et couronnement d'un candélabre). Boîtes w × 100 dessinées
 * comme au début de la bande ; celle de la fin est retournée par le moteur.
 */
@Immutable
class BandEnds(val start: BoxModule?, val end: BoxModule?)

/**
 * Bandes présentes. [outer] est le côté extérieur de la page, à droite sur un recto et à gauche sur un verso, comme
 * dans un livre ; [inner] le côté du pli. Une bande sans voisine court jusqu'au bord du cadre.
 */
@Immutable
class FrameBands(val top: Boolean = true, val outer: Boolean = true, val bottom: Boolean = true, val inner: Boolean = true) {
    val full: Boolean get() = top && outer && bottom && inner

    /** Présence à gauche et à droite, selon le côté de la page. */
    fun left(outerOnRight: Boolean) = if (outerOnRight) inner else outer
    fun right(outerOnRight: Boolean) = if (outerOnRight) outer else inner

    companion object {
        val Full = FrameBands()
    }
}

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
    val bands: FrameBands = FrameBands.Full,
    val topEnds: BandEnds? = null,
    val sideEnds: BandEnds? = null,
    val bottomEnds: BandEnds? = null,
) {
    /**
     * Marges que le contenu doit laisser libres : cadre, baguette intérieure et respiration. Sans bande d'un côté,
     * seules la marge et la respiration restent ; la page est alors asymétrique et suit [outerOnRight].
     */
    fun contentPadding(header: Dp = 0.dp, outerOnRight: Boolean = true): PaddingValues {
        val barHalf = bars?.let { (it.layers.first().second / 2 * minOf(topDepth, sideDepth).value / 100f).dp } ?: 0.dp
        fun band(present: Boolean, depth: Dp) = if (present) depth + barHalf else 0.dp
        return PaddingValues(
            start = margin + band(bands.left(outerOnRight), sideDepth) + textPadding,
            end = margin + band(bands.right(outerOnRight), sideDepth) + textPadding,
            top = margin + header + band(bands.top, topDepth) + textPadding,
            bottom = margin + band(bands.bottom, bottomDepth) + textPadding,
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
        // encres du thème : une gravure tracée en « text » reste lisible sur un fond sombre
        inks["text"] = paper.inkPrimary
        inks["rubric"] = paper.rubric
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
 * [outerOnRight] : recto (côté extérieur à droite) ou verso.
 */
@Composable
fun OrnamentalFrame(
    spec: FrameSpec,
    style: FrameStyle,
    modifier: Modifier = Modifier,
    liturgicalColor: LiturgicalColor? = null,
    headerHeight: Dp = 0.dp,
    outerOnRight: Boolean = true,
    header: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(modifier.fillMaxSize().paperBackground()) {
        OrnamentalFrameLayer(spec, style, liturgicalColor, headerHeight, outerOnRight = outerOnRight)
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = spec.margin, start = spec.margin, end = spec.margin)
                .height(headerHeight),
        ) { header() }
        content(spec.contentPadding(headerHeight, outerOnRight))
    }
}

/**
 * Le cadre seul, dessiné dans sa propre couche sur toute la place disponible, sans fond : pour l'insérer sous une
 * page déjà mise en forme (le lecteur, la doublure). [bottomInset] réserve une bande sous le cadre (les rubans).
 * [outerOnRight] : la page est un recto (côté extérieur à droite) ou un verso.
 */
@Composable
fun OrnamentalFrameLayer(
    spec: FrameSpec,
    style: FrameStyle,
    liturgicalColor: LiturgicalColor? = null,
    headerHeight: Dp = 0.dp,
    bottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
    outerOnRight: Boolean = true,
) {
    val paper = LocalPaperColors.current
    val prepared = remember(spec) { PreparedFrame(spec) }
    val inks = remember(style, liturgicalColor, paper) { style.inks(liturgicalColor, paper) }
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer()
            .drawWithCache {
                val geometry = FrameGeometry(spec, size.width, size.height - bottomInset.toPx(), headerHeight.toPx(), outerOnRight, this)
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

internal class PreparedOp(val ink: String, val path: Path, val stroke: Stroke?)

internal fun List<Op>.prepare(): List<PreparedOp> = map {
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
    val mirrored = m.mirrored
    val spill = m.spill
    val base = m.base.prepare()
    val over = m.over.prepare()
}

private class PreparedBox(m: BoxModule) {
    val width = m.width
    val height = m.height
    val outset = m.outset
    val inset = m.inset
    val above = m.above
    val ops = m.ops.prepare()
}

private class PreparedCenter(m: CenterModule) {
    val width = m.width
    val outset = m.outset
    val inset = m.inset
    val upright = m.upright
    val above = m.above
    val at = m.at
    val ops = m.ops.prepare()
}

private class PreparedEnds(val start: PreparedBox?, val end: PreparedBox?)

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
    val topEnds = ends(spec.topEnds)
    val sideEnds = ends(spec.sideEnds)
    val bottomEnds = ends(spec.bottomEnds)
    private fun ends(e: BandEnds?) = e?.let { PreparedEnds(box(it.start), box(it.end)) }
}

private class FrameGeometry(spec: FrameSpec, width: Float, height: Float, headerPx: Float, outerOnRight: Boolean, density: Density) {
    val top = spec.bands.top
    val bottom = spec.bands.bottom
    val left = spec.bands.left(outerOnRight)
    val right = spec.bands.right(outerOnRight)
    val full = top && bottom && left && right
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
            if (full) {
                fillType = PathFillType.EvenOdd
                addRect(Rect(ox, oy, ox + ow, oy + oh))
                addRect(Rect(ox + s, oy + t, ox + ow - s, oy + oh - b))
            } else {
                // bandes présentes seulement : leurs rectangles se recouvrent aux coins, la règle nonzero les unit
                if (top) addRect(Rect(ox, oy, ox + ow, oy + t))
                if (bottom) addRect(Rect(ox, oy + oh - b, ox + ow, oy + oh))
                if (left) addRect(Rect(ox, oy, ox + s, oy + oh))
                if (right) addRect(Rect(ox + ow - s, oy, ox + ow, oy + oh))
            }
        }
        ext = (spec.bars?.overhang ?: 0f) * u
        // rectangle extérieur, puis baguettes intérieures croisées qui débordent jusqu'aux terminaisons (cadre plein)
        bars = spec.bars?.takeIf { full }?.let {
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

internal fun DrawScope.drawOps(ops: List<PreparedOp>, inks: Map<String, Color>) {
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
    // Un pixel de recouvrement avec la pièce voisine : deux découpes lissées bord à bord laissent un liseré. Une frise
    // qui déborde mord sur ses voisines et ne dessine alors que ses propres éléments.
    val spill = m.spill * k
    val e = 1f + spill
    val first = if (spill > 0f) 0 else -1
    val last = if (spill > 0f) n - 1 else n + 1
    clipped(
        band.x0 + band.ax * (from - e) - band.dx * o, band.y0 + band.ay * (from - e) - band.dy * o,
        band.x0 + band.ax * (from + len + e) + band.dx * (band.depth + i), band.y0 + band.ay * (from + len + e) + band.dy * (band.depth + i),
    ) {
        for (pass in listOf(m.base, m.over)) {
            for (j in first..last) {
                val u0 = (j + m.phase) * step
                withAffine(mtx, sg * band.ax * ka, sg * band.ay * ka, band.dx * k, band.dy * k, x + sg * band.ax * u0, y + sg * band.ay * u0) {
                    drawOps(pass, inks)
                }
            }
        }
    }
}

/**
 * Pièce posée sur la bande entre [from] et [to] (u à l'écran), origine du module en [origin] : pièce centrale ou de
 * bout. [flip] la retourne le long de la bande (pièce de fin). [upright] : la pièce garde l'aspect qu'elle a sur la
 * bande haute ou sur la bande droite ; sur la bande gauche ou du pied, elle est déplacée, jamais retournée ni
 * renversée (son v = 0 passe du côté du texte).
 */
private fun DrawScope.drawPiece(
    ops: List<PreparedOp>, inks: Map<String, Color>, mtx: Matrix, band: Band,
    from: Float, to: Float, origin: Float, outset: Float, inset: Float, flip: Boolean, upright: Boolean,
) {
    val k = band.depth / 100f
    val mirrorBand = band.ax * band.dy - band.ay * band.dx < 0f
    val turn = upright && mirrorBand
    val x0 = if (turn) band.x0 + band.dx * band.depth else band.x0
    val y0 = if (turn) band.y0 + band.dy * band.depth else band.y0
    val dx = if (turn) -band.dx else band.dx
    val dy = if (turn) -band.dy else band.dy
    val out = if (turn) inset else outset
    val inn = if (turn) outset else inset
    val ax = if (flip) -band.ax else band.ax
    val ay = if (flip) -band.ay else band.ay
    val lo = min(from, to) - 1f
    val hi = max(from, to) + 1f
    clipped(
        x0 + band.ax * lo - dx * out * k, y0 + band.ay * lo - dy * out * k,
        x0 + band.ax * hi + dx * (band.depth + inn * k), y0 + band.ay * hi + dy * (band.depth + inn * k),
    ) {
        withAffine(mtx, ax * k, ay * k, dx * k, dy * k, x0 + band.ax * origin, y0 + band.ay * origin) {
            drawOps(ops, inks)
        }
    }
}

/**
 * Une bande entière : pièces de bout, frise, et pièce centrale entre deux demi-frises symétriques. Les pièces
 * « dessus » vont dans [later], dessinées après les filets et les baguettes.
 */
private fun DrawScope.drawBand(
    m: PreparedEdge, center: PreparedCenter?, ends: PreparedEnds?, inks: Map<String, Color>, mtx: Matrix, band: Band,
    later: MutableList<DrawScope.() -> Unit>,
) {
    val k = band.depth / 100f
    val start = ends?.start
    val end = ends?.end
    val a = (start?.width ?: 0f) * k
    val z = (end?.width ?: 0f) * k
    if (start != null) {
        val piece: DrawScope.() -> Unit = { drawPiece(start.ops, inks, mtx, band, 0f, a, 0f, start.outset, start.inset, flip = false, upright = false) }
        if (start.above) later += piece else piece()
    }
    if (end != null) {
        val piece: DrawScope.() -> Unit = { drawPiece(end.ops, inks, mtx, band, band.len - z, band.len, band.len, end.outset, end.inset, flip = true, upright = false) }
        if (end.above) later += piece else piece()
    }
    if (center == null) return drawRun(m, inks, mtx, band, a, band.len - a - z, back = false)
    val cw = center.width * k
    // au milieu, ou à la fraction [CenterModule.at] ; la frise repart en miroir après la pièce, ou dans le même sens
    val mid = band.len * center.at
    drawRun(m, inks, mtx, band, a, mid - cw / 2 - a, back = false)
    drawRun(m, inks, mtx, band, mid + cw / 2, band.len - z - mid - cw / 2, back = m.mirrored)
    val piece: DrawScope.() -> Unit = {
        drawPiece(center.ops, inks, mtx, band, mid - cw / 2, mid + cw / 2, mid, center.outset, center.inset, flip = false, upright = center.upright)
    }
    if (center.above) later += piece else piece()
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

/** Filets le long des bandes présentes ; un filet ouvert court jusqu'au bord du cadre. */
private fun DrawScope.drawRules(spec: FrameSpec, inks: Map<String, Color>, g: FrameGeometry, over: Boolean) {
    for (r in spec.rules) {
        if (r.over != over) continue
        val color = inks[r.ink] ?: continue
        fun off(present: Boolean, depth: Float) = when {
            !present -> 0f
            r.from == RuleFrom.OUTER -> r.at * g.u
            r.from == RuleFrom.INNER -> depth - r.at * g.u
            else -> r.at / 100f * depth
        }
        val top = off(g.top, g.t)
        val left = off(g.left, g.s)
        val right = off(g.right, g.s)
        val bottom = off(g.bottom, g.b)
        val stroke = Stroke(width = r.width * g.u, join = StrokeJoin.Miter)
        if (g.full) {
            drawRect(color, Offset(g.ox + left, g.oy + top), Size(g.ow - left - right, g.oh - top - bottom), style = stroke)
            continue
        }
        // côtés dans l'ordre haut, droite, pied, gauche ; chaque suite de côtés présents fait une ligne brisée
        val xs = floatArrayOf(g.ox + left, g.ox + g.ow - right, g.ox + g.ow - right, g.ox + left)
        val ys = floatArrayOf(g.oy + top, g.oy + top, g.oy + g.oh - bottom, g.oy + g.oh - bottom)
        val sides = booleanArrayOf(g.top, g.right, g.bottom, g.left)
        val first = sides.indexOfFirst { !it }
        val path = Path()
        var open = false
        for (i in 1..4) {
            val k = (first + i) % 4
            if (!sides[k]) {
                open = false
                continue
            }
            if (!open) path.moveTo(xs[k], ys[k])
            open = true
            path.lineTo(xs[(k + 1) % 4], ys[(k + 1) % 4])
        }
        drawPath(path, color, style = stroke)
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
    val later = ArrayList<DrawScope.() -> Unit>()

    // 1. fond des bandes
    spec.ground?.let { ink -> inks[ink]?.let { drawPath(g.ring, it) } }

    drawRules(spec, inks, g, over = false)

    // 2. frises : haut, gauche, droite, pied ; une bande sans voisine court jusqu'au bord du cadre
    val x0 = if (g.left) ox + s else ox
    val x1 = if (g.right) ox + ow - s else ox + ow
    val y0 = if (g.top) oy + t else oy
    val y1 = if (g.bottom) oy + oh - b else oy + oh
    if (g.top) drawBand(f.top, f.topCenter, f.topEnds, inks, mtx, Band(x0, oy, 1f, 0f, 0f, 1f, x1 - x0, t), later)
    if (g.left) drawBand(f.side, f.sideCenter, f.sideEnds, inks, mtx, Band(ox, y0, 0f, 1f, 1f, 0f, y1 - y0, s), later)
    if (g.right) drawBand(f.side, f.sideCenter, f.sideEnds, inks, mtx, Band(ox + ow, y0, 0f, 1f, -1f, 0f, y1 - y0, s), later)
    if (g.bottom) drawBand(f.bottom, f.bottomCenter, f.bottomEnds, inks, mtx, Band(x0, oy + oh, 1f, 0f, 0f, -1f, x1 - x0, b), later)

    // 3. coins, là où deux bandes se rencontrent
    fun corner(m: PreparedBox?, present: Boolean, x: Float, y: Float, w: Float, h: Float, fx: Float, fy: Float) {
        if (m == null || !present) return
        val piece: DrawScope.() -> Unit = { drawCorner(m, f.cornerFill, inks, mtx, x, y, w, h, fx, fy) }
        if (m.above) later += piece else piece()
    }
    corner(f.corner, g.top && g.left, ox, oy, s, t, 1f, 1f)
    corner(f.corner, g.top && g.right, ox + ow - s, oy, s, t, -1f, 1f)
    corner(f.bottomCorner, g.bottom && g.left, ox, oy + oh - b, s, b, 1f, -1f)
    corner(f.bottomCorner, g.bottom && g.right, ox + ow - s, oy + oh - b, s, b, -1f, -1f)

    drawRules(spec, inks, g, over = true)

    // 4. baguettes (cadre plein) : chaque couche (contour, couleur, filet clair) ; les croisements se fondent. Puis terminaisons.
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

    // 5. pièces « dessus » : elles interrompent filets et baguettes
    for (piece in later) piece()

    // 6. ornements extérieurs : aux coins (miroirs), au milieu de chaque côté présent
    fun place(m: PreparedBox?, present: Boolean, a: Float, bb: Float, c: Float, d: Float, e: Float, ff: Float) {
        if (m == null || !present) return
        withAffine(mtx, a * u, bb * u, c * u, d * u, e, ff) { drawOps(m.ops, inks) }
    }
    place(f.outerCorner, g.top && g.left, 1f, 0f, 0f, 1f, ox, oy)
    place(f.outerCorner, g.top && g.right, -1f, 0f, 0f, 1f, ox + ow, oy)
    place(f.outerBottomCorner, g.bottom && g.left, 1f, 0f, 0f, -1f, ox, oy + oh)
    place(f.outerBottomCorner, g.bottom && g.right, -1f, 0f, 0f, -1f, ox + ow, oy + oh)
    place(f.outerMid, g.top, 1f, 0f, 0f, 1f, ox + ow / 2, oy)
    place(f.outerBottomMid, g.bottom, 1f, 0f, 0f, -1f, ox + ow / 2, oy + oh)
    // au milieu de la bande latérale (et de sa pièce centrale), pas du cadre : le pied est souvent plus haut
    val my = (y0 + y1) / 2
    place(f.outerSideMid, g.left, 0f, -1f, 1f, 0f, ox, my)
    place(f.outerSideMid, g.right, 0f, 1f, -1f, 0f, ox + ow, my)
}
