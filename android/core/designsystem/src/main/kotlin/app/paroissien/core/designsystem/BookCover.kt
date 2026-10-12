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
import kotlin.math.abs
import kotlin.math.ln
import kotlin.random.Random

private val GoldShadow = Color(0xFF4A3608)
private val GoldDeep = Color(0xFF8A6A1C)
private val GoldMid = Color(0xFFD2AE52)
private val GoldBright = Color(0xFFF5E2A0)

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
                onDrawBehind {
                    if (!flat) drawPageBlock(board, thickness, identity)
                    drawLeather(board, cover, grainBrush, shadow = !flat)
                    drawSpine(board, cover)
                    drawTooling(board, cover, identity.importance)
                    drawMotif(board, cover)
                }
            },
        ) {
            // Le titre se tient à l'intérieur du cadre, quel que soit le nombre de filets ; son corps diminue pour que le mot
            // le plus long tienne sur une ligne (jamais « JÉSUS-CHRIS / T »).
            val frame = 0.022f * cover.fillets + 0.04f
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
                base.merge(TextStyle(brush = Brush.linearGradient(listOf(GoldDeep, GoldBright, GoldMid, GoldDeep)), shadow = Shadow(GoldShadow.copy(alpha = 0.8f), Offset(0f, 1.6f), 1.2f)))
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

/** La tranche des pages, côté libre et en pied : dorée, rouge ou naturelle ; plus fine est la page, plus serrées les lignes. */
private fun DrawScope.drawPageBlock(board: Rect, thickness: Float, identity: BookIdentity) {
    val block = Rect(board.right - thickness * 0.5f, board.top + thickness * 0.25f, board.right + thickness * 0.95f, board.bottom + thickness * 0.3f)
    val edge = when (identity.edges) {
        EdgeFinish.GILT -> Brush.verticalGradient(listOf(GoldDeep, GoldBright, GoldMid, GoldDeep, GoldMid), startY = block.top, endY = block.bottom)
        EdgeFinish.RED -> Brush.verticalGradient(listOf(Color(0xFF7E1A16), Color(0xFFB0332B), Color(0xFF8A201B)), startY = block.top, endY = block.bottom)
        EdgeFinish.PLAIN -> Brush.verticalGradient(listOf(lerp(identity.paper.tint, Color.Black, 0.08f), identity.paper.tint, lerp(identity.paper.tint, Color.Black, 0.12f)), startY = block.top, endY = block.bottom)
    }
    // Plat arrière, puis bloc des pages.
    drawRoundRect(lerp(identity.cover.leather, Color.Black, 0.35f), topLeft = Offset(block.left, block.top + thickness * 0.1f), size = Size(block.width + thickness * 0.12f, block.height), cornerRadius = CornerRadius(4f))
    drawRect(edge, topLeft = block.topLeft, size = block.size)
    val step = identity.paper.weight.stepDp * density * 0.9f
    var x = block.left + step
    while (x < block.right) {
        drawLine(Color.Black.copy(alpha = 0.10f), Offset(x, block.top), Offset(x, block.bottom), 0.6f)
        x += step
    }
}

// ── Décor ───────────────────────────────────────────────────────────────────

private fun DrawScope.drawTooling(board: Rect, cover: CoverStyle, importance: Int) {
    val left = board.left + board.width * 0.13f
    val inset = board.width * 0.07f
    val gap = board.width * 0.022f
    for (i in 0 until cover.fillets) {
        val r = Rect(left + gap * i, board.top + inset + gap * i, board.right - inset - gap * i, board.bottom - inset - gap * i)
        tool(cover) { brush, width -> drawRect(brush, topLeft = r.topLeft, size = r.size, style = Stroke(width)) }
    }
    if (cover.corners && cover.fillets > 0) {
        val k = cover.fillets
        val r = Rect(left + gap * k + gap, board.top + inset + gap * k + gap, board.right - inset - gap * k - gap, board.bottom - inset - gap * k - gap)
        val s = board.width * (0.035f + 0.01f * importance)
        listOf(r.topLeft to Offset(1f, 1f), Offset(r.right, r.top) to Offset(-1f, 1f), Offset(r.right, r.bottom) to Offset(-1f, -1f), Offset(r.left, r.bottom) to Offset(1f, -1f))
            .forEach { (corner, dir) -> fleuron(corner, dir, s, cover) }
    }
}

/** Un fer : or (dégradé et ombre) ou froid (creux sombre et arête claire). Imprimé : l'encre seule. */
private fun DrawScope.tool(cover: CoverStyle, draw: DrawScope.(Brush, Float) -> Unit) {
    val w = 1.4f * density
    when (cover.tooling) {
        Tooling.GOLD -> {
            translate(0f, 1f) { draw(androidx.compose.ui.graphics.SolidColor(GoldShadow.copy(alpha = 0.6f)), w) }
            draw(Brush.linearGradient(listOf(GoldDeep, GoldBright, GoldMid, GoldDeep), start = Offset.Zero, end = Offset(size.width, size.height)), w)
        }
        Tooling.BLIND -> {
            translate(0f, 1f) { draw(androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.14f)), w) }
            draw(androidx.compose.ui.graphics.SolidColor(Color.Black.copy(alpha = 0.45f)), w)
        }
        Tooling.PRINTED -> draw(androidx.compose.ui.graphics.SolidColor(cover.ink), w * 0.7f)
    }
}

/** Fleuron d'angle : un quadrilobe plein, posé dans l'angle du cadre, et un point vers le centre. */
private fun DrawScope.fleuron(corner: Offset, dir: Offset, s: Float, cover: CoverStyle) {
    val c = Offset(corner.x + dir.x * s * 0.55f, corner.y + dir.y * s * 0.55f)
    val r = s * 0.2f
    val path = Path().apply {
        addOval(Rect(Offset(c.x - r * 1.1f, c.y), r))
        addOval(Rect(Offset(c.x + r * 1.1f, c.y), r))
        addOval(Rect(Offset(c.x, c.y - r * 1.1f), r))
        addOval(Rect(Offset(c.x, c.y + r * 1.1f), r))
        addOval(Rect(Offset(c.x + dir.x * s * 0.62f, c.y + dir.y * s * 0.62f), r * 0.45f))
    }
    tool(cover) { brush, _ -> drawPath(path, brush) }
}

private fun DrawScope.drawMotif(board: Rect, cover: CoverStyle) {
    val cx = board.left + board.width * 0.075f + (board.width * 0.925f) / 2f
    val cy = board.top + board.height * 0.30f
    val h = board.width * 0.22f
    val path = when (cover.motif) {
        CoverMotif.CROSS -> crossPath(Offset(cx, cy), h)
        CoverMotif.CHI_RHO -> chiRhoPath(Offset(cx, cy), h)
        CoverMotif.IHS, CoverMotif.NONE -> return // IHS : composé en lettres, au-dessus
    }
    when (cover.motifTooling) {
        Tooling.GOLD -> {
            translate(0f, 1.5f) { drawPath(path, GoldShadow.copy(alpha = 0.7f)) }
            drawPath(path, Brush.linearGradient(listOf(GoldDeep, GoldBright, GoldMid, GoldDeep), start = Offset(cx - h / 2, cy - h / 2), end = Offset(cx + h / 2, cy + h / 2)))
        }
        Tooling.BLIND -> {
            translate(0f, 1.2f) { drawPath(path, Color.White.copy(alpha = 0.14f)) }
            drawPath(path, Color.Black.copy(alpha = 0.40f))
        }
        Tooling.PRINTED -> drawPath(path, cover.ink)
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
private fun chiRhoPath(c: Offset, h: Float): Path {
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
    CoverMaterial.CLOTH, CoverMaterial.WRAPPER -> null
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
