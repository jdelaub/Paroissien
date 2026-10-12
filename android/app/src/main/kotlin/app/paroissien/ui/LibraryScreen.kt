package app.paroissien.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.library.LibraryEntry
import app.paroissien.library.subtitle
import app.paroissien.library.BookIdentities
import app.paroissien.core.designsystem.BookIdentity
import app.paroissien.core.designsystem.BookCover
import app.paroissien.core.data.ReaderSettings
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement

const val DEMO = "demo"

/**
 * Bibliothèque : un rayonnage de couvertures, chaque livre avec sa reliure (ou toutes pareilles, selon le réglage),
 * la reprise de lecture sous chacun, les quatre Évangiles, et les séries (lettres de Clairval) regroupées.
 */
@Composable
fun LibraryScreen(graph: AppGraph, settings: ReaderSettings, open: (NavKey) -> Unit) {
    val library by graph.library.collectAsState()
    val positions by remember { graph.database.positions().observeAll() }.collectAsState(initial = emptyList())
    val resume = positions.associateBy { it.publication }
    val uniform = !settings.bookIdentity
    Shelf(stringResource(R.string.tab_library)) {
        val lib = library
        if (lib == null) {
            wide { Note(stringResource(R.string.library_loading)) }
            return@Shelf
        }
        lib.problems.forEach { wide { Note(it, error = true) } }
        // App livrée vide (saveur « vide ») ou sans paquet : les livres reviennent par une sauvegarde.
        if (lib.entries.isEmpty()) {
            wide {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Note(stringResource(R.string.library_empty))
                    OutlinedButton(onClick = { open(SettingsKey) }) { Text(stringResource(R.string.backup_import)) }
                }
            }
        }
        val (series, books) = lib.entries.partition { it.series != null }
        items(books, key = { it.publication.id }) { entry ->
            val position = resume[entry.publication.id]
            CoverTile(
                identity = BookIdentities.of(entry.publication.id, entry, uniform),
                title = entry.title,
                subtitle = entry.subtitle(),
                detail = position?.let { stringResource(R.string.library_resume, entry.unitTitle(it.unit)) }
                    ?: pluralStringResource(R.plurals.units, entry.units.size, entry.units.size),
                // Appui long : habiller ce livre (cadres, cadre composé).
                onLongClick = { open(BookDressKey(entry.publication.id)) },
            ) { open(ReaderKey(entry.publication.id, opening = true)) }
        }
        // Les quatre Évangiles : une vue de la Bible (PLAN §3.1), reliée comme elle, ouverte au livre ou là où l'on s'est arrêté.
        lib.entry(GOSPEL_BIBLE)?.let { bible ->
            val gospels = GOSPELS.filter { (unit, _) -> bible.unit(unit) != null }
            if (gospels.isNotEmpty()) wide { Heading(stringResource(R.string.library_gospels)) }
            val identity = BookIdentities.of(GOSPEL_BIBLE, bible, uniform).copy(pages = 400)
            gospels.forEach { (unit, title) ->
                item(key = "gospel:$unit") {
                    val here = resume[GOSPEL_BIBLE]?.takeIf { it.unit == unit }
                    CoverTile(identity, stringResource(title), null, here?.let { stringResource(R.string.library_resume, bible.unitTitle(it.unit)) } ?: bible.title) {
                        open(ReaderKey(GOSPEL_BIBLE, unit, here?.block, here?.charOffset ?: 0, opening = true))
                    }
                }
            }
        }
        series.groupBy { it.series!! }.forEach { (id, letters) ->
            item(key = id) {
                val title = stringResource(R.string.series_clairval)
                CoverTile(
                    if (uniform) BookIdentities.of(id, null, true) else BookIdentities.letter(pages = 40).copy(pages = 60 + letters.size),
                    title, null, pluralStringResource(R.plurals.letters, letters.size, letters.size),
                ) { open(SeriesKey(id, title)) }
            }
        }
        // La bibliothèque des images pieuses, à côté des livres : une boîte, ses premières cartes en éventail.
        item(key = "images") { ImageBoxTile(graph) { open(ImageBoxKey) } }
        item(key = "atelier") { WorkshopTile { open(WorkshopKey) } }
        item(key = DEMO) {
            CoverTile(BookIdentities.of(DEMO, null, true), stringResource(R.string.demo_book), null, stringResource(R.string.demo_book_detail)) { open(ReaderKey(DEMO)) }
        }
    }
}

/** L'atelier sur le rayon : un cadre en petit, pour composer des cadres et dessiner des motifs sans passer par un livre. */
@Composable
private fun WorkshopTile(onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.70f).background(LocalPaperColors.current.background), contentAlignment = Alignment.Center) {
            app.paroissien.core.designsystem.frame.OrnamentalFrames.firstOrNull { it.name == "matin" }?.let { FrameThumb(it, width = 96.dp) }
        }
        Text(stringResource(R.string.library_workshop), style = MaterialTheme.typography.bodySmall, color = LocalPaperColors.current.inkSecondary, maxLines = 2)
    }
}

/** La boîte à images sur le rayon : trois cartes en éventail sur un fond de carton, et leur nombre. */
@Composable
private fun ImageBoxTile(graph: AppGraph, onClick: () -> Unit) {
    val images by graph.images.all.collectAsState()
    val paper = LocalPaperColors.current
    Column(Modifier.clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.70f).background(Color(0xFF5B4632), RoundedCornerShape(4.dp)).padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            val fan = images.take(3)
            fan.forEachIndexed { k, image ->
                graph.images.thumbnail(image.id)?.let {
                    Image(
                        it, contentDescription = null, contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(0.78f).rotate((k - (fan.size - 1) / 2f) * 9f)
                            .offset(x = ((k - (fan.size - 1) / 2f) * 10).dp)
                            .shadow(3.dp, RoundedCornerShape(4)).clip(RoundedCornerShape(4)),
                    )
                }
            }
            if (fan.isEmpty()) Text(stringResource(R.string.images_box), color = Color(0xFFF7F0DD), style = MaterialTheme.typography.titleMedium)
        }
        Text(
            if (images.isEmpty()) stringResource(R.string.images_box_empty) else pluralStringResource(R.plurals.images_box_detail, images.size, images.size),
            style = MaterialTheme.typography.bodySmall, color = paper.inkSecondary, maxLines = 2,
        )
    }
}

/** Les publications d'une série, de la plus récente à la plus ancienne, chacune sous sa couverture de papier. */
@Composable
fun SeriesScreen(graph: AppGraph, settings: ReaderSettings, key: SeriesKey, open: (NavKey) -> Unit, onBack: () -> Unit) {
    val library by graph.library.collectAsState()
    val letters = library?.entries.orEmpty().filter { it.series == key.series }.sortedByDescending { it.issued ?: it.publication.id }
    Shelf(key.title, onBack = onBack) {
        items(letters, key = { it.publication.id }) { entry: LibraryEntry ->
            CoverTile(BookIdentities.of(entry.publication.id, entry, !settings.bookIdentity), entry.title, entry.issued?.let(::frenchDate), null) {
                open(ReaderKey(entry.publication.id))
            }
        }
    }
}

/** Un rayonnage : titre, retour éventuel, couvertures en grille ; `wide` occupe toute une rangée. */
@Composable
private fun Shelf(title: String, onBack: (() -> Unit)? = null, content: LazyGridScope.() -> Unit) {
    Surface(Modifier.fillMaxSize(), color = LocalPaperColors.current.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) TextButton(onClick = onBack) { Text("‹") } else Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall)
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                content = content,
            )
        }
    }
}

private fun LazyGridScope.wide(content: @Composable () -> Unit) = item(span = { GridItemSpan(maxLineSpan) }) { content() }

private const val GOSPEL_BIBLE = "bible.crampon"
private val GOSPELS = listOf(
    "mat" to R.string.gospel_matthew,
    "mrk" to R.string.gospel_mark,
    "luk" to R.string.gospel_luke,
    "jhn" to R.string.gospel_john,
)

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = LocalPaperColors.current.rubric, modifier = Modifier.padding(top = 8.dp))
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CoverTile(identity: BookIdentity, title: String, subtitle: String?, detail: String?, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Column(Modifier.combinedClickable(onLongClick = onLongClick, onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BookCover(identity, title, subtitle, Modifier.fillMaxWidth().aspectRatio(0.70f))
        detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = LocalPaperColors.current.inkSecondary, maxLines = 2) }
    }
}

private val months = listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre")

/** « 1996-07-25 » → « 25 juillet 1996 ». */
fun frenchDate(iso: String): String {
    val parts = iso.split('-')
    val month = parts.getOrNull(1)?.toIntOrNull()?.let { months.getOrNull(it - 1) } ?: return iso
    val day = parts.getOrNull(2)?.toIntOrNull()
    return listOfNotNull(day?.let { if (it == 1) "1er" else "$it" }, month, parts[0]).joinToString(" ")
}

/** Gabarit commun des écrans de liste : titre, retour éventuel, liste paresseuse sur le papier. */
@Composable
fun Screen(title: String, onBack: (() -> Unit)? = null, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Surface(Modifier.fillMaxSize(), color = LocalPaperColors.current.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) TextButton(onClick = onBack) { Text("‹") } else Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall)
            }
            LazyColumn(Modifier.fillMaxSize(), content = content)
        }
    }
}

@Composable
fun BookRow(title: String, detail: String?, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = LocalPaperColors.current.inkSecondary) }
    }
    HorizontalDivider(color = LocalPaperColors.current.hairline)
}

@Composable
fun Note(text: String, error: Boolean = false) {
    Text(text, color = if (error) MaterialTheme.colorScheme.error else LocalPaperColors.current.inkSecondary, modifier = Modifier.padding(20.dp))
}
