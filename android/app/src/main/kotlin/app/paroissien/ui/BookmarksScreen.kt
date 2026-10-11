package app.paroissien.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.RibbonStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Signets : les rubans posés dans chaque livre, les lectures en cours, et la sauvegarde JSON (rapport 05). */
@Composable
fun BookmarksScreen(graph: AppGraph, open: (NavKey) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val library by graph.library.collectAsState()
    val ribbons by remember { graph.database.ribbons().observeAll() }.collectAsState(initial = emptyList())
    val positions by remember { graph.database.positions().observeAll() }.collectAsState(initial = emptyList())
    var message by remember { mutableStateOf<String?>(null) }
    val exported = stringResource(R.string.bookmarks_exported)
    val failed = stringResource(R.string.bookmarks_failed)
    val resources = LocalResources.current

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            message = runCatching {
                withContext(Dispatchers.IO) {
                    val text = graph.transfer.export()
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
                }
                exported
            }.getOrElse { "$failed ${it.message.orEmpty()}" }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            message = runCatching {
                val taken = withContext(Dispatchers.IO) {
                    val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                    graph.transfer.import(text)
                }
                resources.getQuantityString(R.plurals.bookmarks_imported, taken, taken)
            }.getOrElse { "$failed ${it.message.orEmpty()}" }
        }
    }

    Screen(stringResource(R.string.tab_bookmarks)) {
        val lib = library
        if (ribbons.isEmpty() && positions.isEmpty()) item { Note(stringResource(R.string.bookmarks_empty)) }

        ribbons.groupBy { it.publication }.forEach { (publication, placed) ->
            val entry = lib?.entry(publication)
            item(key = "r:$publication") { Heading(entry?.title ?: publication) }
            placed.sortedBy { it.slot }.forEach { r ->
                item(key = "r:$publication:${r.slot}") {
                    Row(
                        Modifier.fillMaxWidth().clickable { open(ReaderKey(publication, r.unit, r.block, r.charOffset)) }.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        RibbonIcon(r.slot)
                        Column {
                            Text(entry?.unitTitle(r.unit) ?: r.unit, style = MaterialTheme.typography.bodyLarge)
                            Text(RibbonStyle.of(r.slot).label, style = MaterialTheme.typography.bodySmall, color = LocalPaperColors.current.inkSecondary)
                        }
                    }
                }
            }
        }

        if (positions.isNotEmpty()) {
            item(key = "reading") { Heading(stringResource(R.string.bookmarks_reading)) }
            positions.sortedByDescending { it.updatedAt }.forEach { p ->
                item(key = "p:${p.publication}") {
                    val entry = lib?.entry(p.publication)
                    BookRow(entry?.title ?: p.publication, entry?.unitTitle(p.unit)) { open(ReaderKey(p.publication, p.unit, p.block, p.charOffset)) }
                }
            }
        }

        item(key = "backup") {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(color = LocalPaperColors.current.hairline)
                Text(stringResource(R.string.bookmarks_backup_hint), style = MaterialTheme.typography.bodySmall, color = LocalPaperColors.current.inkSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exporter.launch("paroissien-signets-${LocalDate.now()}.json") }) { Text(stringResource(R.string.bookmarks_export)) }
                    OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text(stringResource(R.string.bookmarks_import)) }
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = LocalPaperColors.current.rubric, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp))
}

