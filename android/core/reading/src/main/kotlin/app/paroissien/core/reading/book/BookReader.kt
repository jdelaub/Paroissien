package app.paroissien.core.reading.book

import androidx.compose.foundation.Canvas
import app.paroissien.core.designsystem.drawRibbon
import app.paroissien.core.designsystem.RibbonStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import app.paroissien.core.designsystem.EdgeFinish
import app.paroissien.core.designsystem.PaperColors
import app.paroissien.core.designsystem.PaperWeight
import app.paroissien.core.designsystem.paperBackground
import app.paroissien.core.reading.turn.MekuriConfiguration
import app.paroissien.core.reading.turn.MekuriPager
import app.paroissien.core.reading.turn.MekuriSpread
import app.paroissien.core.reading.turn.MekuriSpreadLayout
import app.paroissien.core.reading.turn.rememberMekuriPagerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

/** Page simple ou double page (rapport 07, D7 ; rapport 08) : en Auto, deux pages si chacune garde 300 dp de large. */
enum class SpreadMode { AUTO, SINGLE, DOUBLE }

/**
 * Un ruban du livre (rapport 09). [anchor] le situe dans l'unité ouverte ; sinon [elsewhere] dit s'il marque une
 * unité précédente (-1) ou suivante (+1), pour que sa queue pende du bon côté.
 */
data class RibbonMark(val slot: Int, val anchor: Anchor?, val elsewhere: Int = 0)

/**
 * L'apparence matérielle du livre ouvert : sa tranche (dorée, rouge, naturelle), son papier (plus il est fin, plus
 * les pages de la tranche sont serrées, et le papier bible laisse deviner le verso), sa place dans l'ouvrage
 * ([progress], de 0 à 1, pour l'épaisseur de la tranche de chaque côté) et sa couverture, page d'avant la première.
 */
class BookLook(
    val edges: EdgeFinish = EdgeFinish.PLAIN,
    val weight: PaperWeight = PaperWeight.REGULAR,
    val showThrough: Boolean = false,
    val progress: ClosedFloatingPointRange<Float> = 0f..1f,
    val thickness: Dp = 6.dp,
    val cover: (@Composable () -> Unit)? = null,
)

private val PageHorizontal = 28.dp
private val PageTop = 18.dp
private val PageBottom = 14.dp
private val Header = 30.dp
private val Footer = 26.dp
/** Bande réservée sous le folio, où pendent les queues des rubans : jamais de texte dessous. */
private val RibbonBand = 46.dp
private val TailWidth = 22.dp
private val TailHeight = 40.dp
private val MinimumSpreadPageWidth = 300.dp

/**
 * Un livre paginé qu'on feuillette. La position est une [Anchor] : après une rotation, un changement de police ou
 * de taille, on rouvre la page qui la contient, et l'ancre ne change que si le lecteur tourne la page (pas de dérive).
 */
@Composable
fun BookReader(
    blocks: List<ReadingBlock>,
    typography: BookTypography,
    anchor: Anchor?,
    onAnchorChange: (Anchor) -> Unit,
    modifier: Modifier = Modifier,
    spreadMode: SpreadMode = SpreadMode.AUTO,
    configuration: MekuriConfiguration = MekuriConfiguration(),
    slide: Boolean = false,
    protectEdges: Boolean = false,
    onCenterTap: (() -> Unit)? = null,
    onPaginated: (PagedBook) -> Unit = {},
    paginationContext: CoroutineContext = Dispatchers.Default,
    ribbons: List<RibbonMark> = emptyList(),
    onRibbonTap: (Int) -> Unit = {},
    cards: List<CardMark> = emptyList(),
    onCardTap: (String) -> Unit = {},
    onTurnPastEnd: (() -> Unit)? = null,
    onTurnPastStart: (() -> Unit)? = null,
    initialAtEnd: Boolean = false,
    look: BookLook = BookLook(),
) {
    val paper = typography.paper
    Box(modifier.fillMaxSize().background(paper.background)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val double = when (spreadMode) {
                SpreadMode.SINGLE -> false
                SpreadMode.DOUBLE -> true
                SpreadMode.AUTO -> maxWidth > maxHeight && maxWidth / 2 >= MinimumSpreadPageWidth
            }
            // Le papier court jusqu'au bord de l'écran, sous la caméra ; seul le texte s'en écarte.
            val cutout = WindowInsets.displayCutout.asPaddingValues()
            val direction = LocalLayoutDirection.current
            val margins = PageMargins(
                top = PageTop + cutout.calculateTopPadding(),
                bottom = PageBottom + RibbonBand + cutout.calculateBottomPadding(),
                horizontal = PageHorizontal + max(cutout.calculateStartPadding(direction), cutout.calculateEndPadding(direction)),
            )
            val pageWidth = if (double) maxWidth / 2 else maxWidth
            val contentWidth = pageWidth - margins.horizontal * 2
            val contentHeight = maxHeight - margins.top - Header - Footer - margins.bottom
            val widthPx = with(density) { contentWidth.roundToPx() }
            val heightPx = with(density) { contentHeight.roundToPx() }

            val measurer = rememberTextMeasurer(cacheSize = 0)
            var book by remember { mutableStateOf<PagedBook?>(null) }
            val paginated by rememberUpdatedState(onPaginated)
            LaunchedEffect(blocks, typography, widthPx, heightPx, density) {
                val result = withContext(paginationContext) { paginate(blocks, measurer, typography, density, widthPx, heightPx) }
                book = result
                paginated(result)
            }
            val paged = book ?: return@BoxWithConstraints
            // Avec une couverture, elle est la page 0 du pager : le texte commence à 1.
            val offset = if (look.cover != null) 1 else 0
            val count = paged.pages.size + offset

            val state = rememberMekuriPagerState(pageCount = count, initialPage = anchor?.let { paged.pageOf(it) + offset } ?: 0)
            val currentAnchor by rememberUpdatedState(anchor)
            val anchorChanged by rememberUpdatedState(onAnchorChange)
            LaunchedEffect(paged, double, state) {
                val held = currentAnchor
                when {
                    held != null -> state.scrollToPage(paged.pageOf(held) + offset)
                    initialAtEnd -> state.scrollToPage(count - 1)
                }
                snapshotFlow { state.currentPage }.collect { page ->
                    val shown = visiblePages(page, double, count)
                    val held = currentAnchor?.let { paged.pageOf(it) + offset }
                    // Sur la couverture, rien ne change : la position reste celle du texte.
                    if (held == null || held !in shown) paged.pages.getOrNull(page - offset)?.let { anchorChanged(it.anchor) }
                }
            }

            // Saut demandé de l'extérieur (sommaire, ruban, recherche) : on ouvre la page de l'ancre si elle n'est pas
            // déjà sous les yeux ; un tour de page ordinaire ne déclenche donc rien.
            LaunchedEffect(paged, anchor) {
                val target = anchor?.let { paged.pageOf(it) + offset } ?: return@LaunchedEffect
                if (target !in visiblePages(state.currentPage, double, count)) state.scrollToPage(target)
            }

            MekuriPager(
                state = state,
                modifier = Modifier.fillMaxSize(),
                configuration = configuration,
                onCenterTap = onCenterTap,
                spread = if (double) MekuriSpread.Double else MekuriSpread.Single,
                coverStandsAlone = true,
                pageAspectRatio = pageWidth.value / maxHeight.value,
                slide = slide,
                onBlockedTurn = { forward -> if (forward) onTurnPastEnd?.invoke() else onTurnPastStart?.invoke() },
            ) { index ->
                if (index < offset) {
                    look.cover?.invoke()
                } else {
                    val i = index - offset
                    val side = sideOf(index, double)
                    val here = remember(ribbons, paged, i) { ribbons.filter { it.anchor != null && paged.pageOf(it.anchor) == i }.map { it.slot } }
                    // Le verso d'une page de droite est la suivante ; celui d'une page de gauche, la précédente.
                    val verso = if (look.showThrough) paged.pages.getOrNull(if (side == PageSide.LEFT) i - 1 else i + 1) else null
                    PageView(paged.pages[i], i, side, paged.pages.size, typography, margins, contentWidth, contentHeight, here, look, verso)
                }
            }

            // Queues des rubans qui marquent une autre page : à gauche ce qui précède, à droite ce qui suit.
            val shown = visiblePages(state.currentPage, double, count).map { it - offset }.filter { it >= 0 }.toSet()
            fun sideOfPage(p: Int) = if (shown.isEmpty()) 1 else if (p in shown) 0 else if (p < shown.min()) -1 else 1
            val tails = ribbons.mapNotNull { r ->
                val side = r.anchor?.let { a -> sideOfPage(paged.pageOf(a)) } ?: r.elsewhere
                if (side == 0) null else r.slot to side
            }
            // Elles pendent dans la bande réservée, juste sous le folio.
            val tailBottom = margins.bottom - RibbonBand + (RibbonBand - TailHeight) / 2
            RibbonTails(tails.filter { it.second < 0 }.map { it.first }, typography.paper.isDark, onRibbonTap, Modifier.align(Alignment.BottomStart).padding(start = margins.horizontal, bottom = tailBottom))
            RibbonTails(tails.filter { it.second > 0 }.map { it.first }, typography.paper.isDark, onRibbonTap, Modifier.align(Alignment.BottomEnd).padding(end = margins.horizontal, bottom = tailBottom))

            // Images pieuses : celles de la page ouverte au milieu de la tête, les autres à gauche ou à droite.
            val placed = cards.map { c -> c to (c.anchor?.let { a -> sideOfPage(paged.pageOf(a)) } ?: c.elsewhere) }
            CardTops(placed.filter { it.second == 0 }.map { it.first }, paper, wide = true, onCardTap, Modifier.align(Alignment.TopCenter))
            CardTops(placed.filter { it.second < 0 }.map { it.first }, paper, wide = false, onCardTap, Modifier.align(Alignment.TopStart).padding(start = margins.horizontal))
            CardTops(placed.filter { it.second > 0 }.map { it.first }, paper, wide = false, onCardTap, Modifier.align(Alignment.TopEnd).padding(end = margins.horizontal))

            // Variante de l'ADR 006 : les coins bas protégés du geste « retour » (200 dp au plus par bord).
            if (protectEdges) {
                Box(Modifier.align(Alignment.BottomStart).width(40.dp).height(200.dp).systemGestureExclusion())
                Box(Modifier.align(Alignment.BottomEnd).width(40.dp).height(200.dp).systemGestureExclusion())
            }
        }
    }
}

private enum class PageSide { SINGLE, LEFT, RIGHT }

private class PageMargins(val top: Dp, val bottom: Dp, val horizontal: Dp)

/** Couverture seule à droite, puis paires (1, 2), (3, 4)… : les pages impaires sont à gauche. */
private fun sideOf(index: Int, double: Boolean) = when {
    !double -> PageSide.SINGLE
    index % 2 == 1 -> PageSide.LEFT
    else -> PageSide.RIGHT
}

private fun visiblePages(page: Int, double: Boolean, count: Int): Set<Int> {
    if (!double) return setOf(page)
    val layout = MekuriSpreadLayout(count, true)
    val pages = layout.pages(layout.spreadIndex(page))
    return setOfNotNull(pages.leading, pages.trailing)
}

@Composable
private fun PageView(
    page: LaidOutPage,
    number: Int,
    side: PageSide,
    pageCount: Int,
    t: BookTypography,
    margins: PageMargins,
    contentWidth: Dp,
    contentHeight: Dp,
    ribbonSlots: List<Int>,
    look: BookLook,
    verso: LaidOutPage?,
) {
    Box(Modifier.fillMaxSize().paperBackground().bookEdges(side, number, pageCount, t.paper, look).lyingRibbons(ribbonSlots, side, t.paper.isDark, margins.horizontal)) {
        Column(Modifier.fillMaxSize().padding(start = margins.horizontal, end = margins.horizontal, top = margins.top, bottom = margins.bottom)) {
            BasicText(
                if (number > 0) page.runningHead else "",
                style = t.runningHead,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().height(Header),
            )
            PageContent(page, verso, Modifier.size(contentWidth, contentHeight))
            BasicText(
                if (number > 0) number.toString() else "",
                style = t.folio,
                modifier = Modifier.fillMaxWidth().height(Footer).padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun PageContent(page: LaidOutPage, verso: LaidOutPage?, modifier: Modifier) {
    val density = LocalDensity.current
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            // Papier bible : le texte du verso transparaît à peine, en miroir.
            verso?.let { back ->
                withTransform({ scale(-1f, 1f, pivot = Offset(size.width / 2, size.height / 2)) }) {
                    back.items.forEach { item ->
                        when (item) {
                            is PageItem.Lines -> drawLines(item, alpha = SHOW_THROUGH)
                            is PageItem.Glyph -> translate(item.x, item.y) { drawText(item.layout, alpha = SHOW_THROUGH) }
                        }
                    }
                }
            }
            page.items.forEach { item ->
                when (item) {
                    is PageItem.Lines -> drawLines(item)
                    is PageItem.Glyph -> translate(item.x, item.y) { drawText(item.layout) }
                }
            }
        }
        // TalkBack lit chaque tranche de lignes dans l'ordre de la page (rapport 08, risque n° 1).
        page.items.filterIsInstance<PageItem.Lines>().forEach { lines ->
            val text = remember(lines) { AnnotatedString(lines.text()) }
            Box(
                Modifier
                    .offset { IntOffset(lines.x.roundToInt(), lines.y.roundToInt()) }
                    .size(with(density) { lines.layout.size.width.toDp() }, with(density) { lines.height.toDp() })
                    .semantics { this.text = text },
            )
        }
    }
}

/** Opacité du verso qui transparaît sur un papier bible. */
private const val SHOW_THROUGH = 0.055f

private fun DrawScope.drawLines(item: PageItem.Lines, alpha: Float = 1f) {
    // Un peu de marge latérale pour l'italique qui déborde ; rien au-dessus ni au-dessous des lignes retenues.
    clipRect(left = -size.width, top = item.y, right = size.width * 2, bottom = item.y + item.height) {
        translate(item.x, item.y - item.top) { drawText(item.layout, alpha = alpha) }
    }
}

/**
 * Ombre de la reliure côté dos, et tranche du côté libre : d'autant plus épaisse qu'il reste de pages dans tout
 * l'ouvrage (ou qu'on en a lu, pour une page de gauche) ; dorée, rouge ou naturelle ; lignes serrées sur papier fin.
 */
private fun Modifier.bookEdges(side: PageSide, number: Int, pageCount: Int, paper: PaperColors, look: BookLook): Modifier = drawWithContent {
    drawContent()
    val shade = Color.Black.copy(alpha = if (paper.isDark) 0.35f else 0.10f)
    val gutter = 26.dp.toPx()
    val spineAtRight = side == PageSide.LEFT
    val spineX = if (spineAtRight) size.width else 0f
    val gutterAlpha = if (side == PageSide.SINGLE) 0.6f else 1f
    drawRect(
        brush = Brush.horizontalGradient(
            colors = if (spineAtRight) listOf(Color.Transparent, shade) else listOf(shade, Color.Transparent),
            startX = if (spineAtRight) spineX - gutter else spineX,
            endX = if (spineAtRight) spineX else spineX + gutter,
        ),
        topLeft = Offset(if (spineAtRight) spineX - gutter else spineX, 0f),
        size = Size(gutter, size.height),
        alpha = gutterAlpha,
    )
    val last = (pageCount - 1).coerceAtLeast(1)
    val read = look.progress.start + (look.progress.endInclusive - look.progress.start) * (number / last.toFloat())
    val fraction = (if (side == PageSide.LEFT) read else 1f - read).coerceIn(0f, 1f)
    val band = (1.dp + look.thickness * fraction).toPx()
    val freeAtRight = side != PageSide.LEFT
    val x0 = if (freeAtRight) size.width - band else 0f
    val edge: Brush = when (look.edges) {
        EdgeFinish.GILT -> Brush.verticalGradient(listOf(Color(0xFF8A6A1C), Color(0xFFF0DB94), Color(0xFFC9A44C), Color(0xFF8A6A1C)))
        EdgeFinish.RED -> Brush.verticalGradient(listOf(Color(0xFF7E1A16), Color(0xFFB0332B), Color(0xFF8A201B)))
        EdgeFinish.PLAIN -> SolidColor(paper.hairline.copy(alpha = 0.7f))
    }
    drawRect(edge, topLeft = Offset(x0, 0f), size = Size(band, size.height))
    val step = look.weight.stepDp.dp.toPx()
    val line = if (look.edges == EdgeFinish.PLAIN) paper.surface.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.12f)
    var x = x0 + step / 2
    while (x < x0 + band) {
        drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5.dp.toPx())
        x += step
    }
}

/**
 * Les rubans qui marquent cette page sont couchés dans le petit fond, côté reliure, du haut au bas de la page,
 * sans jamais mordre sur le texte : au-delà de ce que la marge contient, on ne dessine pas.
 */
private fun Modifier.lyingRibbons(slots: List<Int>, side: PageSide, dark: Boolean, margin: Dp): Modifier = if (slots.isEmpty()) this else drawWithContent {
    drawContent()
    val width = 10.dp.toPx()
    val gap = 2.dp.toPx()
    val start = 4.dp.toPx()
    val room = ((margin.toPx() - start - 2.dp.toPx() + gap) / (width + gap)).toInt().coerceAtLeast(1)
    slots.take(room).forEachIndexed { i, slot ->
        val inset = start + i * (width + gap)
        val x = if (side == PageSide.LEFT) size.width - inset - width else inset
        drawRibbon(RibbonStyle.of(slot), dark, Offset(x, 0f), Size(width, size.height + 2.dp.toPx()))
    }
}

/** Queues de rubans qui pendent sous la page ; un toucher ouvre la page marquée (cible de 48 dp de large au moins). */
@Composable
private fun RibbonTails(slots: List<Int>, dark: Boolean, onTap: (Int) -> Unit, modifier: Modifier) {
    if (slots.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        slots.forEach { slot ->
            val style = RibbonStyle.of(slot)
            Box(
                Modifier
                    .size(width = 48.dp, height = TailHeight)
                    .clickable(onClickLabel = "Ouvrir au ruban ${style.label}") { onTap(slot) }
                    .semantics { contentDescription = "Ruban ${style.label}" },
                contentAlignment = Alignment.TopCenter,
            ) {
                Canvas(Modifier.size(width = TailWidth, height = TailHeight)) { drawRibbon(style, dark, Offset.Zero, size) }
            }
        }
    }
}
