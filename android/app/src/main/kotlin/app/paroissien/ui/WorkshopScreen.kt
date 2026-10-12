package app.paroissien.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.data.CustomFrameRow
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.library.CustomFrameSpec
import app.paroissien.library.CustomMotif
import app.paroissien.library.FrameComposer
import app.paroissien.library.MotifStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot

/**
 * L'atelier, sans passer par un livre (demande de l'utilisateur, 12/10/2026) : ses cadres composés et ses motifs,
 * composer un cadre (à la main ou au hasard), dessiner un motif. Ouvert depuis les réglages et depuis la bibliothèque.
 */
@Composable
fun WorkshopScreen(graph: AppGraph, open: (NavKey) -> Unit, onBack: () -> Unit) {
    val rows by remember { graph.database.looks().observeFrames() }.collectAsState(initial = emptyList())
    val frames = rows.filterNot { it.id.startsWith(CustomMotif.PREFIX) }.mapNotNull { r -> CustomFrameSpec.decode(r.json)?.let { s -> FrameComposer.build(s)?.let { s to it } } }
    val motifs = rows.filter { it.id.startsWith(CustomMotif.PREFIX) }.mapNotNull { CustomMotif.decode(it.json) }

    Screen(stringResource(R.string.workshop_title), onBack = onBack) {
        item {
            WorkshopSection(stringResource(R.string.dress_my_frames)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { open(FrameEditorKey()) }) { Text(stringResource(R.string.dress_compose)) }
                    OutlinedButton(onClick = { open(FrameEditorKey(random = true)) }) { Text(stringResource(R.string.editor_random)) }
                }
                if (frames.isEmpty()) Text(stringResource(R.string.workshop_no_frames), style = MaterialTheme.typography.bodyMedium, color = LocalPaperColors.current.inkSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(frames, key = { it.first.id }) { (spec, def) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { open(FrameEditorKey(spec.id)) }) {
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { open(MotifEditorKey(m.id)) }) {
                            FrameThumb(FrameComposer.preview(m), width = 76.dp)
                            Text(m.name, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dessiner un motif : une cellule de frise, où chaque trait du doigt est simplifié puis lissé en courbe ; la symétrie
 * le reflète, l'épaisseur et les couleurs viennent des cadres déjà là ; l'aperçu montre la frise répétée sur un cadre.
 */
@Composable
fun MotifEditorScreen(graph: AppGraph, key: MotifEditorKey, onBack: () -> Unit) {
    val rows by remember { graph.database.looks().observeFrames() }.collectAsState(initial = null)
    val existing = rows?.firstOrNull { it.id == key.id }?.let { CustomMotif.decode(it.json) }
    val defaultName = stringResource(R.string.workshop_motif_default)
    val fan = remember { FrameComposer.fan }
    var text by rememberSaveable(key.id, existing != null) {
        mutableStateOf(CustomMotif.encode(existing ?: CustomMotif(CustomMotif.PREFIX + UUID.randomUUID().toString().take(8), defaultName)))
    }
    val motif = CustomMotif.decode(text) ?: return
    fun update(next: CustomMotif) { text = CustomMotif.encode(next) }
    var width by rememberSaveable { mutableStateOf(6f) }
    var color by rememberSaveable { mutableStateOf((fan.firstOrNull() ?: Color(0xFF7A4B2A)).toArgb().toLong() and 0xFFFFFFFFL) }
    var current by remember { mutableStateOf(listOf<Offset>()) }
    val latest by rememberUpdatedState(motif)
    val paper = LocalPaperColors.current

    Screen(stringResource(R.string.workshop_motif_title), onBack = onBack) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.workshop_motif_hint), style = MaterialTheme.typography.bodyMedium, color = paper.inkSecondary)
                // La cellule : x de 0 à la période, y de 0 (bord extérieur) à 100 (côté texte).
                Canvas(
                    Modifier.fillMaxWidth().aspectRatio(motif.period / 100f)
                        .background(motif.ground?.let { Color(it) } ?: paper.background)
                        .border(1.dp, paper.hairline)
                        .pointerInput(Unit) {
                            detectTapGestures { at ->
                                val u = at.x / size.width * latest.period
                                val v = at.y / size.height * 100f
                                update(latest.copy(strokes = latest.strokes + MotifStroke(listOf(u, v), width, color, dot = true)))
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { current = listOf(it) },
                                onDrag = { change, _ -> current = current + change.position; change.consume() },
                                onDragEnd = {
                                    val pts = current.map { Offset(it.x / size.width * latest.period, it.y / size.height * 100f) }
                                    val kept = simplify(pts, 1.4f)
                                    if (kept.size >= 2) update(latest.copy(strokes = latest.strokes + MotifStroke(kept.flatMap { listOf(it.x, it.y) }, width, color)))
                                    current = emptyList()
                                },
                                onDragCancel = { current = emptyList() },
                            )
                        },
                ) {
                    val k = size.width / motif.period
                    // Axes de symétrie, en pointillés légers.
                    if (motif.mirrorU) drawLine(paper.hairline, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height))
                    if (motif.mirrorV) drawLine(paper.hairline, Offset(0f, size.height / 2), Offset(size.width, size.height / 2))
                    scale(k, k, pivot = Offset.Zero) {
                        val def = FrameComposer.preview(motif)
                        for (op in def.spec.top.base) {
                            val path = PathParser().parsePathString(op.pathData).toPath()
                            val c = def.style.base[op.ink] ?: continue
                            drawPath(path, c, style = Stroke(op.stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                    if (current.size >= 2) {
                        for (i in 1 until current.size) drawLine(Color(color), current[i - 1], current[i], strokeWidth = width * k, cap = StrokeCap.Round)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { if (motif.strokes.isNotEmpty()) update(motif.copy(strokes = motif.strokes.dropLast(1))) }) { Text(stringResource(R.string.workshop_undo)) }
                    TextButton(onClick = { update(motif.copy(strokes = emptyList())) }) { Text(stringResource(R.string.workshop_clear)) }
                }
            }
        }
        item {
            WorkshopSection(stringResource(R.string.workshop_symmetry)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = motif.mirrorU, onClick = { update(motif.copy(mirrorU = !motif.mirrorU)) }, label = { Text(stringResource(R.string.workshop_mirror_u)) })
                    FilterChip(selected = motif.mirrorV, onClick = { update(motif.copy(mirrorV = !motif.mirrorV)) }, label = { Text(stringResource(R.string.workshop_mirror_v)) })
                }
                Text(stringResource(R.string.workshop_period, motif.period.toInt()), style = MaterialTheme.typography.bodyMedium)
                Slider(value = motif.period, onValueChange = { update(motif.copy(period = it)) }, valueRange = 80f..260f)
            }
        }
        item {
            WorkshopSection(stringResource(R.string.workshop_stroke)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3f to R.string.workshop_thin, 6f to R.string.workshop_medium, 10f to R.string.workshop_thick).forEach { (w, label) ->
                        FilterChip(selected = width == w, onClick = { width = w }, label = { Text(stringResource(label)) })
                    }
                }
                Swatches(fan, selected = color) { color = it }
            }
        }
        item {
            WorkshopSection(stringResource(R.string.editor_ground)) {
                FilterChip(selected = motif.ground == null, onClick = { update(motif.copy(ground = null)) }, label = { Text(stringResource(R.string.workshop_paper)) })
                Swatches(fan, selected = motif.ground) { update(motif.copy(ground = it)) }
            }
        }
        item {
            WorkshopSection(stringResource(R.string.workshop_preview)) {
                FrameThumb(FrameComposer.preview(motif), width = 120.dp)
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = motif.name, onValueChange = { update(motif.copy(name = it)) }, singleLine = true, label = { Text(stringResource(R.string.workshop_motif_name)) }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        graph.scope.launch(Dispatchers.IO) { graph.database.looks().saveFrame(CustomFrameRow(motif.id, CustomMotif.encode(motif), System.currentTimeMillis())) }
                        onBack()
                    }, enabled = motif.strokes.isNotEmpty()) { Text(stringResource(R.string.editor_save)) }
                    if (existing != null) {
                        TextButton(onClick = {
                            graph.scope.launch(Dispatchers.IO) { graph.database.looks().deleteFrame(motif.id) }
                            onBack()
                        }) { Text(stringResource(R.string.editor_delete), color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

/** Les couleurs des cadres, en pastilles ; [selected] est cerclée. */
@Composable
private fun Swatches(colors: List<Color>, selected: Long?, onPick: (Long) -> Unit) {
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
private fun WorkshopSection(title: String, content: @Composable () -> Unit) {
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
