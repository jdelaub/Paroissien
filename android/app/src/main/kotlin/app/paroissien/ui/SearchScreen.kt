package app.paroissien.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.library.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Recherche plein texte dans tous les livres installés (FTS5 plié, sans accents ni casse). */
@Composable
fun SearchScreen(graph: AppGraph, open: (NavKey) -> Unit, onBack: (() -> Unit)? = null) {
    val library by graph.library.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>?>(null) }

    LaunchedEffect(query, library) {
        val lib = library
        if (query.isBlank() || lib == null) { results = null; return@LaunchedEffect }
        delay(250)
        results = withContext(Dispatchers.IO) { graph.packs.search(lib, query) }
    }

    Screen(stringResource(R.string.tab_search), onBack = onBack) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        val found = results
        when {
            found == null -> Unit
            found.isEmpty() -> item { Note(stringResource(R.string.search_none)) }
            else -> found.groupBy { it.entry }.forEach { (entry, hits) ->
                item(key = "h:${entry.publication.id}") {
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = LocalPaperColors.current.rubric,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                items(hits, key = { "${it.hit.publication}/${it.hit.blockId}/${it.hit.lang}" }) { r ->
                    Column(
                        Modifier.fillMaxWidth()
                            .clickable { open(ReaderKey(r.hit.publication, r.hit.unit, r.hit.blockId)) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Text(entry.unitTitle(r.hit.unit), style = MaterialTheme.typography.labelMedium, color = LocalPaperColors.current.inkSecondary)
                        Text(
                            highlighted(r.hit.text, r.hit.highlights),
                            style = if (r.hit.heading) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HorizontalDivider(color = LocalPaperColors.current.hairline)
                }
            }
        }
    }
}

@Composable
private fun highlighted(text: String, ranges: List<IntRange>): AnnotatedString {
    val mark = SpanStyle(background = LocalPaperColors.current.highlight, fontWeight = FontWeight.SemiBold)
    // Un extrait centré sur le premier mot trouvé : les versets longs ne cachent pas la réponse.
    val start = ranges.firstOrNull()?.first?.let { (it - 60).coerceAtLeast(0) }?.let { s -> text.lastIndexOf(' ', s).takeIf { it >= 0 && s > 0 }?.plus(1) ?: s } ?: 0
    return buildAnnotatedString {
        if (start > 0) append("… ")
        val offset = length - start
        append(text.substring(start))
        ranges.forEach { r -> if (r.first >= start) addStyle(mark, r.first + offset, (r.last + 1 + offset).coerceAtMost(length)) }
    }
}
