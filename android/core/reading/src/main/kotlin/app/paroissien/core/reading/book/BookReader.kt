package app.paroissien.core.reading.book

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
) {
    val paper = typography.paper
    Box(modifier.fillMaxSize().background(paper.background)) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)) {
            val density = LocalDensity.current
            val double = when (spreadMode) {
                SpreadMode.SINGLE -> false
                SpreadMode.DOUBLE -> true
                SpreadMode.AUTO -> maxWidth > maxHeight && maxWidth / 2 >= MinimumSpreadPageWidth
            }
            val pageWidth = if (double) maxWidth / 2 else maxWidth
            val contentWidth = pageWidth - PageHorizontal * 2
            val contentHeight = maxHeight - PageTop - Header - Footer - PageBottom
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
                currentAnchor?.let { state.scrollToPage(paged.pageOf(it)) }
                snapshotFlow { state.currentPage }.collect { page ->
                    val shown = visiblePages(page, double, paged.pages.size)
                    val held = currentAnchor?.let(paged::pageOf)
                    if (held == null || held !in shown) paged.pages.getOrNull(page)?.let { anchorChanged(it.anchor) }
                }
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
            ) { index ->
                PageView(paged.pages[index], index, sideOf(index, double), paged.pages.size, typography, contentWidth, contentHeight)
            }

            // Variante de l'ADR 006 : les coins bas protégés du geste « retour » (200 dp au plus par bord).
            if (protectEdges) {
                Box(Modifier.align(Alignment.BottomStart).width(40.dp).height(200.dp).systemGestureExclusion())
                Box(Modifier.align(Alignment.BottomEnd).width(40.dp).height(200.dp).systemGestureExclusion())
            }
        }
    }
}

private enum class PageSide { SINGLE, LEFT, RIGHT }

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
    contentWidth: Dp,
    contentHeight: Dp,
) {
    Box(Modifier.fillMaxSize().paperBackground().bookEdges(side, number, pageCount, t.paper)) {
        Column(Modifier.fillMaxSize().padding(start = PageHorizontal, end = PageHorizontal, top = PageTop, bottom = PageBottom)) {
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
