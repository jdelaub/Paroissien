package app.paroissien.core.imaging

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Un point en coordonnées relatives : 0 à 1 sur la largeur et la hauteur de l'image. */
data class Point(val x: Float, val y: Float)

/** Les quatre coins d'une carte, dans l'ordre : haut gauche, haut droite, bas droite, bas gauche. */
data class Quad(val topLeft: Point, val topRight: Point, val bottomRight: Point, val bottomLeft: Point) {
    val points: List<Point> get() = listOf(topLeft, topRight, bottomRight, bottomLeft)

    companion object {
        /** Toute l'image, avec une petite marge : le point de départ quand rien n'est détecté. */
        val FULL = Quad(Point(0.04f, 0.04f), Point(0.96f, 0.04f), Point(0.96f, 0.96f), Point(0.04f, 0.96f))

        /** Remet quatre points dans l'ordre haut gauche, haut droite, bas droite, bas gauche. */
        fun ordered(points: List<Point>): Quad {
            require(points.size == 4)
            val cx = points.map { it.x }.average().toFloat()
            val cy = points.map { it.y }.average().toFloat()
            // Dans le sens des aiguilles d'une montre à l'écran (y vers le bas), en partant du haut gauche.
            val sorted = points.sortedBy { atan2((it.y - cy).toDouble(), (it.x - cx).toDouble()) }
            val start = sorted.indices.minBy { sorted[it].x + sorted[it].y }
            val r = List(4) { sorted[(start + it) % 4] }
            return Quad(r[0], r[1], r[2], r[3])
        }
    }
}

/** Une image RVB en mémoire : `pixels` au format ARGB d'Android, ligne par ligne. */
class RgbImage(val width: Int, val height: Int, val pixels: IntArray) {
    init { require(pixels.size == width * height) }
}

/**
 * Détecte une carte posée sur un fond (rapport 09, §3.3), sans bibliothèque externe :
 * 1. la couleur du fond est estimée sur le pourtour de la photo ;
 * 2. chaque pixel reçoit sa distance à cette couleur, lissée, puis seuillée par la méthode d'Otsu ;
 * 3. une fermeture morphologique rebouche les détails ; on réunit les composantes qui comptent (une gravure foncée ou
 *    des lignes de texte peuvent couper la carte en morceaux), les poussières du fond étant écartées ;
 * 4. de son enveloppe convexe simplifiée, on retient le quadrilatère d'aire maximale.
 * L'image d'entrée doit être petite (500 px de côté au plus) : l'utilisateur ajuste ensuite les coins au doigt.
 */
object CardDetector {

    fun detect(image: RgbImage): Quad? {
        val w = image.width
        val h = image.height
        if (w < 16 || h < 16) return null
        val distance = blur(blur(distanceToBackground(image), w, h), w, h)
        val threshold = otsu(distance)
        if (threshold < 12) return null // photo presque uniforme : rien à détacher du fond
        var mask = BooleanArray(w * h) { distance[it] > threshold }
        mask = erode(erode(dilate(dilate(mask, w, h), w, h), w, h), w, h)
        val component = significantComponents(mask, w, h, minSize = w * h / 2000)
        if (component.size < w * h * 0.08) return null
        val hull = convexHull(rowExtremes(component, w))
        if (hull.size < 4) return null
        val quad = bestQuad(hull) ?: return null
        if (polygonArea(quad) < w * h * 0.08) return null
        return Quad.ordered(quad.map { Point(it.first / (w - 1f), it.second / (h - 1f)) })
    }

    // ── Fond et distance ────────────────────────────────────────────────────

    private fun distanceToBackground(image: RgbImage): IntArray {
        val w = image.width
        val h = image.height
        val band = max(2, (minOf(w, h) * 0.03f).roundToInt())
        val rs = ArrayList<Int>()
        val gs = ArrayList<Int>()
        val bs = ArrayList<Int>()
        for (y in 0 until h) for (x in 0 until w) {
            if (x < band || y < band || x >= w - band || y >= h - band) {
                val p = image.pixels[y * w + x]
                rs += (p shr 16) and 0xFF; gs += (p shr 8) and 0xFF; bs += p and 0xFF
            }
        }
        val r0 = median(rs)
        val g0 = median(gs)
        val b0 = median(bs)
        return IntArray(w * h) { i ->
            val p = image.pixels[i]
            val dr = ((p shr 16) and 0xFF) - r0
            val dg = ((p shr 8) and 0xFF) - g0
            val db = (p and 0xFF) - b0
            (sqrt((dr * dr + dg * dg + db * db).toDouble()) * 255 / 441.7).roundToInt().coerceIn(0, 255)
        }
    }

    private fun median(values: ArrayList<Int>): Int = values.sorted()[values.size / 2]

    /** Moyenne 3 × 3 (deux passes séparables). */
    private fun blur(src: IntArray, w: Int, h: Int): IntArray {
        val tmp = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            var s = 0; var n = 0
            for (dx in -1..1) { val xx = x + dx; if (xx in 0 until w) { s += src[y * w + xx]; n++ } }
            tmp[y * w + x] = s / n
        }
        val out = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            var s = 0; var n = 0
            for (dy in -1..1) { val yy = y + dy; if (yy in 0 until h) { s += tmp[yy * w + x]; n++ } }
            out[y * w + x] = s / n
        }
        return out
    }

    /** Seuil d'Otsu sur des valeurs de 0 à 255 : celui qui sépare le mieux deux populations. */
    internal fun otsu(values: IntArray): Int {
        val hist = IntArray(256)
        values.forEach { hist[it]++ }
        val total = values.size.toDouble()
        val sumAll = hist.indices.sumOf { it * hist[it].toDouble() }
        var sumB = 0.0
        var weightB = 0.0
        var best = 0.0
        var threshold = 0
        for (t in 0 until 256) {
            weightB += hist[t]
            if (weightB == 0.0) continue
            val weightF = total - weightB
            if (weightF == 0.0) break
            sumB += t * hist[t]
            val mB = sumB / weightB
            val mF = (sumAll - sumB) / weightF
            val between = weightB * weightF * (mB - mF) * (mB - mF)
            if (between > best) { best = between; threshold = t }
        }
        return threshold
    }

    // ── Morphologie et composantes ──────────────────────────────────────────

    private fun dilate(m: BooleanArray, w: Int, h: Int) = BooleanArray(w * h) { i ->
        val x = i % w; val y = i / w
        (-1..1).any { dy -> (-1..1).any { dx -> val xx = x + dx; val yy = y + dy; xx in 0 until w && yy in 0 until h && m[yy * w + xx] } }
    }

    private fun erode(m: BooleanArray, w: Int, h: Int) = BooleanArray(w * h) { i ->
        val x = i % w; val y = i / w
        (-1..1).all { dy -> (-1..1).all { dx -> val xx = x + dx; val yy = y + dy; xx !in 0 until w || yy !in 0 until h || m[yy * w + xx] } }
    }

    /** Indices des pixels des composantes 4-connexes d'au moins [minSize] pixels. */
    private fun significantComponents(mask: BooleanArray, w: Int, h: Int, minSize: Int): IntArray {
        val label = IntArray(w * h)
        val queue = IntArray(w * h)
        val kept = ArrayList<IntArray>()
        var current = 0
        for (start in mask.indices) {
            if (!mask[start] || label[start] != 0) continue
            current++
            var head = 0; var tail = 0
            queue[tail++] = start; label[start] = current
            while (head < tail) {
                val i = queue[head++]
                val x = i % w; val y = i / w
                fun visit(j: Int) { if (mask[j] && label[j] == 0) { label[j] = current; queue[tail++] = j } }
                if (x > 0) visit(i - 1)
                if (x < w - 1) visit(i + 1)
                if (y > 0) visit(i - w)
                if (y < h - 1) visit(i + w)
            }
            if (tail >= minSize) kept += queue.copyOf(tail)
        }
        return kept.fold(IntArray(0)) { acc, part -> acc + part }
    }

    /** Le pixel le plus à gauche et le plus à droite de chaque ligne : assez pour l'enveloppe convexe. */
    private fun rowExtremes(component: IntArray, w: Int): List<Pair<Float, Float>> {
        val minX = HashMap<Int, Int>()
        val maxX = HashMap<Int, Int>()
        component.forEach { i ->
            val x = i % w; val y = i / w
            minX[y] = minOf(minX[y] ?: x, x)
            maxX[y] = maxOf(maxX[y] ?: x, x)
        }
        return minX.keys.flatMap { y -> listOf(minX.getValue(y).toFloat() to y.toFloat(), maxX.getValue(y).toFloat() to y.toFloat()) }
    }

    // ── Géométrie ───────────────────────────────────────────────────────────

    /** Enveloppe convexe (chaîne monotone d'Andrew), dans le sens trigonométrique. */
    internal fun convexHull(points: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        val p = points.distinct().sortedWith(compareBy({ it.first }, { it.second }))
        if (p.size < 3) return p
        fun cross(o: Pair<Float, Float>, a: Pair<Float, Float>, b: Pair<Float, Float>) =
            (a.first - o.first) * (b.second - o.second) - (a.second - o.second) * (b.first - o.first)
        val lower = ArrayList<Pair<Float, Float>>()
        for (pt in p) { while (lower.size >= 2 && cross(lower[lower.size - 2], lower.last(), pt) <= 0) lower.removeAt(lower.lastIndex); lower += pt }
        val upper = ArrayList<Pair<Float, Float>>()
        for (pt in p.asReversed()) { while (upper.size >= 2 && cross(upper[upper.size - 2], upper.last(), pt) <= 0) upper.removeAt(upper.lastIndex); upper += pt }
        return lower.dropLast(1) + upper.dropLast(1)
    }

    /** Simplifie l'enveloppe (Douglas-Peucker sur le contour fermé), puis garde les 4 sommets d'aire maximale. */
    private fun bestQuad(hull: List<Pair<Float, Float>>): List<Pair<Float, Float>>? {
        val perimeter = hull.indices.sumOf { i -> val a = hull[i]; val b = hull[(i + 1) % hull.size]; hypot((a.first - b.first).toDouble(), (a.second - b.second).toDouble()) }
        var epsilon = perimeter * 0.01
        var simple = simplifyClosed(hull, epsilon)
        while (simple.size > 12) { epsilon *= 1.5; simple = simplifyClosed(hull, epsilon) }
        val candidates = if (simple.size >= 4) simple else hull
        if (candidates.size < 4) return null
        var best: List<Pair<Float, Float>>? = null
        var bestArea = 0.0
        val n = candidates.size
        for (a in 0 until n) for (b in a + 1 until n) for (c in b + 1 until n) for (d in c + 1 until n) {
            val q = listOf(candidates[a], candidates[b], candidates[c], candidates[d])
            val area = polygonArea(q)
            if (area > bestArea) { bestArea = area; best = q }
        }
        return best
    }

    private fun simplifyClosed(points: List<Pair<Float, Float>>, epsilon: Double): List<Pair<Float, Float>> {
        if (points.size <= 4) return points
        // On coupe le contour entre les deux points les plus éloignés, puis on simplifie chaque moitié.
        var i0 = 0; var i1 = 0; var far = -1.0
        for (i in points.indices) for (j in i + 1 until points.size) {
            val d = hypot((points[i].first - points[j].first).toDouble(), (points[i].second - points[j].second).toDouble())
            if (d > far) { far = d; i0 = i; i1 = j }
        }
        val first = points.subList(i0, i1 + 1)
        val second = points.subList(i1, points.size) + points.subList(0, i0 + 1)
        return douglasPeucker(first, epsilon).dropLast(1) + douglasPeucker(second, epsilon).dropLast(1)
    }

    private fun douglasPeucker(points: List<Pair<Float, Float>>, epsilon: Double): List<Pair<Float, Float>> {
        if (points.size < 3) return points
        val a = points.first(); val b = points.last()
        var index = 0; var dmax = 0.0
        for (i in 1 until points.lastIndex) {
            val d = distanceToSegment(points[i], a, b)
            if (d > dmax) { dmax = d; index = i }
        }
        return if (dmax > epsilon) douglasPeucker(points.subList(0, index + 1), epsilon).dropLast(1) + douglasPeucker(points.subList(index, points.size), epsilon)
        else listOf(a, b)
    }

    private fun distanceToSegment(p: Pair<Float, Float>, a: Pair<Float, Float>, b: Pair<Float, Float>): Double {
        val dx = (b.first - a.first).toDouble(); val dy = (b.second - a.second).toDouble()
        val len = hypot(dx, dy)
        if (len == 0.0) return hypot((p.first - a.first).toDouble(), (p.second - a.second).toDouble())
        return abs(dy * p.first - dx * p.second + b.first * a.second - b.second * a.first) / len
    }

    /** Aire d'un polygone dont les sommets sont donnés dans l'ordre du contour. */
    internal fun polygonArea(points: List<Pair<Float, Float>>): Double {
        var s = 0.0
        for (i in points.indices) {
            val a = points[i]; val b = points[(i + 1) % points.size]
            s += a.first.toDouble() * b.second - b.first.toDouble() * a.second
        }
        return abs(s) / 2
    }
}
