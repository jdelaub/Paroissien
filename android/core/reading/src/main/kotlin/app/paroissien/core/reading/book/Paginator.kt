package app.paroissien.core.reading.book

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/** Ce qu'une page dessine, en pixels dans sa zone de texte. */
sealed interface PageItem {
    val y: Float

    /** Les lignes [fromLine, toLine[ d'une mise en page, posées en (x, y) ; [charStart] est l'ancre de la première. */
    class Lines(
        val layout: TextLayoutResult,
        val fromLine: Int,
        val toLine: Int,
        val x: Float,
        override val y: Float,
        val blockId: String,
        val charStart: Int,
    ) : PageItem {
        val top: Float get() = layout.getLineTop(fromLine)
        val height: Float get() = layout.getLineBottom(toLine - 1) - top

        /** Texte lu par TalkBack, sans les césures conditionnelles. */
        fun text(): String = layout.layoutInput.text.text
            .substring(layout.getLineStart(fromLine), layout.getLineEnd(toLine - 1))
            .replace(SOFT_HYPHEN, "")
    }

    /** Un décor posé tel quel : la lettrine. */
    class Glyph(val layout: TextLayoutResult, val x: Float, override val y: Float) : PageItem

    /** Une vignette gravée, masque alpha posé dans le rectangle (x, y, width, height). */
    class Picture(val image: ImageBitmap, val x: Float, override val y: Float, val width: Float, val height: Float) : PageItem

    /** L'encadré d'un emplacement (« texte de la messe du jour »). */
    class Frame(val x: Float, override val y: Float, val width: Float, val height: Float) : PageItem

    /** Le filet vertical entre deux colonnes en regard, de [y] à [y] + [height]. */
    class Rule(val x: Float, override val y: Float, val height: Float) : PageItem
}

@Immutable
class LaidOutPage(val items: List<PageItem>, val runningHead: String, val anchor: Anchor)

/** Le livre mis en page pour une taille de zone de texte. Les pages se retrouvent par ancre, jamais par numéro. */
@Immutable
class PagedBook(val pages: List<LaidOutPage>, private val blockOrder: Map<String, Int>, val elapsedMillis: Long) {

    /** La page qui contient [anchor] : la dernière dont l'ancre de début la précède ou l'égale. */
    fun pageOf(anchor: Anchor): Int {
        val target = key(anchor) ?: return 0
        var low = 0
        var high = pages.lastIndex
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (compareKeys(key(pages[mid].anchor) ?: (0 to 0), target) <= 0) low = mid else high = mid - 1
        }
        return low
    }

    private fun key(anchor: Anchor): Pair<Int, Int>? = blockOrder[anchor.blockId]?.let { it to anchor.charOffset }

    private fun compareKeys(a: Pair<Int, Int>, b: Pair<Int, Int>) =
        if (a.first != b.first) a.first.compareTo(b.first) else a.second.compareTo(b.second)
}

/**
 * Répartit [blocks] en pages de [width] × [height] px. Règles du livre imprimé : un paragraphe ne laisse pas une
 * ligne seule en bas ni en haut de page, un titre reste avec les deux premières lignes qui le suivent, une lettrine
 * reste avec ses lignes. Appelable hors du fil principal.
 */
fun paginate(
    blocks: List<ReadingBlock>,
    measurer: TextMeasurer,
    typography: BookTypography,
    density: Density,
    width: Int,
    height: Int,
): PagedBook {
    val started = System.nanoTime()
    val measure = BlockMeasure(measurer, typography, density, width)
    val pages = ArrayList<LaidOutPage>()
    var items = ArrayList<PageItem>()
    var y = 0f
    var runningHead = ""
    var pageHead: String? = null
    var pageAnchor: Anchor? = null
    val cache = HashMap<Int, Measured>()
    fun measured(i: Int) = cache.getOrPut(i) { measure(blocks[i]) }
    fun vignetteHeight(v: ReadingBlock.Vignette): Float {
        val ratio = v.image.width / v.image.height.toFloat().coerceAtLeast(1f)
        return minOf(width / ratio, height * 0.4f)
    }

    fun closePage() {
        if (items.isEmpty()) return
        pages += LaidOutPage(items, pageHead ?: runningHead, pageAnchor ?: Anchor(blocks.first().id, 0))
        items = ArrayList()
        y = 0f
        pageHead = null
        pageAnchor = null
    }

    for (index in blocks.indices) {
        val block = blocks[index]
        if (block is ReadingBlock.PageBreak) {
            closePage()
            continue
        }
        if (block is ReadingBlock.Heading && block.runningHead != null) runningHead = block.runningHead
        if (block is ReadingBlock.Vignette) {
            // Pleine largeur du texte, au plus 40 % de la hauteur ; elle part avec le début du bloc suivant.
            val ratio = block.image.width / block.image.height.toFloat().coerceAtLeast(1f)
            val h = vignetteHeight(block)
            val w = h * ratio
            val gap = with(density) { 4.dp.toPx() }
            // ce que le bloc suivant demande de garder avec lui (un titre garde le début de son paragraphe)
            fun solid(i: Int) = blocks[i] !is ReadingBlock.PageBreak && blocks[i] !is ReadingBlock.Vignette
            val next = (index + 1 until blocks.size).firstOrNull(::solid)
            val after = next?.let { i ->
                val n = measured(i)
                val following = (i + 1 until blocks.size).firstOrNull(::solid)
                val keep = if (n.keepWithNext && following != null) n.height + measured(following).let { it.gapBefore + it.minStart } else 0f
                n.gapBefore + maxOf(n.minStart, keep)
            } ?: 0f
            if (y > 0f && y + h + gap + after > height) closePage()
            if (pageHead == null) pageHead = runningHead
            items += PageItem.Picture(block.image, (width - w) / 2, y, w, h)
            y += h + gap
            continue
        }
        val m = measured(index)
        if (m.units.isEmpty()) continue
        val next = (index + 1 until blocks.size).firstOrNull { blocks[it] !is ReadingBlock.PageBreak }
        // Un titre reste avec ce qui le suit : le début du bloc, ou l'image et le début du texte qu'elle illustre.
        val keepHeight = if (m.keepWithNext && next != null) m.height + when (val n = blocks[next]) {
            is ReadingBlock.Vignette -> vignetteHeight(n) + with(density) { 4.dp.toPx() } +
                ((next + 1 until blocks.size).firstOrNull { blocks[it] !is ReadingBlock.PageBreak && blocks[it] !is ReadingBlock.Vignette }
                    ?.let { measured(it).let { f -> f.gapBefore + f.minStart } } ?: 0f)
            else -> measured(next).let { it.gapBefore + it.minStart }
        } else 0f

        if (y > 0f && y + m.gapBefore + maxOf(m.minStart, keepHeight) > height) closePage()
        if (y > 0f) y += m.gapBefore

        var u = 0
        while (u < m.units.size) {
            // Le haut du bloc pour sa première ligne (un encadré commence au-dessus de son texte), sinon la ligne reprise.
            val origin = y - if (u == 0) 0f else m.units[u].top
            var fit = 0
            while (u + fit < m.units.size && origin + m.units[u + fit].bottom <= height + 0.5f) fit++
            val remaining = m.units.size - u
            if (fit < remaining) {
                val minFirst = minOf(remaining, if (u == 0) maxOf(2, m.keepTogether) else 2)
                if (fit < minFirst && y > 0f) {
                    closePage()
                    continue
                }
                if (remaining - fit == 1 && fit - 1 >= minFirst) fit-- // pas de veuve en haut de la page suivante
                if (fit == 0) fit = 1 // ligne plus haute que la page : on la pose quand même
            }
            if (pageHead == null) pageHead = runningHead
            if (u == 0) m.decorations.forEach { items += PageItem.Glyph(it.layout, it.x, origin + it.y) }
            if (u == 0) m.frame?.let { items += PageItem.Frame(0f, origin, width.toFloat(), m.height) }
            val placedTop = origin + m.units[u].top
            val placedBottom = origin + (u until u + fit).maxOf { m.units[it].bottom }
            m.ruleX?.let { items += PageItem.Rule(it, placedTop, placedBottom - placedTop) }
            var first = u
            while (first < u + fit) {
                val part = m.units[first].part
                var last = first
                while (last + 1 < u + fit && m.units[last + 1].part === part) last++
                val line = m.units[first].line
                val lines = PageItem.Lines(
                    layout = part.layout, fromLine = line, toLine = m.units[last].line + 1, x = part.x,
                    y = origin + m.units[first].top, blockId = m.blockId,
                    charStart = part.charBase + canonicalOffset(part.layout.layoutInput.text, part.layout.getLineStart(line)),
                )
                if (pageAnchor == null) pageAnchor = Anchor(lines.blockId, lines.charStart)
                items += lines
                first = last + 1
            }
            y = placedBottom
            u += fit
            if (u >= m.units.size && m.frame != null) y = origin + m.height
            if (u < m.units.size) closePage()
        }
    }
    closePage()
    val order = blocks.withIndex().associate { (i, b) -> b.id to i }
    return PagedBook(pages, order, (System.nanoTime() - started) / 1_000_000)
}

private const val SOFT_HYPHEN = "\u00AD"

/**
 * Décalage canonique (ADR 004) d'un index du texte affiché : en points de code, sans les césures insérées à
 * l'affichage ni les caractères annotés hors canon (numéros de versets). L'ancre ne dépend donc pas de la mise en page.
 */
internal fun canonicalOffset(text: AnnotatedString, index: Int): Int {
    val excluded = text.getStringAnnotations(BookTypography.NON_CANONICAL, 0, index)
    var count = 0
    for (i in 0 until index) {
        val c = text[i]
        if (c == '\u00AD' || Character.isLowSurrogate(c) || excluded.any { i >= it.start && i < it.end }) continue
        count++
    }
    return count
}

private class Part(val layout: TextLayoutResult, val x: Float, val charBase: Int)

private class LineUnit(val part: Part, val line: Int, val top: Float, val bottom: Float)

private class Decoration(val layout: TextLayoutResult, val x: Float, val y: Float)

private class Measured(
    val blockId: String,
    val units: List<LineUnit>,
    val decorations: List<Decoration> = emptyList(),
    val gapBefore: Float,
    val keepWithNext: Boolean = false,
    /** Lignes de tête qui ne se séparent pas (lettrine, titre). */
    val keepTogether: Int = 0,
    /** Marge intérieure d'un encadré autour des lignes, ou null. */
    val frame: Float? = null,
    /** Abscisse du filet entre deux colonnes en regard, ou null. */
    val ruleX: Float? = null,
) {
    val height: Float get() = (units.maxOfOrNull { it.bottom } ?: 0f) + (frame ?: 0f)

    /** Hauteur minimale à poser ensemble au début du bloc. */
    val minStart: Float get() = units.getOrNull(minOf(units.size, maxOf(2, keepTogether)) - 1)?.bottom ?: 0f
}

private class BlockMeasure(
    private val measurer: TextMeasurer,
    private val t: BookTypography,
    private val density: Density,
    val width: Int,
    private val ragged: Boolean = false,
) {
    private val blockGap = with(density) { 8.dp.toPx() }
    private val headingGap = with(density) { 20.dp.toPx() }
    private val capGap = with(density) { 6.dp.toPx() }

    operator fun invoke(block: ReadingBlock): Measured = when (block) {
        is ReadingBlock.Title -> {
            val title = layout(t.render(block.text, BlockLang.FR), t.title)
            val parts = mutableListOf(stack(title, 0f))
            // Le sous-titre prolonge le titre dans les ancres : ses positions suivent celles du titre.
            block.subtitle?.let { parts += stack(layout(t.render(it, BlockLang.FR), t.subtitle), title.size.height + blockGap, charBase = canonicalOffset(title.layoutInput.text, title.layoutInput.text.length)) }
            val units = parts.flatten()
            Measured(block.id, units, gapBefore = headingGap, keepTogether = units.size)
        }
        is ReadingBlock.Heading -> {
            val units = stack(layout(t.render(block.text, block.lang, hyphenate = false), if (block.minor) t.minorHeading else t.heading), 0f)
            Measured(block.id, units, gapBefore = headingGap, keepWithNext = true, keepTogether = units.size)
        }
        is ReadingBlock.Rubric -> Measured(block.id, stack(layout(t.render(block.text, BlockLang.FR), t.rubric.ragged()), 0f), gapBefore = blockGap)
        is ReadingBlock.Versicle -> Measured(block.id, stack(layout(t.render(block.text, block.lang), t.versicle(block.lang).ragged()), 0f), gapBefore = blockGap)
        is ReadingBlock.Ornament -> Measured(block.id, stack(layout(AnnotatedString("❦"), t.ornament), 0f), gapBefore = headingGap)
        is ReadingBlock.Paragraph -> paragraph(block)
        is ReadingBlock.PageBreak, is ReadingBlock.Vignette -> Measured(block.id, emptyList(), gapBefore = 0f)
        is ReadingBlock.Slot -> slot(block)
        is ReadingBlock.Facing -> facing(block)
    }

    /** L'encadré d'un emplacement : son texte centré, avec une marge intérieure ; il ne se coupe pas. */
    private fun slot(block: ReadingBlock.Slot): Measured {
        val pad = with(density) { 8.dp.toPx() }
        val text = layout(t.render(block.text, BlockLang.FR), t.paragraph(BlockLang.FR, TextRole.CENTERED), (width - 2 * pad).toInt())
        val part = Part(text, pad, 0)
        val units = (0 until text.lineCount).map { LineUnit(part, it, pad + text.getLineTop(it), pad + text.getLineBottom(it)) }
        // L'encadré annonce ce qui suit : il ne reste pas seul en bas de page.
        return Measured(block.id, units, gapBefore = headingGap, keepWithNext = true, keepTogether = units.size, frame = pad)
    }

    /** Les colonnes en regard, à la moitié de la largeur : les lignes des deux colonnes, rangées par hauteur. */
    private val gutter = with(density) { 14.dp.toPx() }.toInt()
    private val column: BlockMeasure by lazy { BlockMeasure(measurer, t, density, (width - gutter) / 2, ragged = true) }

    /** En colonne étroite, versets et rubriques vont au fer à gauche : justifiés, deux mots s'y écartent trop. */
    private fun TextStyle.ragged(): TextStyle = if (ragged) copy(textAlign = TextAlign.Start) else this

    private fun facing(block: ReadingBlock.Facing): Measured {
        val left = column(block.left)
        val right = column(block.right)
        val dx = (column.width + gutter).toFloat()
        val moved = HashMap<Part, Part>()
        fun shift(part: Part) = moved.getOrPut(part) { Part(part.layout, part.x + dx, part.charBase) }
        val units = (left.units + right.units.map { LineUnit(shift(it.part), it.line, it.top, it.bottom) }).sortedWith(compareBy({ it.top }, { it.part.x }))
        val heading = left.keepTogether >= left.units.size || right.keepTogether >= right.units.size
        return Measured(
            block.id, units,
            decorations = left.decorations + right.decorations.map { Decoration(it.layout, it.x + dx, it.y) },
            gapBefore = maxOf(left.gapBefore, right.gapBefore),
            keepWithNext = left.keepWithNext || right.keepWithNext,
            keepTogether = if (heading) units.size else 2 * maxOf(2, left.keepTogether, right.keepTogether),
            ruleX = column.width + gutter / 2f,
        )
    }

    private fun paragraph(block: ReadingBlock.Paragraph): Measured {
        val style = t.paragraph(block.lang, block.role)
        val text = t.render(block.text, block.lang)
        val gap = if (block.tight) 0f else blockGap
        if (block.dropCap && text.text.firstOrNull()?.isLetter() == true) dropCap(block, text, style, gap)?.let { return it }
        return Measured(block.id, stack(layout(text, style), 0f), gapBefore = gap)
    }

    /**
     * Lettrine sur deux lignes : la capitale rouge, puis le début du texte composé en retrait sur deux lignes, puis
     * la suite à pleine largeur. La capitale s'aligne sur la ligne de base de la deuxième ligne.
     */
    private fun dropCap(block: ReadingBlock.Paragraph, inline: AnnotatedString, style: TextStyle, gap: Float): Measured? {
        val lines = 2
        val bodyPx = with(density) { style.fontSize.toPx() }
        val lineHeightPx = with(density) { style.lineHeight.toPx() }
        val capHeightRatio = 0.66f
        val capSize = with(density) { (((lines - 1) * lineHeightPx + capHeightRatio * bodyPx) / capHeightRatio).toSp() }
        // La capitale garde sa largeur naturelle : c'est elle qui fixe le retrait des deux premières lignes.
        val cap = measurer.measure(AnnotatedString(inline.text.substring(0, 1)), t.dropCap.copy(fontSize = capSize), density = density)
        val capWidth = cap.size.width + capGap
        if (capWidth > width / 3f) return null
        val rest = inline.subSequence(1, inline.length)
        val head = layout(rest, style, (width - capWidth).toInt())
        val headLines = minOf(lines, head.lineCount)
        val headEnd = head.getLineEnd(headLines - 1)
        val units = ArrayList<LineUnit>()
        val headPart = Part(head, capWidth, charBase = 1)
        for (i in 0 until headLines) units += LineUnit(headPart, i, head.getLineTop(i), head.getLineBottom(i))
        if (headEnd < rest.length) {
            val tail = layout(rest.subSequence(headEnd, rest.length), style)
            val offset = head.getLineBottom(headLines - 1)
            val tailPart = Part(tail, 0f, charBase = 1 + canonicalOffset(rest, headEnd))
            for (i in 0 until tail.lineCount) units += LineUnit(tailPart, i, offset + tail.getLineTop(i), offset + tail.getLineBottom(i))
        }
        val capY = head.getLineBaseline(headLines - 1) - cap.firstBaseline
        return Measured(block.id, units, listOf(Decoration(cap, 0f, capY)), gapBefore = gap, keepTogether = headLines)
    }

    // Largeur imposée : sans largeur minimale, TextMeasurer réduit un texte court à sa largeur naturelle et le
    // centrage des titres n'a plus d'effet.
    private fun layout(text: AnnotatedString, style: TextStyle, maxWidth: Int = width): TextLayoutResult =
        measurer.measure(text, style, constraints = Constraints.fixedWidth(maxWidth), density = density)

    private fun stack(layout: TextLayoutResult, offset: Float, charBase: Int = 0): List<LineUnit> {
        val part = Part(layout, 0f, charBase)
        return (0 until layout.lineCount).map { LineUnit(part, it, offset + layout.getLineTop(it), offset + layout.getLineBottom(it)) }
    }
}

