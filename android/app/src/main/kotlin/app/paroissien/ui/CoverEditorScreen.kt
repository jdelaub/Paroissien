package app.paroissien.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.data.BookLookRow
import app.paroissien.core.data.CustomFrameRow
import app.paroissien.core.designsystem.BookCover
import app.paroissien.core.designsystem.BookIdentity
import app.paroissien.core.designsystem.CoverMaterial
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.library.BindingChoice
import app.paroissien.library.BookDressChoice
import app.paroissien.library.CoverTemplate
import app.paroissien.library.dressed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * L'éditeur de couvertures (atelier, demande du 13/10/2026) : une reliure qu'on compose à vue (matière et sa texture,
 * couleur, fers et leur métal, motif et son encadrement, roulette, filets, fers d'angle, dos, tranche), qu'on garde
 * comme modèle et qu'on applique à un ou plusieurs livres. Rien n'est enregistré tant qu'on n'a pas touché « Enregistrer » ou « Appliquer ».
 */
@Composable
fun CoverEditorScreen(graph: AppGraph, key: CoverEditorKey, onBack: () -> Unit) {
    val rows by remember { graph.database.looks().observeFrames() }.collectAsState(initial = null)
    val existing = rows?.firstOrNull { it.id == key.id }?.let { CoverTemplate.decode(it.json) }
    if (rows == null) return
    val defaultName = stringResource(R.string.cover_editor_default_name)
    var template by rememberSaveable(existing?.id, stateSaver = coverSaver) {
        mutableStateOf(existing ?: CoverTemplate(CoverTemplate.PREFIX + UUID.randomUUID().toString().take(8), defaultName))
    }
    var applying by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    val binding = template.binding
    val base = BookIdentity.uniform(400)
    val identity = base.dressed(binding)
    fun edit(next: BindingChoice) { template = template.copy(binding = next) }
    fun persist() = graph.scope.launch(Dispatchers.IO) {
        graph.database.looks().saveFrame(CustomFrameRow(template.id, CoverTemplate.encode(template), System.currentTimeMillis()))
    }

    Screen(stringResource(R.string.cover_editor_title), onBack = onBack) {
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BookCover(identity, template.name, stringResource(R.string.cover_editor_sample_subtitle), Modifier.width(210.dp).aspectRatio(0.70f).clickable { preview = true })
                PreviewButton { preview = true }
            }
        }
        item {
            WorkshopSection(stringResource(R.string.cover_editor_design)) {
                OutlinedTextField(
                    value = template.name, onValueChange = { template = template.copy(name = it.take(40)) },
                    label = { Text(stringResource(R.string.cover_editor_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Label(R.string.binding_leather)
                ColorPicker(BindingChoice.leathers, binding.leather, identity.cover.leather) { edit(binding.copy(leather = it)) }
                BindingFields(binding, identity) { edit(it) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { persist(); onBack() }) { Text(stringResource(R.string.cover_editor_save)) }
                    OutlinedButton(onClick = { applying = true }) { Text(stringResource(R.string.cover_editor_apply)) }
                    if (existing != null) {
                        TextButton(onClick = {
                            graph.scope.launch(Dispatchers.IO) { graph.database.looks().deleteFrame(template.id) }
                            onBack()
                        }) { Text(stringResource(R.string.cover_editor_delete), color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    if (applying) ApplyToBooks(graph, template, onDone = { applying = false })
    if (preview) CoverPreview(identity, template.name, stringResource(R.string.cover_editor_sample_subtitle), onClose = { preview = false })
}

/** Appliquer le modèle à des livres de la bibliothèque : leur reliure devient celle du modèle (le reste de l'habit reste). */
@Composable
private fun ApplyToBooks(graph: AppGraph, template: CoverTemplate, onDone: () -> Unit) {
    val library by graph.library.collectAsState()
    val books = library?.entries.orEmpty().filter { it.series == null }
    var chosen by remember { mutableStateOf(setOf<String>()) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(R.string.cover_editor_apply_title, template.name)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                items(books, key = { it.publication.id }) { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(entry.publication.id in chosen, { on -> chosen = if (on) chosen + entry.publication.id else chosen - entry.publication.id })
                        Text(entry.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = chosen.isNotEmpty(), onClick = {
                graph.scope.launch(Dispatchers.IO) {
                    val looks = graph.database.looks()
                    graph.database.looks().saveFrame(CustomFrameRow(template.id, CoverTemplate.encode(template), System.currentTimeMillis()))
                    val rows = looks.all().associateBy { it.publication }
                    for (publication in chosen) {
                        val choice = BookDressChoice.decode(rows[publication]?.json) ?: BookDressChoice()
                        looks.save(BookLookRow(publication, BookDressChoice.encode(choice.copy(binding = template.binding)), System.currentTimeMillis()))
                    }
                }
                onDone()
            }) { Text(stringResource(R.string.cover_editor_apply_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.card_cancel)) } },
    )
}

@Composable
private fun Label(@StringRes id: Int) = Text(stringResource(id), style = MaterialTheme.typography.bodyMedium)

/** Les matières, dans l'ordre de l'atelier : les reliures des livres, puis les nouvelles textures. */
internal val MATERIALS = listOf(
    CoverMaterial.MOROCCO to R.string.binding_morocco,
    CoverMaterial.CALF to R.string.binding_calf,
    CoverMaterial.SHAGREEN to R.string.binding_shagreen,
    CoverMaterial.VELLUM to R.string.binding_vellum,
    CoverMaterial.VELVET to R.string.binding_velvet,
    CoverMaterial.CLOTH to R.string.binding_cloth,
    CoverMaterial.LINEN to R.string.binding_linen,
    CoverMaterial.MARBLED to R.string.binding_marbled,
    CoverMaterial.WRAPPER to R.string.binding_wrapper,
)

/** Le modèle en cours survit à une rotation de l'écran (JSON). */
private val coverSaver = androidx.compose.runtime.saveable.Saver<CoverTemplate, String>(
    save = { CoverTemplate.encode(it) },
    restore = { text -> CoverTemplate.decode(text) },
)

/** Une vignette de modèle de couverture, pour l'atelier et l'habit d'un livre. */
@Composable
internal fun CoverThumb(template: CoverTemplate, width: androidx.compose.ui.unit.Dp = 70.dp) {
    BookCover(BookIdentity.uniform(300).dressed(template.binding), template.name, null, Modifier.width(width).aspectRatio(0.70f))
}

internal fun Color.argbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL
