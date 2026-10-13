package app.paroissien.core.designsystem

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.random.Random

/** Épaisseur du bloc des pages, en fraction de la largeur de la couverture : le poids du livre, à l'œil. */
fun bookThickness(pages: Int): Float = (0.025f + 0.07f * (ln(pages.coerceAtLeast(10).toFloat()) / ln(3000f))).coerceIn(0.03f, 0.10f)

/**
 * La couverture d'un livre, comme celle d'un missel ancien : cuir grainé et ombré, mors et nerfs côté dos, filets
 * dorés ou poussés à froid, fleurons aux coins, motif central, titre doré en relief. À droite, la tranche des pages,
 * dorée, rouge ou naturelle, d'autant plus épaisse que le livre est gros. [flat] : le plat seul, plein cadre, pour
 * la couverture qu'on tourne dans le lecteur.
 */
@Composable
fun BookCover(identity: BookIdentity, title: String, subtitle: String? = null, modifier: Modifier = Modifier, flat: Boolean = false, inscriptions: Boolean = true) {
    val fonts = rememberBookFonts(BookFace.GARAMOND)
    val cover = identity.cover
    val grain = remember(cover.material) { grainFor(cover.material) }
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val mw = maxWidth
        val mh = maxHeight
        val edge = if (flat) 0f else bookThickness(identity.pages)
        val thickness = w * edge
        Box(
            Modifier.fillMaxSize().drawWithCache {
                val board = Rect(0f, 0f, size.width - thickness, size.height - thickness * 0.35f)
                val grainBrush = grain?.let { ShaderBrush(ImageShader(it, TileMode.Repeated, TileMode.Repeated)) }
                val decor = coverDecor(board, cover, identity.importance)
                val motifCenter = Offset(board.left + board.width * 0.075f + (board.width * 0.925f) / 2f, board.top + board.height * 0.30f)
                val motifHeight = board.width * 0.22f
                val motif = motifPath(cover.motif, motifCenter, motifHeight)
                val around = centerFrame(cover.center, motifCenter, motifHeight, board.width * 0.0075f)
                onDrawBehind {
                    if (!flat) drawPageBlock(board, thickness, identity)
                    drawLeather(board, cover, grainBrush, shadow = !flat)
                    drawSpine(board, cover)
                    tool(cover.tooling, cover, decor)
                    tool(cover.motifTooling, cover, around)
                    motif?.let { tool(cover.motifTooling, cover, CoverDecor(listOf(it), emptyList())) }
                }
            },
        ) {
            // Le titre se tient à l'intérieur du cadre, quel que soit le nombre de filets ; son corps diminue pour que le mot
            // le plus long tienne sur une ligne (jamais « JÉSUS-CHRIS / T »).
            val frame = CoverFrame.depth(cover) + 0.04f
            val room = w * (1f - (0.13f + frame) - (0.07f + frame + edge))
            val measurer = rememberTextMeasurer()
            val longest = title.uppercase().split(' ').maxByOrNull { it.length }.orEmpty()
            val probe = TextStyle(fontFamily = fonts.medium, fontWeight = FontWeight.Medium, fontSize = (mw.value * 0.075f).sp, letterSpacing = 0.12.em)
            val fit = remember(longest, room, fonts) { (room / measurer.measure(longest, probe).size.width.coerceAtLeast(1)).coerceAtMost(1f) * 0.98f }
            val base = TextStyle(
                fontFamily = fonts.medium,
                fontWeight = FontWeight.Medium,
                fontSize = (mw.value * 0.075f * fit).sp,
                lineHeight = (mw.value * 0.095f * fit).sp,
                letterSpacing = 0.12.em,
                textAlign = TextAlign.Center,
            )
            // Lettres dorées au fer, en léger relief ; imprimées à l'encre sur une couverture de papier.
            val titleStyle = if (cover.tooling == Tooling.PRINTED) {
                base.copy(color = cover.ink)
            } else {
                val m = cover.metal
                base.merge(TextStyle(brush = Brush.linearGradient(listOf(m.deep, m.bright, m.mid, m.deep)), shadow = Shadow(m.shadow.copy(alpha = 0.8f), Offset(0f, 1.6f), 1.2f)))
            }
            // Le second plat, à la fin du livre : la même reliure, sans titre ni monogramme.
            if (!inscriptions) return@BoxWithConstraints
            if (cover.motif == CoverMotif.IHS) {
                val monogram = if (cover.motifTooling == Tooling.PRINTED) base.copy(color = cover.ink) else titleStyle
                Column(
                    Modifier.fillMaxSize().padding(start = mw * 0.075f, end = mw * edge, top = mh * 0.15f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BasicText("✝", style = monogram.merge(TextStyle(fontSize = (mw.value * 0.08f).sp, lineHeight = (mw.value * 0.08f).sp, letterSpacing = 0.em)))
                    BasicText("IHS", style = monogram.merge(TextStyle(fontSize = (mw.value * 0.16f).sp, lineHeight = (mw.value * 0.17f).sp, letterSpacing = 0.04.em)))
                }
            }
            Column(
                Modifier.fillMaxSize()
                    .padding(start = mw * (0.13f + frame), end = mw * (0.07f + frame + edge), top = mh * 0.50f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(mh * 0.025f),
            ) {
                BasicText(title.uppercase(), style = titleStyle, maxLines = 4, overflow = TextOverflow.Ellipsis)
                subtitle?.let {
                    BasicText(
                        it,
                        style = titleStyle.merge(TextStyle(fontFamily = fonts.italic, fontSize = (mw.value * 0.045f).sp, lineHeight = (mw.value * 0.058f).sp, letterSpacing = 0.02.em)),
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ── Matière ─────────────────────────────────────────────────────────────────

private fun DrawScope.drawLeather(board: Rect, cover: CoverStyle, grain: ShaderBrush?, shadow: Boolean) {
    val radius = CornerRadius(if (shadow) board.width * 0.025f else 0f)
    // Ombre portée du livre sur la table.
    if (shadow) drawRoundRect(Color.Black.copy(alpha = 0.28f), topLeft = board.topLeft + Offset(2f, 4f), size = board.size, cornerRadius = radius)
    drawRoundRect(cover.leather, topLeft = board.topLeft, size = board.size, cornerRadius = radius)
    // Le cuir s'use et s'éclaircit au centre, fonce aux bords.
    drawRoundRect(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = if (cover.material == CoverMaterial.WRAPPER) 0.05f else 0.10f), Color.Transparent, Color.Black.copy(alpha = 0.35f)),
            center = board.center - Offset(board.width * 0.08f, board.height * 0.12f),
            radius = board.maxDimension * 0.75f,
        ),
        topLeft = board.topLeft, size = board.size, cornerRadius = radius,
    )
    grain?.let { drawRoundRect(it, topLeft = board.topLeft, size = board.size, cornerRadius = radius) }
    if (cover.material == CoverMaterial.CLOTH) {
        // Toile : un tissage très fin.
        clipRect(board.left, board.top, board.right, board.bottom) {
            val step = 2.2f
            val weave = Color.White.copy(alpha = 0.035f)
            var x = board.left
            while (x < board.right) { drawLine(weave, Offset(x, board.top), Offset(x, board.bottom), 0.8f); x += step }
            var y = board.top
            while (y < board.bottom) { drawLine(Color.Black.copy(alpha = 0.05f), Offset(board.left, y), Offset(board.right, y), 0.8f); y += step }
        }
    }
}

/** Côté dos : le mors (la charnière) et, sur un livre relié, les nerfs en relief. */
private fun DrawScope.drawSpine(board: Rect, cover: CoverStyle) {
    if (cover.material == CoverMaterial.WRAPPER) return
    val joint = board.left + board.width * 0.075f
    if (cover.material == CoverMaterial.MARBLED) {
        // Demi-reliure : le dos et les coins en basane brune, les plats en papier marbré.
        val hide = Color(0xFF3B2416)
        drawRect(hide, topLeft = board.topLeft, size = Size(board.width * 0.16f, board.height))
        drawLine(Color.Black.copy(alpha = 0.4f), Offset(board.left + board.width * 0.16f, board.top), Offset(board.left + board.width * 0.16f, board.bottom), 1.5f)
        val c = board.width * 0.14f
        for ((corner, dy) in listOf(Offset(board.right, board.top) to 1f, Offset(board.right, board.bottom) to -1f)) {
            drawPath(Path().apply { moveTo(corner.x, corner.y); lineTo(corner.x - c, corner.y); lineTo(corner.x, corner.y + c * dy); close() }, hide)
        }
    }
    drawRect(
        Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.White.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.10f)), startX = board.left, endX = joint),
        topLeft = board.topLeft, size = Size(joint - board.left, board.height),
    )
    drawLine(Color.Black.copy(alpha = 0.45f), Offset(joint, board.top + 2f), Offset(joint, board.bottom - 2f), 2f)
    drawLine(Color.White.copy(alpha = 0.12f), Offset(joint + 2f, board.top + 2f), Offset(joint + 2f, board.bottom - 2f), 1.2f)
    if (cover.bands > 0) {
        val n = cover.bands
        for (i in 1..n) {
            val y = board.top + board.height * i / (n + 1)
            drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(board.left, y - 2.5f), Size(joint - board.left, 6f), CornerRadius(3f))
            drawRoundRect(lerp(cover.leather, Color.White, 0.18f), Offset(board.left, y - 3f), Size(joint - board.left, 4.5f), CornerRadius(3f))
        }
    }
}

/** La tranche des pages, côté libre : dorée, teinte, jaspée ou marbrée ; plus fine est la page, plus serrées les lignes. */
private fun DrawScope.drawPageBlock(board: Rect, thickness: Float, identity: BookIdentity) {
    val block = Rect(board.right - thickness * 0.5f, board.top + thickness * 0.25f, board.right + thickness * 0.95f, board.bottom + thickness * 0.3f)
    // Plat arrière, puis bloc des pages.
    drawRoundRect(lerp(identity.cover.leather, Color.Black, 0.35f), topLeft = Offset(block.left, block.top + thickness * 0.1f), size = Size(block.width + thickness * 0.12f, block.height), cornerRadius = CornerRadius(4f))
    val tint = identity.paper.tint
    drawPageEdge(
        block, identity.edges, identity.edgeMetal, identity.edgeTooling,
        plain = tint, plainLine = Color.Black.copy(alpha = 0.10f),
        leaf = identity.paper.weight.stepDp * density * 0.9f,
    )
    if (identity.edges == EdgeFinish.PLAIN) {
        drawRect(Brush.verticalGradient(listOf(lerp(tint, Color.Black, 0.08f), Color.Transparent, lerp(tint, Color.Black, 0.12f)), startY = block.top, endY = block.bottom), block.topLeft, block.size)
    }
}

// ── Décor ───────────────────────────────────────────────────────────────────

/** Pousser des fers : au métal (ombre puis reflet), à froid (creux sombre et arête claire), ou imprimés à l'encre. */
private fun DrawScope.tool(tooling: Tooling, cover: CoverStyle, decor: CoverDecor) {
    if (decor.fills.isEmpty() && decor.strokes.isEmpty()) return
    fun pass(brush: Brush, widen: Float = 1f) {
        decor.fills.forEach { drawPath(it, brush) }
        decor.strokes.forEach { (p, w) -> drawPath(p, brush, style = Stroke(w * widen)) }
    }
    when (tooling) {
        Tooling.GOLD -> {
            val m = cover.metal
            translate(0f, 1f) { pass(SolidColor(m.shadow.copy(alpha = 0.6f))) }
            pass(m.brush(Offset.Zero, Offset(size.width, size.height)))
        }
        Tooling.BLIND -> {
            translate(0f, 1f) { pass(SolidColor(Color.White.copy(alpha = 0.14f))) }
            pass(SolidColor(Color.Black.copy(alpha = 0.45f)))
        }
        Tooling.PRINTED -> pass(SolidColor(cover.ink), 0.7f)
    }
}

/** Croix pattée. */
internal fun crossPath(c: Offset, height: Float): Path {
    val width = height * 0.72f
    val bar = height * 0.16f
    val flare = bar * 0.85f
    val top = c.y - height / 2
    val bot = c.y + height / 2
    val arm = c.y - height * 0.14f
    return Path().apply {
        moveTo(c.x - bar / 2, arm - bar / 2)
        lineTo(c.x - bar / 2 - flare / 2, top); lineTo(c.x + bar / 2 + flare / 2, top); lineTo(c.x + bar / 2, arm - bar / 2)
        lineTo(c.x + width / 2, arm - bar / 2 - flare / 2); lineTo(c.x + width / 2, arm + bar / 2 + flare / 2); lineTo(c.x + bar / 2, arm + bar / 2)
        lineTo(c.x + bar / 2 + flare / 2, bot); lineTo(c.x - bar / 2 - flare / 2, bot); lineTo(c.x - bar / 2, arm + bar / 2)
        lineTo(c.x - width / 2, arm + bar / 2 + flare / 2); lineTo(c.x - width / 2, arm - bar / 2 - flare / 2)
        close()
    }
}

/** Chrisme : le X et le P entrelacés. */
internal fun chiRhoPath(c: Offset, h: Float): Path {
    val t = h * 0.09f
    fun bar(x0: Float, y0: Float, x1: Float, y1: Float) = Path().apply {
        val dx = x1 - x0; val dy = y1 - y0
        val len = kotlin.math.hypot(dx, dy)
        val nx = -dy / len * t / 2; val ny = dx / len * t / 2
        moveTo(x0 + nx, y0 + ny); lineTo(x1 + nx, y1 + ny); lineTo(x1 - nx, y1 - ny); lineTo(x0 - nx, y0 - ny); close()
    }
    return Path().apply {
        addPath(bar(c.x - h * 0.34f, c.y - h * 0.22f, c.x + h * 0.34f, c.y + h * 0.42f))
        addPath(bar(c.x + h * 0.34f, c.y - h * 0.22f, c.x - h * 0.34f, c.y + h * 0.42f))
        addPath(bar(c.x, c.y - h * 0.5f, c.x, c.y + h * 0.5f))
        // La boucle du P.
        addPath(Path().apply {
            addOval(Rect(c.x - h * 0.02f, c.y - h * 0.5f, c.x + h * 0.30f, c.y - h * 0.16f))
            addOval(Rect(c.x + h * 0.05f, c.y - h * 0.43f, c.x + h * 0.23f, c.y - h * 0.23f))
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
        })
    }
}

// ── Grain du cuir ───────────────────────────────────────────────────────────

private const val GRAIN = 192

private fun grainFor(material: CoverMaterial): ImageBitmap? = when (material) {
    CoverMaterial.MOROCCO -> moroccoGrain
    CoverMaterial.CALF -> calfGrain
    CoverMaterial.SHAGREEN -> shagreenGrain
    CoverMaterial.VELVET -> velvetPile
    CoverMaterial.VELLUM -> vellumFibres
    CoverMaterial.MARBLED -> marbledVeins
    CoverMaterial.LINEN -> linenWeave
    CoverMaterial.CLOTH, CoverMaterial.WRAPPER -> null
}

/**
 * Les textures de l'atelier : de la lumière (blanc translucide) et de l'ombre (noir translucide) posées sur la couleur
 * de la couverture, qui vaut donc pour toutes les teintes. Toutes se répètent sans couture ([GRAIN] px de côté).
 */
private fun texture(pixel: (Int, Int) -> Float, amplitude: Float): ImageBitmap {
    val pixels = IntArray(GRAIN * GRAIN) { i ->
        val v = pixel(i % GRAIN, i / GRAIN)
        val alpha = (abs(v) * amplitude * 255f).toInt().coerceIn(0, 255)
        (alpha shl 24) or (if (v > 0f) 0xFFFFFF else 0x000000)
    }
    return Bitmap.createBitmap(pixels, GRAIN, GRAIN, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** Bruit de valeur périodique (période [cells]), entre -1 et 1. */
private fun valueNoise(seed: Int, cells: Int): (Float, Float) -> Float {
    val random = Random(seed)
    val lattice = FloatArray(cells * cells) { random.nextFloat() * 2f - 1f }
    return { x, y ->
        val gx = x / GRAIN * cells; val gy = y / GRAIN * cells
        // floor, pas toInt : un point déplacé hors de la tuile (coordonnée négative) reste sur la bonne maille.
        val x0 = kotlin.math.floor(gx).toInt(); val y0 = kotlin.math.floor(gy).toInt()
        val fx = (gx - x0).let { it * it * (3 - 2 * it) }; val fy = (gy - y0).let { it * it * (3 - 2 * it) }
        fun v(i: Int, j: Int) = lattice[(j.mod(cells)) * cells + i.mod(cells)]
        val top = v(x0, y0) * (1 - fx) + v(x0 + 1, y0) * fx
        val bottom = v(x0, y0 + 1) * (1 - fx) + v(x0 + 1, y0 + 1) * fx
        top * (1 - fy) + bottom * fy
    }
}

/** Chagrin : des galets serrés, bombés (éclairés en haut à gauche), séparés de creux sombres. */
private val shagreenGrain: ImageBitmap by lazy {
    val cells = 16
    val random = Random(2026)
    val px = FloatArray(cells * cells) { random.nextFloat() }
    val py = FloatArray(cells * cells) { random.nextFloat() }
    val step = GRAIN.toFloat() / cells
    texture({ x, y ->
        val cx = (x / step).toInt(); val cy = (y / step).toInt()
        var d1 = Float.MAX_VALUE; var d2 = Float.MAX_VALUE; var nx = 0f; var ny = 0f
        for (j in -1..1) for (i in -1..1) {
            val k = (cy + j).mod(cells) * cells + (cx + i).mod(cells)
            val sx = (cx + i + px[k]) * step; val sy = (cy + j + py[k]) * step
            val d = kotlin.math.hypot(x - sx, y - sy)
            if (d < d1) { d2 = d1; d1 = d; nx = x - sx; ny = y - sy } else if (d < d2) d2 = d
        }
        val crevice = ((d2 - d1) / (step * 0.18f)).coerceIn(0f, 1f)
        val dome = -(nx + ny) / (step * 1.2f)
        if (crevice < 1f) -(1f - crevice) else dome.coerceIn(-0.6f, 0.6f)
    }, amplitude = 0.32f)
}

/** Velours : un poil serré, couché par petites plages qui prennent la lumière, sans grain de peau. */
private val velvetPile: ImageBitmap by lazy {
    val medium = valueNoise(311, 12)
    val small = valueNoise(312, 32)
    val fine = valueNoise(313, 96)
    texture({ x, y ->
        val fx = x.toFloat(); val fy = y.toFloat()
        // Les plages couchées : un reflet doux là où le poil s'incline, une ombre là où il se relève.
        0.55f * medium(fx, fy) + 0.3f * small(fx, fy) + 0.15f * fine(fx, fy)
    }, amplitude = 0.13f)
}

/** Vélin : de grandes marbrures douces de la peau, et des fibres fines et claires. */
private val vellumFibres: ImageBitmap by lazy {
    val blotch = valueNoise(411, 5)
    val fibre = valueNoise(412, 64)
    texture({ x, y ->
        val b = blotch(x.toFloat(), y.toFloat())
        val f = fibre(x.toFloat() * 0.25f, y.toFloat() * 3f)
        0.7f * b + if (f > 0.55f) 0.6f else 0f
    }, amplitude = 0.12f)
}

/**
 * Papier marbré : des veines de couleur que le bain a tordues en volutes. Le dessin est déformé deux fois (on prend
 * le bruit au point déjà déplacé), ce qui enroule les veines au lieu de les onduler en rayures.
 */
private val marbledVeins: ImageBitmap by lazy {
    val n1 = valueNoise(511, 3)
    val n2 = valueNoise(512, 3)
    val n3 = valueNoise(513, 6)
    val n4 = valueNoise(514, 6)
    val period = GRAIN / 5f
    texture({ x, y ->
        val fx = x.toFloat(); val fy = y.toFloat()
        val qx = fx + 34f * n1(fx, fy); val qy = fy + 34f * n2(fx, fy)
        val rx = qx + 22f * n3(qx, qy); val ry = qy + 22f * n4(qx, qy)
        // Coefficients multiples de 1/5 : le motif fait un nombre entier de tours sur la tuile, sans couture.
        val v = kotlin.math.sin(2 * PI.toFloat() * (rx * 0.8f + ry * 0.4f) / period)
        when {
            v > 0.9f -> 0.9f + (v - 0.9f) * 1f
            v < -0.94f -> -1f
            else -> v * 0.18f
        }
    }, amplitude = 0.36f)
}

/** Toile de lin : des fils, chacun un peu plus clair ou plus sombre que son voisin, en travers et en long. */
private val linenWeave: ImageBitmap by lazy {
    val threads = 64
    val random = Random(611)
    val warp = FloatArray(threads) { random.nextFloat() * 2f - 1f }
    val weft = FloatArray(threads) { random.nextFloat() * 2f - 1f }
    val step = GRAIN / threads
    texture({ x, y ->
        val over = ((x / step) + (y / step)) % 2 == 0
        val inThread = if (over) (x % step) / step.toFloat() else (y % step) / step.toFloat()
        val ridge = 0.5f - abs(inThread - 0.5f)
        (if (over) warp[x / step] else weft[y / step]) * 0.35f + ridge - 0.25f
    }, amplitude = 0.2f)
}

/** Maroquin : un grain serré et marqué. Veau : une peau plus lisse, à peine marbrée. */
private val moroccoGrain: ImageBitmap by lazy { leatherGrain(seed = 1527, cells = 48, amplitude = 0.16f) }
private val calfGrain: ImageBitmap by lazy { leatherGrain(seed = 1622, cells = 12, amplitude = 0.10f) }

private fun leatherGrain(seed: Int, cells: Int, amplitude: Float): ImageBitmap {
    val random = Random(seed)
    val n = cells + 1
    val lattice = FloatArray(n * n) { random.nextFloat() * 2f - 1f }
    fun at(x: Float, y: Float): Float {
        val gx = x / GRAIN * cells; val gy = y / GRAIN * cells
        val x0 = gx.toInt().coerceAtMost(cells - 1); val y0 = gy.toInt().coerceAtMost(cells - 1)
        val fx = gx - x0; val fy = gy - y0
        fun v(i: Int, j: Int) = lattice[(j % cells) * n + (i % cells)]
        val top = v(x0, y0) * (1 - fx) + v(x0 + 1, y0) * fx
        val bottom = v(x0, y0 + 1) * (1 - fx) + v(x0 + 1, y0 + 1) * fx
        return top * (1 - fy) + bottom * fy
    }
    val pixels = IntArray(GRAIN * GRAIN) { i ->
        val x = (i % GRAIN).toFloat(); val y = (i / GRAIN).toFloat()
        val v = 0.7f * at(x, y) + 0.3f * (random.nextFloat() * 2f - 1f)
        // Les creux du grain sont sombres, les sommets clairs.
        val alpha = (abs(v) * amplitude * 255f).toInt().coerceIn(0, 255)
        (alpha shl 24) or (if (v > 0f) 0xFFFFFF else 0x000000)
    }
    return Bitmap.createBitmap(pixels, GRAIN, GRAIN, Bitmap.Config.ARGB_8888).asImageBitmap()
}
