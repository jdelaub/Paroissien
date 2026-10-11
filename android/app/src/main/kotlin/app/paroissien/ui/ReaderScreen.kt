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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.layout.ContentScale
import androidx.navigation3.runtime.NavKey
import app.paroissien.core.data.CardPlacement
import app.paroissien.core.reading.book.CardMark
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
    val cardMarks = cards.mapNotNull { c ->
        val index = units.indexOfFirst { it.id == c.unit }
        val image = graph.images[c.card] ?: return@mapNotNull null
        when {
            index < 0 -> null
            c.unit == unitId -> CardMark(c.card, Anchor(c.block, c.charOffset), image = graph.images.thumbnail(c.card), label = image.title.fr)
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
    val paper = LocalPaperColors.current
    val typography = remember(fonts, paper, settings.scaleIndex) { BookTypography(fonts, paper, SCALES[settings.scaleIndex.coerceIn(0, SCALES.lastIndex)]) }
    val turn = settings.turnStyle()
    val configuration = remember(turn) { MekuriConfiguration(reducedMotionOverride = if (turn == TurnStyle.AUCUN) true else null) }

    Box(Modifier.fillMaxSize().background(paper.background)) {
        blocks?.let { book ->
            key(unitId, language) {
                BookReader(
                    blocks = book,
                    typography = typography,
                    anchor = anchorBlock?.let { Anchor(it, anchorOffset) },
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
                    onTurnPastEnd = units.getOrNull(unitIndex + 1)?.let { next -> { openAt(next.id, null, 0) } },
                    onTurnPastStart = units.getOrNull(unitIndex - 1)?.let { previous -> { openAt(previous.id, null, 0, end = true) } },
                    initialAtEnd = atEnd,
                )
            }
        }

        when (panel) {
            Panel.CHROME -> Chrome(
                title = if (entry == null) stringResource(R.string.demo_book) else listOfNotNull(entry.title, unit?.title?.takeIf { it != entry.title }).joinToString(" · "),
                onBack = onBack,
                onToc = { panel = Panel.TOC }.takeIf { entry != null },
                onRibbons = { panel = Panel.RIBBONS }.takeIf { !isDemo && entry != null },
                onImages = { panel = Panel.IMAGES }.takeIf { !isDemo && entry != null && graph.images.all.isNotEmpty() },
                onPray = { open(PrayerKey(publication.takeIf { !isDemo }, unitId, anchorBlock)) },
                onSettings = { panel = Panel.SETTINGS },
                modifier = Modifier.align(Alignment.TopCenter),
            )
            Panel.TOC -> if (entry != null) TocPanel(graph, entry, language, onClose = { panel = null }) { e -> openAt(e.unit, e.blockId, 0); panel = null }
            Panel.RIBBONS -> RibbonPanel(
                ribbons = ribbons,
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
                ReaderSettingsRow(settings) { transform -> graph.scope.launch { graph.settings.update(transform) } }
            }
            null -> Unit
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

@Composable
private fun Chrome(title: String, onBack: () -> Unit, onToc: (() -> Unit)?, onRibbons: (() -> Unit)?, onImages: (() -> Unit)?, onPray: () -> Unit, onSettings: () -> Unit, modifier: Modifier) {
    Surface(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing), shadowElevation = 4.dp) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Retour à la bibliothèque" }) { Text("‹") }
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            onToc?.let { TextButton(onClick = it) { Text(stringResource(R.string.reader_toc)) } }
            onRibbons?.let { TextButton(onClick = it) { Text(stringResource(R.string.reader_ribbons)) } }
            onImages?.let { TextButton(onClick = it) { Text(stringResource(R.string.reader_images)) } }
            TextButton(onClick = onPray) { Text(stringResource(R.string.reader_pray)) }
            TextButton(onClick = onSettings) { Text(stringResource(R.string.reader_settings)) }
        }
    }
}

@Composable
private fun TocPanel(graph: AppGraph, entry: LibraryEntry, language: String, onClose: () -> Unit, onOpen: (TocEntry) -> Unit) {
    val toc by produceState<List<TocEntry>?>(null, entry, language) { value = withContext(Dispatchers.IO) { graph.packs.toc(entry, language) } }
    Screen(stringResource(R.string.reader_toc_of, entry.title), onBack = onClose) {
        items(toc.orEmpty(), key = { "${it.unit}/${it.blockId}" }) { e ->
            Text(
                e.title,
                style = if (e.level == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().clickable { onOpen(e) }.padding(start = (20 + 16 * e.level).dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RibbonPanel(
    ribbons: List<Ribbon>,
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
                RibbonStyle.slots.indices.forEach { slot ->
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
private fun ImagePanel(graph: AppGraph, placedHere: Set<String>, onPlace: (String) -> Unit, onRemove: (String) -> Unit, onClose: () -> Unit, modifier: Modifier) {
    Surface(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing), shadowElevation = 6.dp) {
        Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.images_hint), style = MaterialTheme.typography.bodySmall, color = LocalPaperColors.current.inkSecondary, modifier = Modifier.padding(horizontal = 16.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(graph.images.all, key = { it.id }) { image ->
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
