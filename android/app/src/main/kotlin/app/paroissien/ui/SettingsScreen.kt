package app.paroissien.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.data.ReaderSettings
import app.paroissien.core.designsystem.BookFace
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.RibbonFabric
import app.paroissien.core.reading.book.SpreadMode
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

/**
 * Tous les réglages, ouverts depuis l'accueil (ou depuis Aa dans le lecteur) : lecture, livres, calendrier, temps de
 * prière. Chaque choix s'enregistre aussitôt.
 */
@Composable
fun SettingsScreen(graph: AppGraph, settings: ReaderSettings, onBack: () -> Unit) {
    fun update(transform: (ReaderSettings) -> ReaderSettings) = graph.scope.launch { graph.settings.update(transform) }

    Screen(stringResource(R.string.settings_title), onBack = onBack) {
        item {
            Group(stringResource(R.string.settings_reading)) {
                Choice(stringResource(R.string.settings_theme), PaperTheme.entries, settings.paperTheme(), { stringResource(themeLabel(it)) }) { v -> update { it.copy(theme = v.name) } }
                Choice(stringResource(R.string.settings_font), BookFace.entries, settings.bookFace(), { stringResource(if (it == BookFace.GARAMOND) R.string.font_garamond else R.string.font_literata) }) { v ->
                    update { it.copy(face = v.name) }
                }
                Choice(stringResource(R.string.settings_size), SCALES.indices.toList(), settings.scaleIndex.coerceIn(0, SCALES.lastIndex), { "${(SCALES[it] * 100).toInt()} %" }) { v ->
                    update { it.copy(scaleIndex = v) }
                }
                Choice(stringResource(R.string.settings_spread), SpreadMode.entries, settings.spreadMode(), { stringResource(spreadLabel(it)) }) { v -> update { it.copy(spread = v.name) } }
                Choice(stringResource(R.string.settings_turn), TurnStyle.entries, settings.turnStyle(), { stringResource(turnLabel(it)) }) { v -> update { it.copy(turn = v.name) } }
                Choice(stringResource(R.string.settings_language), listOf("fr", "la"), settings.language, { stringResource(if (it == "la") R.string.language_la else R.string.language_fr) }) { v ->
                    update { it.copy(language = v) }
                }
                SwitchRow(stringResource(R.string.settings_edges), settings.protectEdges) { v -> update { it.copy(protectEdges = v) } }
            }
        }
        item {
            Group(stringResource(R.string.settings_books)) {
                SwitchRow(stringResource(R.string.settings_identity), settings.bookIdentity) { v -> update { it.copy(bookIdentity = v) } }
                SwitchRow(stringResource(R.string.settings_ribbons_across), settings.ribbonsAcross) { v -> update { it.copy(ribbonsAcross = v) } }
                Choice(stringResource(R.string.settings_fabric), RibbonFabric.entries, settings.ribbonFabric(), {
                    stringResource(if (it == RibbonFabric.SATIN) R.string.fabric_satin else R.string.fabric_textured)
                }) { v -> update { it.copy(ribbonFabric = if (v == RibbonFabric.SATIN) "SATIN" else "TISSU") } }
                SwitchRow(stringResource(R.string.settings_cards_on_page), settings.cardsOnPage) { v -> update { it.copy(cardsOnPage = v) } }
                SwitchRow(stringResource(R.string.settings_frames), settings.ornamentalFrames) { v -> update { it.copy(ornamentalFrames = v) } }
                SwitchRow(stringResource(R.string.settings_vignettes), settings.vignettes) { v -> update { it.copy(vignettes = v) } }
            }
        }
        item {
            Group(stringResource(R.string.calendar_settings)) {
                Choice(stringResource(R.string.settings_rite), listOf("1962", "1969"), settings.rite, { stringResource(if (it == "1962") R.string.rite_1962 else R.string.rite_1969) }) { v ->
                    update { it.copy(rite = v) }
                }
                SwitchRow(stringResource(R.string.setting_external), settings.externalSolemnities) { v -> update { it.copy(externalSolemnities = v) } }
                SwitchRow(stringResource(R.string.setting_traditional), settings.traditionalPenance) { v -> update { it.copy(traditionalPenance = v) } }
            }
        }
        item {
            Group(stringResource(R.string.settings_prayer)) {
                Choice(stringResource(R.string.prayer_duration), DURATIONS, settings.prayerMinutes, { stringResource(R.string.minutes, it) }) { v -> update { it.copy(prayerMinutes = v) } }
                SwitchRow(stringResource(R.string.prayer_bells), settings.prayerBells) { v -> update { it.copy(prayerBells = v) } }
                SwitchRow(stringResource(R.string.prayer_dnd), settings.prayerDoNotDisturb) { v -> update { it.copy(prayerDoNotDisturb = v) } }
            }
        }
        item { BackupGroup(graph) }
        item {
            Group(stringResource(R.string.settings_trials)) {
                SwitchRow(stringResource(R.string.settings_measures), settings.showMeasures) { v -> update { it.copy(showMeasures = v) } }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Sauvegarde complète : tout exporter dans un fichier, ou l'importer (après confirmation). Le travail tourne dans la
 * portée de l'application : il continue si l'on quitte l'écran.
 */
@Composable
private fun BackupGroup(graph: AppGraph) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var status by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<Uri?>(null) }
    val exporting = stringResource(R.string.backup_exporting)
    val importing = stringResource(R.string.backup_importing)
    fun failed(e: Throwable) = resources.getString(R.string.backup_failed, e.message ?: e.javaClass.simpleName)
    fun count(id: Int, n: Int) = resources.getQuantityString(id, n, n)

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        status = exporting
        graph.scope.launch {
            status = runCatching {
                val s = context.contentResolver.openOutputStream(uri, "wt")!!.use { graph.backup.export(it) }
                resources.getString(R.string.backup_exported, count(R.plurals.backup_books, s.books), count(R.plurals.backup_images, s.images))
            }.getOrElse(::failed)
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pending = uri }

    Group(stringResource(R.string.settings_backup)) {
        Text(stringResource(R.string.backup_explain), style = MaterialTheme.typography.bodyMedium, color = LocalPaperColors.current.inkSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { exporter.launch("paroissien-sauvegarde-${LocalDate.now()}.zip") }) { Text(stringResource(R.string.backup_export)) }
            OutlinedButton(onClick = { importer.launch(arrayOf("application/zip", "application/octet-stream")) }) { Text(stringResource(R.string.backup_import)) }
        }
        status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }

    pending?.let { uri ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.backup_confirm_title)) },
            text = { Text(stringResource(R.string.backup_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    status = importing
                    graph.scope.launch {
                        status = runCatching {
                            val s = context.contentResolver.openInputStream(uri)!!.use { graph.backup.import(it) }
                            val settings = if (s.settings) resources.getString(R.string.backup_restored_settings) else ""
                            (listOf(resources.getString(R.string.backup_restored, count(R.plurals.backup_books, s.books), count(R.plurals.backup_images, s.images), count(R.plurals.backup_marks, s.marks), settings)) + s.problems)
                                .joinToString("\n")
                        }.getOrElse(::failed)
                    }
                }) { Text(stringResource(R.string.backup_confirm)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.backup_cancel)) } },
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title.uppercase(Locale.FRENCH), style = MaterialTheme.typography.labelMedium, color = LocalPaperColors.current.rubric, modifier = Modifier.padding(top = 10.dp))
        content()
    }
}

/** Un choix parmi quelques valeurs, en pastilles qui passent à la ligne sur un écran étroit. */
@Composable
private fun <T> Choice(label: String, values: List<T>, selected: T, name: @Composable (T) -> String, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { v ->
                FilterChip(selected = v == selected, onClick = { onSelect(v) }, label = { Text(name(v).replaceFirstChar { it.titlecase(Locale.FRENCH) }) })
            }
        }
    }
}
