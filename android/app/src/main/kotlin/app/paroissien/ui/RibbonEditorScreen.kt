package app.paroissien.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.data.ReaderSettings
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.LocalRibbonFabric
import app.paroissien.core.designsystem.RibbonEmblem
import app.paroissien.core.designsystem.RibbonEnd
import app.paroissien.core.designsystem.RibbonFabric
import app.paroissien.core.designsystem.RibbonStyle
import app.paroissien.core.designsystem.drawRibbonAlong
import app.paroissien.core.designsystem.liturgicalTokens
import app.paroissien.library.RibbonChoice
import app.paroissien.library.RibbonChoices
import app.paroissien.library.with
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * L'éditeur de rubans (atelier, demande du 13/10/2026) : les six rubans des livres, pendus côte à côte comme sous la
 * tranche ; on en touche un pour le retoucher : son nom, sa couleur (liturgique ou libre), son tissu, son extrémité,
 * sa broderie. Chaque retouche s'enregistre aussitôt dans les réglages (donc dans la sauvegarde complète) ; les livres
 * gardent leurs emplacements, seul le ruban change.
 */
@Composable
fun RibbonEditorScreen(graph: AppGraph, settings: ReaderSettings, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val choices = RibbonChoices.decode(settings.ribbons).let { list -> List(RibbonStyle.slots.size) { list.getOrNull(it) ?: RibbonChoice() } }
    val styles = RibbonStyle.slots.mapIndexed { slot, base -> base.with(choices[slot]) }
    fun save(slot: Int, choice: RibbonChoice) {
        val next = choices.toMutableList().also { it[slot] = choice }
        scope.launch { graph.settings.update { it.copy(ribbons = RibbonChoices.encode(next)) } }
    }
    val choice = choices[selected]
    val base = RibbonStyle.slots[selected]

    Screen(stringResource(R.string.ribbons_editor_title), onBack = onBack) {
        item { RibbonRack(styles, selected) { selected = it } }
        item {
            WorkshopSection(stringResource(R.string.ribbons_editor_slot, styles[selected].label)) {
                // Le nom se tape dans un état local (le curseur ne saute pas) et s'enregistre à chaque lettre.
                var name by rememberSaveable(selected) { mutableStateOf(choice.label.orEmpty()) }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30); save(selected, choice.copy(label = name.ifBlank { null })) },
                    label = { Text(stringResource(R.string.ribbons_editor_name)) },
                    placeholder = { Text(base.label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.ribbons_editor_color), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(choice.argb == null, { save(selected, choice.copy(argb = null)) }, { Text(stringResource(R.string.ribbons_editor_liturgical, base.label)) })
                }
                val dark = LocalPaperColors.current.isDark
                ColorPicker(RibbonChoices.palette, choice.argb, liturgicalTokens(base.color, dark).fabric) { save(selected, choice.copy(argb = it)) }
                Text(stringResource(R.string.ribbons_editor_fabric), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(choice.fabric == null, { save(selected, choice.copy(fabric = null)) }, { Text(stringResource(R.string.ribbons_editor_fabric_default)) })
                    RibbonFabric.entries.forEach { f ->
                        FilterChip(choice.fabric == f.name, { save(selected, choice.copy(fabric = f.name)) }, { Text(stringResource(fabricLabel(f))) })
                    }
                }
                Text(stringResource(R.string.ribbons_editor_end), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RibbonEnd.entries.forEach { e ->
                        FilterChip(styles[selected].end == e, { save(selected, choice.copy(end = e.name.takeIf { e != base.end })) }, { Text(stringResource(endLabel(e))) })
                    }
                }
                Text(stringResource(R.string.ribbons_editor_emblem), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RibbonEmblem.entries.forEach { e ->
                        FilterChip(styles[selected].emblem == e, { save(selected, choice.copy(emblem = e.name.takeIf { e != base.emblem })) }, {
                            Text(stringResource(if (e == RibbonEmblem.CROSS) R.string.ribbons_editor_cross else R.string.ribbons_editor_plain))
                        })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!choice.isEmpty) OutlinedButton(onClick = { save(selected, RibbonChoice()) }) { Text(stringResource(R.string.ribbons_editor_reset)) }
                    if (choices.any { !it.isEmpty }) OutlinedButton(onClick = { scope.launch { graph.settings.update { it.copy(ribbons = "") } } }) {
                        Text(stringResource(R.string.ribbons_editor_reset_all))
                    }
                }
            }
        }
    }
}

/** Les rubans pendus côte à côte, à la taille d'un vrai signet ; celui qu'on retouche est souligné. */
@Composable
private fun RibbonRack(styles: List<RibbonStyle>, selected: Int, onSelect: (Int) -> Unit) {
    val paper = LocalPaperColors.current
    val fabric = LocalRibbonFabric.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).background(paper.surface, RoundedCornerShape(6.dp)).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        styles.forEachIndexed { slot, style ->
            Column(
                Modifier.clickable { onSelect(slot) }.semantics { contentDescription = style.label },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Canvas(Modifier.size(width = 30.dp, height = 170.dp)) {
                    val w = 14.dp.toPx()
                    // Un ruban qui pend, à peine courbé, comme sorti de la tranche.
                    val spine = List(40) { k ->
                        val u = k / 39f
                        Offset(size.width / 2 + sin(u * 2.4f + slot) * 3.dp.toPx() * u, u * size.height)
                    }
                    drawRibbonAlong(style, paper.isDark, spine, w, ripple = 0.6f, fabric = fabric)
                }
                Text(style.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Canvas(Modifier.size(width = 24.dp, height = 3.dp)) { if (slot == selected) drawRect(paper.rubric) }
            }
        }
    }
}

/** Le nom d'une extrémité de ruban. */
fun endLabel(end: RibbonEnd) = when (end) {
    RibbonEnd.BIAS -> R.string.ribbon_end_bias
    RibbonEnd.STRAIGHT -> R.string.ribbon_end_straight
    RibbonEnd.FORKED -> R.string.ribbon_end_forked
    RibbonEnd.FERRET -> R.string.ribbon_end_ferret
}
