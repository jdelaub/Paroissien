package app.paroissien.ui

import app.paroissien.library.isFrame

import app.paroissien.core.designsystem.LocalRibbonSet
import app.paroissien.core.designsystem.of

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.data.BookLookRow
import app.paroissien.core.data.CustomFrameRow
import app.paroissien.core.designsystem.BookCover
import app.paroissien.core.designsystem.CoverMaterial
import app.paroissien.core.designsystem.Metal
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.RibbonStyle
import app.paroissien.core.designsystem.frame.EndpaperPatterns
import app.paroissien.core.designsystem.frame.OrnamentalFrameDef
import app.paroissien.core.designsystem.frame.OrnamentalFrameLayer
import app.paroissien.core.designsystem.frame.OrnamentalFrames
import app.paroissien.core.liturgy.LiturgicalColor
import app.paroissien.library.BindingChoice
import app.paroissien.library.CoverTemplate
import app.paroissien.library.BookDressChoice
import app.paroissien.library.BookDresses
import app.paroissien.library.BookIdentities
import app.paroissien.library.CustomFrameSpec
import app.paroissien.library.CustomMotif
import app.paroissien.library.FrameComposer
import app.paroissien.library.FrameMode
import app.paroissien.library.LibraryEntry
import app.paroissien.library.ModuleRef
import app.paroissien.library.dressed
import app.paroissien.library.enumOrNull
import app.paroissien.library.imported
import app.paroissien.library.subtitle
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * « Habiller ce livre » (ATELIER §9) : comment le livre reçoit ses cadres (selon le sommaire, un seul cadre, un cadre
 * par chapitre, au hasard, aucun), et les cadres que l'utilisateur a composés. Chaque choix s'enregistre aussitôt.
 */
@Composable
fun BookDressScreen(graph: AppGraph, key: BookDressKey, open: (NavKey) -> Unit, onBack: () -> Unit) {
    val library by graph.library.collectAsState()
    val entry = library?.entry(key.publication)
    val row by remember(key.publication) { graph.database.looks().observe(key.publication) }.collectAsState(initial = null)
    val choice = BookDressChoice.decode(row?.json) ?: BookDressChoice()
    val customRows by remember { graph.database.looks().observeFrames() }.collectAsState(initial = emptyList())
    val custom = remember(customRows) {
        customRows.filter { it.isFrame }
            .mapNotNull { r -> CustomFrameSpec.decode(r.json)?.let { spec -> FrameComposer.build(spec)?.let { spec to it } } }
    }
    val frames = OrnamentalFrames.filter { it.name != "doublure" } + custom.map { it.second }
    var picking by remember { mutableStateOf<String?>(null) }
    var removing by remember { mutableStateOf(false) }

    fun save(next: BookDressChoice) = graph.scope.launch(Dispatchers.IO) {
        graph.database.looks().save(BookLookRow(key.publication, BookDressChoice.encode(next), System.currentTimeMillis()))
    }

    Screen(stringResource(R.string.dress_title, entry?.title ?: key.publication), onBack = onBack) {
        item {
            val templates = customRows.filter { it.id.startsWith(CoverTemplate.PREFIX) }.mapNotNull { CoverTemplate.decode(it.json) }
            BindingSection(key.publication, entry, choice.binding ?: BindingChoice(), templates) { save(choice.copy(binding = it.takeUnless { b -> b.isEmpty })) }
        }
        item {
            Section(stringResource(R.string.dress_frames)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        FrameMode.AUTO to R.string.dress_mode_auto, FrameMode.SINGLE to R.string.dress_mode_single,
                        FrameMode.PER_CHAPTER to R.string.dress_mode_chapter, FrameMode.RANDOM to R.string.dress_mode_random,
                        FrameMode.NONE to R.string.dress_mode_none,
                    ).forEach { (mode, label) ->
                        FilterChip(selected = choice.frameMode == mode, onClick = { save(choice.copy(frameMode = mode)) }, label = { Text(stringResource(label)) })
                    }
                }
            }
        }
        when (choice.frameMode) {
            FrameMode.AUTO -> item { Hint(stringResource(R.string.dress_auto_hint)) }
            FrameMode.SINGLE -> item {
                FrameGallery(frames, selected = setOfNotNull(choice.frame)) { save(choice.copy(frame = it)) }
            }
            FrameMode.PER_CHAPTER -> items(entry?.units.orEmpty().size) { k ->
                val unit = entry!!.units[k]
                val name = choice.chapterFrames[unit.id]
                val label = when (name) {
                    null -> stringResource(R.string.dress_chapter_rule)
                    "" -> stringResource(R.string.dress_chapter_none)
                    else -> frames.firstOrNull { it.name == name }?.title ?: name
                }
                Row(
                    Modifier.fillMaxWidth().clickable { picking = unit.id }.padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(unit.title ?: unit.id, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = LocalPaperColors.current.rubric)
                }
            }
            FrameMode.RANDOM -> item {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Hint(stringResource(R.string.dress_random_hint), padded = false)
                    FrameGallery(frames, selected = choice.pool.toSet(), padded = false) { name ->
                        save(choice.copy(pool = if (name in choice.pool) choice.pool - name else choice.pool + name))
                    }
                    OutlinedButton(onClick = { save(choice.copy(seed = choice.seed + 1)) }) { Text(stringResource(R.string.dress_draw_again)) }
                }
            }
            FrameMode.NONE -> Unit
        }
        if (choice.frameMode != FrameMode.NONE) item {
            Section(stringResource(R.string.dress_colors)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = choice.colors == null, onClick = { save(choice.copy(colors = null)) }, label = { Text(stringResource(R.string.dress_colors_book)) })
                    FilterChip(selected = choice.colors == "jour", onClick = { save(choice.copy(colors = "jour")) }, label = { Text(stringResource(R.string.dress_colors_day)) })
                    LiturgicalColor.entries.forEach { color ->
                        FilterChip(selected = choice.colors == color.name, onClick = { save(choice.copy(colors = color.name)) }, label = { Text(stringResource(colorLabel(color))) })
                    }
                }
                // Les ors des cadres : à plat, ou en reflet d'or, d'argent, de bronze ou de cuivre.
                Text(stringResource(R.string.dress_frame_metal), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = choice.frameMetal == null, onClick = { save(choice.copy(frameMetal = null)) }, label = { Text(stringResource(R.string.metal_flat)) })
                    METALS.forEach { (metal, label) ->
                        FilterChip(selected = choice.frameMetal == metal.name, onClick = { save(choice.copy(frameMetal = metal.name)) }, label = { Text(stringResource(label)) })
                    }
                }
                // Aperçu : un cadre de la sélection dans les couleurs et le métal choisis.
                val sample = frames.firstOrNull { it.name == choice.frame } ?: frames.firstOrNull { it.name == "matin" }
                val shown = choice.colors?.let { c -> LiturgicalColor.entries.firstOrNull { it.name == c } }
                sample?.let { FrameThumb(it, width = 90.dp, liturgicalColor = shown, metal = enumOrNull<Metal>(choice.frameMetal)) }
            }
        }
        item {
            Section(stringResource(R.string.dress_my_frames)) {
                custom.forEach { (spec, def) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FrameThumb(def, width = 54.dp)
                        Text(spec.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { open(FrameEditorKey(spec.id)) }) { Text(stringResource(R.string.dress_edit)) }
                    }
                }
                Button(onClick = { open(FrameEditorKey()) }) { Text(stringResource(R.string.dress_compose)) }
            }
        }
        // Un livre importé par l'utilisateur se retire de la bibliothèque (il reste dans les sauvegardes complètes).
        if (entry?.imported == true) item {
            TextButton(onClick = { removing = true }, modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(stringResource(R.string.book_remove), color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (removing && entry != null) {
        AlertDialog(
            onDismissRequest = { removing = false },
            text = { Text(stringResource(R.string.book_remove_confirm, entry.title)) },
            confirmButton = {
                TextButton(onClick = {
                    removing = false
                    graph.scope.launch(Dispatchers.IO) { if (graph.packs.removeBook(entry)) graph.reloadLibrary() }
                    onBack()
                }) { Text(stringResource(R.string.card_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removing = false }) { Text(stringResource(R.string.card_cancel)) } },
        )
    }

    picking?.let { unitId ->
        val unit = entry?.unit(unitId)
        AlertDialog(
            onDismissRequest = { picking = null },
            title = { Text(stringResource(R.string.dress_pick_frame, unit?.title ?: unitId)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = unitId !in choice.chapterFrames, onClick = {
                            save(choice.copy(chapterFrames = choice.chapterFrames - unitId)); picking = null
                        }, label = { Text(stringResource(R.string.dress_chapter_rule)) })
                        FilterChip(selected = choice.chapterFrames[unitId] == "", onClick = {
                            save(choice.copy(chapterFrames = choice.chapterFrames + (unitId to ""))); picking = null
                        }, label = { Text(stringResource(R.string.dress_chapter_none)) })
                    }
                    FrameGallery(frames, selected = setOfNotNull(choice.chapterFrames[unitId]), padded = false) { name ->
                        save(choice.copy(chapterFrames = choice.chapterFrames + (unitId to name))); picking = null
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picking = null }) { Text(stringResource(R.string.card_cancel)) } },
        )
    }
}

/**
 * Composer un cadre (ATELIER §9) : on assemble des modules existants (frises, coins, médaillon) pris dans les cadres de
 * la bibliothèque, on choisit la disposition, l'épaisseur, le fond et les filets ; l'aperçu suit chaque choix.
 */
@Composable
fun FrameEditorScreen(graph: AppGraph, key: FrameEditorKey, onBack: () -> Unit) {
    val rows by remember { graph.database.looks().observeFrames() }.collectAsState(initial = null)
    val existing = rows?.firstOrNull { it.id == key.id }?.let { CustomFrameSpec.decode(it.json) }
    val defaultName = stringResource(R.string.editor_default_name)
    var text by rememberSaveable(key.id, existing != null) {
        mutableStateOf(
            CustomFrameSpec.encode(
                existing ?: ("perso-" + UUID.randomUUID().toString().take(8)).let { id ->
                    if (key.random) FrameComposer.random(System.nanoTime(), id, defaultName)
                    else CustomFrameSpec(
                        id = id, name = defaultName,
                        top = ModuleRef("matin", "top"), corner = ModuleRef("matin", "corner"), ground = "matin", fillets = "cadre",
                    )
                },
            ),
        )
    }
    val spec = CustomFrameSpec.decode(text) ?: return
    fun update(next: CustomFrameSpec) { text = CustomFrameSpec.encode(next) }
    val built = remember(text) { FrameComposer.build(spec) }
    val friezes = remember { FrameComposer.modules("frise") }
    val corners = remember { FrameComposer.modules("coin") }
    val centers = remember { FrameComposer.modules("medaillon") }
    val usedFrames = listOfNotNull(spec.top, spec.side, spec.bottom, spec.corner, spec.center).map { it.frame }.distinct()

    Screen(stringResource(R.string.editor_title), onBack = onBack) {
        item {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                built?.let { FrameThumb(it, width = 200.dp) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.Center) {
                // Une composition tirée au hasard, d'un même esprit ; on la retouche ensuite.
                OutlinedButton(onClick = { update(FrameComposer.random(System.nanoTime(), spec.id, spec.name)) }) { Text(stringResource(R.string.editor_random)) }
            }
        }
        item {
            OutlinedTextField(
                value = spec.name, onValueChange = { update(spec.copy(name = it)) }, singleLine = true,
                label = { Text(stringResource(R.string.editor_name)) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
        }
        item {
            Section(stringResource(R.string.editor_colors)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = spec.palette == null, onClick = { update(spec.copy(palette = null)) }, label = { Text(stringResource(R.string.editor_colors_source)) })
                    LiturgicalColor.entries.forEach { color ->
                        FilterChip(selected = spec.palette == color.name, onClick = { update(spec.copy(palette = color.name)) }, label = { Text(stringResource(colorLabel(color))) })
                    }
                }
            }
        }
        item { ModulePicker(stringResource(R.string.editor_top), friezes, "frise", spec.top, null) { update(spec.copy(top = it ?: spec.top)) } }
        item { ModulePicker(stringResource(R.string.editor_side), friezes, "frise", spec.side, stringResource(R.string.editor_like_top)) { update(spec.copy(side = it)) } }
        item { ModulePicker(stringResource(R.string.editor_bottom), friezes, "frise", spec.bottom, stringResource(R.string.editor_like_top)) { update(spec.copy(bottom = it)) } }
        item { ModulePicker(stringResource(R.string.editor_corner), corners, "coin", spec.corner, stringResource(R.string.editor_none)) { update(spec.copy(corner = it)) } }
        item { ModulePicker(stringResource(R.string.editor_center), centers, "medaillon", spec.center, stringResource(R.string.editor_none)) { update(spec.copy(center = it, sideCenter = it)) } }
        item {
            Section(stringResource(R.string.editor_disposition)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FrameComposer.dispositions.keys.forEach { d ->
                        FilterChip(selected = spec.disposition == d, onClick = { update(spec.copy(disposition = d)) }, label = { Text(stringResource(dispositionLabel(d))) })
                    }
                }
                Text(stringResource(R.string.editor_depth, spec.depth), style = MaterialTheme.typography.bodyMedium)
                Slider(value = spec.depth.toFloat(), onValueChange = { update(spec.copy(depth = it.toInt())) }, valueRange = 12f..40f)
                Text(stringResource(R.string.editor_bottom_depth, spec.bottomDepth), style = MaterialTheme.typography.bodyMedium)
                Slider(value = spec.bottomDepth.toFloat(), onValueChange = { update(spec.copy(bottomDepth = it.toInt())) }, valueRange = 12f..56f)
            }
        }
        item {
            Section(stringResource(R.string.editor_ground)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = spec.ground == null, onClick = { update(spec.copy(ground = null)) }, label = { Text(stringResource(R.string.editor_none)) })
                    usedFrames.forEach { f ->
                        FilterChip(selected = spec.ground == f, onClick = { update(spec.copy(ground = f)) }, label = { Text(FrameComposer.title(f)) })
                    }
                }
            }
        }
        item {
            Section(stringResource(R.string.editor_fillets)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("aucun" to R.string.editor_none, "cadre" to R.string.editor_fillets_frame, "rouge" to R.string.editor_fillets_red).forEach { (v, label) ->
                        FilterChip(selected = spec.fillets == v, onClick = { update(spec.copy(fillets = v)) }, label = { Text(stringResource(label)) })
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    graph.scope.launch(Dispatchers.IO) { graph.database.looks().saveFrame(CustomFrameRow(spec.id, CustomFrameSpec.encode(spec), System.currentTimeMillis())) }
                    onBack()
                }) { Text(stringResource(R.string.editor_save)) }
                if (existing != null) {
                    TextButton(onClick = {
                        graph.scope.launch(Dispatchers.IO) { graph.database.looks().deleteFrame(spec.id) }
                        onBack()
                    }) { Text(stringResource(R.string.editor_delete), color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

/** Un module à choisir dans la bibliothèque : chacun montré sur un petit cadre ; [noneLabel] propose de n'en mettre aucun. */
@Composable
private fun ModulePicker(title: String, modules: List<ModuleRef>, kind: String, selected: ModuleRef?, noneLabel: String?, onPick: (ModuleRef?) -> Unit) {
    Section(title) {
        noneLabel?.let { FilterChip(selected = selected == null, onClick = { onPick(null) }, label = { Text(it) }) }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(modules, key = { "${it.frame}/${it.part}" }) { ref ->
                val sample = remember(ref, kind) { FrameComposer.sample(ref, kind) } ?: return@items
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(66.dp).clickable { onPick(ref) }) {
                    FrameThumb(sample, width = 60.dp, selected = selected == ref)
                    Text(FrameComposer.title(ref.frame), style = MaterialTheme.typography.labelSmall, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Les cadres en vignettes, à toucher pour choisir ; [selected] porte un filet rouge. */
@Composable
private fun FrameGallery(frames: List<OrnamentalFrameDef>, selected: Set<String>, padded: Boolean = true, onPick: (String) -> Unit) {
    LazyRow(
        Modifier.padding(horizontal = if (padded) 20.dp else 0.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(frames, key = { it.name }) { def ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp).clickable { onPick(def.name) }) {
                FrameThumb(def, width = 70.dp, selected = def.name in selected)
                Text(def.title, style = MaterialTheme.typography.labelSmall, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * Un cadre en petit : dessiné sur une page de 180 × 300 dp, réduite à [width] ; les bandes gardent ainsi leurs
 * proportions réelles sur la page.
 */
@Composable
fun FrameThumb(def: OrnamentalFrameDef, width: Dp, selected: Boolean = false, liturgicalColor: LiturgicalColor? = null, metal: Metal? = null) {
    val paper = LocalPaperColors.current
    val scale = width / 180.dp
    Box(
        Modifier.size(width, width * (300f / 180f)).clipToBounds()
            .border(if (selected) 2.dp else 0.5.dp, if (selected) paper.rubric else paper.hairline, RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.requiredSize(180.dp, 300.dp).graphicsLayer { scaleX = scale; scaleY = scale }.background(paper.background)) {
            OrnamentalFrameLayer(def.spec, def.style.withMetal(metal), liturgicalColor)
        }
    }
}

/**
 * La reliure du livre (ATELIER §9) : peau et couleur, décor, motif, tranche, garde et rubans, sur un aperçu de la
 * couverture et de la garde. Chaque retouche s'enregistre aussitôt ; « Reliure d'origine » les efface toutes.
 */
@Composable
private fun BindingSection(publication: String, entry: LibraryEntry?, binding: BindingChoice, templates: List<CoverTemplate>, onChange: (BindingChoice) -> Unit) {
    val base = remember(publication, entry) { BookIdentities.of(publication, entry, uniform = false) }
    val identity = base.dressed(binding)
    Section(stringResource(R.string.binding_title)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(identity, entry?.title ?: publication, entry?.subtitle(), Modifier.width(110.dp).aspectRatio(0.66f))
            val endpaper = BookDresses.endpaper(publication, entry, binding.endpaper)
            Box(Modifier.width(80.dp).aspectRatio(0.66f).clipToBounds().border(1.dp, LocalPaperColors.current.hairline)) {
                if (endpaper != null) endpaper() else Box(Modifier.matchParentSize().background(identity.paper.tint))
            }
        }
        // Les couvertures composées dans l'atelier : un toucher habille le livre de l'une d'elles.
        if (templates.isNotEmpty()) {
            Text(stringResource(R.string.binding_templates), style = MaterialTheme.typography.bodyMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(templates, key = { it.id }) { t ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onChange(t.binding.copy(endpaper = binding.endpaper, ribbons = binding.ribbons)) }) {
                        CoverThumb(t, width = 56.dp)
                        Text(t.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
        Text(stringResource(R.string.binding_leather), style = MaterialTheme.typography.bodyMedium)
        Swatches(BindingChoice.leathers.map { Color(it) }, selected = binding.leather ?: (base.cover.leather.toArgb().toLong() and 0xFFFFFFFFL)) {
            onChange(binding.copy(leather = it))
        }
        BindingFields(binding, identity, onChange)
        Text(stringResource(R.string.binding_endpaper), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = binding.endpaper == null, onClick = { onChange(binding.copy(endpaper = null)) }, label = { Text(stringResource(R.string.binding_book_default)) })
            FilterChip(selected = binding.endpaper == BindingChoice.SILK, onClick = { onChange(binding.copy(endpaper = BindingChoice.SILK)) }, label = { Text(stringResource(R.string.binding_silk)) })
            EndpaperPatterns.forEach { pattern ->
                FilterChip(selected = binding.endpaper == pattern.name, onClick = { onChange(binding.copy(endpaper = pattern.name)) }, label = { Text(pattern.title) })
            }
            FilterChip(selected = binding.endpaper == BindingChoice.NO_ENDPAPER, onClick = { onChange(binding.copy(endpaper = BindingChoice.NO_ENDPAPER)) }, label = { Text(stringResource(R.string.binding_none)) })
        }
        Text(stringResource(R.string.binding_ribbons), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LocalRibbonSet.current.forEachIndexed { slot, ribbon ->
                val on = slot in identity.ribbons
                FilterChip(
                    selected = on,
                    onClick = {
                        val next = if (on) identity.ribbons - slot else identity.ribbons + slot
                        // Au moins un ruban : un livre relié en a toujours un.
                        if (next.isNotEmpty()) onChange(binding.copy(ribbons = next))
                    },
                    label = { Text(ribbon.label) },
                    leadingIcon = { ColorDot(ribbon.color) },
                )
            }
        }
        if (!binding.isEmpty) OutlinedButton(onClick = { onChange(BindingChoice()) }) { Text(stringResource(R.string.binding_reset)) }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = LocalPaperColors.current.rubric)
        content()
    }
}

@Composable
private fun Hint(text: String, padded: Boolean = true) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = LocalPaperColors.current.inkSecondary, modifier = if (padded) Modifier.padding(horizontal = 20.dp) else Modifier)
}

/** Le nom d'une couleur liturgique, pour les cadres. */
fun colorLabel(color: LiturgicalColor) = when (color) {
    LiturgicalColor.GREEN -> R.string.color_green
    LiturgicalColor.VIOLET -> R.string.color_violet
    LiturgicalColor.RED -> R.string.color_red
    LiturgicalColor.ROSE -> R.string.color_rose
    LiturgicalColor.WHITE -> R.string.color_white
    LiturgicalColor.BLACK -> R.string.color_black
    LiturgicalColor.GOLD -> R.string.color_gold
}

private fun dispositionLabel(d: String) = when (d) {
    "laterale" -> R.string.disposition_laterale
    "tete" -> R.string.disposition_tete
    "tete_pied" -> R.string.disposition_tete_pied
    "equerre_tete" -> R.string.disposition_equerre_tete
    "equerre_pied" -> R.string.disposition_equerre_pied
    "ouverte" -> R.string.disposition_ouverte
    else -> R.string.disposition_pleine
}
