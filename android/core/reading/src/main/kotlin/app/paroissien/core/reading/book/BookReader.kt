package app.paroissien.core.reading.book

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.toSize
import app.paroissien.core.designsystem.drawRibbonAlong
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
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
    ribbonsAcross: Boolean = false,
    cardsOnPage: Boolean = false,
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

            // Rubans écartés dans le pli, images mises de côté, le temps de la lecture : un ruban qu'on rouvre par sa
            // queue, ou qu'on pose ailleurs, revient en travers de la page ; une image déplacée revient sur la sienne.
            var asideRibbons by remember { mutableStateOf(mapOf<Int, Anchor?>()) }
            var asideCards by remember { mutableStateOf(mapOf<String, Anchor?>()) }
            fun CardMark.aside() = asideCards.containsKey(id) && asideCards[id] == anchor

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
                    val here = remember(ribbons, paged, i, asideRibbons) {
                        ribbons.filter { it.anchor != null && paged.pageOf(it.anchor) == i }
                            .map { PageRibbon(it.slot, aside = asideRibbons.containsKey(it.slot) && asideRibbons[it.slot] == it.anchor) }
                    }
                    val laid = if (cardsOnPage) cards.filter { it.anchor != null && paged.pageOf(it.anchor) == i } else emptyList()
                    // Le verso d'une page de droite est la suivante ; celui d'une page de gauche, la précédente.
                    val verso = if (look.showThrough) paged.pages.getOrNull(if (side == PageSide.LEFT) i - 1 else i + 1) else null
                    PageView(
                        paged.pages[i], i, side, paged.pages.size, typography, margins, contentWidth, contentHeight, look, verso,
                        ribbons = here,
                        across = ribbonsAcross,
                        cards = laid,
                        cardsAside = laid.filter { it.aside() }.map { it.id }.toSet(),
                        objects = PageObjects(
                            ribbonAside = { slot -> ribbons.firstOrNull { it.slot == slot }?.let { asideRibbons = asideRibbons + (slot to it.anchor) } },
                            cardAside = { id -> cards.firstOrNull { it.id == id }?.let { asideCards = asideCards + (id to it.anchor) } },
                            cardOpen = onCardTap,
                        ),
                    )
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
            // Ouvrir le livre par la queue d'un ruban : il est de nouveau en travers de la page.
            val openAtRibbon: (Int) -> Unit = { slot -> asideRibbons = asideRibbons - slot; onRibbonTap(slot) }
            RibbonTails(tails.filter { it.second < 0 }.map { it.first }, typography.paper.isDark, openAtRibbon, Modifier.align(Alignment.BottomStart).padding(start = margins.horizontal, bottom = tailBottom))
            RibbonTails(tails.filter { it.second > 0 }.map { it.first }, typography.paper.isDark, openAtRibbon, Modifier.align(Alignment.BottomEnd).padding(end = margins.horizontal, bottom = tailBottom))

            // Images pieuses : celles de la page ouverte au milieu de la tête (posées sur la page, seulement celles qu'on
            // a mises de côté ; un toucher les y remet), les autres à gauche ou à droite.
            val placed = cards.map { c -> c to (c.anchor?.let { a -> sideOfPage(paged.pageOf(a)) } ?: c.elsewhere) }
            val current = placed.filter { it.second == 0 }.map { it.first }
            if (cardsOnPage) {
                CardTops(current.filter { it.aside() }, paper, wide = true, { id -> asideCards = asideCards - id }, onCardTap, Modifier.align(Alignment.TopCenter))
            } else {
                CardTops(current, paper, wide = true, onCardTap, onCardTap, Modifier.align(Alignment.TopCenter))
            }
            CardTops(placed.filter { it.second < 0 }.map { it.first }, paper, wide = false, onCardTap, onCardTap, Modifier.align(Alignment.TopStart).padding(start = margins.horizontal))
            CardTops(placed.filter { it.second > 0 }.map { it.first }, paper, wide = false, onCardTap, onCardTap, Modifier.align(Alignment.TopEnd).padding(end = margins.horizontal))

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

/** Un ruban qui marque cette page ; [aside] : écarté dans le pli. */
private data class PageRibbon(val slot: Int, val aside: Boolean)

/** Ce qu'on fait des objets posés sur la page : écarter un ruban dans le pli, mettre une image de côté, l'ouvrir. */
private class PageObjects(val ribbonAside: (Int) -> Unit, val cardAside: (String) -> Unit, val cardOpen: (String) -> Unit)

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
    look: BookLook,
    verso: LaidOutPage?,
    ribbons: List<PageRibbon>,
    across: Boolean,
    cards: List<CardMark>,
    cardsAside: Set<String>,
    objects: PageObjects,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val gutterAtRight = side == PageSide.LEFT
    // 0 : en travers de la page ; 1 : couché dans le petit fond. Le ruban qu'on touche y glisse.
    val moves = ribbons.map { r -> key(r.slot) { animateFloatAsState(if (!across || r.aside) 1f else 0f, tween(640, easing = FastOutSlowInEasing), label = "ruban") } }
    var pageSize by remember { mutableStateOf(Size.Zero) }
    val margin = margins.horizontal
    val tap by rememberUpdatedState { at: Offset ->
        val ribbon = ribbons.indices.reversed().firstOrNull { k ->
            across && !ribbons[k].aside && with(density) { near(ribbonSpine(k, 0f, gutterAtRight, pageSize, margin), at, (AcrossWidth / 2 + 14.dp).toPx()) }
        }
        val card = if (ribbon != null) null else cards.indices.reversed().firstOrNull { k -> cards[k].id !in cardsAside && with(density) { cardRect(cards[k].ratio(), k, pageSize) }.contains(at) }
        ribbon?.let { objects.ribbonAside(ribbons[it].slot) }
        card?.let { objects.cardAside(cards[it].id) }
        ribbon != null || card != null
    }
    val longPress by rememberUpdatedState { at: Offset ->
        val card = cards.indices.reversed().firstOrNull { k -> cards[k].id !in cardsAside && with(density) { cardRect(cards[k].ratio(), k, pageSize) }.contains(at) }
        card?.let { haptic.performHapticFeedback(HapticFeedbackType.LongPress); objects.cardOpen(cards[it].id) }
        card != null
    }
    Box(
        Modifier.fillMaxSize().paperBackground().bookEdges(side, number, pageCount, t.paper, look)
            .pageRibbons(ribbons, moves, gutterAtRight, t.paper.isDark, margin)
            .onSizeChanged { pageSize = it.toSize() }
            .pointerInput(Unit) { objectTouches({ tap(it) }, { longPress(it) }) },
    ) {
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
        if (pageSize != Size.Zero) {
            cards.forEachIndexed { k, card ->
                key(card.id) { CardOnPage(card, with(density) { cardRect(card.ratio(), k, pageSize) }, visible = card.id !in cardsAside, objects) }
            }
            // TalkBack : chaque ruban en travers de la page se dit, et s'écarte d'un double toucher.
            ribbons.forEachIndexed { k, r ->
                if (!across || r.aside) return@forEachIndexed
                val label = RibbonStyle.of(r.slot).label
                val middle = with(density) { ribbonSpine(k, 0f, gutterAtRight, pageSize, margin) }.let { it[it.size / 2] }
                Box(
                    Modifier
                        .offset { IntOffset((middle.x - 24.dp.toPx()).roundToInt(), (middle.y - 24.dp.toPx()).roundToInt()) }
                        .size(48.dp)
                        .semantics {
                            contentDescription = "Ruban $label, en travers de la page"
                            onClick("l'écarter dans le pli") { objects.ribbonAside(r.slot); true }
                        },
                )
            }
        }
    }
}

/** Le rapport largeur sur hauteur de l'image, d'après celle qu'on a sous la main (une carte, sinon). */
private fun CardMark.ratio(): Float = (picture ?: image)?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } ?: (2f / 3.5f)

private const val PHI = 1.618034f

/**
 * Où l'image est posée : la page vaut φ fois l'image (nombre d'or), qui garde ses proportions et tient dans ce cadre ;
 * centrée, et placée un peu au-dessus du milieu, les blancs du haut et du bas dans le rapport 1 : φ, comme le bloc de
 * texte d'une belle page. Les suivantes, si l'on en a glissé plusieurs, sont un peu décalées, en pile.
 */
private fun Density.cardRect(ratio: Float, index: Int, page: Size): Rect {
    val boxW = page.width / PHI
    val boxH = page.height / PHI
    val (w, h) = if (ratio > boxW / boxH) boxW to boxW / ratio else boxH * ratio to boxH
    val shift = (7 * index).dp.toPx()
    val left = (page.width - w) / 2 + shift
    val top = (page.height - h) / (1 + PHI) + shift
    return Rect(left, top, left + w, top + h)
}

/** Une image pieuse posée sur la page, un rien de travers, avec son ombre ; mise de côté, elle glisse vers la tête. */
@Composable
private fun CardOnPage(card: CardMark, rect: Rect, visible: Boolean, objects: PageObjects) {
    val density = LocalDensity.current
    val tilt = (card.id.hashCode().mod(5) - 2) * 0.6f
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(380)) + slideInVertically(tween(420)) { -it / 2 },
        exit = fadeOut(tween(380)) + slideOutVertically(tween(420)) { -it / 2 },
        modifier = Modifier.offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) },
    ) {
        Box(
            Modifier
                .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                .graphicsLayer {
                    rotationZ = tilt
                    shadowElevation = 5.dp.toPx()
                }
                .background(Color(0xFFF7F0DD))
                .semantics {
                    contentDescription = "Image pieuse : ${card.label}"
                    onClick("la mettre de côté") { objects.cardAside(card.id); true }
                    customActions = listOf(CustomAccessibilityAction("Ouvrir l'image") { objects.cardOpen(card.id); true })
                },
        ) {
            (card.picture ?: card.image)?.let { Image(it, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize()) }
        }
    }
}

/**
 * Toucher un ruban ou une image posés sur la page sans rien ôter au feuilletage : le doigt qui glisse tourne la page
 * comme ailleurs. Seuls un toucher bref ou un appui long qui tombent sur un objet sont consommés ; ailleurs, le livre
 * reçoit le geste (bords : tourner, milieu : la barre).
 */
private suspend fun PointerInputScope.objectTouches(onTap: (Offset) -> Boolean, onLongPress: (Offset) -> Boolean) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var moved = false
        val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            awaitRelease(down, viewConfiguration.touchSlop).also { if (it == null) moved = true }
        }
        when {
            up != null -> if (onTap(down.position)) up.consume()
            !moved && onLongPress(down.position) -> {
                // Le reste du geste appartient à l'image : ni page tournée, ni barre.
                do {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
            }
        }
    }
}

/** Le relâchement du doigt, ou rien s'il a glissé ou si un autre a pris le geste. */
private suspend fun AwaitPointerEventScope.awaitRelease(down: PointerInputChange, slop: Float): PointerInputChange? {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return null
        if (change.isConsumed) return null
        if (!change.pressed) return change
        if ((change.position - down.position).getDistance() > slop) return null
    }
}

/** Largeur d'un ruban en travers de la page, et couché dans le petit fond. */
private val AcrossWidth = 16.dp
private val LyingWidth = 11.dp
private const val SPINE_POINTS = 96

/**
 * La ligne médiane du ruban n° [index] de la page, de la tête vers l'extrémité. À [t] = 0, en travers de la page : il
 * sort de la tête près du pli, descend en biais en serpentant à peine, et son extrémité pend dans la bande du bas. À
 * [t] = 1, couché dans le petit fond, sans mordre sur le texte ; entre les deux pendant qu'il y glisse.
 */
private fun Density.ribbonSpine(index: Int, t: Float, gutterAtRight: Boolean, size: Size, margin: Dp): List<Offset> {
    val w = size.width
    val h = size.height
    val x0 = (16 + 18 * index).dp.toPx()
    val x3 = w * (0.5f + 0.12f * index).coerceAtMost(0.8f)
    val p0 = Offset(x0, -4.dp.toPx())
    val p1 = Offset(x0 + w * 0.03f, h * 0.32f)
    val p2 = Offset(x3 - w * 0.14f, h * 0.68f)
    val p3 = Offset(x3, h - 8.dp.toPx())
    val room = ((margin - 6.dp + 2.dp) / (LyingWidth + 2.dp)).toInt().coerceAtLeast(1)
    val lyingX = (4.dp + (LyingWidth + 2.dp) * min(index, room - 1)).toPx() + LyingWidth.toPx() / 2
    val top = -4.dp.toPx()
    val bottom = h + 2.dp.toPx()
    val sway = 1.3.dp.toPx()
    val waves = h / 420.dp.toPx()
    return List(SPINE_POINTS + 1) { k ->
        val u = k / SPINE_POINTS.toFloat()
        val v = 1 - u
        val curve = p0 * (v * v * v) + p1 * (3 * v * v * u) + p2 * (3 * v * u * u) + p3 * (u * u * u)
        val lying = Offset(lyingX, top + (bottom - top) * u)
        val across = curve + Offset(sway * sin(u * waves * 2 * PI.toFloat() + index * 1.3f), 0f)
        val p = across + (lying - across) * t
        if (gutterAtRight) Offset(w - p.x, p.y) else p
    }
}

/** Le point est-il à moins de [reach] de la polyligne ? */
private fun near(line: List<Offset>, at: Offset, reach: Float): Boolean = line.zipWithNext().any { (a, b) ->
    val ab = b - a
    val k = (((at - a).x * ab.x + (at - a).y * ab.y) / (ab.x * ab.x + ab.y * ab.y).coerceAtLeast(1e-3f)).coerceIn(0f, 1f)
    (a + ab * k - at).getDistance() <= reach
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
 * Les rubans qui marquent cette page, par-dessus le texte : en travers de la page, ou couchés dans le petit fond, côté
 * reliure, sans mordre sur le texte (rapport 09). L'ombre du pli, dessinée ensuite, les recouvre comme le papier.
 */
private fun Modifier.pageRibbons(ribbons: List<PageRibbon>, moves: List<State<Float>>, gutterAtRight: Boolean, dark: Boolean, margin: Dp): Modifier =
    if (ribbons.isEmpty()) this else drawWithContent {
        drawContent()
        ribbons.forEachIndexed { k, r ->
            val t = moves[k].value
            drawRibbonAlong(
                RibbonStyle.of(r.slot), dark,
                spine = ribbonSpine(k, t, gutterAtRight, size, margin),
                width = (AcrossWidth + (LyingWidth - AcrossWidth) * t).toPx(),
                ripple = 1f - 0.55f * t,
                phase = 2.4f * t + k,
            )
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
