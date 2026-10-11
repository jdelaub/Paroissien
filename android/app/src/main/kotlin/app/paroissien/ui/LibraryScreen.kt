package app.paroissien.ui

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

const val DEMO = "demo"

/** Bibliothèque : chaque livre, la reprise de lecture, et les séries (lettres de Clairval) regroupées. */
@Composable
fun LibraryScreen(graph: AppGraph, open: (NavKey) -> Unit) {
    val library by graph.library.collectAsState()
    val positions by remember { graph.database.positions().observeAll() }.collectAsState(initial = emptyList())
    val resume = positions.associateBy { it.publication }
    Screen(stringResource(R.string.tab_library)) {
        val lib = library
        if (lib == null) {
            item { Note(stringResource(R.string.library_loading)) }
            return@Screen
        }
        lib.problems.forEach { item { Note(it, error = true) } }
        val (series, books) = lib.entries.partition { it.series != null }
        items(books, key = { it.publication.id }) { entry ->
            val position = resume[entry.publication.id]
            BookRow(
                title = entry.title,
                detail = position?.let { stringResource(R.string.library_resume, entry.unitTitle(it.unit)) }
                    ?: pluralStringResource(R.plurals.units, entry.units.size, entry.units.size),
            ) { open(ReaderKey(entry.publication.id)) }
        }
        series.groupBy { it.series!! }.forEach { (id, letters) ->
            item(key = id) {
                val title = stringResource(R.string.series_clairval)
                BookRow(title, pluralStringResource(R.plurals.letters, letters.size, letters.size)) { open(SeriesKey(id, title)) }
            }
        }
        item(key = DEMO) { BookRow(stringResource(R.string.demo_book), stringResource(R.string.demo_book_detail)) { open(ReaderKey(DEMO)) } }
    }
}

/** Les publications d'une série, de la plus récente à la plus ancienne. */
@Composable
fun SeriesScreen(graph: AppGraph, key: SeriesKey, open: (NavKey) -> Unit, onBack: () -> Unit) {
    val library by graph.library.collectAsState()
    val letters = library?.entries.orEmpty().filter { it.series == key.series }.sortedByDescending { it.issued ?: it.publication.id }
    Screen(key.title, onBack = onBack) {
        items(letters, key = { it.publication.id }) { entry: LibraryEntry ->
            BookRow(entry.title, entry.issued?.let(::frenchDate)) { open(ReaderKey(entry.publication.id)) }
        }
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
