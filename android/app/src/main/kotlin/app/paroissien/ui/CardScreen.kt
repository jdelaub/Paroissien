package app.paroissien.ui

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import app.paroissien.library.CardCategoryInfo
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AssistChip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.data.CardPlacement
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.library.PiousImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Papier crème et filet doré du cadre : la carte reste une carte, quel que soit le thème du livre. */
private val CARD_PAPER = Color(0xFFF7F0DD)
private val CARD_GOLD = Color(0xFFB8912F)
private val CARD_INK = Color(0xFF2B2416)
private val CARD_RUBRIC = Color(0xFFA3171C)

/**
 * Une image pieuse sortie du livre (rapport 09, §3.2) : un toucher la retourne. Au verso, la photo du dos pour une
 * image numérisée, sinon la prière suggérée et le crédit. De quoi la glisser dans le livre en cours, la remettre dans
 * la boîte, et pour une image personnelle, la renommer, numériser son verso ou la supprimer.
 */
@Composable
fun CardScreen(graph: AppGraph, key: CardKey, open: (NavKey) -> Unit, onBack: () -> Unit) {
    val images by graph.images.all.collectAsState()
    val image = images.firstOrNull { it.id == key.id }
    var flipped by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, tween(450), label = "retournement")
    val placements by remember { graph.database.cards().observeAll() }.collectAsState(initial = emptyList())
    val positions by remember { graph.database.positions().observeAll() }.collectAsState(initial = emptyList())
    val library by graph.library.collectAsState()
    val placed = placements.firstOrNull { it.card == key.id }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    val categories by graph.images.categories.collectAsState()
    val categoriesOf by graph.images.categoriesOf.collectAsState()

    Screen(image?.title?.fr ?: "", onBack = onBack) {
        if (image == null) return@Screen
        item {
            // Une vraie carte garde ses proportions ; une image de la bibliothèque d'art, celles du cadre.
            val ratio = if (image.physical) graph.images.thumbnail(image.id)?.let { it.width / it.height.toFloat() } ?: 0.62f else 0.62f
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.width(if (ratio > 1f) 320.dp else 280.dp).aspectRatio(ratio)
                        .graphicsLayer { rotationY = rotation; cameraDistance = 16 * density }
                        .clickable { flipped = !flipped },
                ) {
                    if (rotation <= 90f) CardFront(graph, image) else Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) { CardBack(graph, image) }
                }
            }
        }
        item {
            CardCategoriesRow(
                categories, categoriesOf[image.id].orEmpty(),
                onToggle = { id, member -> graph.scope.launch { graph.images.setCategory(image.id, id, member) } },
                onCreate = { creating = true },
            )
        }
        item { CardDetails(image) }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.card_flip_hint), style = MaterialTheme.typography.bodySmall, color = LocalPaperColors.current.inkSecondary)
                placed?.let { p ->
                    val book = library?.entry(p.publication)
                    Text(stringResource(R.string.card_placed_in, book?.title ?: p.publication, book?.unitTitle(p.unit) ?: p.unit), style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { open(ReaderKey(p.publication, p.unit, p.block, p.charOffset)) }) { Text(stringResource(R.string.card_open_page)) }
                    OutlinedButton(onClick = { graph.scope.launch(Dispatchers.IO) { graph.database.cards().remove(key.id) } }) { Text(stringResource(R.string.card_to_box)) }
                }
                // Glisser dans le livre en cours, à la page où l'on s'est arrêté.
                positions.firstOrNull()?.let { pos ->
                    val book = library?.entry(pos.publication) ?: return@let
                    if (placed?.publication == pos.publication && placed.block == pos.block) return@let
                    OutlinedButton(onClick = {
                        graph.scope.launch(Dispatchers.IO) {
                            graph.database.cards().place(CardPlacement(key.id, pos.publication, pos.unit, pos.block, pos.charOffset, null, System.currentTimeMillis()))
                        }
                    }) { Text(stringResource(R.string.card_slip_into, book.title)) }
                }
                if (image.personal) {
                    if (!image.hasBack) OutlinedButton(onClick = { open(ScanKey(backOf = image.id)) }) { Text(stringResource(R.string.card_scan_back)) }
                    OutlinedButton(onClick = { renaming = true }) { Text(stringResource(R.string.card_rename)) }
                    TextButton(onClick = { deleting = true }) { Text(stringResource(R.string.card_delete), color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (renaming && image != null) {
        var name by remember { mutableStateOf(image.title.fr) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.scan_name)) }, singleLine = true) },
            confirmButton = { TextButton(onClick = { renaming = false; graph.scope.launch { graph.images.rename(image.id, name.trim().ifEmpty { image.title.fr }) } }) { Text(stringResource(R.string.card_ok)) } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(stringResource(R.string.card_cancel)) } },
        )
    }
    if (creating && image != null) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { creating = false },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.category_name)) }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    creating = false
                    if (name.isNotBlank()) graph.scope.launch { graph.images.setCategory(image.id, graph.images.createCategory(name), true) }
                }) { Text(stringResource(R.string.card_ok)) }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text(stringResource(R.string.card_cancel)) } },
        )
    }
    if (deleting && image != null) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            text = { Text(stringResource(R.string.card_delete_confirm, image.title.fr)) },
            confirmButton = { TextButton(onClick = { deleting = false; graph.scope.launch { graph.images.delete(image.id) }; onBack() }) { Text(stringResource(R.string.card_delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.card_cancel)) } },
        )
    }
}

/**
 * Coins d'une vraie carte : arrondis, comme le sont presque toutes les images pieuses imprimées. Une carte détourée
 * (sa silhouette en transparence) garde en plus ses vrais bords, dentelle comprise.
 */
val CardShape = RoundedCornerShape(percent = 4)

@Composable
private fun CardFront(graph: AppGraph, image: PiousImage) {
    if (image.physical) {
        graph.images.picture(image.id)?.let {
            Image(it, contentDescription = image.title.fr, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().shadow(6.dp, CardShape).clip(CardShape))
        }
        return
    }
    Box(Modifier.fillMaxSize().background(CARD_PAPER).border(1.dp, CARD_GOLD).padding(10.dp).border(0.5.dp, CARD_GOLD).padding(6.dp)) {
        // Une image numérisée garde ses proportions : rien de la carte n'est rogné.
        graph.images.picture(image.id)?.let { Image(it, contentDescription = image.work.ifEmpty { image.title.fr }, contentScale = if (image.personal) ContentScale.Fit else ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
private fun CardBack(graph: AppGraph, image: PiousImage) {
    val back = if (image.hasBack) graph.images.back(image.id) else null
    if (back != null && image.physical) {
        Image(back, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().shadow(6.dp, CardShape).clip(CardShape))
        return
    }
    if (back != null) {
        Box(Modifier.fillMaxSize().background(CARD_PAPER).border(1.dp, CARD_GOLD).padding(6.dp)) {
            Image(back, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
        return
    }
    Column(
        Modifier.fillMaxSize().background(CARD_PAPER).border(1.dp, CARD_GOLD).padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("✠", color = CARD_RUBRIC, style = MaterialTheme.typography.headlineSmall)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(image.title.fr, color = CARD_INK, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            image.prayer?.let { Text(it, color = CARD_RUBRIC, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center) }
        }
        Text(image.credit, color = CARD_INK.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

/**
 * La boîte à images : celles qu'on a numérisées et celles de la bibliothèque, rangées par catégories (prédéfinies ou
 * créées) ; filet rouge pour celles d'un livre.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageBoxScreen(graph: AppGraph, open: (NavKey) -> Unit, onBack: () -> Unit) {
    val all by graph.images.all.collectAsState()
    val placements by remember { graph.database.cards().observeAll() }.collectAsState(initial = emptyList())
    val placed = placements.map { it.card }.toSet()
    val paper = LocalPaperColors.current
    val categories by graph.images.categories.collectAsState()
    val categoriesOf by graph.images.categoriesOf.collectAsState()
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var removing by remember { mutableStateOf<CardCategoryInfo?>(null) }
    // Appui long sur une image : on entre en sélection ; un toucher en ajoute ou en ôte ; « Retirer » les ôte de la boîte.
    var selection by remember { mutableStateOf(setOf<String>()) }
    var confirming by remember { mutableStateOf(false) }
    val hidden by graph.images.hidden.collectAsState()
    var showHidden by rememberSaveable { mutableStateOf(false) }
    fun toggle(id: String) { selection = if (id in selection) selection - id else selection + id }
    // Seules les catégories qui ont des images, plus celles de l'utilisateur (même vides).
    val used = categoriesOf.values.flatten().toSet()
    val shown = categories.filter { it.id in used || it.custom }
    val images = filter?.let { f -> all.filter { f in categoriesOf[it.id].orEmpty() } } ?: all
    Surface(Modifier.fillMaxSize(), color = paper.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (selection.isEmpty()) {
                    TextButton(onClick = onBack) { Text("‹") }
                    Text(stringResource(R.string.images_box), style = MaterialTheme.typography.headlineSmall)
                } else {
                    TextButton(onClick = { selection = emptySet() }) { Text("✕") }
                    Text(pluralStringResource(R.plurals.images_selected, selection.size, selection.size), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { confirming = true }) { Text(stringResource(R.string.images_remove), color = MaterialTheme.colorScheme.error) }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(96.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { open(ScanKey()) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.images_scan)) }
                        if (all.isEmpty()) Text(stringResource(R.string.images_empty), style = MaterialTheme.typography.bodyMedium, color = paper.inkSecondary)
                        if (shown.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text(stringResource(R.string.images_all)) })
                                shown.forEach { c ->
                                    // Appui long sur une catégorie de l'utilisateur : la supprimer.
                                    Box(Modifier.combinedClickable(onClick = { filter = c.id.takeIf { it != filter } }, onLongClick = { if (c.custom) removing = c })) {
                                        FilterChip(selected = filter == c.id, onClick = { filter = c.id.takeIf { it != filter } }, label = { Text(c.label) })
                                    }
                                }
                            }
                        }
                    }
                }
                items(images, key = { it.id }) { image ->
                    val chosen = image.id in selection
                    Column(
                        Modifier
                            .combinedClickable(
                                onLongClickLabel = stringResource(R.string.images_select),
                                onLongClick = { toggle(image.id) },
                            ) { if (selection.isNotEmpty()) toggle(image.id) else open(CardKey(image.id)) }
                            .then(if (chosen) Modifier.background(paper.rubric.copy(alpha = 0.14f), RoundedCornerShape(6.dp)).border(2.dp, paper.rubric, RoundedCornerShape(6.dp)) else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        graph.images.thumbnail(image.id)?.let {
                            if (image.physical) {
                                // Une vraie carte : ses proportions, ses coins, un filet rouge si elle est dans un livre.
                                Box(Modifier.fillMaxWidth().aspectRatio(0.62f), contentAlignment = Alignment.Center) {
                                    Image(
                                        it, contentDescription = image.title.fr, contentScale = ContentScale.Fit,
                                        modifier = Modifier.aspectRatio(it.width / it.height.toFloat()).shadow(2.dp, CardShape).clip(CardShape)
                                            .then(if (image.id in placed) Modifier.border(2.dp, paper.rubric, CardShape) else Modifier),
                                    )
                                }
                            } else {
                                Image(
                                    it, contentDescription = image.title.fr, contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(0.62f).border(if (image.id in placed) 2.dp else 0.5.dp, if (image.id in placed) paper.rubric else CARD_GOLD),
                                )
                            }
                        }
                        Text(image.title.fr, style = MaterialTheme.typography.labelSmall, maxLines = 2, textAlign = TextAlign.Center)
                    }
                }
                // Les images de la bibliothèque qu'on a retirées : un toucher les rend à la boîte.
                if (hidden.isNotEmpty() && selection.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        TextButton(onClick = { showHidden = !showHidden }) {
                            Text(pluralStringResource(R.plurals.images_hidden, hidden.size, hidden.size) + if (showHidden) " ▴" else " ▾")
                        }
                    }
                    if (showHidden) {
                        items(hidden, key = { "masquee:" + it.id }) { image ->
                            Column(Modifier.clickable { graph.scope.launch { graph.images.restore(image.id) } }, horizontalAlignment = Alignment.CenterHorizontally) {
                                graph.images.thumbnail(image.id)?.let {
                                    Image(it, contentDescription = image.title.fr, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().aspectRatio(0.62f).graphicsLayer { alpha = 0.45f })
                                }
                                Text(stringResource(R.string.images_restore), style = MaterialTheme.typography.labelSmall, color = paper.rubric)
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirming) {
        val count = selection.size
        AlertDialog(
            onDismissRequest = { confirming = false },
            text = { Text(pluralStringResource(R.plurals.images_remove_confirm, count, count)) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    val ids = selection
                    selection = emptySet()
                    graph.scope.launch { graph.images.remove(ids) }
                }) { Text(stringResource(R.string.images_remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.card_cancel)) } },
        )
    }
    removing?.let { c ->
        AlertDialog(
            onDismissRequest = { removing = null },
            text = { Text(stringResource(R.string.category_delete_confirm, c.label)) },
            confirmButton = {
                TextButton(onClick = {
                    removing = null
                    if (filter == c.id) filter = null
                    graph.scope.launch { graph.images.deleteCategory(c.id) }
                }) { Text(stringResource(R.string.card_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.card_cancel)) } },
        )
    }
}

/** Les catégories d'une carte, à cocher ; « Nouvelle catégorie… » en crée une et y range la carte. */
@Composable
private fun CardCategoriesRow(all: List<CardCategoryInfo>, member: Set<String>, onToggle: (String, Boolean) -> Unit, onCreate: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.card_categories), style = MaterialTheme.typography.labelLarge, color = LocalPaperColors.current.rubric)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            all.forEach { c -> FilterChip(selected = c.id in member, onClick = { onToggle(c.id, c.id !in member) }, label = { Text(c.label) }) }
            AssistChip(onClick = onCreate, label = { Text(stringResource(R.string.category_new)) })
        }
    }
}

/** Ce que dit la carte : date, personnes, lieu, dédicace manuscrite, texte imprimé, éditeur, taille. */
@Composable
private fun CardDetails(image: PiousImage) {
    val paper = LocalPaperColors.current
    val facts = listOfNotNull(
        image.date.takeIf { it.isNotBlank() },
        image.people.joinToString(", ").takeIf { it.isNotBlank() },
        image.place.takeIf { it.isNotBlank() },
        listOf(image.work, image.artist).filter { it.isNotBlank() }.joinToString(", ").takeIf { it.isNotBlank() },
    )
    if (facts.isEmpty() && image.inscription.isBlank() && image.prayer.isNullOrBlank() && image.credit.isBlank() && image.sizeMm == null) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        facts.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        if (image.inscription.isNotBlank()) Text("« ${image.inscription} »", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
        image.prayer?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = paper.inkSecondary) }
        val small = listOfNotNull(
            image.credit.takeIf { it.isNotBlank() },
            image.sizeMm?.let { stringResource(R.string.card_size, it[0] / 10f, it[1] / 10f) },
        )
        small.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = paper.inkSecondary) }
    }
}

