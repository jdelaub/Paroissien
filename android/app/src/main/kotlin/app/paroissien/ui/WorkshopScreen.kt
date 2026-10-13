package app.paroissien.ui

import app.paroissien.library.isFrame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.paroissien.library.dressed
import app.paroissien.core.designsystem.BookIdentity
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.data.CustomFrameRow
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.library.CustomFrameSpec
import app.paroissien.library.CoverTemplate
import app.paroissien.library.CustomMotif
import app.paroissien.core.designsystem.LocalRibbonSet
import app.paroissien.library.FrameComposer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

/**
 * L'atelier, onglet à lui (demandes du 12 et du 13/10/2026) : les couvertures (modèles de reliure, leurs textures),
 * les rubans, les cadres composés et les motifs, et le livre d'essai du lecteur.
 */
@Composable
fun WorkshopScreen(graph: AppGraph, open: (NavKey) -> Unit, onBack: (() -> Unit)? = null) {
    val rows by remember { graph.database.looks().observeFrames() }.collectAsState(initial = emptyList())
    val frames = rows.filter { it.isFrame }.mapNotNull { r -> CustomFrameSpec.decode(r.json)?.let { s -> FrameComposer.build(s)?.let { s to it } } }
    val motifs = rows.filter { it.id.startsWith(CustomMotif.PREFIX) }.mapNotNull { CustomMotif.decode(it.json) }
    val covers = rows.filter { it.id.startsWith(CoverTemplate.PREFIX) }.mapNotNull { CoverTemplate.decode(it.json) }
    val ribbons = LocalRibbonSet.current

    Screen(stringResource(R.string.workshop_title), onBack = onBack) {
        item { Text(stringResource(R.string.workshop_long_press_hint), style = MaterialTheme.typography.bodySmall, color = LocalPaperColors.current.inkSecondary, modifier = Modifier.padding(horizontal = 20.dp)) }
        item {
            WorkshopSection(stringResource(R.string.workshop_covers)) {
                Text(stringResource(R.string.workshop_covers_hint), style = MaterialTheme.typography.bodySmall, color = LocalPaperColors.current.inkSecondary)
                Button(onClick = { open(CoverEditorKey()) }) { Text(stringResource(R.string.workshop_new_cover)) }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(covers, key = { it.id }) { c ->
                        var preview by remember { mutableStateOf(false) }
                        if (preview) CoverPreview(BookIdentity.uniform(300).dressed(c.binding), c.name, null, onClose = { preview = false })
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.previewOnLongPress({ preview = true }) { open(CoverEditorKey(c.id)) }) {
                            CoverThumb(c)
                            Text(c.name, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                        }
                    }
                }
            }
        }
        item {
            WorkshopSection(stringResource(R.string.workshop_ribbons)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ribbons.indices.forEach { RibbonIcon(it, Modifier.size(width = 12.dp, height = 30.dp)) }
                }
                OutlinedButton(onClick = { open(RibbonEditorKey) }) { Text(stringResource(R.string.workshop_edit_ribbons)) }
            }
        }
        item {
            WorkshopSection(stringResource(R.string.dress_my_frames)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { open(FrameEditorKey()) }) { Text(stringResource(R.string.dress_compose)) }
                    OutlinedButton(onClick = { open(FrameEditorKey(random = true)) }) { Text(stringResource(R.string.editor_random)) }
                }
                if (frames.isEmpty()) Text(stringResource(R.string.workshop_no_frames), style = MaterialTheme.typography.bodyMedium, color = LocalPaperColors.current.inkSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(frames, key = { it.first.id }) { (spec, def) ->
                        var preview by remember { mutableStateOf(false) }
                        if (preview) FramePreview(def, onClose = { preview = false })
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.previewOnLongPress({ preview = true }) { open(FrameEditorKey(spec.id)) }) {
                            FrameThumb(def, width = 76.dp)
                            Text(spec.name, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                        }
                    }
                }
            }
        }
        item {
            WorkshopSection(stringResource(R.string.workshop_motifs)) {
                Button(onClick = { open(MotifEditorKey()) }) { Text(stringResource(R.string.workshop_new_motif)) }
                if (motifs.isEmpty()) Text(stringResource(R.string.workshop_no_motifs), style = MaterialTheme.typography.bodyMedium, color = LocalPaperColors.current.inkSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(motifs, key = { it.id }) { m ->
                        var preview by remember { mutableStateOf(false) }
                        if (preview) FramePreview(FrameComposer.preview(m), onClose = { preview = false })
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.previewOnLongPress({ preview = true }) { open(MotifEditorKey(m.id)) }) {
                            FrameThumb(FrameComposer.preview(m), width = 76.dp)
                            Text(m.name, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                        }
                    }
                }
            }
        }
        // Le livre d'essai du lecteur (ordinaire, latin, lettrines) : il n'est pas un livre de la bibliothèque.
        item {
            WorkshopSection(stringResource(R.string.workshop_trials)) {
                OutlinedButton(onClick = { open(ReaderKey(DEMO)) }) { Text(stringResource(R.string.demo_book)) }
                Text(stringResource(R.string.demo_book_detail), style = MaterialTheme.typography.bodySmall, color = LocalPaperColors.current.inkSecondary)
            }
        }
    }
}

/** Les couleurs des cadres, en pastilles ; [selected] est cerclée. */
@Composable
internal fun Swatches(colors: List<Color>, selected: Long?, onPick: (Long) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(colors) { c ->
            val argb = c.toArgb().toLong() and 0xFFFFFFFFL
            Box(
                Modifier.size(30.dp).background(c, CircleShape)
                    .border(if (argb == selected) 3.dp else 1.dp, if (argb == selected) LocalPaperColors.current.rubric else LocalPaperColors.current.hairline, CircleShape)
                    .clickable { onPick(argb) },
            )
        }
    }
}

@Composable
internal fun WorkshopSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = LocalPaperColors.current.rubric)
        content()
    }
}

/** Douglas-Peucker : on garde les points qui s'écartent de plus de [epsilon] de la corde ; le tracé s'apaise. */
internal fun simplify(points: List<Offset>, epsilon: Float): List<Offset> {
    if (points.size < 3) return points
    fun distance(p: Offset, a: Offset, b: Offset): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = hypot(dx, dy)
        return if (len < 1e-3f) hypot(p.x - a.x, p.y - a.y) else abs(dy * p.x - dx * p.y + b.x * a.y - b.y * a.x) / len
    }
    var index = -1
    var max = 0f
    for (i in 1 until points.size - 1) {
        val d = distance(points[i], points.first(), points.last())
        if (d > max) {
            max = d
            index = i
        }
    }
    return if (max > epsilon) {
        simplify(points.subList(0, index + 1), epsilon).dropLast(1) + simplify(points.subList(index, points.size), epsilon)
    } else {
        listOf(points.first(), points.last())
    }
}
