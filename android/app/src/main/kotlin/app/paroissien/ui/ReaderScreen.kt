package app.paroissien.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.content.TocEntry
import app.paroissien.core.data.ReaderSettings
import app.paroissien.core.data.Ribbon
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.RibbonStyle
import app.paroissien.core.designsystem.rememberBookFonts
import app.paroissien.core.reading.book.Anchor
import app.paroissien.core.reading.book.BookReader
import app.paroissien.core.reading.book.BookTypography
import app.paroissien.core.reading.book.ReadingBlock
import app.paroissien.core.reading.book.RibbonMark
import androidx.compose.foundation.Image
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.layout.ContentScale
import androidx.navigation3.runtime.NavKey
import app.paroissien.core.data.CardPlacement
import app.paroissien.core.reading.book.CardMark
import app.paroissien.library.subtitle
import app.paroissien.library.BookIdentities
import app.paroissien.library.BookFrames
import app.paroissien.core.reading.book.FrameLook
import app.paroissien.core.reading.book.BookLook
import app.paroissien.core.designsystem.bookThickness
import app.paroissien.core.designsystem.PaperWeight
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.BookCover
import app.paroissien.core.designsystem.CoverMaterial
import app.paroissien.core.designsystem.DoublurePage
import androidx.compose.runtime.CompositionLocalProvider
import app.paroissien.library.PiousImage
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.style.TextAlign
import app.paroissien.core.reading.book.packToReadingBlocks
import app.paroissien.core.reading.turn.MekuriConfiguration
import app.paroissien.library.LibraryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.sign

private enum class Panel { CHROME, TOC, RIBBONS, IMAGES, SETTINGS }

private class Loaded(val unit: String?, val language: String, val blocks: List<ReadingBlock>)

/**
 * Le lecteur : ouvre la publication à sa position enregistrée, la garde à jour à chaque page, enchaîne les unités en
 * tournant la page au bord du livre. Toucher le milieu ouvre la barre : sommaire, rubans, images, prière, réglages.
 */
@Composable
fun ReaderScreen(graph: AppGraph, key: ReaderKey, settings: ReaderSettings, open: (NavKey) -> Unit, onBack: () -> Unit) {
    val library by graph.library.collectAsState()
    val isDemo = key.publication == DEMO
    val entry = library?.entry(key.publication)
    val publication = key.publication

    var unitId by rememberSaveable { mutableStateOf(key.unit) }
    var anchorBlock by rememberSaveable { mutableStateOf(key.block) }
    var anchorOffset by rememberSaveable { mutableIntStateOf(key.offset) }
    var atEnd by rememberSaveable { mutableStateOf(false) }
    var resolved by rememberSaveable { mutableStateOf(key.unit != null || isDemo) }
    var panel by rememberSaveable { mutableStateOf<Panel?>(null) }
    // Pris sur le rayon, le livre s'ouvre par sa couverture, puis sa doublure, avant la page où l'on en était.
    var opening by rememberSaveable { mutableStateOf(key.opening && !isDemo) }
    var pagination by remember { mutableStateOf("") }

    // Ouverture : la position enregistrée, sinon la première unité.
    LaunchedEffect(entry, resolved) {
        if (resolved || entry == null) return@LaunchedEffect
        val saved = withContext(Dispatchers.IO) { graph.database.positions().get(publication) }
        unitId = saved?.unit?.takeIf { entry.unit(it) != null } ?: entry.units.first().id
        anchorBlock = saved?.block
        anchorOffset = saved?.charOffset ?: 0
        resolved = true
    }

    val unit = entry?.unit(unitId)
    val units = entry?.units.orEmpty()
    val unitIndex = units.indexOfFirst { it.id == unitId }
    val language = settings.language
    val loaded by produceState<Loaded?>(null, entry, unitId, language, resolved) {
        value = when {
            isDemo -> Loaded(null, language, demoBook())
            !resolved || entry == null || unit == null -> null
            else -> withContext(Dispatchers.IO) {
                Loaded(unit.id, language, packToReadingBlocks(graph.packs.blocks(entry, unit), unit.title ?: entry.title, language, entry.publication.primaryLanguage))
            }
        }
    }
    // Jamais les blocs de l'unité précédente sous la clé de la nouvelle : la position enregistrée serait fausse.
    val blocks = loaded?.takeIf { isDemo || (it.unit == unitId && it.language == language) }?.blocks
    val ribbonFlow = remember(publication) { if (isDemo) flowOf(emptyList()) else graph.database.ribbons().observe(publication) }
    val ribbons by ribbonFlow.collectAsState(initial = emptyList())
    val marks = ribbons.mapNotNull { r ->
        val index = units.indexOfFirst { it.id == r.unit }
        when {
            index < 0 -> null
            r.unit == unitId -> RibbonMark(r.slot, Anchor(r.block, r.charOffset))
            else -> RibbonMark(r.slot, null, elsewhere = (index - unitIndex).sign)
        }
    }
    val cardFlow = remember(publication) { if (isDemo) flowOf(emptyList()) else graph.database.cards().observe(publication) }
    val cards by cardFlow.collectAsState(initial = emptyList())
    val images by graph.images.all.collectAsState()
    // Les images glissées dans l'unité ouverte, entières : elles sont posées sur leur page.
    val pictures by produceState(emptyMap<String, ImageBitmap>(), cards, unitId, settings.cardsOnPage) {
        val here = cards.filter { it.unit == unitId && settings.cardsOnPage }.map { it.card }
        value = withContext(Dispatchers.IO) { here.mapNotNull { id -> graph.images.picture(id)?.let { id to it } }.toMap() }
    }
    val cardMarks = cards.mapNotNull { c ->
        val index = units.indexOfFirst { it.id == c.unit }
        val image = images.firstOrNull { it.id == c.card } ?: return@mapNotNull null
        when {
            index < 0 -> null
            c.unit == unitId -> CardMark(c.card, Anchor(c.block, c.charOffset), image = graph.images.thumbnail(c.card), label = image.title.fr, picture = pictures[c.card])
            else -> CardMark(c.card, null, elsewhere = (index - unitIndex).sign, image = graph.images.thumbnail(c.card), label = image.title.fr)
        }
    }

    fun openAt(unit: String?, block: String?, offset: Int, end: Boolean = false) {
        // Le début de l'unité déjà ouverte : une ancre explicite, sinon le lecteur ne bougerait pas.
        anchorBlock = block ?: blocks?.firstOrNull()?.id?.takeIf { unit == unitId }
        unitId = unit
        anchorOffset = offset
        atEnd = end
    }

    BackHandler(enabled = panel != null) { panel = null }

    val fonts = rememberBookFonts(settings.bookFace())
    // L'identité du livre : son papier en thème clair (les autres thèmes, choisis pour le confort, l'emportent), sa
    // tranche, sa couverture, ses rubans ; un papier fin plie plus volontiers qu'un papier épais.
    val identity = remember(publication, entry, settings.bookIdentity) { BookIdentities.of(publication, entry, uniform = !settings.bookIdentity || isDemo) }
    val themePaper = LocalPaperColors.current
    val ownPaper = settings.bookIdentity && !isDemo && settings.paperTheme() == PaperTheme.CLAIR
    val paper = remember(identity, themePaper, ownPaper) { if (ownPaper) identity.paper.colors(themePaper) else themePaper }
    val typography = remember(fonts, paper, settings.scaleIndex) { BookTypography(fonts, paper, SCALES[settings.scaleIndex.coerceIn(0, SCALES.lastIndex)]) }
    val turn = settings.turnStyle()
    val configuration = remember(turn, identity.paper.weight) {
        MekuriConfiguration(foldRadius = identity.paper.weight.foldRadius, reducedMotionOverride = if (turn == TurnStyle.AUCUN) true else null)
    }
    val totalBlocks = units.sumOf { it.blockCount }.coerceAtLeast(1)
    val before = units.take(unitIndex.coerceAtLeast(0)).sumOf { it.blockCount }
    // Une couverture de papier n'a pas de doublure de soie.
    val bound = identity.cover.material != CoverMaterial.WRAPPER
    val look = BookLook(
        edges = identity.edges,
        weight = identity.paper.weight,
        showThrough = ownPaper && identity.paper.weight == PaperWeight.BIBLE,
        progress = (before / totalBlocks.toFloat())..((before + (unit?.blockCount ?: 0)) / totalBlocks.toFloat()),
        thickness = (bookThickness(identity.pages) * 80).dp,
        cover = if (unitIndex == 0 && entry != null) {
            { BookCover(identity, entry.title, entry.subtitle(), Modifier.fillMaxSize(), flat = true) }
        } else null,
        endpaper = if (unitIndex == 0 && entry != null && bound) { { DoublurePage() } } else null,
        frame = remember(publication, entry, unitIndex, settings.ornamentalFrames) {
            if (settings.ornamentalFrames && !isDemo) BookFrames.of(publication, entry, unitIndex, unit)?.let { FrameLook(it.spec, it.style) } else null
        },
    )

    Box(Modifier.fillMaxSize().background(paper.background)) {
        blocks?.let { book ->
            key(unitId, language) {
                CompositionLocalProvider(LocalPaperColors provides paper) {
                BookReader(
                    blocks = book,
                    typography = typography,
                    // Ouvert depuis le rayon sans position enregistrée : la première page de texte, pas la couverture
                    // qu'on vient de tourner.
                    anchor = (anchorBlock ?: book.firstOrNull()?.id?.takeIf { key.opening })?.let { Anchor(it, anchorOffset) },
                    onAnchorChange = { a ->
                        anchorBlock = a.blockId
                        anchorOffset = a.charOffset
                        atEnd = false
                        unitId?.let { if (!isDemo) graph.savePosition(publication, it, a.blockId, a.charOffset) }
                    },
                    spreadMode = settings.spreadMode(),
                    configuration = configuration,
                    slide = turn == TurnStyle.GLISSEMENT,
                    protectEdges = settings.protectEdges,
                    onCenterTap = { panel = if (panel == null) Panel.CHROME else null },
                    onPaginated = { pagination = String.format(Locale.FRANCE, "%d pages · pagination %d ms", it.pages.size, it.elapsedMillis) },
                    ribbons = marks,
                    onRibbonTap = { slot -> ribbons.firstOrNull { it.slot == slot }?.let { openAt(it.unit, it.block, it.charOffset) } },
                    cards = cardMarks,
                    onCardTap = { id -> open(CardKey(id)) },
                    ribbonsAcross = settings.ribbonsAcross,
                    cardsOnPage = settings.cardsOnPage,
                    onTurnPastEnd = units.getOrNull(unitIndex + 1)?.let { next -> { openAt(next.id, null, 0) } },
                    onTurnPastStart = units.getOrNull(unitIndex - 1)?.let { previous -> { openAt(previous.id, null, 0, end = true) } },
                    initialAtEnd = atEnd,
                    look = look,
                )
                }
            }
        }

        when (panel) {
            Panel.CHROME -> Chrome(
                title = if (entry == null) stringResource(R.string.demo_book) else listOfNotNull(entry.title, unit?.title?.takeIf { it != entry.title }).joinToString(" · "),
                onBack = onBack,
                onToc = { panel = Panel.TOC }.takeIf { entry != null },
                onRibbons = { panel = Panel.RIBBONS }.takeIf { !isDemo && entry != null },
                onImages = { panel = Panel.IMAGES }.takeIf { !isDemo && entry != null },
                onPray = { open(PrayerKey(publication.takeIf { !isDemo }, unitId, anchorBlock)) },
                onSettings = { panel = Panel.SETTINGS },
                modifier = Modifier.align(Alignment.TopCenter),
            )
            Panel.TOC -> if (entry != null) TocPanel(graph, entry, language, unitId, onClose = { panel = null }) { e -> openAt(e.unit, e.blockId, 0); panel = null }
            Panel.RIBBONS -> RibbonPanel(
                ribbons = ribbons,
                // Les rubans cousus dans ce livre, plus ceux qu'on y aurait posés en mode « tous pareils ».
                slots = (identity.ribbons + ribbons.map { it.slot }).distinct().sorted(),
                unitTitle = { entry?.unitTitle(it) ?: "" },
                onPlace = { slot ->
                    val here = unitId
                    val block = anchorBlock ?: blocks?.firstOrNull()?.id
                    if (here != null && block != null) graph.scope.launch(Dispatchers.IO) {
                        graph.database.ribbons().place(Ribbon(publication, slot, here, block, anchorOffset, null, System.currentTimeMillis()))
                    }
                },
                onRemove = { slot -> graph.scope.launch(Dispatchers.IO) { graph.database.ribbons().remove(publication, slot) } },
                onClose = { panel = null },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            Panel.IMAGES -> ImagePanel(
                graph = graph,
                images = images,
                onScan = { panel = null; open(ScanKey()) },
                placedHere = cards.map { it.card }.toSet(),
                onPlace = { card ->
                    val here = unitId
                    val block = anchorBlock ?: blocks?.firstOrNull()?.id
                    if (here != null && block != null) graph.scope.launch(Dispatchers.IO) {
                        graph.database.cards().place(CardPlacement(card, publication, here, block, anchorOffset, null, System.currentTimeMillis()))
                    }
                },
                onRemove = { card -> graph.scope.launch(Dispatchers.IO) { graph.database.cards().remove(card) } },
                onClose = { panel = null },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            Panel.SETTINGS -> Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing), shadowElevation = 6.dp) {
                ReaderSettingsRow(settings, onAll = { panel = null; open(SettingsKey) }) { transform -> graph.scope.launch { graph.settings.update(transform) } }
            }
            null -> Unit
        }

        if (opening && entry != null) {
            BookOpening(
                cover = { BookCover(identity, entry.title, entry.subtitle(), Modifier.fillMaxSize(), flat = true) },
                endpaper = if (bound) { { DoublurePage() } } else null,
                onOpened = { opening = false },
            )
        }

        if (settings.showMeasures) {
            val anchorText = anchorBlock?.let { "$it:$anchorOffset" } ?: "—"
            FrameStatsHud(
                extra = "$pagination · ancre $anchorText",
                modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(bottom = 56.dp),
            )
        }
    }
}

/** La barre du lecteur : le titre sur sa ligne, puis les boutons, qui défilent quand l'écran est trop étroit. */
@Composable
private fun Chrome(title: String, onBack: () -> Unit, onToc: (() -> Unit)?, onRibbons: (() -> Unit)?, onImages: (() -> Unit)?, onPray: () -> Unit, onSettings: () -> Unit, modifier: Modifier) {
    Surface(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing), shadowElevation = 4.dp) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Retour à la bibliothèque" }) { Text("‹") }
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
            ScrollingRow {
                onToc?.let { TextButton(onClick = it) { Text(stringResource(R.string.reader_toc)) } }
                onRibbons?.let { TextButton(onClick = it) { Text(stringResource(R.string.reader_ribbons)) } }
                onImages?.let { TextButton(onClick = it) { Text(stringResource(R.string.reader_images)) } }
                TextButton(onClick = onPray) { Text(stringResource(R.string.reader_pray)) }
                TextButton(onClick = onSettings) { Text(stringResource(R.string.reader_settings)) }
            }
        }
    }
}

/**
 * Une rangée qui défile de côté. Quand des éléments sont cachés, un simple ‹ ou › le dit, sur un fondu de la couleur
 * de la barre ; le toucher fait défiler d'un écran.
 */
@Composable
fun ScrollingRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val state = rememberScrollState()
    val scope = rememberCoroutineScope()
    val surface = MaterialTheme.colorScheme.surface
    Box(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Row(Modifier.horizontalScroll(state), verticalAlignment = Alignment.CenterVertically, content = content)
        if (state.canScrollBackward) {
            ScrollHint("‹", stringResource(R.string.reader_more_before), Brush.horizontalGradient(listOf(surface, surface, surface.copy(alpha = 0f))), Modifier.align(Alignment.CenterStart)) {
                scope.launch { state.animateScrollBy(-state.viewportSize * 0.7f) }
            }
        }
        if (state.canScrollForward) {
            ScrollHint("›", stringResource(R.string.reader_more_after), Brush.horizontalGradient(listOf(surface.copy(alpha = 0f), surface, surface)), Modifier.align(Alignment.CenterEnd)) {
                scope.launch { state.animateScrollBy(state.viewportSize * 0.7f) }
            }
        }
    }
}

@Composable
private fun ScrollHint(glyph: String, label: String, fade: Brush, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxHeight().width(32.dp).background(fade).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = MaterialTheme.typography.titleMedium, color = LocalPaperColors.current.inkTertiary)
    }
}

/** Une unité du sommaire et ce qu'elle contient (titres de niveau 1 et 2). */
private class TocGroup(val unit: TocEntry, val children: List<TocEntry>) {
    /** « Chapitre 12 » → 12 : la Bible montre ses chapitres en grille de numéros plutôt qu'en longue liste. */
    val chapterNumbers: List<Pair<String, TocEntry>>? = children.map { c -> CHAPTER.matchEntire(c.title)?.groupValues?.get(1)?.let { it to c } }
        .takeIf { it.isNotEmpty() && it.all { p -> p != null } }?.filterNotNull()

    private companion object { val CHAPTER = Regex("""Chapitre\s+(\d+)""") }
}

/**
 * Sommaire repliable : chaque unité (livre de la Bible, livre de l'Imitation…) se déplie d'un toucher ; celle qu'on lit
 * est ouverte d'emblée. Une unité sans subdivision s'ouvre directement.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TocPanel(graph: AppGraph, entry: LibraryEntry, language: String, current: String?, onClose: () -> Unit, onOpen: (TocEntry) -> Unit) {
    val toc by produceState<List<TocEntry>?>(null, entry, language) { value = withContext(Dispatchers.IO) { graph.packs.toc(entry, language) } }
    val groups = remember(toc) {
        val out = mutableListOf<TocGroup>()
        toc.orEmpty().forEach { e ->
            if (e.level == 0) out += TocGroup(e, emptyList())
            else out.lastOrNull()?.let { g ->
                // Le titre du livre, répété en tête de l'unité (« Genèse »), n'apporte rien.
                if (e.title.trim() != g.unit.title.trim()) out[out.lastIndex] = TocGroup(g.unit, g.children + e)
            }
        }
        out.toList()
    }
    var expandedRaw by rememberSaveable { mutableStateOf(current.orEmpty()) }
    val expanded = expandedRaw.split('\n').filter { it.isNotEmpty() }.toSet()
    fun toggle(unit: String) { expandedRaw = (if (unit in expanded) expanded - unit else expanded + unit).joinToString("\n") }
    val single = groups.size == 1
    val listState = rememberLazyListState()
    LaunchedEffect(groups) {
        val i = groups.indexOfFirst { it.unit.unit == current }
        if (i > 0) listState.scrollToItem(i)
    }
    val paper = LocalPaperColors.current

    Surface(Modifier.fillMaxSize(), color = paper.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("‹") }
                Text(stringResource(R.string.reader_toc_of, entry.title), style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            LazyColumn(Modifier.fillMaxSize(), state = listState) {
                groups.forEach { g ->
                    val open = single || g.unit.unit in expanded
                    item(key = "u:${g.unit.unit}") {
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { if (g.children.isEmpty() || single) onOpen(g.unit) else toggle(g.unit.unit) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (g.children.isEmpty() || single) " " else if (open) "▾" else "▸",
                                color = paper.inkSecondary,
                                modifier = Modifier.width(24.dp),
                            )
                            Text(
                                g.unit.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (g.unit.unit == current) paper.rubric else paper.inkPrimary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (!open) return@forEach
                    val numbers = g.chapterNumbers
                    if (numbers != null) {
                        item(key = "n:${g.unit.unit}") {
                            FlowRow(Modifier.fillMaxWidth().padding(start = 36.dp, end = 12.dp, bottom = 8.dp)) {
                                numbers.forEach { (n, e) ->
                                    Box(
                                        Modifier.size(48.dp).clickable { onOpen(e) },
                                        contentAlignment = Alignment.Center,
                                    ) { Text(n, style = MaterialTheme.typography.bodyLarge) }
                                }
                            }
                        }
                    } else {
                        items(g.children, key = { "c:${it.unit}/${it.blockId}" }) { e ->
                            Text(
                                e.title,
                                style = if (e.level == 1) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                                color = if (e.level == 1) paper.inkPrimary else paper.inkSecondary,
                                modifier = Modifier.fillMaxWidth().clickable { onOpen(e) }
                                    .padding(start = (36 + 16 * (e.level - 1)).dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
                            )
                        }
                    }
                    item(key = "d:${g.unit.unit}") { HorizontalDivider(color = paper.hairline) }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RibbonPanel(
    ribbons: List<Ribbon>,
    slots: List<Int>,
    unitTitle: (String) -> String,
    onPlace: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    Surface(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing), shadowElevation = 6.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.ribbons_hint), style = MaterialTheme.typography.bodySmall, color = LocalPaperColors.current.inkSecondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                slots.forEach { slot ->
                    val placed = ribbons.firstOrNull { it.slot == slot }
                    Column(
                        Modifier.combinedClickable(onClick = { onPlace(slot); onClose() }, onLongClick = { onRemove(slot) }).padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        RibbonIcon(slot, Modifier.size(width = 14.dp, height = 34.dp))
                        Text(RibbonStyle.of(slot).label, style = MaterialTheme.typography.labelSmall)
                        Text(
                            placed?.let { unitTitle(it.unit) } ?: stringResource(R.string.ribbon_free),
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalPaperColors.current.inkTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.size(width = 52.dp, height = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

/** La boîte à images, ouverte dans le livre : toucher glisse la carte à la page ; appui long la remet dans la boîte. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImagePanel(graph: AppGraph, images: List<PiousImage>, onScan: () -> Unit, placedHere: Set<String>, onPlace: (String) -> Unit, onRemove: (String) -> Unit, onClose: () -> Unit, modifier: Modifier) {
    Surface(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing), shadowElevation = 6.dp) {
        Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.images_hint), style = MaterialTheme.typography.bodySmall, color = LocalPaperColors.current.inkSecondary, modifier = Modifier.padding(horizontal = 16.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item(key = "scan") {
                    OutlinedButton(onClick = onScan, modifier = Modifier.width(96.dp).aspectRatio(0.62f)) { Text(stringResource(R.string.images_scan_short), textAlign = TextAlign.Center) }
                }
                items(images, key = { it.id }) { image ->
                    Column(
                        Modifier.width(64.dp).combinedClickable(onClick = { onPlace(image.id); onClose() }, onLongClick = { onRemove(image.id) }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        graph.images.thumbnail(image.id)?.let {
                            Image(it, contentDescription = image.title.fr, contentScale = ContentScale.Crop, modifier = Modifier.width(56.dp).aspectRatio(0.58f).border(if (image.id in placedHere) 2.dp else 0.dp, LocalPaperColors.current.rubric))
                        }
                    }
                }
            }
        }
    }
}
