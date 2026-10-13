package app.paroissien.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * La tranche d'un livre (atelier, 13/10/2026), dessinée dans [rect] : la couverture fermée (bloc des pages à droite)
 * et la page ouverte (bande au bord libre) la partagent. [vertical] : la tranche court de haut en bas (le bord libre
 * d'une page) ; sinon de gauche à droite (la tranche vue de face). Les feuillets font des lignes parallèles à la
 * tranche, espacées de [leaf] px ; [plain] est la teinte d'une tranche naturelle, [plainLine] la ligne de ses feuillets.
 */
fun DrawScope.drawPageEdge(
    rect: Rect,
    finish: EdgeFinish,
    metal: Metal,
    tooling: EdgeTooling,
    plain: Color,
    plainLine: Color,
    leaf: Float,
    vertical: Boolean = true,
) {
    if (rect.width <= 0f || rect.height <= 0f) return
    val along0 = rect.topLeft
    val along1 = if (vertical) Offset(rect.left, rect.bottom) else Offset(rect.right, rect.top)
    clipRect(rect.left, rect.top, rect.right, rect.bottom) {
        when (finish) {
            EdgeFinish.GILT -> drawRect(metal.edge(along0, along1), rect.topLeft, rect.size)
            EdgeFinish.RED_UNDER_GOLD -> {
                drawRect(metal.edge(along0, along1), rect.topLeft, rect.size)
                // le rouge sous l'or affleure entre les feuillets et dans les creux
                drawRect(Color(0xFF9A2A1E).copy(alpha = 0.28f), rect.topLeft, rect.size)
            }
            EdgeFinish.RED -> drawRect(dyed(Color(0xFF7E1A16), Color(0xFFB0332B), along0, along1), rect.topLeft, rect.size)
            EdgeFinish.RED_ORANGE -> drawRect(dyed(Color(0xFF9C3418), Color(0xFFD9653A), along0, along1), rect.topLeft, rect.size)
            EdgeFinish.BLUE -> drawRect(dyed(Color(0xFF1F3554), Color(0xFF4A6B96), along0, along1), rect.topLeft, rect.size)
            EdgeFinish.GREEN -> drawRect(dyed(Color(0xFF233F2C), Color(0xFF4F7A57), along0, along1), rect.topLeft, rect.size)
            EdgeFinish.YELLOW -> drawRect(dyed(Color(0xFFA9822A), Color(0xFFE2C25C), along0, along1), rect.topLeft, rect.size)
            EdgeFinish.SPRINKLED -> sprinkled(rect)
            EdgeFinish.MARBLED -> marbled(rect, vertical)
            EdgeFinish.PLAIN -> drawRect(plain, rect.topLeft, rect.size)
        }
        val gilt = finish == EdgeFinish.GILT || finish == EdgeFinish.RED_UNDER_GOLD
        if (gilt && tooling != EdgeTooling.NONE) chased(rect, metal, tooling, vertical)
        // les feuillets
        val line = when (finish) {
            EdgeFinish.PLAIN -> plainLine
            EdgeFinish.RED_UNDER_GOLD -> Color(0xFF7E1A16).copy(alpha = 0.30f)
            else -> Color.Black.copy(alpha = 0.10f)
        }
        val across = if (vertical) rect.width else rect.height
        var k = leaf / 2
        while (k < across) {
            if (vertical) drawLine(line, Offset(rect.left + k, rect.top), Offset(rect.left + k, rect.bottom), 0.6f)
            else drawLine(line, Offset(rect.left, rect.top + k), Offset(rect.right, rect.top + k), 0.6f)
            k += leaf
        }
    }
}

/** Une tranche teinte : la couleur plus claire au milieu, où le bloc arrondi prend la lumière. */
private fun dyed(dark: Color, light: Color, a: Offset, b: Offset) =
    Brush.linearGradient(listOf(dark, light, lerp(dark, light, 0.6f), dark), start = a, end = b)

/** Tranche jaspée : un fond ocre clair moucheté de rouge brun, au pinceau secoué. */
private fun DrawScope.sprinkled(r: Rect) {
    drawRect(Color(0xFFD9C29A), r.topLeft, r.size)
    val random = Random(7)
    val n = ((r.width * r.height) / 9f).toInt().coerceIn(40, 4000)
    repeat(n) {
        val x = r.left + random.nextFloat() * r.width
        val y = r.top + random.nextFloat() * r.height
        val red = random.nextFloat() < 0.8f
        drawCircle(if (red) Color(0xFF8E2A1C).copy(alpha = 0.75f) else Color(0xFF3B2A1A).copy(alpha = 0.6f), 0.5f + random.nextFloat() * 1.1f, Offset(x, y))
    }
}

/**
 * Tranche marbrée, façon « caillouté » : des taches de bleu, de rouge et d'ocre serrées les unes contre les autres,
 * cernées de veines sombres, que le bain de marbrure dépose sur la tranche ; quelques veines claires les traversent.
 */
private fun DrawScope.marbled(r: Rect, vertical: Boolean) {
    drawRect(Color(0xFFE3D6B8), r.topLeft, r.size)
    val inks = listOf(Color(0xFF3E5A82), Color(0xFF9E4434), Color(0xFFC4A15A), Color(0xFF3E5A82), Color(0xFF5E7553), Color(0xFF7A3F44))
    val random = Random(11)
    val length = if (vertical) r.height else r.width
    val across = if (vertical) r.width else r.height
    val size = (minOf(length / 40f, across * 0.35f)).coerceIn(1.5f, 10f)
    val n = ((length * across) / (size * size) * 0.9f).toInt().coerceIn(20, 2500)
    repeat(n) {
        val a = random.nextFloat() * length
        val c = random.nextFloat() * across
        // taches étirées le long de la tranche, comme tirées par le peigne
        val ra = size * (0.7f + random.nextFloat() * 1.1f)
        val rc = ra * (0.30f + random.nextFloat() * 0.3f)
        val color = inks[random.nextInt(inks.size)]
        val center = if (vertical) Offset(r.left + c, r.top + a) else Offset(r.left + a, r.top + c)
        val oval = if (vertical) Size(rc * 2, ra * 2) else Size(ra * 2, rc * 2)
        drawOval(Color(0xFF2A2018).copy(alpha = 0.35f), center - Offset(oval.width / 2 + 0.4f, oval.height / 2 + 0.4f), Size(oval.width + 0.8f, oval.height + 0.8f))
        drawOval(color.copy(alpha = 0.85f), center - Offset(oval.width / 2, oval.height / 2), oval)
    }
    // veines claires, peignées le long de la tranche
    for (k in 0 until 3) {
        val path = Path()
        val c0 = across * (0.2f + 0.3f * k)
        val steps = 40
        for (i in 0..steps) {
            val a = length * i / steps
            val c = c0 + sin(a / length * 2 * PI.toFloat() * (3 + k)) * across * 0.12f
            val pt = if (vertical) Offset(r.left + c, r.top + a) else Offset(r.left + a, r.top + c)
            if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        drawPath(path, Color(0xFFF1E6CC).copy(alpha = 0.45f), style = Stroke((across * 0.02f).coerceAtLeast(0.5f)))
    }
}

/** Ciselure : le fer creuse l'or (trait sombre) et en relève l'arête (trait clair). */
private fun DrawScope.chased(r: Rect, metal: Metal, tooling: EdgeTooling, vertical: Boolean) {
    val length = if (vertical) r.height else r.width
    val across = if (vertical) r.width else r.height
    val dark = metal.shadow.copy(alpha = 0.75f)
    val light = metal.bright.copy(alpha = 0.85f)
    val line = (across * 0.045f).coerceIn(0.7f, 3f)
    val cell = (across * 0.9f).coerceIn(3f, 60f)
    fun at(a: Float, c: Float) = if (vertical) Offset(r.left + c, r.top + a) else Offset(r.left + a, r.top + c)
    // le fer creuse (trait sombre), l'arête relevée accroche la lumière (trait clair décalé)
    fun cut(path: Path) {
        drawPath(path, light, style = Stroke(line * 0.8f))
        translate(-line * 0.5f, -line * 0.5f) { drawPath(path, dark, style = Stroke(line)) }
    }
    when (tooling) {
        EdgeTooling.POINTILLE -> {
            var a = cell * 0.35f
            var row = 0
            while (a < length) {
                for (c in listOf(0.22f, 0.5f, 0.78f)) {
                    val cc = across * c + if (row % 2 == 0) 0f else across * 0.14f
                    if (cc > across * 0.92f) continue
                    drawCircle(light, cell * 0.07f + 0.5f, at(a, cc) + Offset(0.5f, 0.5f))
                    drawCircle(dark, cell * 0.07f + 0.5f, at(a, cc))
                }
                a += cell * 0.35f
                row++
            }
        }
        EdgeTooling.LOSANGES -> {
            var a = 0f
            while (a < length) {
                val q = listOf(at(a, across / 2), at(a + cell / 2, across * 0.12f), at(a + cell, across / 2), at(a + cell / 2, across * 0.88f))
                cut(Path().apply { moveTo(q[0].x, q[0].y); q.drop(1).forEach { lineTo(it.x, it.y) }; close() })
                drawCircle(dark, line * 1.1f, at(a + cell / 2, across / 2))
                a += cell
            }
        }
        EdgeTooling.RINCEAUX -> {
            val step = cell * 1.4f
            var a = 0f
            var flip = 1f
            val c0 = across / 2
            while (a < length) {
                val p = Path()
                val first = at(a, c0)
                p.moveTo(first.x, first.y)
                for (s in 1..16) {
                    val tt = s / 16f
                    val pt = at(a + step * tt, c0 + flip * sin(tt * PI.toFloat()) * across * 0.30f)
                    p.lineTo(pt.x, pt.y)
                }
                cut(p)
                // une volute et une feuille de part et d'autre de la tige
                val curl = Path()
                for (s in 0..12) {
                    val ang = s / 12f * 2 * PI.toFloat()
                    val rr = across * 0.16f * (1 - s / 14f)
                    val pt = at(a + step * 0.5f + cos(ang) * rr, c0 - flip * across * 0.12f + sin(ang) * rr)
                    if (s == 0) curl.moveTo(pt.x, pt.y) else curl.lineTo(pt.x, pt.y)
                }
                cut(curl)
                drawCircle(dark, line * 1.3f, at(a + step * 0.25f, c0 + flip * across * 0.26f))
                a += step
                flip = -flip
            }
        }
        EdgeTooling.NONE -> Unit
    }
}

/**
 * La tranche d'un livre vue de face, en grand, pour l'atelier : le bloc des pages entre les deux plats.
 * On y juge la teinte, le métal et la ciselure.
 */
@Composable
fun BookEdgeSample(identity: BookIdentity, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val board = size.height * 0.16f
        val radius = CornerRadius(board * 0.6f)
        drawRoundRect(identity.cover.leather, Offset.Zero, size, radius)
        drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent, Color.Black.copy(alpha = 0.35f))), Offset.Zero, size, radius)
        val inset = size.width * 0.02f
        val edge = Rect(inset, board, size.width - inset, size.height - board)
        drawPageEdge(
            edge, identity.edges, identity.edgeMetal, identity.edgeTooling,
            plain = identity.paper.tint, plainLine = lerp(identity.paper.tint, Color.Black, 0.10f),
            leaf = identity.paper.weight.stepDp * density * 1.4f, vertical = false,
        )
        // ombre des plats sur le bloc
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.30f), Color.Transparent), startY = edge.top, endY = edge.top + edge.height * 0.18f), edge.topLeft, Size(edge.width, edge.height * 0.18f))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)), startY = edge.bottom - edge.height * 0.15f, endY = edge.bottom), Offset(edge.left, edge.bottom - edge.height * 0.15f), Size(edge.width, edge.height * 0.15f))
    }
}
