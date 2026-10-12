package app.paroissien.core.designsystem

import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.VertexMode
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import app.paroissien.core.liturgy.LiturgicalColor
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Les six rubans de satin d'un livre (rapport 09), à la couleur liturgique, coupés en biais et brodés d'une croix
 * dorée près de l'extrémité, comme les signets des missels. Le nom de la couleur est toujours écrit à côté
 * (panneau des rubans, signets, TalkBack) : la couleur n'est jamais le seul signal.
 */
data class RibbonStyle(val color: LiturgicalColor, val label: String) {
    companion object {
        val slots = listOf(
            RibbonStyle(LiturgicalColor.RED, "rouge"),
            RibbonStyle(LiturgicalColor.GREEN, "vert"),
            RibbonStyle(LiturgicalColor.VIOLET, "violet"),
            RibbonStyle(LiturgicalColor.WHITE, "blanc"),
            RibbonStyle(LiturgicalColor.ROSE, "rose"),
            RibbonStyle(LiturgicalColor.BLACK, "noir"),
        )

        fun of(slot: Int) = slots[slot.coerceIn(0, slots.lastIndex)]
    }
}

private val GoldDark = Color(0xFF8A6A1C)
private val Gold = Color(0xFFD8B65A)
private val GoldLight = Color(0xFFF3DE96)

/**
 * Un ruban vertical de [size] posé en [topLeft]. Le satin tient à trois choses : un reflet clair un peu décentré,
 * des bords plus sombres là où le tissu tourne, et de longs plis doux. L'extrémité est coupée en biais (le côté
 * gauche descend le plus bas), une croix dorée brodée juste au-dessus.
 */
fun DrawScope.drawRibbon(style: RibbonStyle, dark: Boolean, topLeft: Offset, size: Size) {
    val tokens = liturgicalTokens(style.color, dark)
    val w = size.width
    val h = size.height
    val x = topLeft.x
    val y = topLeft.y
    val bias = (w * 0.9f).coerceAtMost(h * 0.4f)
    val bottom = y + h
    val path = Path().apply {
        moveTo(x, y)
        lineTo(x + w, y)
        lineTo(x + w, bottom - bias)
        lineTo(x, bottom)
        close()
    }
    // Ombre portée très douce sur le papier.
    drawPath(path, Color.Black.copy(alpha = if (dark) 0.35f else 0.14f), style = Stroke(width = 2.5f))
    drawPath(path, tokens.fabric)

    clipPath(path) {
        // Le reflet du satin, en travers du ruban.
        val light = if (style.color == LiturgicalColor.BLACK) 0.22f else if (style.color == LiturgicalColor.WHITE) 0.55f else 0.32f
        drawRect(
            Brush.horizontalGradient(
                0f to Color.Black.copy(alpha = 0.22f),
                0.16f to Color.Transparent,
                0.34f to Color.White.copy(alpha = light),
                0.46f to Color.White.copy(alpha = light * 0.35f),
                0.72f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.26f),
                startX = x, endX = x + w,
            ),
            topLeft = topLeft, size = size,
        )
        // De longs plis doux : le tissu n'est jamais tout à fait plat.
        if (h > w * 3) {
            val fold = Color.Black.copy(alpha = if (dark) 0.16f else 0.10f)
            val shine = Color.White.copy(alpha = if (dark) 0.06f else 0.10f)
            drawRect(
                Brush.verticalGradient(
                    0f to Color.Transparent, 0.22f to shine, 0.38f to fold, 0.55f to Color.Transparent,
                    0.7f to shine, 0.86f to fold, 1f to Color.Transparent,
                    startY = y, endY = bottom,
                ),
                topLeft = topLeft, size = size,
            )
        }
        // Lisières : un fil plus sombre de chaque côté.
        val edge = Color.Black.copy(alpha = if (dark) 0.35f else 0.18f)
        val e = (w * 0.05f).coerceIn(0.6f, 2f)
        drawRect(edge, Offset(x, y), Size(e, h))
        drawRect(edge, Offset(x + w - e, y), Size(e, h))
    }
    tokens.trim?.let { drawPath(path, it, style = Stroke(width = 1.2f)) }
    drawCross(Offset(x + w / 2, bottom - bias - w * 0.62f), w)
}

/** Colonnes du maillage dans la largeur (0 = lisière gauche), et leur relief : lisières sombres, reflet décentré. */
private val Across = floatArrayOf(0f, 0.05f, 0.16f, 0.34f, 0.46f, 0.72f, 0.95f, 1f)
private val AcrossLight = floatArrayOf(-0.36f, -0.14f, 0f, 1f, 0.42f, 0f, -0.16f, -0.38f)
private val Sheen = booleanArrayOf(false, false, false, true, true, false, false, false)

/**
 * Un ruban couché sur la page le long de [spine], sa ligne médiane, de la tête du livre vers l'extrémité. Le satin
 * ondule légèrement : dans la longueur, le reflet s'allume et s'éteint au gré de longues ondes douces, le ruban serpente
 * à peine et sa largeur respire comme un tissu qui se tord un peu ; dans la largeur, lisières sombres et reflet
 * décentré. [ripple] (0 à 1) règle la force des ondulations et [phase] les fait glisser, quand on déplace le ruban.
 * Une ombre douce le détache du papier ; même coupe en biais et même croix brodée que [drawRibbon].
 */
fun DrawScope.drawRibbonAlong(style: RibbonStyle, dark: Boolean, spine: List<Offset>, width: Float, ripple: Float = 1f, phase: Float = 0f) {
    if (spine.size < 2 || width <= 0f) return
    val line = resample(spine, (width / 5f).coerceIn(1.5f, 6f))
    val n = line.size
    if (n < 2) return
    val dist = FloatArray(n)
    for (i in 1 until n) dist[i] = dist[i - 1] + (line[i] - line[i - 1]).getDistance()
    val length = dist[n - 1]
    val tangents = Array(n) { i -> unit(line[min(i + 2, n - 1)] - line[max(i - 2, 0)]) }

    // Le point et la direction de la ligne médiane à la distance s, avec le léger serpentement du tissu.
    val meander = width * 0.07f * ripple
    val waveA = (2 * PI / (width * 9f)).toFloat()
    val waveB = (2 * PI / (width * 3.7f)).toFloat()
    fun frame(s: Float): Pair<Offset, Offset> {
        val k = dist.binarySearch(s).let { if (it >= 0) it else (-it - 2) }.coerceIn(0, n - 2)
        val f = ((s - dist[k]) / (dist[k + 1] - dist[k]).coerceAtLeast(1e-3f)).coerceIn(0f, 1f)
        val t = unit(tangents[k] * (1 - f) + tangents[k + 1] * f)
        val normal = Offset(-t.y, t.x)
        val p = line[k] * (1 - f) + line[k + 1] * f + normal * (meander * sin(s * waveA * 0.55f + phase * 0.7f))
        return p to t
    }

    val tokens = liturgicalTokens(style.color, dark)
    val highlight = when (style.color) { LiturgicalColor.BLACK -> 0.24f; LiturgicalColor.WHITE -> 0.5f; else -> 0.34f }
    fun shade(s: Float, j: Int): Color {
        val wave = ripple * (0.62f * cos(s * waveA + phase) + 0.38f * cos(s * waveB + 1.7f + phase * 1.6f))
        val relief = AcrossLight[j]
        var light = if (Sheen[j]) relief * highlight * (0.7f + 0.6f * wave) else relief * (1f - 0.3f * wave)
        light += 0.13f * wave
        return if (light >= 0f) lerp(tokens.fabric, Color.White, light.coerceAtMost(0.8f)) else lerp(tokens.fabric, Color.Black, (-light).coerceAtMost(0.8f))
    }

    // Le maillage : une rangée de sommets tous les quelques pixels, puis la coupe en biais (la lisière gauche descend
    // le plus bas). Les couleurs s'interpolent d'un sommet à l'autre : ni bande ni couture.
    val bias = (width * 0.9f).coerceAtMost(length * 0.4f)
    val cut = length - bias
    val m = Across.size
    val positions = ArrayList<Offset>()
    val colors = ArrayList<Color>()
    fun row(at: (Int) -> Float) {
        for (j in 0 until m) {
            val s = at(j)
            val (p, t) = frame(s)
            val breath = 1f + 0.035f * ripple * sin(s * waveB * 0.8f + phase)
            positions += p + Offset(-t.y, t.x) * ((0.5f - Across[j]) * width * breath)
            colors += shade(s, j)
        }
    }
    for (i in 0 until n) if (dist[i] < cut) row { dist[i] }
    row { j -> length - bias * Across[j] }
    val rows = positions.size / m
    val indices = ArrayList<Int>((rows - 1) * (m - 1) * 6)
    for (r in 0 until rows - 1) for (j in 0 until m - 1) {
        val a = r * m + j
        indices += a; indices += a + m; indices += a + 1
        indices += a + 1; indices += a + m; indices += a + m + 1
    }
    val outline = Path().apply {
        moveTo(positions[0].x, positions[0].y)
        for (r in 1 until rows) lineTo(positions[r * m].x, positions[r * m].y)
        for (r in rows - 1 downTo 0) lineTo(positions[r * m + m - 1].x, positions[r * m + m - 1].y)
        close()
    }

    // Ombre portée : la lumière vient d'en haut à gauche.
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.Black.copy(alpha = if (dark) 0.5f else 0.3f).toArgb()
            maskFilter = BlurMaskFilter(width * 0.16f + 1f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.nativeCanvas.save()
        canvas.nativeCanvas.translate(width * 0.06f, width * 0.12f)
        canvas.nativeCanvas.drawPath(outline.asAndroidPath(), paint)
        canvas.nativeCanvas.restore()
    }
    drawContext.canvas.drawVertices(Vertices(VertexMode.Triangles, positions, positions, colors, indices), BlendMode.Modulate, Paint().apply { color = Color.White })
    // Les lisières, d'un trait lissé : elles effacent aussi l'escalier des bords du maillage.
    val selvedge = lerp(tokens.fabric, Color.Black, if (dark) 0.5f else 0.38f)
    drawPath(outline, selvedge.copy(alpha = 0.85f), style = Stroke(width = (width * 0.04f).coerceIn(0.8f, 1.6f), join = StrokeJoin.Round))
    tokens.trim?.let { drawPath(outline, it, style = Stroke(width = 1.2f, join = StrokeJoin.Round)) }

    val (center, t) = frame(cut - width * 0.62f)
    rotate(degrees = (atan2(t.y, t.x) * 180 / PI).toFloat() - 90f, pivot = center) { drawCross(center, width) }
}

private fun unit(v: Offset): Offset {
    val d = v.getDistance()
    return if (d < 1e-4f) Offset(0f, 1f) else v / d
}

/** La polyligne rééchantillonnée à pas régulier, extrémités comprises. */
private fun resample(points: List<Offset>, step: Float): List<Offset> {
    val out = ArrayList<Offset>()
    out += points[0]
    var carry = 0f
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        val d = (b - a).getDistance()
        var s = step - carry
        while (s <= d) {
            out += a + (b - a) * (s / d)
            s += step
        }
        carry = d - (s - step)
    }
    if ((out.last() - points.last()).getDistance() > step * 0.25f) out += points.last()
    return out
}

/** Une croix pattée dorée, brodée : dégradé d'or et fin contour plus sombre. Rien si le ruban est trop étroit. */
private fun DrawScope.drawCross(center: Offset, ribbonWidth: Float) {
    val height = ribbonWidth * 0.78f
    if (height < 9f) return
    val width = height * 0.72f
    val bar = height * 0.16f
    val flare = bar * 0.85f
    val cx = center.x
    val cy = center.y
    val top = cy - height / 2
    val bot = cy + height / 2
    val arm = cy - height * 0.14f // la traverse, un peu au-dessus du milieu
    val path = Path().apply {
        // Branche du haut, évasée.
        moveTo(cx - bar / 2, arm - bar / 2)
        lineTo(cx - bar / 2 - flare / 2, top)
        lineTo(cx + bar / 2 + flare / 2, top)
        lineTo(cx + bar / 2, arm - bar / 2)
        // Bras droit.
        lineTo(cx + width / 2, arm - bar / 2 - flare / 2)
        lineTo(cx + width / 2, arm + bar / 2 + flare / 2)
        lineTo(cx + bar / 2, arm + bar / 2)
        // Pied.
        lineTo(cx + bar / 2 + flare / 2, bot)
        lineTo(cx - bar / 2 - flare / 2, bot)
        lineTo(cx - bar / 2, arm + bar / 2)
        // Bras gauche.
        lineTo(cx - width / 2, arm + bar / 2 + flare / 2)
        lineTo(cx - width / 2, arm - bar / 2 - flare / 2)
        close()
    }
    drawPath(path, Brush.linearGradient(listOf(GoldDark, GoldLight, Gold, GoldDark), start = Offset(cx - width / 2, top), end = Offset(cx + width / 2, bot)))
    drawPath(path, GoldDark.copy(alpha = 0.7f), style = Stroke(width = (height * 0.05f).coerceAtLeast(0.8f)))
}
