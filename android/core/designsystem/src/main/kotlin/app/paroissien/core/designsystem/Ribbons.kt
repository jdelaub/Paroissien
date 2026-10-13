package app.paroissien.core.designsystem

import android.graphics.BlurMaskFilter
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import app.paroissien.core.liturgy.LiturgicalColor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Le tissu des rubans (réglage, ou propre à un ruban dans l'atelier) : mat, tissé de fines côtes qui courent dans la
 * longueur ; satin brillant ; moiré, satin parcouru d'ondes comme une soie moirée ; velours, profond et sans reflet.
 */
enum class RibbonFabric { TEXTURED, SATIN, MOIRE, VELVET }

/** L'extrémité d'un ruban : coupée en biais, droite, en queue d'aronde, ou serrée d'un ferret doré. */
enum class RibbonEnd { BIAS, STRAIGHT, FORKED, FERRET }

/** Ce qui est brodé près de l'extrémité : une croix dorée, ou rien. */
enum class RibbonEmblem { CROSS, NONE }

/** Le tissu choisi, fourni à la racine de l'application d'après les réglages. */
val LocalRibbonFabric = staticCompositionLocalOf { RibbonFabric.TEXTURED }

/**
 * Les six rubans d'un livre (rapport 09), à la couleur liturgique, coupés en biais et brodés d'une croix dorée près
 * de l'extrémité, comme les signets des missels. [ribs] : le nombre de côtes du tissu mat, de 3 à 7, propre à chaque
 * ruban. Le nom de la couleur est toujours écrit à côté (panneau des rubans, signets, TalkBack) : la couleur n'est
 * jamais le seul signal.
 */
data class RibbonStyle(
    val color: LiturgicalColor,
    val label: String,
    val ribs: Int,
    /** Une teinte choisie dans l'atelier, à la place de celle de [color] (qui reste pour les noms et l'accessibilité). */
    val custom: Color? = null,
    /** Le tissu de ce ruban ; null : le réglage général. */
    val fabric: RibbonFabric? = null,
    val end: RibbonEnd = RibbonEnd.BIAS,
    val emblem: RibbonEmblem = RibbonEmblem.CROSS,
) {
    val ferret: Boolean get() = end == RibbonEnd.FERRET

    companion object {
        /** L'emplacement du ruban posé tout seul : la messe du jour, dans le missel (rapport 09, PLAN phase 5). */
        const val MASS_SLOT = 6

        /** Le ruban de la messe du jour : à la couleur liturgique du jour, coupé droit et serré d'un ferret doré. */
        fun mass(color: LiturgicalColor) = RibbonStyle(color, "messe du jour", ribs = 5, end = RibbonEnd.FERRET)

        val slots = listOf(
            RibbonStyle(LiturgicalColor.RED, "rouge", ribs = 5),
            RibbonStyle(LiturgicalColor.GREEN, "vert", ribs = 3),
            RibbonStyle(LiturgicalColor.VIOLET, "violet", ribs = 4),
            RibbonStyle(LiturgicalColor.WHITE, "blanc", ribs = 7),
            RibbonStyle(LiturgicalColor.ROSE, "rose", ribs = 6),
            RibbonStyle(LiturgicalColor.BLACK, "noir", ribs = 3),
        )

        fun of(slot: Int) = slots[slot.coerceIn(0, slots.lastIndex)]
    }
}

/** Les rubans de l'utilisateur (atelier), un par emplacement de [RibbonStyle.slots], fournis à la racine de l'app. */
val LocalRibbonSet = staticCompositionLocalOf { RibbonStyle.slots }

/** Le ruban d'un emplacement dans un jeu de rubans. */
fun List<RibbonStyle>.of(slot: Int): RibbonStyle = this[slot.coerceIn(0, lastIndex)]

/** Les couleurs d'un ruban : sa couleur liturgique, ou la teinte libre de l'atelier, éclaircie sur papier sombre. */
fun ribbonTokens(style: RibbonStyle, dark: Boolean): LiturgicalTokens {
    val custom = style.custom ?: return liturgicalTokens(style.color, dark)
    val fabric = if (dark) lerp(custom, Color.White, 0.12f) else custom
    val luminance = 0.2126f * custom.red + 0.7152f * custom.green + 0.0722f * custom.blue
    // Un ruban très clair ou très sombre se perd sur le papier : un liseré le détache, comme le blanc et le noir.
    val trim = when {
        luminance > 0.82f -> Color(0xFFB8912F)
        dark && luminance < 0.06f -> Color(0xFF5A5650)
        else -> null
    }
    return LiturgicalTokens(fabric, fabric, trim)
}

private val GoldDark = Color(0xFF8A6A1C)
private val Gold = Color(0xFFD8B65A)
private val GoldLight = Color(0xFFF3DE96)

/** Un ruban vertical de [size] posé en [topLeft] : une queue qui pend sous la page, une petite icône. */
fun DrawScope.drawRibbon(style: RibbonStyle, dark: Boolean, topLeft: Offset, size: Size, fabric: RibbonFabric = RibbonFabric.TEXTURED) {
    val x = topLeft.x + size.width / 2
    val spine = listOf(Offset(x, topLeft.y), Offset(x, topLeft.y + size.height))
    drawRibbonAlong(style, dark, spine, size.width, ripple = if (size.height > size.width * 3) 0.35f else 0f, fabric = fabric)
}

/** Colonnes du maillage dans la largeur (0 = lisière gauche). */
private val Across = floatArrayOf(0f, 0.05f, 0.16f, 0.34f, 0.46f, 0.72f, 0.95f, 1f)

/** Relief du satin dans la largeur : lisières sombres, reflet décentré (les colonnes [Sheen]). */
private val SatinLight = floatArrayOf(-0.36f, -0.14f, 0f, 1f, 0.42f, 0f, -0.16f, -0.38f)
private val Sheen = booleanArrayOf(false, false, false, true, true, false, false, false)

/** Relief du tissu mat : à peine bombé, sans reflet. */
private val MatteLight = floatArrayOf(-0.24f, -0.1f, 0.02f, 0.07f, 0.06f, 0f, -0.09f, -0.26f)

/**
 * Un ruban couché sur la page le long de [spine], sa ligne médiane, de la tête du livre vers l'extrémité. Il ondule
 * légèrement : le ruban serpente à peine, sa largeur respire comme un tissu qui se tord un peu, et la lumière suit de
 * longues ondes douces. [ripple] (0 à 1) règle la force des ondulations et [phase] les fait glisser, quand on déplace
 * le ruban. Le [fabric] mat est tissé : un grain fin en travers, et ses côtes ([RibbonStyle.ribs]) qui courent dans
 * la longueur, éclairées d'un côté, ombrées de l'autre ; le satin a ses lisières sombres et son reflet décentré qui
 * s'allume et s'éteint. Une ombre douce le détache du papier ; coupe en biais, croix brodée.
 */
fun DrawScope.drawRibbonAlong(
    style: RibbonStyle,
    dark: Boolean,
    spine: List<Offset>,
    width: Float,
    ripple: Float = 1f,
    phase: Float = 0f,
    fabric: RibbonFabric = RibbonFabric.TEXTURED,
) {
    if (spine.size < 2 || width <= 0f) return
    val cloth = style.fabric ?: fabric
    val satin = cloth == RibbonFabric.SATIN || cloth == RibbonFabric.MOIRE
    val moire = cloth == RibbonFabric.MOIRE
    val velvet = cloth == RibbonFabric.VELVET
    // Le tissu mat a un rang de sommets à chaque duite, ou presque : c'est ce qui fait son grain.
    val line = resample(spine, if (satin) (width / 5f).coerceIn(1.5f, 6f) else (width / 12f).coerceIn(1.2f, 2.5f))
    val n = line.size
    if (n < 2) return
    val dist = FloatArray(n)
    for (i in 1 until n) dist[i] = dist[i - 1] + (line[i] - line[i - 1]).getDistance()
    val length = dist[n - 1]
    val reach = (width * 0.4f / (length / n)).toInt().coerceAtLeast(2)
    val tangents = Array(n) { i -> unit(line[min(i + reach, n - 1)] - line[max(i - reach, 0)]) }

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

    val tokens = ribbonTokens(style, dark)
    val waveM = (2 * PI / (width * 1.6f)).toFloat()
    val highlight = when (style.color) { LiturgicalColor.BLACK -> 0.24f; LiturgicalColor.WHITE -> 0.5f; else -> 0.34f }
    fun shade(s: Float, j: Int, r: Int): Color {
        val wave = ripple * (0.62f * cos(s * waveA + phase) + 0.38f * cos(s * waveB + 1.7f + phase * 1.6f))
        val light = if (velvet) {
            // Velours : un poil serré qui boit la lumière ; les bords s'enfoncent, le milieu luit à peine.
            val u = Across[j]
            -0.3f * (2 * u - 1) * (2 * u - 1) + 0.06f + 0.06f * wave + 0.03f * grain(r * 17 + j)
        } else if (satin) {
            val relief = SatinLight[j]
            val base = (if (Sheen[j]) relief * highlight * (0.7f + 0.6f * wave) else relief * (1f - 0.3f * wave)) + 0.13f * wave
            // Moiré : des ondes de lumière en travers, comme le grain d'eau d'une soie moirée.
            if (moire) {
                // Les lignes d'eau : de fines crêtes claires (une sinusoïde élevée à une puissance paire), qui ondulent.
                val m = sin(s * waveM * 1.6f + Across[j] * 7f + 2.6f * sin(s * waveA * 1.1f + j * 0.8f))
                val ridge = m * m * m * m
                base + 0.22f * ridge - 0.06f
            } else base
        } else {
            // Mat : la lumière diffuse suit à peine les ondulations ; le grain change d'une duite à l'autre.
            MatteLight[j] + 0.09f * wave + 0.035f * grain(r) + 0.025f * grain(r * 31 + j)
        }
        return if (light >= 0f) lerp(tokens.fabric, Color.White, light.coerceAtMost(0.8f)) else lerp(tokens.fabric, Color.Black, (-light).coerceAtMost(0.8f))
    }

    // Le maillage : une rangée de sommets tous les quelques pixels, puis la coupe en biais (la lisière gauche descend
    // le plus bas), ou droite sous un ferret. Les couleurs s'interpolent d'un sommet à l'autre : ni bande ni couture.
    // Sous un ferret, le tissu s'arrête à l'intérieur de l'embout : il ne dépasse pas de sa pointe resserrée.
    val ferretLength = if (style.ferret) (width * 1.35f).coerceAtMost(length * 0.3f) else 0f
    val end = length - ferretLength * 0.7f
    val bias = when (style.end) {
        RibbonEnd.BIAS -> (width * 0.9f).coerceAtMost(length * 0.4f)
        RibbonEnd.FORKED -> (width * 0.7f).coerceAtMost(length * 0.3f)
        else -> 0f
    }
    val cut = end - bias
    val m = Across.size
    val positions = ArrayList<Offset>()
    val colors = ArrayList<Color>()
    fun row(r: Int, at: (Int) -> Float) {
        for (j in 0 until m) {
            val s = at(j)
            val (p, t) = frame(s)
            val breath = 1f + 0.035f * ripple * sin(s * waveB * 0.8f + phase)
            positions += p + Offset(-t.y, t.x) * ((0.5f - Across[j]) * width * breath)
            colors += shade(s, j, r)
        }
    }
    var r = 0
    for (i in 0 until n) if (dist[i] < cut) row(r++) { dist[i] }
    // La dernière rangée dessine la coupe : en biais, ou en queue d'aronde (le milieu remonte, les pointes descendent).
    row(r) { j -> if (style.end == RibbonEnd.FORKED) end - bias * (1f - abs(1f - 2f * Across[j])) else end - bias * Across[j] }
    val rows = positions.size / m
    val indices = ArrayList<Int>((rows - 1) * (m - 1) * 6)
    for (q in 0 until rows - 1) for (j in 0 until m - 1) {
        val a = q * m + j
        indices += a; indices += a + m; indices += a + 1
        indices += a + 1; indices += a + m; indices += a + m + 1
    }
    val outline = Path().apply {
        moveTo(positions[0].x, positions[0].y)
        for (q in 1 until rows) lineTo(positions[q * m].x, positions[q * m].y)
        for (j in 1 until m) lineTo(positions[(rows - 1) * m + j].x, positions[(rows - 1) * m + j].y)
        for (q in rows - 2 downTo 0) lineTo(positions[q * m + m - 1].x, positions[q * m + m - 1].y)
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

    // Les côtes du tissu mat, en long : un fil de lumière du côté de la lisière gauche, un fil d'ombre de l'autre.
    val ribs = style.ribs
    if (!satin && !velvet && width / (ribs + 1) >= 3f) {
        val rib = (width * 0.022f).coerceIn(0.7f, 1.4f)
        val lit = Color.White.copy(alpha = if (dark) 0.08f else 0.16f)
        val shaded = Color.Black.copy(alpha = if (dark) 0.34f else 0.2f)
        for (k in 1..ribs) {
            val u = k / (ribs + 1f)
            val j = Across.indexOfLast { it <= u }.coerceIn(0, m - 2)
            val f = (u - Across[j]) / (Across[j + 1] - Across[j])
            val light = Path()
            val shadow = Path()
            for (q in 0 until rows) {
                val a = positions[q * m + j]
                val p = a + (positions[q * m + j + 1] - a) * f
                val towardsLeft = unit(positions[q * m] - positions[q * m + m - 1]) * (rib * 0.6f)
                val pl = p + towardsLeft
                val ps = p - towardsLeft
                if (q == 0) { light.moveTo(pl.x, pl.y); shadow.moveTo(ps.x, ps.y) } else { light.lineTo(pl.x, pl.y); shadow.lineTo(ps.x, ps.y) }
            }
            drawPath(shadow, shaded, style = Stroke(width = rib, join = StrokeJoin.Round))
            drawPath(light, lit, style = Stroke(width = rib, join = StrokeJoin.Round))
        }
    }

    // Les lisières, d'un trait lissé : elles effacent aussi l'escalier des bords du maillage.
    val selvedge = lerp(tokens.fabric, Color.Black, if (dark) 0.5f else 0.38f)
    drawPath(outline, selvedge.copy(alpha = 0.85f), style = Stroke(width = (width * 0.04f).coerceIn(0.8f, 1.6f), join = StrokeJoin.Round))
    tokens.trim?.let { drawPath(outline, it, style = Stroke(width = 1.2f, join = StrokeJoin.Round)) }

    val (center, t) = frame(cut - ferretLength * 0.3f - width * 0.62f)
    if (style.emblem == RibbonEmblem.CROSS) rotate(degrees = (atan2(t.y, t.x) * 180 / PI).toFloat() - 90f, pivot = center) { drawCross(center, width) }
    if (style.ferret) {
        val (tip, dir) = frame(length)
        rotate(degrees = (atan2(dir.y, dir.x) * 180 / PI).toFloat() - 90f, pivot = tip) { drawFerret(tip, width, ferretLength) }
    }
}

/**
 * Le ferret : un embout de laiton doré serti sur l'extrémité du ruban, qui se resserre un peu vers la pointe, avec
 * deux bagues gravées. Dessiné la pointe en [tip], le ruban au-dessus.
 */
private fun DrawScope.drawFerret(tip: Offset, ribbonWidth: Float, length: Float) {
    if (length < 4f) return
    val top = tip.y - length
    val wTop = ribbonWidth * 1.04f
    val wTip = ribbonWidth * 0.52f
    val body = Path().apply {
        moveTo(tip.x - wTop / 2, top)
        lineTo(tip.x + wTop / 2, top)
        lineTo(tip.x + wTip / 2, tip.y - length * 0.12f)
        quadraticTo(tip.x, tip.y + length * 0.05f, tip.x - wTip / 2, tip.y - length * 0.12f)
        close()
    }
    val metal = Brush.horizontalGradient(listOf(GoldDark, Gold, GoldLight, Gold, GoldDark), startX = tip.x - wTop / 2, endX = tip.x + wTop / 2)
    drawPath(body, metal)
    drawPath(body, GoldDark, style = Stroke(width = (ribbonWidth * 0.05f).coerceAtLeast(0.8f)))
    val ring = (length * 0.07f).coerceAtLeast(0.8f)
    for (f in floatArrayOf(0.16f, 0.3f)) {
        val y = top + length * f
        drawLine(GoldDark.copy(alpha = 0.8f), Offset(tip.x - wTop / 2 * 0.98f, y), Offset(tip.x + wTop / 2 * 0.98f, y), strokeWidth = ring)
    }
}

/** Un bruit fixe entre -0,5 et 0,5, tiré de [i] : le grain du tissu, le même à chaque dessin. */
private fun grain(i: Int): Float {
    var h = i * 374761393
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)) and 0xFFFF) / 65535f - 0.5f
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
