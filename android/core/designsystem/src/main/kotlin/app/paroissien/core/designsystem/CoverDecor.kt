package app.paroissien.core.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Les fers d'une couverture (atelier, 13/10/2026), en chemins prêts à dorer : filets, roulette, fers d'angle,
 * encadrement du motif, caissons du dos. Tout se pousse du même fer ([CoverStyle.tooling] dans [CoverStyle.metal]) ;
 * le motif central, qui peut avoir son propre fer, est à part ([motifPath]).
 */
internal class CoverDecor(val fills: List<Path>, val strokes: List<Pair<Path, Float>>)

/** Les mesures du cadre du plat, en fraction de la largeur du plat : filets, roulette, et ce qui reste au titre. */
internal object CoverFrame {
    const val LEFT = 0.13f
    const val INSET = 0.07f
    const val GAP = 0.022f
    const val ROLL = 0.05f

    /** Profondeur totale du cadre (filets et roulette), en fraction de la largeur du plat. */
    fun depth(cover: CoverStyle): Float = rings(cover, 1f).second
    fun rings(cover: CoverStyle, w: Float): Pair<List<Pair<Boolean, Float>>, Float> {
        // (roulette ?, distance au bord extérieur du cadre) ; la roulette est repérée par son milieu. La profondeur rendue
        // compte un intervalle après le dernier élément : c'est là que se posent les fers d'angle.
        val out = ArrayList<Pair<Boolean, Float>>()
        var o = 0f
        val f = cover.fillets
        if (cover.border == CoverBorder.NONE) {
            repeat(f) { out += false to o; o += GAP * w }
            return out to o
        }
        if (f >= 1) { out += false to o; o += GAP * w * 0.55f }
        out += true to (o + ROLL * w / 2)
        o += ROLL * w + GAP * w * 0.55f
        repeat((f - 1).coerceAtLeast(0)) { out += false to o; o += GAP * w }
        return out to o
    }
}

internal fun coverDecor(board: Rect, cover: CoverStyle, importance: Int): CoverDecor {
    val fills = ArrayList<Path>()
    val strokes = ArrayList<Pair<Path, Float>>()
    val w = board.width
    val outer = Rect(board.left + w * CoverFrame.LEFT, board.top + w * CoverFrame.INSET, board.right - w * CoverFrame.INSET, board.bottom - w * CoverFrame.INSET)
    val (rings, depth) = CoverFrame.rings(cover, w)
    val hair = (w * 0.0075f).coerceAtLeast(0.8f)
    for ((roll, at) in rings) {
        val r = outer.deflate(at)
        if (!roll) strokes += Path().apply { addRect(r) } to hair
        else roll(r, w * CoverFrame.ROLL, cover.border, fills, strokes, hair)
    }
    if (cover.cornerStyle != CoverCorner.NONE) {
        val r = outer.deflate(depth + w * CoverFrame.GAP)
        val s = w * (0.035f + 0.01f * importance)
        listOf(r.topLeft to Offset(1f, 1f), Offset(r.right, r.top) to Offset(-1f, 1f), Offset(r.right, r.bottom) to Offset(-1f, -1f), Offset(r.left, r.bottom) to Offset(1f, -1f))
            .forEach { (corner, dir) -> cornerTool(cover.cornerStyle, corner, dir, s, outer, fills, strokes, hair) }
    }
    if (cover.spineTooling && cover.material != CoverMaterial.WRAPPER) spine(board, cover, fills, strokes, hair)
    return CoverDecor(fills, strokes)
}

// ── Un tracé dans un repère local ──────────────────────────────────────────────────────────────────────────────────

/** Un repère : [o] l'origine, [ax] et [ay] les axes (vecteurs unitaires), [sx] et [sy] l'unité de chaque axe en px. */
private class Pen(val o: Offset, val ax: Offset, val ay: Offset, val sx: Float, val sy: Float = sx) {
    val path = Path()
    fun p(x: Float, y: Float) = Offset(o.x + ax.x * x * sx + ay.x * y * sy, o.y + ax.y * x * sx + ay.y * y * sy)
    fun move(x: Float, y: Float) = p(x, y).let { path.moveTo(it.x, it.y) }
    fun line(x: Float, y: Float) = p(x, y).let { path.lineTo(it.x, it.y) }
    fun cubic(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) {
        val a = p(x1, y1); val b = p(x2, y2); val c = p(x, y)
        path.cubicTo(a.x, a.y, b.x, b.y, c.x, c.y)
    }
    fun close() = path.close()
    /** Une ellipse en quatre arcs de Bézier, qui tourne avec le repère. */
    fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float = rx) {
        val k = 0.5523f
        move(cx + rx, cy)
        cubic(cx + rx, cy + ry * k, cx + rx * k, cy + ry, cx, cy + ry)
        cubic(cx - rx * k, cy + ry, cx - rx, cy + ry * k, cx - rx, cy)
        cubic(cx - rx, cy - ry * k, cx - rx * k, cy - ry, cx, cy - ry)
        cubic(cx + rx * k, cy - ry, cx + rx, cy - ry * k, cx + rx, cy)
        close()
    }
    fun polygon(vararg xy: Float) {
        move(xy[0], xy[1])
        var i = 2
        while (i < xy.size) { line(xy[i], xy[i + 1]); i += 2 }
        close()
    }
}

private fun pen(c: Offset, s: Float) = Pen(c, Offset(1f, 0f), Offset(0f, 1f), s)

private fun union(paths: List<Path>): Path = paths.drop(1).fold(paths.first()) { acc, p -> Path().apply { op(acc, p, PathOperation.Union) } }
private fun minus(a: Path, b: Path): Path = Path().apply { op(a, b, PathOperation.Difference) }

// ── Fleur de lys, la pièce la plus employée ────────────────────────────────────────────────────────────────────────

/** Fleur de lys héraldique, pointe vers −y, haute d'environ 1 (de −0,5 à 0,3), dans le repère [frame]. */
private fun lys(frame: () -> Pen): Path {
    val parts = ArrayList<Path>()
    frame().apply {
        move(0f, -0.5f)
        cubic(0.07f, -0.42f, 0.14f, -0.27f, 0.10f, -0.12f)
        cubic(0.085f, -0.05f, 0.07f, 0f, 0.06f, 0.05f)
        line(-0.06f, 0.05f)
        cubic(-0.07f, 0f, -0.085f, -0.05f, -0.10f, -0.12f)
        cubic(-0.14f, -0.27f, -0.07f, -0.42f, 0f, -0.5f)
        close()
        parts += path
    }
    for (m in listOf(1f, -1f)) frame().apply {
        move(0.05f * m, 0.04f)
        cubic(0.12f * m, -0.12f, 0.32f * m, -0.24f, 0.39f * m, -0.09f)
        cubic(0.42f * m, -0.01f, 0.37f * m, 0.08f, 0.29f * m, 0.08f)
        cubic(0.32f * m, 0.02f, 0.30f * m, -0.06f, 0.23f * m, -0.05f)
        cubic(0.16f * m, -0.04f, 0.11f * m, 0.02f, 0.09f * m, 0.08f)
        close()
        parts += path
    }
    frame().apply { polygon(-0.18f, 0.05f, 0.18f, 0.05f, 0.18f, 0.11f, -0.18f, 0.11f); parts += path }
    frame().apply {
        move(-0.05f, 0.11f); cubic(-0.05f, 0.24f, 0.05f, 0.24f, 0.05f, 0.11f); close()
        parts += path
    }
    for (m in listOf(1f, -1f)) frame().apply {
        move(0.03f * m, 0.11f)
        cubic(0.11f * m, 0.12f, 0.20f * m, 0.17f, 0.21f * m, 0.27f)
        cubic(0.13f * m, 0.27f, 0.07f * m, 0.20f, 0.02f * m, 0.15f)
        close()
        parts += path
    }
    return union(parts)
}

// ── Roulette ───────────────────────────────────────────────────────────────────────────────────────────────────────

/** La roulette le long du rectangle [r] (son milieu), large de [w] : un carré à chaque angle, la frise entre deux. */
private fun roll(r: Rect, w: Float, border: CoverBorder, fills: MutableList<Path>, strokes: MutableList<Pair<Path, Float>>, hair: Float) {
    val corners = listOf(r.topLeft, Offset(r.right, r.top), Offset(r.right, r.bottom), Offset(r.left, r.bottom))
    for (c in corners) {
        val block = w * 0.42f
        val sq = Path().apply { addRect(Rect(c.x - block, c.y - block, c.x + block, c.y + block)) }
        val hole = pen(c, w).apply { polygon(0f, -0.2f, 0.2f, 0f, 0f, 0.2f, -0.2f, 0f) }.path
        fills += minus(sq, hole)
    }
    val period = when (border) {
        CoverBorder.RINCEAUX -> 1.9f
        CoverBorder.DENTELLE -> 0.85f
        CoverBorder.PERLES -> 0.42f
        else -> 0.95f
    }
    for (k in 0 until 4) {
        val a = corners[k]
        val b = corners[(k + 1) % 4]
        val len = hypot(b.x - a.x, b.y - a.y)
        val ax = Offset((b.x - a.x) / len, (b.y - a.y) / len)
        val ay = Offset(-ax.y, ax.x) // vers l'intérieur, en tournant dans le sens des aiguilles d'une montre
        val room = len - w
        if (room <= w) continue
        val n = (room / (period * w)).roundToInt().coerceAtLeast(1)
        val step = room / n
        if (border == CoverBorder.PERLES) {
            // deux filets fins encadrent les perles
            for (side in listOf(-0.36f, 0.36f)) {
                val p0 = a + ax * (w / 2) + ay * (side * w)
                val p1 = b - ax * (w / 2) + ay * (side * w)
                strokes += Path().apply { moveTo(p0.x, p0.y); lineTo(p1.x, p1.y) } to hair * 0.8f
            }
        }
        for (i in 0 until n) {
            val c = a + ax * (w / 2 + step * (i + 0.5f))
            stamp(border, Pen(c, ax, ay, w), i, step / w, fills, strokes, hair)
        }
    }
}

private operator fun Offset.times(f: Float) = Offset(x * f, y * f)

/** Un coup de roulette centré sur l'origine du repère : x le long de la frise (unité : sa largeur), y vers l'intérieur. */
private fun stamp(border: CoverBorder, base: Pen, i: Int, period: Float, fills: MutableList<Path>, strokes: MutableList<Pair<Path, Float>>, hair: Float) {
    fun frame(sx: Float = base.sx) = Pen(base.o, base.ax, base.ay, sx, base.sy)
    when (border) {
        CoverBorder.LYS -> fills += lys { Pen(base.o + base.ay * (base.sy * 0.08f), base.ax, base.ay, base.sx * 0.78f) }
        CoverBorder.PALMETTES -> if (i % 2 == 0) {
            // palmette : cinq lobes en éventail, pointe vers l'extérieur, sur un petit cœur
            val parts = ArrayList<Path>()
            for (j in -2..2) {
                val ang = j * 0.42f
                val dx = sin(ang); val dy = -cos(ang)
                val len = if (j == 0) 0.42f else 0.34f - 0.03f * kotlin.math.abs(j)
                frame().apply {
                    val tip = Offset(dx * len, 0.18f + dy * (len + 0.18f))
                    move(0f, 0.2f)
                    cubic(0.10f * dy + dx * 0.1f, 0.1f, tip.x + 0.09f * -dy, tip.y + 0.09f * dx, tip.x, tip.y)
                    cubic(tip.x - 0.09f * -dy, tip.y - 0.09f * dx, -0.10f * dy + dx * 0.1f, 0.1f, 0f, 0.2f)
                    close()
                    parts += path
                }
            }
            frame().apply { ellipse(0f, 0.26f, 0.12f, 0.09f); parts += path }
            fills += union(parts)
        } else {
            // fleuron de lotus, tourné vers l'intérieur
            val parts = ArrayList<Path>()
            frame().apply { move(0f, 0.38f); cubic(0.12f, 0.2f, 0.08f, -0.05f, 0f, -0.2f); cubic(-0.08f, -0.05f, -0.12f, 0.2f, 0f, 0.38f); close(); parts += path }
            for (m in listOf(1f, -1f)) frame().apply {
                move(0.02f * m, -0.15f); cubic(0.22f * m, -0.12f, 0.3f * m, 0.1f, 0.2f * m, 0.3f); cubic(0.18f * m, 0.12f, 0.1f * m, -0.02f, 0.02f * m, -0.05f); close()
                parts += path
            }
            frame().apply { polygon(-0.16f, -0.24f, 0.16f, -0.24f, 0.16f, -0.18f, -0.16f, -0.18f); parts += path }
            fills += union(parts)
        }
        CoverBorder.RINCEAUX -> {
            // une ondulation par période, une volute vers l'extérieur et une feuille vers l'intérieur
            val f = frame(base.sx * period)
            f.move(-0.5f, 0f)
            for (s in 1..16) {
                val t = s / 16f
                f.line(-0.5f + t, -0.22f * sin(2 * PI.toFloat() * t) / period * 1.6f)
            }
            strokes += f.path to hair * 1.3f
            val curl = frame()
            val cx = -0.25f * period
            curl.move(cx, -0.18f)
            curl.cubic(cx + 0.18f, -0.42f, cx + 0.36f, -0.16f, cx + 0.2f, -0.12f)
            curl.cubic(cx + 0.12f, -0.1f, cx + 0.14f, -0.22f, cx + 0.2f, -0.2f)
            strokes += curl.path to hair * 1.1f
            fills += frame().apply { ellipse(cx + 0.2f, -0.17f, 0.05f) }.path
            val leaf = frame()
            val lx = 0.25f * period
            leaf.move(lx, 0.17f)
            leaf.cubic(lx + 0.12f, 0.22f, lx + 0.22f, 0.34f, lx + 0.3f, 0.42f)
            leaf.cubic(lx + 0.14f, 0.42f, lx + 0.02f, 0.32f, lx, 0.17f)
            leaf.close()
            fills += leaf.path
        }
        CoverBorder.DENTELLE -> {
            // trèfle de petits fers vers l'extérieur, point, et feston vers l'intérieur
            val parts = ArrayList<Path>()
            frame().apply { ellipse(0f, -0.26f, 0.1f); parts += path }
            frame().apply { ellipse(-0.12f, -0.12f, 0.09f); parts += path }
            frame().apply { ellipse(0.12f, -0.12f, 0.09f); parts += path }
            frame().apply { polygon(-0.03f, -0.1f, 0.03f, -0.1f, 0.03f, 0.12f, -0.03f, 0.12f); parts += path }
            fills += union(parts)
            fills += frame().apply { ellipse(0.5f * period, -0.05f, 0.05f) }.path
            val festoon = frame()
            festoon.move(-0.5f * period, 0.38f)
            festoon.cubic(-0.3f * period, 0.12f, 0.3f * period, 0.12f, 0.5f * period, 0.38f)
            strokes += festoon.path to hair
            fills += frame().apply { ellipse(0f, 0.28f, 0.05f) }.path
        }
        CoverBorder.PERLES -> fills += frame().apply { ellipse(0f, 0f, 0.17f) }.path
        CoverBorder.NONE -> Unit
    }
}

// ── Fers d'angle ───────────────────────────────────────────────────────────────────────────────────────────────────

private fun cornerTool(style: CoverCorner, corner: Offset, dir: Offset, s: Float, outer: Rect, fills: MutableList<Path>, strokes: MutableList<Pair<Path, Float>>, hair: Float) {
    val c = Offset(corner.x + dir.x * s * 0.55f, corner.y + dir.y * s * 0.55f)
    when (style) {
        CoverCorner.QUADRILOBE -> {
            val r = s * 0.2f
            fills += Path().apply {
                addOval(Rect(Offset(c.x - r * 1.1f, c.y), r))
                addOval(Rect(Offset(c.x + r * 1.1f, c.y), r))
                addOval(Rect(Offset(c.x, c.y - r * 1.1f), r))
                addOval(Rect(Offset(c.x, c.y + r * 1.1f), r))
                addOval(Rect(Offset(c.x + dir.x * s * 0.62f, c.y + dir.y * s * 0.62f), r * 0.45f))
            }
        }
        CoverCorner.FLEUR_DE_LYS -> {
            // la fleur pointe vers le centre du plat, en diagonale
            val n = 1f / kotlin.math.sqrt(2f)
            val ay = Offset(-dir.x * n, -dir.y * n)
            val ax = Offset(-ay.y, ay.x)
            fills += lys { Pen(c + Offset(dir.x * s * 0.1f, dir.y * s * 0.1f), ax, ay, s * 1.15f) }
            fills += Path().apply { addOval(Rect(Offset(corner.x + dir.x * s * 0.12f, corner.y + dir.y * s * 0.12f), s * 0.08f)) }
        }
        CoverCorner.ECOINCON -> {
            // deux rinceaux le long des côtés de l'angle, terminés en volute, et un fleuron dans la diagonale
            val q = Pen(corner, Offset(dir.x, 0f), Offset(0f, dir.y), s)
            for (swap in listOf(false, true)) {
                fun pt(x: Float, y: Float) = if (swap) y to x else x to y
                val f = Pen(corner, Offset(dir.x, 0f), Offset(0f, dir.y), s)
                val (x0, y0) = pt(0.15f, 0.12f); f.move(x0, y0)
                val (a1, b1) = pt(0.7f, 0.0f); val (a2, b2) = pt(1.1f, 0.5f); val (x1, y1) = pt(1.6f, 0.22f)
                f.cubic(a1, b1, a2, b2, x1, y1)
                val (c1, d1) = pt(1.95f, 0.05f); val (c2, d2) = pt(2.05f, 0.45f); val (x2, y2) = pt(1.8f, 0.42f)
                f.cubic(c1, d1, c2, d2, x2, y2)
                val (e1, g1) = pt(1.62f, 0.4f); val (e2, g2) = pt(1.66f, 0.26f); val (x3, y3) = pt(1.78f, 0.3f)
                f.cubic(e1, g1, e2, g2, x3, y3)
                strokes += f.path to hair * 1.4f
                // feuille qui part du rinceau vers l'intérieur
                val leaf = Pen(corner, Offset(dir.x, 0f), Offset(0f, dir.y), s)
                val (l0, m0) = pt(0.9f, 0.28f); leaf.move(l0, m0)
                val (l1, m1) = pt(1.0f, 0.6f); val (l2, m2) = pt(1.2f, 0.75f); val (l3, m3) = pt(1.35f, 0.8f)
                leaf.cubic(l1, m1, l2, m2, l3, m3)
                val (k1, j1) = pt(1.1f, 0.55f); val (k2, j2) = pt(1.0f, 0.45f)
                leaf.cubic(k1, j1, k2, j2, l0, m0)
                leaf.close()
                fills += leaf.path
                fills += Pen(corner, Offset(dir.x, 0f), Offset(0f, dir.y), s).apply { val (u, v) = pt(1.8f, 0.31f); ellipse(u, v, 0.06f) }.path
            }
            val parts = ArrayList<Path>()
            q.apply { ellipse(0.42f, 0.42f, 0.13f); parts += path }
            Pen(corner, Offset(dir.x, 0f), Offset(0f, dir.y), s).apply { ellipse(0.62f, 0.62f, 0.08f); parts += path }
            Pen(corner, Offset(dir.x, 0f), Offset(0f, dir.y), s).apply { ellipse(0.28f, 0.6f, 0.07f); parts += path }
            Pen(corner, Offset(dir.x, 0f), Offset(0f, dir.y), s).apply { ellipse(0.6f, 0.28f, 0.07f); parts += path }
            fills += union(parts)
        }
        CoverCorner.CARRE -> {
            // carré plein posé sur l'angle du cadre extérieur, un losange réservé au milieu
            val k = when { dir.x > 0 && dir.y > 0 -> outer.topLeft; dir.x < 0 && dir.y > 0 -> Offset(outer.right, outer.top); dir.x < 0 -> Offset(outer.right, outer.bottom); else -> Offset(outer.left, outer.bottom) }
            val h = s * 0.42f
            val sq = Path().apply { addRect(Rect(k.x - h, k.y - h, k.x + h, k.y + h)) }
            val hole = pen(k, s).apply { polygon(0f, -0.22f, 0.22f, 0f, 0f, 0.22f, -0.22f, 0f) }.path
            fills += minus(sq, hole)
            fills += pen(k, s).apply { ellipse(0f, 0f, 0.08f) }.path
        }
        CoverCorner.ROSETTE -> {
            val parts = ArrayList<Path>()
            for (j in 0 until 8) {
                val a = j * PI.toFloat() / 4
                parts += Pen(c, Offset(cos(a), sin(a)), Offset(-sin(a), cos(a)), s).apply { ellipse(0.24f, 0f, 0.15f, 0.07f) }.path
            }
            fills += union(parts)
            fills += minus(pen(c, s).apply { ellipse(0f, 0f, 0.12f) }.path, pen(c, s).apply { ellipse(0f, 0f, 0.06f) }.path)
        }
        CoverCorner.NONE -> Unit
    }
}

// ── Dos à caissons ─────────────────────────────────────────────────────────────────────────────────────────────────

private fun spine(board: Rect, cover: CoverStyle, fills: MutableList<Path>, strokes: MutableList<Pair<Path, Float>>, hair: Float) {
    val joint = board.left + board.width * 0.075f
    val x0 = board.left + 1.5f
    val x1 = joint - 2f
    val n = cover.bands
    val ys = if (n > 0) (1..n).map { board.top + board.height * it / (n + 1) } else listOf(board.top + board.height * 0.06f, board.bottom - board.height * 0.06f)
    for (y in ys) for (d in listOf(-7f, 7f)) strokes += Path().apply { moveTo(x0, y + d); lineTo(x1, y + d) } to hair * 0.9f
    val bounds = listOf(board.top) + ys + listOf(board.bottom)
    val mid = (x0 + x1) / 2
    val s = (x1 - x0) * 0.9f
    for (i in 0 until bounds.size - 1) {
        val y = (bounds[i] + bounds[i + 1]) / 2
        if (bounds[i + 1] - bounds[i] < s * 1.2f) continue
        // un petit fer : losange et quatre points
        fills += pen(Offset(mid, y), s).apply { polygon(0f, -0.32f, 0.16f, 0f, 0f, 0.32f, -0.16f, 0f) }.path
        for ((dx, dy) in listOf(0f to -0.45f, 0f to 0.45f, -0.28f to 0f, 0.28f to 0f)) fills += pen(Offset(mid, y), s).apply { ellipse(dx, dy, 0.06f) }.path
    }
}

// ── Motif central et son encadrement ───────────────────────────────────────────────────────────────────────────────

/** Le motif du plat, haut de [h] et centré sur [c] ; null pour IHS (composé en lettres) et sans motif. */
internal fun motifPath(motif: CoverMotif, c: Offset, h: Float): Path? = when (motif) {
    CoverMotif.CROSS -> crossPath(c, h)
    CoverMotif.CHI_RHO -> chiRhoPath(c, h)
    CoverMotif.FLEUR_DE_LYS -> lys { Pen(c + Offset(0f, h * 0.1f), Offset(1f, 0f), Offset(0f, 1f), h * 1.15f) }
    CoverMotif.CROSS_FLEURY -> crossFleury(c, h)
    CoverMotif.LILY -> lily(c, h)
    CoverMotif.ROSE -> rose(c, h)
    CoverMotif.SACRED_HEART -> sacredHeart(c, h)
    CoverMotif.STAR -> star(c, h)
    CoverMotif.CHALICE -> chalice(c, h)
    CoverMotif.IHS, CoverMotif.NONE -> null
}

/** Croix tréflée : chaque bras finit en trois lobes. */
private fun crossFleury(c: Offset, h: Float): Path {
    val parts = ArrayList<Path>()
    val t = 0.075f
    val arm = -0.12f
    pen(c, h).apply { polygon(-t, -0.4f, t, -0.4f, t, 0.46f, -t, 0.46f); parts += path }
    pen(c, h).apply { polygon(-0.34f, arm - t, 0.34f, arm - t, 0.34f, arm + t, -0.34f, arm + t); parts += path }
    for ((x, y, ux, uy) in listOf(Quad(0f, -0.42f, 0f, -1f), Quad(0.36f, arm, 1f, 0f), Quad(-0.36f, arm, -1f, 0f))) {
        val vx = -uy; val vy = ux
        pen(c, h).apply { ellipse(x + ux * 0.06f, y + uy * 0.06f, 0.075f); parts += path }
        pen(c, h).apply { ellipse(x + vx * 0.08f, y + vy * 0.08f, 0.06f); parts += path }
        pen(c, h).apply { ellipse(x - vx * 0.08f, y - vy * 0.08f, 0.06f); parts += path }
    }
    pen(c, h).apply { polygon(-0.13f, 0.44f, 0.13f, 0.44f, 0.09f, 0.5f, -0.09f, 0.5f); parts += path }
    return union(parts)
}

private data class Quad(val a: Float, val b: Float, val c: Float, val d: Float)

/** Lis au naturel : trois pétales en trompette, un bouton, des feuilles le long de la tige. */
private fun lily(c: Offset, h: Float): Path {
    val parts = ArrayList<Path>()
    pen(c, h).apply { move(-0.018f, 0.5f); cubic(-0.02f, 0.2f, -0.01f, 0f, -0.012f, -0.14f); line(0.012f, -0.14f); cubic(0.01f, 0f, 0.02f, 0.2f, 0.018f, 0.5f); close(); parts += path }
    // pétales
    pen(c, h).apply { move(-0.04f, -0.14f); cubic(-0.08f, -0.3f, -0.03f, -0.44f, 0f, -0.5f); cubic(0.03f, -0.44f, 0.08f, -0.3f, 0.04f, -0.14f); close(); parts += path }
    for (m in listOf(1f, -1f)) pen(c, h).apply {
        move(0.02f * m, -0.14f)
        cubic(0.08f * m, -0.24f, 0.16f * m, -0.36f, 0.26f * m, -0.40f)
        cubic(0.3f * m, -0.41f, 0.31f * m, -0.36f, 0.27f * m, -0.34f)
        cubic(0.2f * m, -0.3f, 0.12f * m, -0.2f, 0.05f * m, -0.1f)
        close()
        parts += path
    }
    // bouton à droite, sur sa petite tige
    pen(c, h).apply { move(0.01f, 0.02f); cubic(0.08f, -0.02f, 0.12f, -0.08f, 0.14f, -0.12f); line(0.155f, -0.11f); cubic(0.13f, -0.06f, 0.09f, 0.0f, 0.01f, 0.05f); close(); parts += path }
    pen(c, h).apply { move(0.15f, -0.12f); cubic(0.13f, -0.2f, 0.15f, -0.27f, 0.19f, -0.3f); cubic(0.22f, -0.25f, 0.21f, -0.17f, 0.165f, -0.11f); close(); parts += path }
    // feuilles
    for ((y, m, len) in listOf(Triple(0.32f, -1f, 0.24f), Triple(0.22f, 1f, 0.22f), Triple(0.1f, -1f, 0.2f))) pen(c, h).apply {
        move(0f, y)
        cubic(0.06f * m, y - 0.04f, len * 0.8f * m, y - 0.1f, len * m, y - 0.16f)
        cubic(len * 0.7f * m, y - 0.04f, 0.08f * m, y + 0.02f, 0f, y + 0.03f)
        close()
        parts += path
    }
    return union(parts)
}

/** Rose héraldique : cinq pétales, cinq sépales, le cœur réservé. */
private fun rose(c: Offset, h: Float): Path {
    val parts = ArrayList<Path>()
    for (j in 0 until 5) {
        val a = -PI.toFloat() / 2 + j * 2 * PI.toFloat() / 5
        parts += pen(c, h).apply { ellipse(cos(a) * 0.2f, sin(a) * 0.2f, 0.17f) }.path
        val b = a + PI.toFloat() / 5
        parts += pen(c, h).apply {
            polygon(cos(b) * 0.18f + sin(b) * 0.05f, sin(b) * 0.18f - cos(b) * 0.05f, cos(b) * 0.48f, sin(b) * 0.48f, cos(b) * 0.18f - sin(b) * 0.05f, sin(b) * 0.18f + cos(b) * 0.05f)
        }.path
    }
    val body = minus(union(parts), pen(c, h).apply { ellipse(0f, 0f, 0.13f) }.path)
    return union(listOf(body, pen(c, h).apply { ellipse(0f, 0f, 0.08f) }.path))
}

/** Sacré-Cœur : le cœur, la couronne d'épines réservée en travers, les flammes et la croix au-dessus. */
private fun sacredHeart(c: Offset, h: Float): Path {
    val heart = pen(c, h).apply {
        move(0f, -0.06f)
        cubic(-0.06f, -0.2f, -0.34f, -0.18f, -0.32f, 0.02f)
        cubic(-0.3f, 0.2f, -0.1f, 0.3f, 0f, 0.44f)
        cubic(0.1f, 0.3f, 0.3f, 0.2f, 0.32f, 0.02f)
        cubic(0.34f, -0.18f, 0.06f, -0.2f, 0f, -0.06f)
        close()
    }.path
    val crown = pen(c, h).apply { polygon(-0.34f, 0.02f, 0.34f, -0.06f, 0.34f, 0.0f, -0.34f, 0.08f) }.path
    val parts = arrayListOf(minus(heart, crown))
    // épines : de petites pointes le long de la couronne
    for (j in -4..4) {
        val x = j * 0.065f
        val y = 0.04f - x * 0.118f
        parts += pen(c, h).apply { polygon(x - 0.015f, y - 0.02f, x + 0.015f, y - 0.02f, x + 0.03f, y - 0.06f) }.path
    }
    // flammes
    for ((x, top) in listOf(-0.09f to -0.3f, 0f to -0.38f, 0.09f to -0.3f)) parts += pen(c, h).apply {
        move(x - 0.05f, -0.1f); cubic(x - 0.07f, -0.2f, x - 0.01f, top + 0.06f, x, top); cubic(x + 0.01f, top + 0.06f, x + 0.07f, -0.2f, x + 0.05f, -0.1f); close()
    }.path
    parts += pen(c, h).apply { polygon(-0.012f, -0.5f, 0.012f, -0.5f, 0.012f, -0.37f, -0.012f, -0.37f) }.path
    parts += pen(c, h).apply { polygon(-0.05f, -0.465f, 0.05f, -0.465f, 0.05f, -0.44f, -0.05f, -0.44f) }.path
    return union(parts)
}

/** Étoile à huit rais, longs et courts en alternance. */
private fun star(c: Offset, h: Float): Path = pen(c, h).apply {
    val pts = ArrayList<Float>()
    for (j in 0 until 16) {
        val a = -PI.toFloat() / 2 + j * PI.toFloat() / 8
        val r = when { j % 4 == 0 -> 0.5f; j % 2 == 0 -> 0.34f; else -> 0.14f }
        pts += cos(a) * r; pts += sin(a) * r
    }
    polygon(*pts.toFloatArray())
}.path.let { minus(it, pen(c, h).apply { ellipse(0f, 0f, 0.05f) }.path) }

/** Calice et hostie rayonnante, la croix réservée dans l'hostie. */
private fun chalice(c: Offset, h: Float): Path {
    val parts = ArrayList<Path>()
    pen(c, h).apply { move(-0.2f, -0.04f); line(0.2f, -0.04f); cubic(0.2f, 0.1f, 0.1f, 0.16f, 0.03f, 0.17f); line(-0.03f, 0.17f); cubic(-0.1f, 0.16f, -0.2f, 0.1f, -0.2f, -0.04f); close(); parts += path }
    pen(c, h).apply { ellipse(0f, 0.22f, 0.06f, 0.04f); parts += path }
    pen(c, h).apply { polygon(-0.025f, 0.17f, 0.025f, 0.17f, 0.03f, 0.4f, -0.03f, 0.4f); parts += path }
    pen(c, h).apply { move(-0.03f, 0.38f); cubic(-0.1f, 0.42f, -0.2f, 0.44f, -0.22f, 0.5f); line(0.22f, 0.5f); cubic(0.2f, 0.44f, 0.1f, 0.42f, 0.03f, 0.38f); close(); parts += path }
    val host = minus(
        pen(c, h).apply { ellipse(0f, -0.26f, 0.13f) }.path,
        union(listOf(pen(c, h).apply { polygon(-0.015f, -0.35f, 0.015f, -0.35f, 0.015f, -0.17f, -0.015f, -0.17f) }.path, pen(c, h).apply { polygon(-0.07f, -0.285f, 0.07f, -0.285f, 0.07f, -0.255f, -0.07f, -0.255f) }.path)),
    )
    parts += host
    for (j in 0 until 16) {
        val a = j * PI.toFloat() / 8
        if (kotlin.math.abs(sin(a) - 1f) < 0.3f) continue // pas de rai dans la coupe
        val r0 = 0.16f; val r1 = if (j % 2 == 0) 0.25f else 0.21f
        parts += pen(c, h).apply {
            polygon(cos(a - 0.06f) * r0, -0.26f + sin(a - 0.06f) * r0, cos(a) * r1, -0.26f + sin(a) * r1, cos(a + 0.06f) * r0, -0.26f + sin(a + 0.06f) * r0)
        }.path
    }
    return union(parts)
}

/** L'encadrement du motif : médaillon, losange, cartouche, ou gloire de rayons ; en fers comme le cadre. */
internal fun centerFrame(center: CoverCenter, c: Offset, h: Float, hair: Float): CoverDecor {
    val fills = ArrayList<Path>()
    val strokes = ArrayList<Pair<Path, Float>>()
    val rx = h * 0.62f
    val ry = h * 0.74f
    when (center) {
        CoverCenter.MEDAILLON -> {
            for (k in listOf(0f, 0.07f)) strokes += pen(c, 1f).apply { ellipse(0f, 0f, rx * (1 + k), ry * (1 + k)) }.path to hair
            for ((x, y) in listOf(0f to -1f, 0f to 1f, -1f to 0f, 1f to 0f)) fills += pen(c, 1f).apply { ellipse(x * rx * 1.18f, y * ry * 1.15f, h * 0.035f) }.path
        }
        CoverCenter.LOSANGE -> {
            for (k in listOf(0f, 0.08f)) strokes += pen(c, 1f).apply { polygon(0f, -ry * 1.15f * (1 + k), rx * 1.1f * (1 + k), 0f, 0f, ry * 1.15f * (1 + k), -rx * 1.1f * (1 + k), 0f) }.path to hair
            for ((x, y) in listOf(0f to -1f, 0f to 1f, -1f to 0f, 1f to 0f)) fills += pen(c, 1f).apply { ellipse(x * rx * 1.32f, y * ry * 1.36f, h * 0.035f) }.path
        }
        CoverCenter.CARTOUCHE -> {
            for (k in listOf(0f, h * 0.06f)) {
                val w = rx * 1.05f + k
                val hh = ry * 1.0f + k
                val r = h * 0.16f
                strokes += pen(c, 1f).apply {
                    move(-w + r, -hh)
                    line(w - r, -hh); cubic(w - r, -hh + r * 0.55f, w - r * 0.55f, -hh + r, w, -hh + r)
                    line(w, hh - r); cubic(w - r * 0.55f, hh - r, w - r, hh - r * 0.55f, w - r, hh)
                    line(-w + r, hh); cubic(-w + r, hh - r * 0.55f, -w + r * 0.55f, hh - r, -w, hh - r)
                    line(-w, -hh + r); cubic(-w + r * 0.55f, -hh + r, -w + r, -hh + r * 0.55f, -w + r, -hh)
                    close()
                }.path to hair
            }
            for ((x, y) in listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f)) fills += lys {
                val n = 1f / kotlin.math.sqrt(2f)
                val ay = Offset(x * n, y * n)
                Pen(c + Offset(x * (rx * 1.05f + h * 0.02f), y * (ry + h * 0.02f)), Offset(-ay.y, ay.x), Offset(-ay.x, -ay.y), h * 0.22f)
            }
        }
        CoverCenter.GLOIRE -> for (j in 0 until 32) {
            val a = j * PI.toFloat() / 16
            val r0 = h * 0.58f
            val r1 = if (j % 2 == 0) h * 0.95f else h * 0.78f
            val wdt = 0.045f
            fills += pen(c, 1f).apply { polygon(cos(a - wdt) * r0, sin(a - wdt) * r0, cos(a) * r1, sin(a) * r1, cos(a + wdt) * r0, sin(a + wdt) * r0) }.path
        }
        CoverCenter.NONE -> Unit
    }
    return CoverDecor(fills, strokes)
}
