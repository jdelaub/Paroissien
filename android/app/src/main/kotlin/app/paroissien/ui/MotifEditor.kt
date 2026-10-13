package app.paroissien.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.data.CustomFrameRow
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.Metal
import app.paroissien.library.CustomMotif
import app.paroissien.library.FrameComposer
import app.paroissien.library.MotifGeometry
import app.paroissien.library.MotifShape
import app.paroissien.library.MotifStamp
import app.paroissien.library.MotifStamps
import app.paroissien.library.MotifStroke
import app.paroissien.library.StampGroup
import app.paroissien.library.enumOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/** Les outils de l'atelier des motifs. */
private enum class MotifTool { TRAIT, FORME, MOTIF, CHOIX }

/** L'encre courante : une couleur des cadres, ou un métal. */
private data class MotifInk(val color: Long, val metal: String? = null)

/**
 * Dessiner un motif (atelier, 12 puis 13/10/2026) : une cellule de frise où l'on trace à main levée, où l'on pose des
 * formes (ligne, losange, cercle, ovale…) et des motifs de la réserve (fleurs, fruits, feuilles, symboles), qu'on
 * déplace d'un doigt, agrandit et tourne à deux doigts ; épaisseur au curseur ; couleurs des cadres et quatre métaux.
 * La symétrie reflète tout ; l'aperçu montre la frise répétée sur un cadre. Un plein écran agrandit la cellule (en
 * paysage, les réglages passent à droite).
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
    // Les états précédents, pour « Annuler » (au plus 40), séparés par un caractère qui n'apparaît pas en JSON.
    var history by rememberSaveable(key.id) { mutableStateOf("") }
    val motif = CustomMotif.decode(text) ?: return
    fun push() {
        val list = history.split(SEP).filter { it.isNotEmpty() } + text
        history = list.takeLast(40).joinToString(SEP)
    }
    fun commit(next: CustomMotif) { push(); text = CustomMotif.encode(next) }
    fun replace(next: CustomMotif) { text = CustomMotif.encode(next) }
    fun undo() {
        val list = history.split(SEP).filter { it.isNotEmpty() }
        if (list.isEmpty()) return
        text = list.last()
        history = list.dropLast(1).joinToString(SEP)
    }

    var toolName by rememberSaveable { mutableStateOf(MotifTool.TRAIT.name) }
    var shapeName by rememberSaveable { mutableStateOf(MotifShape.LOSANGE.name) }
    var stampId by rememberSaveable { mutableStateOf("fraise") }
    var groupName by rememberSaveable { mutableStateOf(StampGroup.FLEURS.name) }
    var width by rememberSaveable { mutableStateOf(5f) }
    // L'encre de départ : l'or, lisible sur le papier et dans l'esprit des cadres.
    var inkColor by rememberSaveable { mutableStateOf(Metal.GOLD.mid.toArgb().toLong() and 0xFFFFFFFFL) }
    var inkMetal by rememberSaveable { mutableStateOf<String?>(Metal.GOLD.name) }
    var filled by rememberSaveable { mutableStateOf(true) }
    var selectedIndex by rememberSaveable { mutableStateOf(-1) }
    var full by rememberSaveable { mutableStateOf(false) }

    val tool = MotifTool.valueOf(toolName)
    val shape = enumValues<MotifShape>().firstOrNull { it.name == shapeName } ?: MotifShape.LOSANGE
    val stamp = MotifStamps.byId(stampId)
    val ink = MotifInk(inkColor, inkMetal)
    val selected = selectedIndex.takeIf { it in motif.strokes.indices }

    val state = EditorState(
        motif = motif, tool = tool, shape = shape, stamp = stamp, width = width, ink = ink, filled = filled, selected = selected,
        fan = fan,
        group = enumValues<StampGroup>().firstOrNull { it.name == groupName } ?: StampGroup.FLEURS,
        canUndo = history.isNotEmpty(),
        onTool = { toolName = it.name },
        onShape = { shapeName = it.name; toolName = MotifTool.FORME.name },
        onStamp = { stampId = it.id; toolName = MotifTool.MOTIF.name },
        onGroup = { groupName = it.name },
        onWidth = { width = it },
        onInk = { inkColor = it.color; inkMetal = it.metal },
        onFilled = { filled = it },
        onSelect = { selectedIndex = it ?: -1 },
        onAdd = { element, select ->
            commit(motif.copy(strokes = motif.strokes + element))
            if (select) {
                selectedIndex = motif.strokes.size
                toolName = MotifTool.CHOIX.name
            }
        },
        onBegin = { push() },
        onReplace = { replace(it) },
        onCommit = { commit(it) },
        onUndo = { undo() },
        onClear = { commit(motif.copy(strokes = emptyList())); selectedIndex = -1 },
    )

    BackHandler(enabled = full) { full = false }
    if (full) {
        FullScreenMotif(state, onExit = { full = false })
        return
    }

    val paper = LocalPaperColors.current
    Screen(stringResource(R.string.workshop_motif_title), onBack = onBack) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(hintOf(tool)), style = MaterialTheme.typography.bodyMedium, color = paper.inkSecondary)
                MotifCanvas(state, Modifier.fillMaxWidth().aspectRatio(motif.period / 100f))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { undo() }, enabled = state.canUndo) { Text(stringResource(R.string.workshop_undo)) }
                    TextButton(onClick = state.onClear) { Text(stringResource(R.string.workshop_clear)) }
                    OutlinedButton(onClick = { full = true }) { Text(stringResource(R.string.motif_full_screen)) }
                }
            }
        }
        item { WorkshopSection(stringResource(R.string.motif_tools)) { ToolChips(state); ToolOptions(state) } }
        item {
            WorkshopSection(stringResource(R.string.workshop_symmetry)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = motif.mirrorU, onClick = { commit(motif.copy(mirrorU = !motif.mirrorU)) }, label = { Text(stringResource(R.string.workshop_mirror_u)) })
                    FilterChip(selected = motif.mirrorV, onClick = { commit(motif.copy(mirrorV = !motif.mirrorV)) }, label = { Text(stringResource(R.string.workshop_mirror_v)) })
                }
                Text(stringResource(R.string.workshop_period, motif.period.toInt()), style = MaterialTheme.typography.bodyMedium)
                Slider(value = motif.period, onValueChange = { replace(motif.copy(period = it)) }, valueRange = 80f..260f)
            }
        }
        item {
            WorkshopSection(stringResource(R.string.editor_ground)) {
                FilterChip(selected = motif.ground == null && motif.groundMetal == null, onClick = { commit(motif.copy(ground = null, groundMetal = null)) }, label = { Text(stringResource(R.string.workshop_paper)) })
                InkSwatches(fan, MotifInk(motif.ground ?: 0L, motif.groundMetal).takeIf { motif.ground != null || motif.groundMetal != null }) {
                    commit(motif.copy(ground = if (it.metal == null) it.color else null, groundMetal = it.metal))
                }
            }
        }
        item { WorkshopSection(stringResource(R.string.workshop_preview)) { FrameThumb(FrameComposer.preview(motif), width = 120.dp) } }
        item {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = motif.name, onValueChange = { replace(motif.copy(name = it)) }, singleLine = true, label = { Text(stringResource(R.string.workshop_motif_name)) }, modifier = Modifier.fillMaxWidth())
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

private const val SEP = "\u001E"

/** Tout ce qu'il faut aux morceaux de l'éditeur, en mode normal comme en plein écran. */
private class EditorState(
    val motif: CustomMotif,
    val tool: MotifTool,
    val shape: MotifShape,
    val stamp: MotifStamp?,
    val width: Float,
    val ink: MotifInk,
    val filled: Boolean,
    val selected: Int?,
    val fan: List<Color>,
    val group: StampGroup,
    val canUndo: Boolean,
    val onTool: (MotifTool) -> Unit,
    val onShape: (MotifShape) -> Unit,
    val onStamp: (MotifStamp) -> Unit,
    val onGroup: (StampGroup) -> Unit,
    val onWidth: (Float) -> Unit,
    val onInk: (MotifInk) -> Unit,
    val onFilled: (Boolean) -> Unit,
    val onSelect: (Int?) -> Unit,
    /** Ajouter un élément ; [select] : le choisir aussitôt (outil « Choisir »), pour le placer, l'agrandir, le tourner. */
    val onAdd: (MotifStroke, select: Boolean) -> Unit,
    /** Le début d'une retouche continue (geste, curseur) : l'état d'avant entre dans l'historique. */
    val onBegin: () -> Unit,
    /** Une étape d'une retouche continue, sans historique. */
    val onReplace: (CustomMotif) -> Unit,
    val onCommit: (CustomMotif) -> Unit,
    val onUndo: () -> Unit,
    val onClear: () -> Unit,
) {
    fun element(): MotifStroke? = selected?.let { motif.strokes.getOrNull(it) }
    fun withElement(e: MotifStroke): CustomMotif = motif.copy(strokes = motif.strokes.toMutableList().also { it[selected!!] = e })
}

private fun hintOf(tool: MotifTool) = when (tool) {
    MotifTool.TRAIT -> R.string.workshop_motif_hint
    MotifTool.FORME -> R.string.motif_shape_hint
    MotifTool.MOTIF -> R.string.motif_stamp_hint
    MotifTool.CHOIX -> R.string.motif_select_hint
}

// ── Plein écran ─────────────────────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun FullScreenMotif(state: EditorState, onExit: () -> Unit) {
    val paper = LocalPaperColors.current
    // Les barres du système s'effacent ; un glissement depuis le bord les fait revenir un instant.
    val activity = LocalContext.current as? Activity
    val view = LocalView.current
    DisposableEffect(activity) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(paper.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        val landscape = maxWidth > maxHeight
        val panel = maxHeight * 0.42f
        val bar: @Composable () -> Unit = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onExit) { Text(stringResource(R.string.motif_exit_full)) }
                TextButton(onClick = state.onUndo, enabled = state.canUndo) { Text(stringResource(R.string.workshop_undo)) }
                ToolChips(state)
            }
        }
        val options: @Composable (Modifier) -> Unit = { m ->
            Column(m.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { ToolOptions(state) }
        }
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
                    bar()
                    FittedCanvas(state, Modifier.weight(1f).fillMaxWidth())
                }
                options(Modifier.width(320.dp).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                bar()
                FittedCanvas(state, Modifier.weight(1f).fillMaxWidth())
                options(Modifier.fillMaxWidth().heightIn(max = panel))
            }
        }
    }
}

/** La cellule, la plus grande possible dans la place donnée, à ses proportions. */
@Composable
private fun FittedCanvas(state: EditorState, modifier: Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val ratio = state.motif.period / 100f
        val wide = maxWidth / maxHeight > ratio
        MotifCanvas(state, if (wide) Modifier.fillMaxHeight().aspectRatio(ratio, matchHeightConstraintsFirst = true) else Modifier.fillMaxWidth().aspectRatio(ratio))
    }
}

// ── La cellule ──────────────────────────────────────────────────────────────────────────────────────────────────────

/** Un tracé prêt à peindre : son chemin (unités de la cellule), son encre (couleur ou métal), plein ou au trait. */
private class Painted(val path: Path, val color: Color, val metal: Metal?, val stroke: Float)

private fun painted(motif: CustomMotif): List<Painted> {
    val def = FrameComposer.preview(motif)
    return def.spec.top.base.mapNotNull { op ->
        val color = def.style.base[op.ink] ?: return@mapNotNull null
        Painted(PathParser().parsePathString(op.pathData).toPath(), color, def.style.metalInks[op.ink], op.stroke)
    }
}

private fun DrawScope.paint(items: List<Painted>) {
    for (p in items) {
        val brush = p.metal?.brush(Offset.Zero, Offset(36f, 36f), TileMode.Mirror) ?: SolidColor(p.color)
        if (p.stroke > 0f) drawPath(p.path, brush, style = Stroke(p.stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        else drawPath(p.path, brush)
    }
}

@Composable
private fun MotifCanvas(state: EditorState, modifier: Modifier) {
    val paper = LocalPaperColors.current
    val motif = state.motif
    val latest by rememberUpdatedState(state)
    val items = remember(motif) { painted(motif) }
    var current by remember { mutableStateOf(listOf<Offset>()) }
    var draft by remember { mutableStateOf<MotifStroke?>(null) }
    val description = stringResource(R.string.workshop_motif_title)
    Canvas(
        modifier
            .background(motif.ground?.let { Color(it) } ?: paper.background)
            .border(1.dp, paper.hairline)
            .semantics { contentDescription = description }
            .pointerInput(state.tool) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val s = latest
                    val k = size.width / s.motif.period
                    fun cell(p: Offset) = Offset(p.x / k, p.y / k)
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = down.position
                    var moved = false
                    when (s.tool) {
                        MotifTool.TRAIT -> {
                            current = listOf(start)
                            while (true) {
                                val event = awaitPointerEvent()
                                val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!ch.pressed) break
                                if ((ch.position - start).getDistance() > slop) moved = true
                                if (moved) { current = current + ch.position; ch.consume() }
                            }
                            val pts = current.map(::cell)
                            current = emptyList()
                            if (!moved) s.onAdd(MotifStroke(listOf(cell(start).x, cell(start).y), s.width, s.ink.color, dot = true, metal = s.ink.metal), false)
                            else simplify(pts, 1.2f).takeIf { it.size >= 2 }?.let { kept ->
                                s.onAdd(MotifStroke(kept.flatMap { listOf(it.x, it.y) }, s.width, s.ink.color, metal = s.ink.metal), false)
                            }
                        }
                        MotifTool.FORME -> {
                            var end = start
                            while (true) {
                                val event = awaitPointerEvent()
                                val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!ch.pressed) break
                                if ((ch.position - start).getDistance() > slop) moved = true
                                if (moved) {
                                    end = ch.position
                                    draft = shapeBetween(s, cell(start), cell(end))
                                    ch.consume()
                                }
                            }
                            draft = null
                            val e = if (moved) shapeBetween(s, cell(start), cell(end)) else shapeBetween(s, cell(start) - Offset(12f, 12f), cell(start) + Offset(12f, 12f))
                            s.onAdd(e, true)
                        }
                        MotifTool.MOTIF -> {
                            while (true) {
                                val event = awaitPointerEvent()
                                val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!ch.pressed) break
                                if ((ch.position - start).getDistance() > slop) moved = true
                            }
                            val st = s.stamp
                            if (!moved && st != null) {
                                val c = cell(start)
                                s.onAdd(MotifStroke(listOf(c.x, c.y), s.width, s.ink.color, stamp = st.id, sx = 18f, sy = 18f, filled = s.filled, metal = s.ink.metal), true)
                            }
                        }
                        MotifTool.CHOIX -> {
                            val at = cell(start)
                            // le dernier posé est dessus : on cherche du haut vers le bas, et dans les reflets de la symétrie
                            val probes = buildList {
                                add(at)
                                if (s.motif.mirrorU) add(Offset(s.motif.period - at.x, at.y))
                                if (s.motif.mirrorV) add(Offset(at.x, 100f - at.y))
                                if (s.motif.mirrorU && s.motif.mirrorV) add(Offset(s.motif.period - at.x, 100f - at.y))
                            }
                            val hit = s.motif.strokes.indices.reversed().firstOrNull { i -> probes.any { MotifGeometry.hit(s.motif.strokes[i], it, 6f) } }
                            if (hit != null && hit != s.selected) s.onSelect(hit)
                            val index = hit ?: s.selected
                            var begun = false
                            var working = s.motif
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.none { it.pressed }) break
                                val pan = event.calculatePan()
                                val zoom = event.calculateZoom()
                                val rotation = event.calculateRotation()
                                if (!moved && (event.changes.size > 1 || event.changes.any { (it.position - start).getDistance() > slop })) moved = true
                                if (moved && index != null && index in working.strokes.indices) {
                                    if (!begun) { latest.onBegin(); begun = true }
                                    val e = MotifGeometry.transform(working.strokes[index], Offset(pan.x / k, pan.y / k), zoom, rotation)
                                    working = working.copy(strokes = working.strokes.toMutableList().also { it[index] = e })
                                    latest.onReplace(working)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                            if (!moved && hit == null) s.onSelect(null)
                        }
                    }
                }
            },
    ) {
        val k = size.width / motif.period
        motif.groundMetal?.let { name -> enumOrNull<Metal>(name)?.let { drawRect(it.brush(Offset.Zero, Offset(36f * k, 36f * k), TileMode.Mirror)) } }
        // Axes de symétrie, en filets légers.
        if (motif.mirrorU) drawLine(paper.hairline, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height))
        if (motif.mirrorV) drawLine(paper.hairline, Offset(0f, size.height / 2), Offset(size.width, size.height / 2))
        scale(k, k, pivot = Offset.Zero) {
            paint(items)
            draft?.let { d -> paint(painted(motif.copy(strokes = listOf(d), mirrorU = false, mirrorV = false, ground = null, groundMetal = null))) }
            state.element()?.let { e ->
                val corners = MotifGeometry.frame(e)
                if (corners.isNotEmpty()) {
                    val path = Path().apply { moveTo(corners[0].x, corners[0].y); corners.drop(1).forEach { lineTo(it.x, it.y) }; close() }
                    drawPath(path, paper.rubric, style = Stroke(1.6f / k, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f / k, 4f / k))))
                    corners.forEach { drawCircle(paper.rubric, 3.5f / k, it) }
                }
            }
        }
        if (current.size >= 2) {
            val brush = state.ink.metal?.let { enumOrNull<Metal>(it) }?.brush(Offset.Zero, Offset(36f * k, 36f * k), TileMode.Mirror) ?: SolidColor(Color(state.ink.color))
            for (i in 1 until current.size) drawLine(brush, current[i - 1], current[i], strokeWidth = state.width * k, cap = StrokeCap.Round)
        }
    }
}

/** La forme tracée d'un coin à l'autre : une ligne d'un bout à l'autre, un cercle aussi haut que large, sinon la boîte. */
private fun shapeBetween(s: EditorState, a: Offset, b: Offset): MotifStroke {
    val c = (a + b) / 2f
    val base = MotifStroke(listOf(c.x, c.y), s.width, s.ink.color, shape = s.shape.name, filled = s.filled, metal = s.ink.metal)
    return when (s.shape) {
        MotifShape.LIGNE -> {
            val half = (hypot(b.x - a.x, b.y - a.y) / 2).coerceAtLeast(2f)
            base.copy(sx = half, sy = half / 4, rotation = Math.toDegrees(atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())).toFloat())
        }
        MotifShape.CERCLE -> {
            val r = (maxOf(abs(b.x - a.x), abs(b.y - a.y)) / 2).coerceAtLeast(2f)
            base.copy(sx = r, sy = r)
        }
        else -> base.copy(sx = (abs(b.x - a.x) / 2).coerceAtLeast(2f), sy = (abs(b.y - a.y) / 2).coerceAtLeast(2f))
    }
}

// ── Outils et réglages ─────────────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun ToolChips(state: EditorState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(MotifTool.TRAIT to R.string.motif_tool_draw, MotifTool.FORME to R.string.motif_tool_shape, MotifTool.MOTIF to R.string.motif_tool_stamp, MotifTool.CHOIX to R.string.motif_tool_select)
            .forEach { (t, label) -> FilterChip(state.tool == t, { state.onTool(t) }, { Text(stringResource(label)) }) }
    }
}

@Composable
private fun ToolOptions(state: EditorState) {
    when (state.tool) {
        MotifTool.TRAIT -> {
            WidthSlider(state.width, state.onWidth)
            InkSwatches(state.fan, state.ink, state.onInk)
        }
        MotifTool.FORME -> {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                enumValues<MotifShape>().forEach { sh ->
                    Tile(sh.title, state.shape == sh, onClick = { state.onShape(sh) }) { drawUnit(sh.figure, sh.line || !state.filled, state.ink) }
                }
            }
            FilledChips(state)
            WidthSlider(state.width, state.onWidth)
            InkSwatches(state.fan, state.ink, state.onInk)
        }
        MotifTool.MOTIF -> {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                enumValues<StampGroup>().forEach { g -> FilterChip(state.group == g, { state.onGroup(g) }, { Text(g.title) }) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MotifStamps.all.filter { it.group == state.group }.forEach { st ->
                    Tile(st.title, state.stamp?.id == st.id, onClick = { state.onStamp(st) }) { drawUnit(st.figure, st.line || !state.filled, state.ink) }
                }
            }
            FilledChips(state)
            if (!state.filled || state.stamp?.line == true) WidthSlider(state.width, state.onWidth)
            InkSwatches(state.fan, state.ink, state.onInk)
        }
        MotifTool.CHOIX -> SelectionOptions(state)
    }
}

@Composable
private fun FilledChips(state: EditorState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(state.filled, { state.onFilled(true) }, { Text(stringResource(R.string.motif_filled)) })
        FilterChip(!state.filled, { state.onFilled(false) }, { Text(stringResource(R.string.motif_outline)) })
    }
}

@Composable
private fun WidthSlider(width: Float, onChange: (Float) -> Unit, onFinished: () -> Unit = {}) {
    Text(stringResource(R.string.motif_width, "%.1f".format(width)), style = MaterialTheme.typography.bodyMedium)
    Slider(value = width, onValueChange = onChange, valueRange = 1f..24f, onValueChangeFinished = onFinished)
}

/** Les réglages de l'élément choisi : taille, étirement, rotation, épaisseur, plein ou au trait, couleur, et les gestes. */
@Composable
private fun SelectionOptions(state: EditorState) {
    val e = state.element()
    if (e == null) {
        Text(stringResource(R.string.motif_select_hint), style = MaterialTheme.typography.bodyMedium, color = LocalPaperColors.current.inkSecondary)
        return
    }
    // Une retouche au curseur n'entre qu'une fois dans l'historique.
    var editing by remember { mutableStateOf(false) }
    fun change(next: MotifStroke) {
        if (!editing) { state.onBegin(); editing = true }
        state.onReplace(state.withElement(next))
    }
    val done = { editing = false }
    val placed = e.stamp != null || e.shape != null
    val line = MotifGeometry.isLine(e)
    if (placed) {
        val ratio = if (e.sx > 0f) e.sy / e.sx else 1f
        Text(stringResource(R.string.motif_size, e.sx.toInt() * 2), style = MaterialTheme.typography.bodyMedium)
        Slider(value = e.sx.coerceIn(2f, 120f), onValueChange = { change(e.copy(sx = it, sy = it * ratio)) }, valueRange = 2f..120f, onValueChangeFinished = done)
        if (e.shape != MotifShape.CERCLE.name && e.shape != MotifShape.LIGNE.name) {
            Text(stringResource(R.string.motif_stretch), style = MaterialTheme.typography.bodyMedium)
            Slider(value = ratio.coerceIn(0.25f, 4f), onValueChange = { change(e.copy(sy = e.sx * it)) }, valueRange = 0.25f..4f, onValueChangeFinished = done)
        }
        Text(stringResource(R.string.motif_rotation, e.rotation.toInt()), style = MaterialTheme.typography.bodyMedium)
        Slider(value = e.rotation, onValueChange = { change(e.copy(rotation = it)) }, valueRange = -180f..180f, onValueChangeFinished = done)
        val canFill = (e.stamp != null && MotifStamps.byId(e.stamp)?.line != true) || (e.shape != null && enumValues<MotifShape>().firstOrNull { it.name == e.shape }?.line != true)
        if (canFill) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(e.filled, { state.onCommit(state.withElement(e.copy(filled = true))) }, { Text(stringResource(R.string.motif_filled)) })
                FilterChip(!e.filled, { state.onCommit(state.withElement(e.copy(filled = false))) }, { Text(stringResource(R.string.motif_outline)) })
            }
        }
    }
    if (line) WidthSlider(e.width, { change(e.copy(width = it)) }, done)
    // Une pastille recolore l'élément choisi.
    InkSwatches(state.fan, MotifInk(e.color, e.metal)) { picked ->
        state.onInk(picked)
        state.onCommit(state.withElement(e.copy(color = picked.color, metal = picked.metal)))
    }
    val index = state.selected!!
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = { state.onCommit(state.withElement(MotifGeometry.flipped(e))) }) { Text(stringResource(R.string.motif_flip)) }
        OutlinedButton(onClick = {
            val copy = MotifGeometry.transform(e, Offset(8f, 8f), 1f, 0f)
            state.onCommit(state.motif.copy(strokes = state.motif.strokes + copy))
            state.onSelect(state.motif.strokes.size)
        }) { Text(stringResource(R.string.motif_duplicate)) }
        OutlinedButton(onClick = {
            val list = state.motif.strokes.toMutableList().also { it.removeAt(index); it.add(e) }
            state.onCommit(state.motif.copy(strokes = list)); state.onSelect(list.size - 1)
        }) { Text(stringResource(R.string.motif_front)) }
        OutlinedButton(onClick = {
            val list = state.motif.strokes.toMutableList().also { it.removeAt(index); it.add(0, e) }
            state.onCommit(state.motif.copy(strokes = list)); state.onSelect(0)
        }) { Text(stringResource(R.string.motif_back)) }
        TextButton(onClick = {
            state.onCommit(state.motif.copy(strokes = state.motif.strokes.filterIndexed { i, _ -> i != index })); state.onSelect(null)
        }) { Text(stringResource(R.string.motif_delete), color = MaterialTheme.colorScheme.error) }
    }
}

/** Une case de la réserve : la forme ou le motif dessiné en petit, dans l'encre courante. */
@Composable
private fun Tile(title: String, selected: Boolean, onClick: () -> Unit, draw: DrawScope.() -> Unit) {
    val paper = LocalPaperColors.current
    Box(
        Modifier.size(52.dp)
            .border(if (selected) 2.dp else 1.dp, if (selected) paper.rubric else paper.hairline, RoundedCornerShape(6.dp))
            .clickable(onClickLabel = title, onClick = onClick)
            .semantics { contentDescription = title }
            .padding(7.dp),
    ) { Canvas(Modifier.fillMaxSize(), onDraw = draw) }
}

/** Dessiner une figure de la boîte unité (−0,5 à 0,5) dans toute la case. */
private fun DrawScope.drawUnit(figure: app.paroissien.library.Figure, line: Boolean, ink: MotifInk) {
    val s = minOf(size.width, size.height)
    val path = PathParser().parsePathString(figure.normalized().svg()).toPath()
    val brush = ink.metal?.let { enumOrNull<Metal>(it) }?.brush(Offset(-0.5f, -0.5f), Offset(0.5f, 0.5f)) ?: SolidColor(Color(ink.color))
    translate(size.width / 2, size.height / 2) {
        scale(s, s, pivot = Offset.Zero) {
            if (line) drawPath(path, brush, style = Stroke(0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round)) else drawPath(path, brush)
        }
    }
}

/** Les encres : les quatre métaux (en reflet), puis les couleurs des cadres. */
@Composable
private fun InkSwatches(fan: List<Color>, selected: MotifInk?, onPick: (MotifInk) -> Unit) {
    val paper = LocalPaperColors.current
    val metals = listOf(Metal.GOLD to R.string.metal_gold, Metal.SILVER to R.string.metal_silver, Metal.BRONZE to R.string.metal_bronze, Metal.COPPER to R.string.metal_copper)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(metals) { (metal, label) ->
            val on = selected?.metal == metal.name
            val name = stringResource(label)
            Box(
                Modifier.size(30.dp)
                    .background(Brush.linearGradient(listOf(metal.deep, metal.bright, metal.mid, metal.deep)), CircleShape)
                    .border(if (on) 3.dp else 1.dp, if (on) paper.rubric else paper.hairline, CircleShape)
                    .clickable(onClickLabel = name) { onPick(MotifInk(metal.mid.toArgb().toLong() and 0xFFFFFFFFL, metal.name)) }
                    .semantics { contentDescription = name },
            )
        }
        items(fan) { c ->
            val argb = c.toArgb().toLong() and 0xFFFFFFFFL
            val on = selected?.metal == null && selected?.color == argb
            Box(
                Modifier.size(30.dp).background(c, CircleShape)
                    .border(if (on) 3.dp else 1.dp, if (on) paper.rubric else paper.hairline, CircleShape)
                    .clickable { onPick(MotifInk(argb, null)) },
            )
        }
    }
}
