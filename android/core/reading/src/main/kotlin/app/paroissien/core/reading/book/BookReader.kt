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
import app.paroissien.core.designsystem.PaperColors
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

private val PageHorizontal = 28.dp
private val PageTop = 18.dp
private val PageBottom = 14.dp
private val Header = 30.dp
private val Footer = 26.dp
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
    onTurnPastEnd: (() -> Unit)? = null,
    onTurnPastStart: (() -> Unit)? = null,
    initialAtEnd: Boolean = false,
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
                bottom = PageBottom + cutout.calculateBottomPadding(),
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

            val state = rememberMekuriPagerState(pageCount = paged.pages.size, initialPage = anchor?.let(paged::pageOf) ?: 0)
            val currentAnchor by rememberUpdatedState(anchor)
            val anchorChanged by rememberUpdatedState(onAnchorChange)
            LaunchedEffect(paged, double, state) {
                val held = currentAnchor
                when {
                    held != null -> state.scrollToPage(paged.pageOf(held))
                    initialAtEnd -> state.scrollToPage(paged.pages.lastIndex)
                }
                snapshotFlow { state.currentPage }.collect { page ->
                    val shown = visiblePages(page, double, paged.pages.size)
                    val held = currentAnchor?.let(paged::pageOf)
                    if (held == null || held !in shown) paged.pages.getOrNull(page)?.let { anchorChanged(it.anchor) }
                }
            }

            // Saut demandé de l'extérieur (sommaire, ruban, recherche) : on ouvre la page de l'ancre si elle n'est pas
            // déjà sous les yeux ; un tour de page ordinaire ne déclenche donc rien.
            LaunchedEffect(paged, anchor) {
                val target = anchor?.let(paged::pageOf) ?: return@LaunchedEffect
                if (target !in visiblePages(state.currentPage, double, paged.pages.size)) state.scrollToPage(target)
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
                val here = remember(ribbons, paged, index) { ribbons.filter { it.anchor != null && paged.pageOf(it.anchor) == index }.map { it.slot } }
                PageView(paged.pages[index], index, sideOf(index, double), paged.pages.size, typography, margins, contentWidth, contentHeight, here)
            }

            // Queues des rubans qui marquent une autre page : à gauche ce qui précède, à droite ce qui suit.
            val shown = visiblePages(state.currentPage, double, paged.pages.size)
            val tails = ribbons.mapNotNull { r ->
                val side = r.anchor?.let { a -> paged.pageOf(a).let { p -> if (p in shown) 0 else if (p < shown.min()) -1 else 1 } } ?: r.elsewhere
                if (side == 0) null else r.slot to side
            }
            RibbonTails(tails.filter { it.second < 0 }.map { it.first }, typography.paper.isDark, onRibbonTap, Modifier.align(Alignment.BottomStart).padding(start = margins.horizontal))
            RibbonTails(tails.filter { it.second > 0 }.map { it.first }, typography.paper.isDark, onRibbonTap, Modifier.align(Alignment.BottomEnd).padding(end = margins.horizontal))

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
) {
    Box(Modifier.fillMaxSize().paperBackground().bookEdges(side, number, pageCount, t.paper).lyingRibbons(ribbonSlots, side, t.paper.isDark)) {
        Column(Modifier.fillMaxSize().padding(start = margins.horizontal, end = margins.horizontal, top = margins.top, bottom = margins.bottom)) {
            BasicText(
                if (number > 0) page.runningHead else "",
                style = t.runningHead,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().height(Header),
            )
            PageContent(page, Modifier.size(contentWidth, contentHeight))
            BasicText(
                if (number > 0) number.toString() else "",
                style = t.folio,
                modifier = Modifier.fillMaxWidth().height(Footer).padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun PageContent(page: LaidOutPage, modifier: Modifier) {
    val density = LocalDensity.current
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
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

private fun DrawScope.drawLines(item: PageItem.Lines) {
    // Un peu de marge latérale pour l'italique qui déborde ; rien au-dessus ni au-dessous des lignes retenues.
    clipRect(left = -size.width, top = item.y, right = size.width * 2, bottom = item.y + item.height) {
        translate(item.x, item.y - item.top) { drawText(item.layout) }
    }
}

/** Ombre de la reliure côté dos, et tranche du côté libre, d'autant plus épaisse qu'il reste de pages (rapport 07). */
private fun Modifier.bookEdges(side: PageSide, number: Int, pageCount: Int, paper: PaperColors): Modifier = drawWithContent {
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
    val fraction = if (side == PageSide.LEFT) number / last.toFloat() else (last - number) / last.toFloat()
    val band = (1.dp + 5.dp * fraction.coerceIn(0f, 1f)).toPx()
    val freeAtRight = side != PageSide.LEFT
    val x0 = if (freeAtRight) size.width - band else 0f
    drawRect(paper.hairline.copy(alpha = 0.7f), topLeft = Offset(x0, 0f), size = Size(band, size.height))
    val step = 1.5.dp.toPx()
    var x = x0 + step / 2
    while (x < x0 + band) {
        drawLine(paper.surface.copy(alpha = 0.9f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5.dp.toPx())
        x += step
    }
}

/** Les rubans qui marquent cette page sont couchés dans le petit fond, côté reliure, du haut au bas de la page. */
private fun Modifier.lyingRibbons(slots: List<Int>, side: PageSide, dark: Boolean): Modifier = if (slots.isEmpty()) this else drawWithContent {
    drawContent()
    val width = 9.dp.toPx()
    val gap = 4.dp.toPx()
    slots.forEachIndexed { i, slot ->
        val inset = 10.dp.toPx() + i * (width + gap)
        val x = if (side == PageSide.LEFT) size.width - inset - width else inset
        drawRibbon(RibbonStyle.of(slot), dark, Offset(x, 0f), Size(width, size.height + 2.dp.toPx()))
    }
}

/** Queues de rubans qui pendent sous la page ; un toucher ouvre la page marquée. */
@Composable
private fun RibbonTails(slots: List<Int>, dark: Boolean, onTap: (Int) -> Unit, modifier: Modifier) {
    if (slots.isEmpty()) return
    Row(modifier.windowInsetsPadding(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        slots.forEach { slot ->
            val style = RibbonStyle.of(slot)
            Canvas(
                Modifier
                    .size(width = 14.dp, height = 40.dp)
                    .clickable(onClickLabel = "Ouvrir au ruban ${style.label}") { onTap(slot) }
                    .semantics { contentDescription = "Ruban ${style.label}" },
            ) { drawRibbon(style, dark, Offset.Zero, size) }
        }
    }
}
