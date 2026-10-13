package app.paroissien.library

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Un chemin fait de sous-chemins (un départ, des segments droits ou courbes, fermé ou non), qu'on déplace, tourne,
 * met à l'échelle ou reflète point par point, puis qu'on écrit en SVG. Un sous-chemin « trou » (graines d'une fraise,
 * nervure d'une feuille) est orienté à l'inverse des autres : avec la règle de remplissage nonzero, il se vide.
 */
internal class Figure(val subs: List<Sub>) {
    sealed interface Seg { val p: Offset }
    data class Line(override val p: Offset) : Seg
    data class Cubic(val c1: Offset, val c2: Offset, override val p: Offset) : Seg
    class Sub(val start: Offset, val segs: List<Seg>, val closed: Boolean, val hole: Boolean = false)

    fun map(f: (Offset) -> Offset) = Figure(subs.map { s ->
        Sub(f(s.start), s.segs.map { g -> if (g is Cubic) Cubic(f(g.c1), f(g.c2), f(g.p)) else Line(f(g.p)) }, s.closed, s.hole)
    })

    /** Chaque sous-chemin fermé tourne dans le même sens (les trous à l'inverse), même après un miroir. */
    fun normalized() = Figure(subs.map { s ->
        if (!s.closed) return@map s
        val pts = listOf(s.start) + s.segs.flatMap { g -> if (g is Cubic) listOf(g.c1, g.c2, g.p) else listOf(g.p) }
        var area = 0f
        for (i in pts.indices) {
            val a = pts[i]
            val b = pts[(i + 1) % pts.size]
            area += a.x * b.y - b.x * a.y
        }
        if ((area >= 0f) != s.hole) s else reversed(s)
    })

    private fun reversed(s: Sub): Sub {
        val segs = s.segs.toMutableList()
        if (segs.isEmpty()) return s
        if (segs.last().p != s.start) segs += Line(s.start)
        val ends = listOf(s.start) + segs.map { it.p }
        val out = ArrayList<Seg>()
        for (i in segs.indices.reversed()) {
            val g = segs[i]
            out += if (g is Cubic) Cubic(g.c2, g.c1, ends[i]) else Line(ends[i])
        }
        return Sub(ends.last(), out, true, s.hole)
    }

    fun svg(): String {
        fun f(v: Float) = "%.2f".format(java.util.Locale.ROOT, v)
        val sb = StringBuilder()
        for (s in subs) {
            sb.append("M${f(s.start.x)} ${f(s.start.y)}")
            for (g in s.segs) when (g) {
                is Line -> sb.append("L${f(g.p.x)} ${f(g.p.y)}")
                is Cubic -> sb.append("C${f(g.c1.x)} ${f(g.c1.y)} ${f(g.c2.x)} ${f(g.c2.y)} ${f(g.p.x)} ${f(g.p.y)}")
            }
            if (s.closed) sb.append('Z')
        }
        return sb.toString()
    }

    fun bounds(): Rect {
        val pts = subs.flatMap { s -> listOf(s.start) + s.segs.map { it.p } }
        if (pts.isEmpty()) return Rect.Zero
        return Rect(pts.minOf { it.x }, pts.minOf { it.y }, pts.maxOf { it.x }, pts.maxOf { it.y })
    }
}

/** Construire une [Figure] : formes de base, feuilles, tiges en ruban, courbes lisses, volutes. Angles en degrés, y vers le bas. */
internal class FigureBuilder {
    private val subs = ArrayList<Figure.Sub>()
    private var start: Offset? = null
    private var segs = ArrayList<Figure.Seg>()
    private var hole = false

    fun moveTo(x: Float, y: Float) { flush(false); start = Offset(x, y) }
    fun lineTo(x: Float, y: Float) { segs += Figure.Line(Offset(x, y)) }
    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) { segs += Figure.Cubic(Offset(x1, y1), Offset(x2, y2), Offset(x, y)) }
    fun close() = flush(true)
    fun end() = flush(false)
    private fun flush(closed: Boolean) {
        start?.let { if (segs.isNotEmpty()) subs += Figure.Sub(it, segs, closed, hole) }
        start = null
        segs = ArrayList()
    }

    /** Ce qu'on trace dans [block] est évidé. */
    fun hole(block: FigureBuilder.() -> Unit) {
        flush(false)
        hole = true
        block()
        flush(false)
        hole = false
    }

    fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float = rx, rotation: Float = 0f) {
        val k = 0.5523f
        val a = rotation * PI.toFloat() / 180f
        fun p(x: Float, y: Float) = Offset(cx + x * cos(a) - y * sin(a), cy + x * sin(a) + y * cos(a))
        fun c(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) {
            val q1 = p(x1, y1); val q2 = p(x2, y2); val q = p(x, y)
            cubicTo(q1.x, q1.y, q2.x, q2.y, q.x, q.y)
        }
        p(rx, 0f).let { moveTo(it.x, it.y) }
        c(rx, ry * k, rx * k, ry, 0f, ry)
        c(-rx * k, ry, -rx, ry * k, -rx, 0f)
        c(-rx, -ry * k, -rx * k, -ry, 0f, -ry)
        c(rx * k, -ry, rx, -ry * k, rx, 0f)
        close()
    }

    fun circle(cx: Float, cy: Float, r: Float) = ellipse(cx, cy, r, r)

    fun poly(vararg xy: Float, closed: Boolean = true) {
        moveTo(xy[0], xy[1])
        var i = 2
        while (i < xy.size) { lineTo(xy[i], xy[i + 1]); i += 2 }
        if (closed) close() else end()
    }

    fun rect(x0: Float, y0: Float, x1: Float, y1: Float) = poly(x0, y0, x1, y0, x1, y1, x0, y1)

    /** Feuille lancéolée : la base en (x, y), la pointe dans la direction [deg], longue de [len], large de [w]. */
    fun leaf(x: Float, y: Float, deg: Float, len: Float, w: Float, bend: Float = 0f) {
        val a = deg * PI.toFloat() / 180f
        fun p(u: Float, v: Float) = Offset(x + u * cos(a) - v * sin(a), y + u * sin(a) + v * cos(a))
        val b = bend * len
        val p0 = p(0f, 0f)
        moveTo(p0.x, p0.y)
        p(0.3f * len, -0.55f * w + b).let { c1 -> p(0.75f * len, -0.45f * w + b).let { c2 -> p(len, b).let { e -> cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y) } } }
        p(0.75f * len, 0.45f * w + b).let { c1 -> p(0.3f * len, 0.55f * w + b).let { c2 -> cubicTo(c1.x, c1.y, c2.x, c2.y, p0.x, p0.y) } }
        close()
    }

    /** Pétale en goutte : base ronde en (x, y), pointe dans la direction [deg]. */
    fun petal(x: Float, y: Float, deg: Float, len: Float, w: Float) {
        val a = deg * PI.toFloat() / 180f
        fun p(u: Float, v: Float) = Offset(x + u * cos(a) - v * sin(a), y + u * sin(a) + v * cos(a))
        val p0 = p(0f, 0f)
        moveTo(p0.x, p0.y)
        p(0.22f * len, -0.62f * w).let { c1 -> p(0.72f * len, -0.52f * w).let { c2 -> p(len, 0f).let { e -> cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y) } } }
        p(0.72f * len, 0.52f * w).let { c1 -> p(0.22f * len, 0.62f * w).let { c2 -> cubicTo(c1.x, c1.y, c2.x, c2.y, p0.x, p0.y) } }
        close()
    }

    /** Courbe lisse par des points (Catmull-Rom). */
    fun smooth(vararg xy: Float, closed: Boolean) {
        val pts = xy.toList().chunked(2).map { Offset(it[0], it[1]) }
        val n = pts.size
        moveTo(pts[0].x, pts[0].y)
        fun at(i: Int) = if (closed) pts[(i % n + n) % n] else pts[i.coerceIn(0, n - 1)]
        val last = if (closed) n else n - 1
        for (i in 0 until last) {
            val p0 = at(i - 1); val p1 = at(i); val p2 = at(i + 1); val p3 = at(i + 2)
            cubicTo(p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f, p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f, p2.x, p2.y)
        }
        if (closed) close() else end()
    }

    /** Tige en ruban plein, de largeur [w], le long d'une courbe lisse par des points. */
    fun stem(vararg xy: Float, w: Float) {
        val pts = sample(xy.toList().chunked(2).map { Offset(it[0], it[1]) })
        val left = ArrayList<Offset>()
        val right = ArrayList<Offset>()
        for (i in pts.indices) {
            val a = pts[(i - 1).coerceAtLeast(0)]
            val b = pts[(i + 1).coerceAtMost(pts.size - 1)]
            val d = hypot(b.x - a.x, b.y - a.y).takeIf { it > 1e-6f } ?: 1f
            val nx = -(b.y - a.y) / d * w / 2
            val ny = (b.x - a.x) / d * w / 2
            left += Offset(pts[i].x + nx, pts[i].y + ny)
            right += Offset(pts[i].x - nx, pts[i].y - ny)
        }
        val all = left + right.reversed()
        moveTo(all[0].x, all[0].y)
        all.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }

    /** Volute : spirale d'Archimède de [r0] à [r1] en [turns] tours, départ à l'angle [a0] (ligne ouverte). */
    fun spiral(cx: Float, cy: Float, r0: Float, r1: Float, turns: Float, a0: Float = 0f) {
        val n = (turns * 16).toInt().coerceAtLeast(4)
        val xy = FloatArray((n + 1) * 2)
        for (i in 0..n) {
            val t = i / n.toFloat()
            val a = (a0 + 360f * turns * t) * PI.toFloat() / 180f
            val r = r0 + (r1 - r0) * t
            xy[2 * i] = cx + r * cos(a)
            xy[2 * i + 1] = cy + r * sin(a)
        }
        smooth(*xy, closed = false)
    }

    fun build(): Figure { flush(false); return Figure(subs.toList()) }

    private fun sample(pts: List<Offset>): List<Offset> {
        if (pts.size < 3) return pts
        val out = ArrayList<Offset>()
        val n = pts.size
        for (i in 0 until n - 1) {
            val p0 = pts[(i - 1).coerceAtLeast(0)]; val p1 = pts[i]; val p2 = pts[i + 1]; val p3 = pts[(i + 2).coerceAtMost(n - 1)]
            for (s in 0 until 10) {
                val t = s / 10f
                val t2 = t * t; val t3 = t2 * t
                fun cr(a: Float, b: Float, c: Float, d: Float) = 0.5f * ((2 * b) + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t2 + (-a + 3 * b - 3 * c + d) * t3)
                out += Offset(cr(p0.x, p1.x, p2.x, p3.x), cr(p0.y, p1.y, p2.y, p3.y))
            }
        }
        out += pts.last()
        return out
    }
}

internal fun figure(block: FigureBuilder.() -> Unit): Figure = FigureBuilder().apply(block).build()

/**
 * Les formes géométriques de l'atelier des motifs (13/10/2026), dans une boîte unité de −0,5 à 0,5 : on les étire
 * ensuite en largeur et en hauteur. [line] : une ligne, toujours tracée au trait.
 */
enum class MotifShape(val title: String, val line: Boolean = false) {
    LIGNE("Ligne", true), LOSANGE("Losange"), RECTANGLE("Rectangle"), CERCLE("Cercle"), OVALE("Ovale"),
    TRIANGLE("Triangle"), ETOILE("Étoile"), ARC("Arc", true), SPIRALE("Spirale", true), VAGUE("Vague", true);

    internal val figure: Figure by lazy {
        when (this) {
            LIGNE -> figure { poly(-0.5f, 0f, 0.5f, 0f, closed = false) }
            LOSANGE -> figure { poly(0f, -0.5f, 0.5f, 0f, 0f, 0.5f, -0.5f, 0f) }
            RECTANGLE -> figure { rect(-0.5f, -0.5f, 0.5f, 0.5f) }
            CERCLE, OVALE -> figure { circle(0f, 0f, 0.5f) }
            TRIANGLE -> figure { poly(0f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f) }
            ETOILE -> figure { poly(*star(5, 0.5f, 0.2f)) }
            ARC -> figure {
                val xy = (0..16).flatMap { i -> val a = PI.toFloat() * (1 + i / 16f); listOf(0.5f * cos(a), 0.25f + 0.5f * sin(a)) }
                smooth(*xy.toFloatArray(), closed = false)
            }
            SPIRALE -> figure { spiral(0f, 0f, 0.02f, 0.5f, 2.2f, -90f) }
            VAGUE -> figure {
                val xy = (0..24).flatMap { i -> val t = i / 24f; listOf(-0.5f + t, -0.3f * sin(2 * PI.toFloat() * t)) }
                smooth(*xy.toFloatArray(), closed = false)
            }
        }
    }
}

private fun star(points: Int, outer: Float, inner: Float): FloatArray {
    val out = FloatArray(points * 4)
    for (j in 0 until points * 2) {
        val a = -PI.toFloat() / 2 + j * PI.toFloat() / points
        val r = if (j % 2 == 0) outer else inner
        out[2 * j] = r * cos(a)
        out[2 * j + 1] = r * sin(a)
    }
    return out
}

/** Les familles de la réserve de motifs. */
enum class StampGroup(val title: String) { FLEURS("Fleurs"), FRUITS("Fruits"), FEUILLES("Feuilles"), SYMBOLES("Symboles"), TRAITS("Volutes") }

/**
 * Un motif de la réserve (13/10/2026) : une silhouette très simple, d'une seule couleur, dans une boîte unité
 * (hauteur 1, de −0,5 à 0,5), qu'on pose, agrandit, tourne et retourne dans la cellule, et qu'on marie aux traits à
 * main levée. [line] : un trait (volute, vrille), toujours tracé avec l'épaisseur du trait.
 */
class MotifStamp internal constructor(val id: String, val title: String, val group: StampGroup, val line: Boolean = false, build: FigureBuilder.() -> Unit) {
    internal val figure: Figure by lazy { figure(build) }
}

object MotifStamps {
    val all: List<MotifStamp> = listOf(
        // ── Fleurs ──
        MotifStamp("lys", "Fleur de lys", StampGroup.FLEURS) {
            val s = 0.01f
            fun X(x: Float) = x * s
            fun Y(y: Float) = y * s + 0.5f
            moveTo(X(0f), Y(-100f))
            cubicTo(X(11f), Y(-86f), X(16f), Y(-66f), X(12f), Y(-50f)); cubicTo(X(10f), Y(-42f), X(7f), Y(-37f), X(5f), Y(-34f))
            lineTo(X(-5f), Y(-34f))
            cubicTo(X(-7f), Y(-37f), X(-10f), Y(-42f), X(-12f), Y(-50f)); cubicTo(X(-16f), Y(-66f), X(-11f), Y(-86f), X(0f), Y(-100f))
            close()
            for (m in listOf(1f, -1f)) {
                moveTo(X(5f * m), Y(-36f))
                cubicTo(X(10f * m), Y(-54f), X(25f * m), Y(-64f), X(31f * m), Y(-53f)); cubicTo(X(35f * m), Y(-46f), X(33f * m), Y(-38f), X(26f * m), Y(-36f))
                cubicTo(X(28f * m), Y(-42f), X(25f * m), Y(-47f), X(20f * m), Y(-45f)); cubicTo(X(14f * m), Y(-42f), X(10f * m), Y(-36f), X(9f * m), Y(-30f))
                close()
            }
            rect(X(-15f), Y(-35f), X(15f), Y(-26f))
            moveTo(X(-7f), Y(-26f)); cubicTo(X(-8f), Y(-16f), X(-4f), Y(-7f), X(0f), Y(0f)); cubicTo(X(4f), Y(-7f), X(8f), Y(-16f), X(7f), Y(-26f)); close()
        },
        MotifStamp("lis", "Lis", StampGroup.FLEURS) {
            stem(0f, 0.5f, -0.01f, 0.2f, 0f, -0.14f, w = 0.035f)
            petal(0f, -0.12f, -90f, 0.38f, 0.16f)
            petal(-0.01f, -0.12f, -135f, 0.34f, 0.12f)
            petal(0.01f, -0.12f, -45f, 0.34f, 0.12f)
            leaf(0f, 0.32f, -150f, 0.26f, 0.1f)
            leaf(0f, 0.2f, -30f, 0.24f, 0.09f)
            leaf(0f, 0.08f, -155f, 0.2f, 0.08f)
        },
        MotifStamp("rose", "Rose", StampGroup.FLEURS) {
            for (j in 0 until 5) {
                val a = (-90f + j * 72f) * PI.toFloat() / 180f
                circle(0.22f * cos(a), 0.22f * sin(a), 0.16f)
                val b = a + PI.toFloat() / 5
                poly(cos(b) * 0.2f + sin(b) * 0.05f, sin(b) * 0.2f - cos(b) * 0.05f, cos(b) * 0.48f, sin(b) * 0.48f, cos(b) * 0.2f - sin(b) * 0.05f, sin(b) * 0.2f + cos(b) * 0.05f)
            }
            circle(0f, 0f, 0.14f)
            hole { circle(0f, 0f, 0.065f) }
            circle(0f, 0f, 0.032f)
        },
        MotifStamp("marguerite", "Marguerite", StampGroup.FLEURS) {
            for (j in 0 until 12) ellipse(0.27f * cos(j * PI.toFloat() / 6), 0.27f * sin(j * PI.toFloat() / 6), 0.17f, 0.055f, j * 30f)
            circle(0f, 0f, 0.12f)
        },
        MotifStamp("tulipe", "Tulipe", StampGroup.FLEURS) {
            stem(0f, 0.04f, 0.01f, 0.25f, 0f, 0.5f, w = 0.04f)
            petal(-0.05f, 0.06f, -112f, 0.42f, 0.22f)
            petal(0.05f, 0.06f, -68f, 0.42f, 0.22f)
            petal(0f, 0.07f, -90f, 0.5f, 0.26f)
            leaf(0f, 0.42f, -135f, 0.32f, 0.11f, 0.1f)
            leaf(0f, 0.34f, -45f, 0.28f, 0.1f, -0.1f)
        },
        MotifStamp("eglantine", "Églantine", StampGroup.FLEURS) {
            for (j in 0 until 5) {
                val a = (-90f + j * 72f) * PI.toFloat() / 180f
                circle(0.2f * cos(a), 0.2f * sin(a), 0.17f)
            }
            hole { circle(0f, 0f, 0.075f) }
            circle(0f, 0f, 0.04f)
        },
        MotifStamp("bouton", "Bouton", StampGroup.FLEURS) {
            stem(0f, 0.1f, 0.03f, 0.3f, 0f, 0.5f, w = 0.04f)
            petal(0f, 0.12f, -90f, 0.46f, 0.3f)
            leaf(0f, 0.14f, -128f, 0.2f, 0.08f)
            leaf(0f, 0.14f, -52f, 0.2f, 0.08f)
        },
        MotifStamp("clochette", "Clochette", StampGroup.FLEURS) {
            smooth(0f, -0.22f, 0.14f, -0.18f, 0.18f, 0.06f, 0.3f, 0.24f, 0.12f, 0.17f, 0f, 0.24f, -0.12f, 0.17f, -0.3f, 0.24f, -0.18f, 0.06f, -0.14f, -0.18f, closed = true)
            stem(0f, -0.2f, 0.06f, -0.38f, 0.2f, -0.5f, w = 0.035f)
        },
        MotifStamp("trefle", "Trèfle", StampGroup.FLEURS) {
            circle(0f, -0.22f, 0.15f)
            circle(-0.17f, -0.02f, 0.15f)
            circle(0.17f, -0.02f, 0.15f)
            circle(0f, -0.06f, 0.08f)
            stem(0f, 0.02f, 0.03f, 0.25f, 0.1f, 0.5f, w = 0.05f)
        },
        MotifStamp("fleuron", "Fleuron", StampGroup.FLEURS) {
            val s = 1f / 30f
            circle(0f, -21 * s + 0.45f, 7.5f * s)
            circle(-8.5f * s, -11 * s + 0.45f, 6.5f * s)
            circle(8.5f * s, -11 * s + 0.45f, 6.5f * s)
            circle(0f, -12 * s + 0.45f, 5.5f * s)
            poly(-4.5f * s, -8 * s + 0.45f, 4.5f * s, -8 * s + 0.45f, 0f, 2 * s + 0.45f)
        },
        // ── Fruits ──
        MotifStamp("fraise", "Fraise", StampGroup.FRUITS) {
            smooth(0f, -0.2f, 0.2f, -0.14f, 0.24f, 0.08f, 0.14f, 0.32f, 0f, 0.42f, -0.14f, 0.32f, -0.24f, 0.08f, -0.2f, -0.14f, closed = true)
            hole {
                for ((x, y) in listOf(-0.08f to -0.04f, 0.07f to -0.06f, -0.13f to 0.1f, 0f to 0.06f, 0.12f to 0.1f, -0.06f to 0.2f, 0.06f to 0.22f, 0f to 0.33f)) circle(x, y, 0.022f)
            }
            for (k in 0 until 5) petal(0f, -0.2f, -170f + k * 40f, 0.2f, 0.09f)
            stem(0f, -0.24f, 0.03f, -0.5f, w = 0.04f)
        },
        MotifStamp("cerise", "Cerises", StampGroup.FRUITS) {
            circle(-0.18f, 0.28f, 0.17f)
            circle(0.17f, 0.3f, 0.17f)
            stem(-0.16f, 0.12f, -0.08f, -0.15f, 0.03f, -0.4f, w = 0.03f)
            stem(0.15f, 0.14f, 0.12f, -0.12f, 0.03f, -0.4f, w = 0.03f)
            leaf(0.03f, -0.4f, -15f, 0.34f, 0.14f)
        },
        MotifStamp("raisin", "Raisin", StampGroup.FRUITS) {
            val rows = listOf(-0.22f to listOf(-0.24f, -0.08f, 0.08f, 0.24f), -0.09f to listOf(-0.16f, 0f, 0.16f), 0.04f to listOf(-0.16f, 0f, 0.16f), 0.17f to listOf(-0.08f, 0.08f), 0.3f to listOf(0f))
            for ((y, xs) in rows) for (x in xs) circle(x, y, 0.085f)
            stem(0f, -0.3f, 0.02f, -0.5f, w = 0.035f)
            leaf(0.02f, -0.42f, -12f, 0.3f, 0.16f)
        },
        MotifStamp("pomme", "Pomme", StampGroup.FRUITS) {
            smooth(0f, -0.18f, 0.14f, -0.26f, 0.3f, -0.18f, 0.36f, 0.02f, 0.28f, 0.26f, 0.12f, 0.36f, 0f, 0.32f, -0.12f, 0.36f, -0.28f, 0.26f, -0.36f, 0.02f, -0.3f, -0.18f, -0.14f, -0.26f, closed = true)
            stem(0f, -0.18f, 0.02f, -0.32f, 0.06f, -0.44f, w = 0.035f)
            leaf(0.05f, -0.36f, -20f, 0.28f, 0.12f)
        },
        MotifStamp("poire", "Poire", StampGroup.FRUITS) {
            smooth(0f, -0.3f, 0.08f, -0.26f, 0.12f, -0.08f, 0.26f, 0.1f, 0.28f, 0.28f, 0.14f, 0.42f, 0f, 0.44f, -0.14f, 0.42f, -0.28f, 0.28f, -0.26f, 0.1f, -0.12f, -0.08f, -0.08f, -0.26f, closed = true)
            stem(0f, -0.28f, -0.02f, -0.42f, 0.04f, -0.5f, w = 0.03f)
            leaf(0f, -0.42f, -30f, 0.24f, 0.1f)
        },
        MotifStamp("grenade", "Grenade", StampGroup.FRUITS) {
            circle(0f, 0.12f, 0.3f)
            poly(-0.12f, -0.14f, -0.15f, -0.32f, -0.06f, -0.22f, 0f, -0.38f, 0.06f, -0.22f, 0.15f, -0.32f, 0.12f, -0.14f)
            leaf(-0.08f, -0.24f, -160f, 0.24f, 0.1f)
        },
        MotifStamp("gland", "Gland", StampGroup.FRUITS) {
            // le fruit, effilé vers le bas, et la cupule qui le coiffe à peine plus large que lui
            moveTo(0f, 0.46f)
            cubicTo(-0.08f, 0.4f, -0.17f, 0.25f, -0.17f, 0.08f); cubicTo(-0.17f, -0.06f, -0.1f, -0.12f, 0f, -0.12f)
            cubicTo(0.1f, -0.12f, 0.17f, -0.06f, 0.17f, 0.08f); cubicTo(0.17f, 0.25f, 0.08f, 0.4f, 0f, 0.46f)
            close()
            moveTo(-0.21f, 0.0f)
            cubicTo(-0.22f, -0.16f, -0.12f, -0.26f, 0f, -0.26f); cubicTo(0.12f, -0.26f, 0.22f, -0.16f, 0.21f, 0.0f)
            cubicTo(0.12f, 0.04f, -0.12f, 0.04f, -0.21f, 0.0f)
            close()
            hole { for (x in listOf(-0.12f, -0.04f, 0.04f, 0.12f)) rect(x - 0.012f, -0.17f, x + 0.012f, -0.04f) }
            stem(0f, -0.25f, 0.02f, -0.36f, 0.07f, -0.44f, w = 0.045f)
        },
        MotifStamp("olive", "Olivier", StampGroup.FRUITS) {
            stem(-0.42f, 0.38f, 0f, 0f, 0.32f, -0.36f, w = 0.03f)
            leaf(-0.25f, 0.21f, -165f, 0.25f, 0.08f)
            leaf(-0.1f, 0.08f, -15f, 0.26f, 0.08f)
            leaf(0.1f, -0.1f, -165f, 0.24f, 0.08f)
            leaf(0.22f, -0.24f, -10f, 0.22f, 0.07f)
            ellipse(0.04f, 0.24f, 0.075f, 0.105f, 20f)
            ellipse(0.22f, 0.06f, 0.07f, 0.1f, 20f)
        },
        // ── Feuilles ──
        MotifStamp("feuille", "Feuille", StampGroup.FEUILLES) {
            leaf(-0.36f, 0.36f, -45f, 0.98f, 0.42f, 0.06f)
            hole { stem(-0.28f, 0.28f, 0f, 0f, 0.24f, -0.24f, w = 0.02f) }
        },
        MotifStamp("vigne", "Feuille de vigne", StampGroup.FEUILLES) {
            val xy = (0 until 60).flatMap { i ->
                val t = i / 60f * 2 * PI.toFloat()
                val r = 0.36f + 0.1f * cos(5 * (t + PI.toFloat() / 2))
                listOf(r * cos(t), 0.04f + r * sin(t))
            }
            smooth(*xy.toFloatArray(), closed = true)
            hole {
                stem(0f, 0.16f, 0f, -0.26f, w = 0.018f)
                stem(0f, 0.16f, -0.24f, -0.06f, w = 0.018f)
                stem(0f, 0.16f, 0.24f, -0.06f, w = 0.018f)
            }
            stem(0f, 0.3f, 0.03f, 0.5f, w = 0.04f)
        },
        MotifStamp("acanthe", "Acanthe", StampGroup.FEUILLES) {
            val pts = listOf(0f, 0f, 8f, -6f, 16f, -14f, 20f, -9f, 26f, -19f, 32f, -14f, 40f, -22f, 44f, -14f, 48f, -10f, 50f, -2f, 46f, 5f, 40f, 4f, 43f, -2f, 38f, -4f, 30f, 2f, 20f, 6f, 10f, 6f, 2f, 4f)
            smooth(*pts.chunked(2).flatMap { listOf(it[0] / 50f - 0.5f, it[1] / 50f + 0.1f) }.toFloatArray(), closed = true)
        },
        MotifStamp("palmette", "Palmette", StampGroup.FEUILLES) {
            val lengths = listOf(0.36f, 0.46f, 0.56f, 0.62f, 0.56f, 0.46f, 0.36f)
            for (k in 0 until 7) petal(0f, 0.3f, -90f + (k - 3) * 22f, lengths[k], 0.34f * lengths[k])
            ellipse(0f, 0.33f, 0.12f, 0.08f)
        },
        MotifStamp("epi", "Épi", StampGroup.FEUILLES) {
            stem(0f, 0.5f, 0f, -0.3f, w = 0.035f)
            for (i in 0 until 5) {
                val y = 0.22f - 0.13f * i
                ellipse(-0.07f, y, 0.05f, 0.1f, -25f)
                ellipse(0.07f, y - 0.06f, 0.05f, 0.1f, 25f)
            }
            ellipse(0f, -0.42f, 0.045f, 0.09f)
        },
        MotifStamp("rameau", "Rameau", StampGroup.FEUILLES) {
            stem(-0.05f, 0.5f, 0f, 0.1f, 0.08f, -0.45f, w = 0.03f)
            leaf(-0.03f, 0.32f, -150f, 0.3f, 0.1f)
            leaf(0f, 0.18f, -30f, 0.3f, 0.1f)
            leaf(0.02f, 0.02f, -150f, 0.28f, 0.1f)
            leaf(0.04f, -0.13f, -30f, 0.27f, 0.1f)
            leaf(0.06f, -0.28f, -145f, 0.22f, 0.08f)
            leaf(0.08f, -0.42f, -70f, 0.14f, 0.07f)
        },
        // ── Symboles ──
        MotifStamp("croix", "Croix", StampGroup.SYMBOLES) {
            rect(-0.06f, -0.5f, 0.06f, 0.5f)
            rect(-0.3f, -0.24f, 0.3f, -0.12f)
        },
        MotifStamp("croix_pattee", "Croix pattée", StampGroup.SYMBOLES) {
            val bar = 0.16f; val flare = bar * 0.85f; val arm = -0.14f; val w = 0.72f
            poly(
                -bar / 2, arm - bar / 2, -bar / 2 - flare / 2, -0.5f, bar / 2 + flare / 2, -0.5f, bar / 2, arm - bar / 2,
                w / 2, arm - bar / 2 - flare / 2, w / 2, arm + bar / 2 + flare / 2, bar / 2, arm + bar / 2,
                bar / 2 + flare / 2, 0.5f, -bar / 2 - flare / 2, 0.5f, -bar / 2, arm + bar / 2,
                -w / 2, arm + bar / 2 + flare / 2, -w / 2, arm - bar / 2 - flare / 2,
            )
        },
        MotifStamp("coeur", "Cœur", StampGroup.SYMBOLES) {
            moveTo(0f, -0.22f)
            cubicTo(-0.08f, -0.4f, -0.46f, -0.37f, -0.43f, -0.1f)
            cubicTo(-0.4f, 0.13f, -0.13f, 0.26f, 0f, 0.44f)
            cubicTo(0.13f, 0.26f, 0.4f, 0.13f, 0.43f, -0.1f)
            cubicTo(0.46f, -0.37f, 0.08f, -0.4f, 0f, -0.22f)
            close()
        },
        MotifStamp("etoile", "Étoile", StampGroup.SYMBOLES) { poly(*star(5, 0.5f, 0.2f)) },
        MotifStamp("etoile8", "Étoile à huit rais", StampGroup.SYMBOLES) {
            val xy = (0 until 16).flatMap { j ->
                val a = -PI.toFloat() / 2 + j * PI.toFloat() / 8
                val r = when { j % 4 == 0 -> 0.5f; j % 2 == 0 -> 0.34f; else -> 0.14f }
                listOf(r * cos(a), r * sin(a))
            }
            poly(*xy.toFloatArray())
        },
        MotifStamp("couronne", "Couronne", StampGroup.SYMBOLES) {
            rect(-0.4f, 0.12f, 0.4f, 0.27f)
            poly(-0.4f, 0.13f, -0.42f, -0.2f, -0.2f, 0.02f, 0f, -0.3f, 0.2f, 0.02f, 0.42f, -0.2f, 0.4f, 0.13f)
            circle(-0.42f, -0.25f, 0.055f); circle(0f, -0.36f, 0.06f); circle(0.42f, -0.25f, 0.055f)
        },
        MotifStamp("ancre", "Ancre", StampGroup.SYMBOLES) {
            circle(0f, -0.4f, 0.085f)
            hole { circle(0f, -0.4f, 0.04f) }
            rect(-0.035f, -0.32f, 0.035f, 0.38f)
            rect(-0.2f, -0.26f, 0.2f, -0.19f)
            stem(-0.34f, 0.1f, -0.25f, 0.32f, 0f, 0.42f, 0.25f, 0.32f, 0.34f, 0.1f, w = 0.06f)
            poly(-0.34f, 0.02f, -0.43f, 0.17f, -0.27f, 0.15f)
            poly(0.34f, 0.02f, 0.43f, 0.17f, 0.27f, 0.15f)
        },
        MotifStamp("poisson", "Poisson", StampGroup.SYMBOLES) {
            smooth(-0.42f, 0f, -0.2f, -0.18f, 0.1f, -0.16f, 0.3f, 0f, 0.1f, 0.16f, -0.2f, 0.18f, closed = true)
            poly(0.26f, 0f, 0.46f, -0.17f, 0.42f, 0f, 0.46f, 0.17f)
            hole { circle(-0.24f, -0.04f, 0.03f) }
        },
        MotifStamp("goutte", "Goutte", StampGroup.SYMBOLES) {
            moveTo(0f, -0.48f)
            cubicTo(0.1f, -0.3f, 0.3f, -0.05f, 0.3f, 0.12f); cubicTo(0.3f, 0.32f, 0.15f, 0.45f, 0f, 0.45f)
            cubicTo(-0.15f, 0.45f, -0.3f, 0.32f, -0.3f, 0.12f); cubicTo(-0.3f, -0.05f, -0.1f, -0.3f, 0f, -0.48f)
            close()
        },
        MotifStamp("calice", "Calice", StampGroup.SYMBOLES) {
            moveTo(-0.24f, -0.12f); lineTo(0.24f, -0.12f); cubicTo(0.24f, 0.06f, 0.12f, 0.14f, 0.035f, 0.15f); lineTo(-0.035f, 0.15f); cubicTo(-0.12f, 0.14f, -0.24f, 0.06f, -0.24f, -0.12f); close()
            ellipse(0f, 0.2f, 0.07f, 0.045f)
            rect(-0.03f, 0.15f, 0.03f, 0.4f)
            moveTo(-0.035f, 0.37f); cubicTo(-0.12f, 0.41f, -0.24f, 0.43f, -0.26f, 0.5f); lineTo(0.26f, 0.5f); cubicTo(0.24f, 0.43f, 0.12f, 0.41f, 0.035f, 0.37f); close()
            circle(0f, -0.32f, 0.15f)
            hole { rect(-0.016f, -0.42f, 0.016f, -0.22f); rect(-0.08f, -0.34f, 0.08f, -0.305f) }
        },
        // ── Volutes (des traits) ──
        MotifStamp("volute", "Volute", StampGroup.TRAITS, line = true) { spiral(0f, 0f, 0.02f, 0.45f, 2.2f, -90f) },
        MotifStamp("vrille", "Vrille", StampGroup.TRAITS, line = true) {
            val tail = listOf(-0.48f, 0.4f, -0.3f, 0.32f, -0.1f, 0.12f)
            val curl = (0..28).flatMap { i ->
                val t = i / 28f
                val a = (180f + 540f * t) * PI.toFloat() / 180f
                val r = 0.26f * (1 - 0.8f * t)
                listOf(0.16f + r * cos(a), 0.12f + r * sin(a))
            }
            smooth(*(tail + curl).toFloatArray(), closed = false)
        },
        MotifStamp("crosse", "Crosse", StampGroup.TRAITS, line = true) {
            val stemPts = listOf(0f, 0.5f, 0f, 0.1f, 0.02f, -0.15f)
            val curl = (0..24).flatMap { i ->
                val t = i / 24f
                val a = (180f + 300f * t) * PI.toFloat() / 180f
                val r = 0.22f * (1 - 0.65f * t)
                listOf(0.24f + r * cos(a), -0.2f + r * sin(a))
            }
            smooth(*(stemPts + curl).toFloatArray(), closed = false)
        },
        MotifStamp("esse", "Rinceau en S", StampGroup.TRAITS, line = true) {
            val a = (0..18).flatMap { i -> val t = i / 18f; val ang = (90f + 330f * t) * PI.toFloat() / 180f; val r = 0.04f + 0.16f * t; listOf(-0.28f + r * cos(ang), -0.22f + r * sin(ang)) }
            val mid = listOf(0f, 0f)
            val b = (0..18).flatMap { i -> val t = 1f - i / 18f; val ang = (270f + 330f * t) * PI.toFloat() / 180f; val r = 0.04f + 0.16f * t; listOf(0.28f + r * cos(ang), 0.22f + r * sin(ang)) }
            smooth(*(a + mid + b).toFloatArray(), closed = false)
        },
    )

    fun byId(id: String?): MotifStamp? = all.firstOrNull { it.id == id }
}

/**
 * La géométrie d'un élément de motif, dans les unités de la cellule (x le long de la bande, y de 0 à 100) : trait à
 * main levée, forme ou motif de la réserve (posé en son centre, étiré, tourné, retourné).
 */
internal object MotifGeometry {
    fun isLine(e: MotifStroke): Boolean = when {
        e.stamp != null -> MotifStamps.byId(e.stamp)?.line == true || !e.filled
        e.shape != null -> enumValues<MotifShape>().firstOrNull { it.name == e.shape }?.line == true || !e.filled
        else -> true
    }

    fun figure(e: MotifStroke): Figure? {
        val unit = when {
            e.stamp != null -> MotifStamps.byId(e.stamp)?.figure ?: return null
            e.shape != null -> enumValues<MotifShape>().firstOrNull { it.name == e.shape }?.figure ?: return null
            else -> return freehand(e)
        }
        val cx = e.points.getOrElse(0) { 0f }
        val cy = e.points.getOrElse(1) { 0f }
        val a = e.rotation * PI.toFloat() / 180f
        val ca = cos(a)
        val sa = sin(a)
        val fx = if (e.flip) -1f else 1f
        return unit.map { p ->
            val x = p.x * fx * 2 * e.sx
            val y = p.y * 2 * e.sy
            Offset(cx + x * ca - y * sa, cy + x * sa + y * ca)
        }
    }

    /** Un trait à main levée : la courbe lisse qui passe par ses points ; un point seul fait un rond. */
    private fun freehand(e: MotifStroke): Figure {
        val xs = e.points.filterIndexed { i, _ -> i % 2 == 0 }
        val ys = e.points.filterIndexed { i, _ -> i % 2 == 1 }
        if (xs.size == 1) return figure { poly(xs[0], ys[0], xs[0] + 0.01f, ys[0], closed = false) }
        return figure { smooth(*xs.zip(ys).flatMap { listOf(it.first, it.second) }.toFloatArray(), closed = false) }
    }

    /** Le centre d'un élément : sa position pour une forme ou un motif, le centre de ses points pour un trait. */
    fun center(e: MotifStroke): Offset =
        if (e.stamp != null || e.shape != null) Offset(e.points.getOrElse(0) { 0f }, e.points.getOrElse(1) { 0f })
        else {
            val xs = e.points.filterIndexed { i, _ -> i % 2 == 0 }
            val ys = e.points.filterIndexed { i, _ -> i % 2 == 1 }
            Offset(xs.average().toFloat(), ys.average().toFloat())
        }

    /** Déplacer, agrandir et tourner un élément autour de son centre. */
    fun transform(e: MotifStroke, pan: Offset, zoom: Float, degrees: Float): MotifStroke {
        if (e.stamp != null || e.shape != null) {
            val c = center(e) + pan
            return e.copy(
                points = listOf(c.x, c.y),
                sx = (e.sx * zoom).coerceIn(1f, 300f),
                sy = (e.sy * zoom).coerceIn(1f, 300f),
                rotation = normalizeDegrees(e.rotation + degrees),
            )
        }
        val c = center(e)
        val a = degrees * PI.toFloat() / 180f
        val pts = e.points.chunked(2).flatMap { (x, y) ->
            val dx = (x - c.x) * zoom
            val dy = (y - c.y) * zoom
            listOf(c.x + dx * cos(a) - dy * sin(a) + pan.x, c.y + dx * sin(a) + dy * cos(a) + pan.y)
        }
        return e.copy(points = pts)
    }

    /** Le reflet gauche-droite d'un élément, autour de son centre. */
    fun flipped(e: MotifStroke): MotifStroke {
        if (e.stamp != null || e.shape != null) return e.copy(flip = !e.flip, rotation = normalizeDegrees(-e.rotation))
        val c = center(e)
        return e.copy(points = e.points.chunked(2).flatMap { (x, y) -> listOf(2 * c.x - x, y) })
    }

    /** L'élément est-il sous [at] (unités de la cellule) ? [slack] : la marge du doigt. */
    fun hit(e: MotifStroke, at: Offset, slack: Float): Boolean {
        if (e.stamp != null || e.shape != null) {
            val c = center(e)
            val a = -e.rotation * PI.toFloat() / 180f
            val dx = at.x - c.x
            val dy = at.y - c.y
            val lx = dx * cos(a) - dy * sin(a)
            val ly = dx * sin(a) + dy * cos(a)
            return kotlin.math.abs(lx) <= e.sx + slack && kotlin.math.abs(ly) <= e.sy + slack
        }
        return e.points.chunked(2).any { (x, y) -> hypot(x - at.x, y - at.y) <= e.width / 2 + slack }
    }

    /** Les quatre coins du cadre de sélection (tourné avec l'élément), en unités de la cellule. */
    fun frame(e: MotifStroke): List<Offset> {
        if (e.stamp != null || e.shape != null) {
            val c = center(e)
            val a = e.rotation * PI.toFloat() / 180f
            return listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f).map { (u, v) ->
                val x = u * (e.sx + 2f)
                val y = v * (e.sy + 2f)
                Offset(c.x + x * cos(a) - y * sin(a), c.y + x * sin(a) + y * cos(a))
            }
        }
        val b = figure(e)?.bounds() ?: return emptyList()
        val m = e.width / 2 + 2f
        return listOf(Offset(b.left - m, b.top - m), Offset(b.right + m, b.top - m), Offset(b.right + m, b.bottom + m), Offset(b.left - m, b.bottom + m))
    }

    private fun normalizeDegrees(d: Float): Float {
        var r = d % 360f
        if (r > 180f) r -= 360f
        if (r < -180f) r += 360f
        return r
    }
}
