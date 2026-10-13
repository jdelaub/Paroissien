package app.paroissien.core.reading.book

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.paroissien.core.designsystem.PaperColors

/**
 * Une image pieuse glissée dans le livre (rapport 09, §3.2.1) : [anchor] la situe dans l'unité ouverte, sinon
 * [elsewhere] dit si elle est avant (-1) ou après (+1). Sur sa page, elle est posée sur le texte ([picture], l'image
 * entière, si on l'a déjà chargée) ; ailleurs, on n'en voit que le haut, qui dépasse par la tête du livre.
 */
data class CardMark(
    val id: String,
    val anchor: Anchor?,
    val elsewhere: Int = 0,
    val image: ImageBitmap?,
    val label: String,
    val picture: ImageBitmap? = null,
    /** Le verso, s'il y en a un : la carte posée sur la page se retourne d'un toucher. */
    val back: ImageBitmap? = null,
)

/** Le haut d'une carte : son filet doré, l'image, une inclinaison fixe de ± 2° tirée de son identifiant. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CardTop(card: CardMark, paper: PaperColors, wide: Boolean, onTap: (String) -> Unit, onLongTap: (String) -> Unit) {
    val tilt = (card.id.hashCode().mod(5) - 2).toFloat()
    // Le petit haut de carte ne fait que 14 à 18 dp ; on le touche dans une zone de 48 dp, qui ne laisse rien au livre.
    Box(
        Modifier
            .size(width = if (wide) 64.dp else 48.dp, height = 48.dp)
            .combinedClickable(onLongClickLabel = "Ouvrir l'image ${card.label}", onLongClick = { onLongTap(card.id) }) { onTap(card.id) }
            .semantics { contentDescription = "Image pieuse : ${card.label}" },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .size(width = if (wide) 64.dp else 40.dp, height = if (wide) 18.dp else 14.dp)
                .rotate(tilt)
                .background(Color(0xFFF7F0DD))
                .border(1.dp, Color(0xFFB8912F))
                .padding(2.dp)
                .clipToBounds(),
        ) {
            card.image?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize()) }
                ?: Box(Modifier.fillMaxSize().background(paper.hairline))
        }
    }
}

/** Les cartes d'une rangée, en haut de la page, sous la zone des gestes système. */
@Composable
internal fun CardTops(cards: List<CardMark>, paper: PaperColors, wide: Boolean, onTap: (String) -> Unit, onLongTap: (String) -> Unit, modifier: Modifier) {
    if (cards.isEmpty()) return
    Row(modifier.windowInsetsPadding(WindowInsets.systemGestures.only(WindowInsetsSides.Top)), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Au-delà de quatre, l'épaisseur suffit : on ne dessine pas tout.
        cards.take(4).forEach { CardTop(it, paper, wide, onTap, onLongTap) }
    }
}
