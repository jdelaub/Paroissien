package app.paroissien.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
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
 * Une image pieuse sortie du livre (rapport 09, §3.2) : un toucher la retourne ; au verso, la prière suggérée, le
 * crédit de l'œuvre, et de quoi la glisser dans le livre en cours ou la remettre dans la boîte.
 */
@Composable
fun CardScreen(graph: AppGraph, key: CardKey, open: (NavKey) -> Unit, onBack: () -> Unit) {
    val image = graph.images[key.id]
    var flipped by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, tween(450), label = "retournement")
    val placements by remember { graph.database.cards().observeAll() }.collectAsState(initial = emptyList())
    val positions by remember { graph.database.positions().observeAll() }.collectAsState(initial = emptyList())
    val library by graph.library.collectAsState()
    val placed = placements.firstOrNull { it.card == key.id }

    Screen(image?.title?.fr ?: "", onBack = onBack) {
        if (image == null) return@Screen
        item {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.width(280.dp).aspectRatio(0.62f)
                        .graphicsLayer { rotationY = rotation; cameraDistance = 16 * density }
                        .clickable { flipped = !flipped },
                ) {
                    if (rotation <= 90f) CardFront(graph, image) else Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) { CardBack(image) }
                }
            }
        }
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
            }
        }
    }
}

@Composable
private fun CardFront(graph: AppGraph, image: PiousImage) {
    Box(Modifier.fillMaxSize().background(CARD_PAPER).border(1.dp, CARD_GOLD).padding(10.dp).border(0.5.dp, CARD_GOLD).padding(6.dp)) {
        graph.images.picture(image.id)?.let { Image(it, contentDescription = image.work.ifEmpty { image.title.fr }, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
private fun CardBack(image: PiousImage) {
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

/** La boîte à images : toutes les cartes, celles qui sont dans un livre marquées d'un filet rouge. */
@Composable
fun ImageBoxScreen(graph: AppGraph, open: (NavKey) -> Unit, onBack: () -> Unit) {
    val placements by remember { graph.database.cards().observeAll() }.collectAsState(initial = emptyList())
    val placed = placements.map { it.card }.toSet()
    val paper = LocalPaperColors.current
    Surface(Modifier.fillMaxSize(), color = paper.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("‹") }
                Text(stringResource(R.string.images_box), style = MaterialTheme.typography.headlineSmall)
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(96.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(graph.images.all, key = { it.id }) { image ->
                    Column(Modifier.clickable { open(CardKey(image.id)) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        graph.images.thumbnail(image.id)?.let {
                            Image(
                                it, contentDescription = image.title.fr, contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().aspectRatio(0.58f).border(if (image.id in placed) 2.dp else 0.5.dp, if (image.id in placed) paper.rubric else CARD_GOLD),
                            )
                        }
                        Text(image.title.fr, style = MaterialTheme.typography.labelSmall, maxLines = 2, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
