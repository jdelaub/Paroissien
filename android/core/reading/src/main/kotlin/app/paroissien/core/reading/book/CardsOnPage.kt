package app.paroissien.core.reading.book

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt

/** Une page ouverte, vue du lecteur : son rectangle à l'écran, le coin haut gauche de son texte, ses images. */
internal class PageSlot(val page: Rect, val textLeft: Float, val textTop: Float, val cards: List<CardMark>)

/** Coins d'une image pieuse : arrondis, comme une vraie carte. */
private val CardCorners = RoundedCornerShape(percent = 4)

private const val PHI = 1.618034f

/**
 * Les images pieuses de la page ouverte, posées au-dessus du livre et non dans la feuille qui se tourne : leurs gestes
 * ne vont qu'à elles, jamais au feuilletage ni à la barre (demande de l'utilisateur, 12/10/2026).
 *
 * - Posée en haut à gauche du texte, là où on la retrouve dans un vrai livre, à la taille du nombre d'or (la page vaut
 *   φ fois l'image) ; les suivantes un peu décalées, en pile.
 * - Un toucher la retourne : son verso (la photo du dos, sinon son titre).
 * - Un glissé, dans n'importe quel sens, la range en miniature dans la marge de tête, en haut à gauche ; un toucher sur
 *   la miniature la repose sur la page.
 * - Un appui long l'ouvre en grand.
 * - Pendant qu'une page tourne, elles s'effacent ; elles reviennent sur leur page.
 */
@Composable
internal fun CardsOverlay(
    slots: List<PageSlot>,
    isAside: (CardMark) -> Boolean,
    turning: Boolean,
    topInset: Float,
    onAside: (String) -> Unit,
    onBack: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    val density = LocalDensity.current
    val shown by animateFloatAsState(if (turning) 0f else 1f, tween(if (turning) 120 else 320), label = "images")
    for (slot in slots) {
        slot.cards.forEachIndexed { k, card ->
            key(card.id) {
                val full = with(density) { fullRect(card.ratio(), k, slot) }
                val mini = with(density) { miniRect(card.ratio(), k, slot, topInset) }
                CardOverlay(card, full, mini, isAside(card), shown, onAside, onBack, onOpen)
            }
        }
    }
}

@Composable
private fun CardOverlay(
    card: CardMark,
    full: Rect,
    mini: Rect,
    aside: Boolean,
    shown: Float,
    onAside: (String) -> Unit,
    onBack: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var flipped by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    val tucked by animateFloatAsState(if (aside) 1f else 0f, tween(420, easing = FastOutSlowInEasing), label = "rangée")
    val turn by animateFloatAsState(if (flipped && !aside) 180f else 0f, tween(450), label = "retournée")
    val rect = Rect(
        lerp(full.left, mini.left, tucked), lerp(full.top, mini.top, tucked),
        lerp(full.right, mini.right, tucked), lerp(full.bottom, mini.bottom, tucked),
    )
    val tilt = (card.id.hashCode().mod(5) - 2) * 0.7f
    val current by rememberUpdatedState(aside)
    val slop = with(density) { 10.dp.toPx() }
    val away = with(density) { 48.dp.toPx() }

    Box(
        Modifier
            .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
            .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
            // Le geste entier appartient à l'image : rien ne passe au livre dessous.
            .pointerInput(card.id) {
                val longPress = viewConfiguration.longPressTimeoutMillis
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var moved = Offset.Zero
                    var released = false
                    val pressed = withTimeoutOrNull(longPress) {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull false
                            change.consume()
                            if (!change.pressed) {
                                released = true
                                return@withTimeoutOrNull false
                            }
                            moved = change.position - down.position
                            if (moved.getDistance() > slop) return@withTimeoutOrNull false
                        }
                        @Suppress("UNREACHABLE_CODE") false
                    }
                    when {
                        pressed == null -> {
                            // Appui long sans bouger : l'image s'ouvre en grand ; la suite du geste est ignorée.
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpen(card.id)
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                        }
                        released -> if (current) onBack(card.id) else flipped = !flipped
                        else -> {
                            // Glissé : l'image suit le doigt, puis se range si on l'a assez écartée.
                            drag = moved
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                change.consume()
                                if (!change.pressed) break
                                drag = change.position - down.position
                            }
                            if (!current && drag.getDistance() > away) {
                                flipped = false
                                onAside(card.id)
                            }
                            drag = Offset.Zero
                        }
                    }
                }
            }
            .semantics {
                contentDescription = "Image pieuse : ${card.label}"
                val open = CustomAccessibilityAction("L'ouvrir en grand") { onOpen(card.id); true }
                if (aside) {
                    onClick("la reposer sur la page") { onBack(card.id); true }
                    customActions = listOf(open)
                } else {
                    onClick("la retourner") { flipped = !flipped; true }
                    customActions = listOf(CustomAccessibilityAction("La ranger en miniature") { onAside(card.id); true }, open)
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = drag.x
                    translationY = drag.y
                    rotationZ = tilt
                    rotationY = turn
                    cameraDistance = 14 * density.density
                    alpha = shown
                }
                .shadow(if (aside) 2.dp else 6.dp, CardCorners)
                .clip(CardCorners)
                .background(Color(0xFFF7F0DD)),
        ) {
            if (turn <= 90f) {
                (if (aside) card.image ?: card.picture else card.picture ?: card.image)?.let {
                    Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                }
            } else {
                Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }, contentAlignment = Alignment.Center) {
                    card.back?.let { Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
                        ?: BasicText(
                            card.label,
                            style = TextStyle(color = Color(0xFF2B2416), fontSize = 15.sp, textAlign = TextAlign.Center),
                            modifier = Modifier.padding(16.dp),
                        )
                }
            }
        }
    }
}

internal fun CardMark.ratio(): Float = (picture ?: image)?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } ?: (2f / 3.5f)

/** Posée sur la page : en haut à gauche du texte, la page valant φ fois l'image ; en pile si plusieurs. */
private fun Density.fullRect(ratio: Float, index: Int, slot: PageSlot): Rect {
    val boxW = slot.page.width / PHI
    val boxH = slot.page.height / PHI
    val (w, h) = if (ratio > boxW / boxH) boxW to boxW / ratio else boxH * ratio to boxH
    val shift = (10 * index).dp.toPx()
    val left = slot.page.left + slot.textLeft + shift
    val top = slot.page.top + slot.textTop + shift
    return Rect(left, top, left + w, top + h)
}

/** Rangée : une miniature dans la marge de tête, en haut à gauche de la page ; côte à côte si plusieurs. */
private fun Density.miniRect(ratio: Float, index: Int, slot: PageSlot, topInset: Float): Rect {
    val h = 64.dp.toPx()
    val w = (h * ratio).coerceIn(36.dp.toPx(), 96.dp.toPx())
    val left = slot.page.left + 10.dp.toPx() + index * (w + 8.dp.toPx())
    val top = slot.page.top + topInset + 4.dp.toPx()
    return Rect(left, top, left + w, top + h)
}
