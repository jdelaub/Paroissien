// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Single-page mode: the base page fills the container and the leaf folds across
 * the whole of it, its reverse the front mirrored. A turn with no base draws
 * none: a blocked turn lifts its page over whatever is behind the pager, never
 * over a live copy of itself.
 */
@Composable
internal fun MekuriSingleLayers(
    controller: MekuriPagerController,
    rigid: (Int) -> Boolean,
    content: @Composable (Int) -> Unit,
) {
    val turn = controller.turn
    val base = if (turn != null) turn.baseIndex else controller.settledPage
    Box(Modifier.fillMaxSize()) {
        base?.let { MekuriPage(it, MekuriPageMode.Live, controller.arrangement, content) }
        MekuriCrossfadeOverlay(controller) { page ->
            MekuriPage(page, MekuriPageMode.Live, controller.arrangement, content)
        }
        val turning = turn?.turningIndex
        if (turn != null && turning != null && rigid(turning)) {
            // Un plat de reliure pivote d'un bloc autour du dos, sans se plier (Paroissien).
            key(turn.id) {
                val progress = remember { { controller.foldProgress() } }
                MekuriRigidLeaf(progress, spineAtStart = controller.direction == MekuriDirection.LeftToRight, Modifier.fillMaxSize()) {
                    MekuriPage(turning, MekuriPageMode.Turning, controller.arrangement, content)
                }
            }
        } else if (turn != null && turning != null) {
            key(turn.id) {
                val progress = remember { { controller.foldProgress() } }
                MekuriFoldedPage(
                    progress = progress,
                    direction = controller.direction,
                    configuration = controller.configuration,
                    modifier = Modifier.fillMaxSize(),
                    liftsFromBottom = turn.liftsFromBottom,
                ) {
                    MekuriPage(turning, MekuriPageMode.Turning, controller.arrangement, content)
                }
            }
        }
    }
}

/**
 * Spread mode, drawn in order: the two live slots, then the leaf's shadow and
 * the leaf, both at the full two-slot width. The whole stack is shifted as one
 * piece. [pageSize] is in dp.
 */
@Composable
internal fun MekuriSpreadLayers(
    controller: MekuriPagerController,
    pageSize: Size,
    rigid: (Int) -> Boolean,
    content: @Composable (Int) -> Unit,
) {
    val turn = controller.turn
    val slots = turn?.let { MekuriSpreadPages(it.leading, it.trailing) }
        ?: controller.arrangement.slots(controller.settledPage)
    val span = controller.arrangement.shiftSpan(controller.settledPage, turn, controller.direction)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size((pageSize.width * 2).dp, pageSize.height.dp)
                .graphicsLayer { translationX = span.value(controller.progress) * density },
        ) {
            slots.leading?.let { MekuriSlotLayer(it, MekuriSlot.Leading, pageSize, controller, content) }
            slots.trailing?.let { MekuriSlotLayer(it, MekuriSlot.Trailing, pageSize, controller, content) }
            MekuriCrossfadeOverlay(controller) { page ->
                val faded = controller.arrangement.slots(page)
                faded.leading?.let { MekuriSlotLayer(it, MekuriSlot.Leading, pageSize, controller, content) }
                faded.trailing?.let { MekuriSlotLayer(it, MekuriSlot.Trailing, pageSize, controller, content) }
            }
            val rigidLeaf = turn != null && (turn.leafFront?.let(rigid) == true || turn.leafBack?.let(rigid) == true)
            if (turn != null && rigidLeaf) {
                // Un plat de reliure : il pivote autour du dos, au milieu de la double page, sans se plier (Paroissien).
                key(turn.id) {
                    val progress = remember { { controller.foldProgress() } }
                    val ltr = controller.direction == MekuriDirection.LeftToRight
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(pageSize.width.dp, pageSize.height.dp).graphicsLayer { translationX = (if (ltr) 1f else -1f) * pageSize.width * density / 2f }) {
                            MekuriRigidLeaf(
                                progress, spineAtStart = ltr, Modifier.fillMaxSize(),
                                back = turn.leafBack?.let { b -> { MekuriFace(b, controller.arrangement, content) } },
                            ) { MekuriFace(turn.leafFront, controller.arrangement, content) }
                        }
                    }
                }
            } else if (turn != null) {
                key(turn.id) {
                    val progress = remember { { controller.foldProgress() } }
                    MekuriLeafShadow(
                        progress = progress,
                        direction = controller.direction,
                        configuration = controller.configuration,
                        modifier = Modifier.fillMaxSize(),
                        liftsFromBottom = turn.liftsFromBottom,
                    )
                    MekuriFoldedLeaf(
                        progress = progress,
                        direction = controller.direction,
                        configuration = controller.configuration,
                        modifier = Modifier.fillMaxSize(),
                        liftsFromBottom = turn.liftsFromBottom,
                        back = { MekuriFace(turn.leafBack, controller.arrangement, content) },
                    ) {
                        MekuriFace(turn.leafFront, controller.arrangement, content)
                    }
                }
            }
        }
    }
}

/**
 * Placement is a geometry transform, never layout alignment and never an offset:
 * the layout direction must not move a slot.
 */
@Composable
private fun MekuriSlotLayer(
    page: Int,
    slot: MekuriSlot,
    pageSize: Size,
    controller: MekuriPagerController,
    content: @Composable (Int) -> Unit,
) {
    val side = if (slot == MekuriSlot.Leading) -1f else 1f
    val reading = if (controller.direction == MekuriDirection.LeftToRight) 1f else -1f
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(pageSize.width.dp, pageSize.height.dp)
                .graphicsLayer { translationX = side * reading * pageSize.width * density / 2f },
        ) {
            MekuriPage(page, MekuriPageMode.Live, controller.arrangement, content)
        }
    }
}

/** A face with no page is clear, so whatever lies beneath shows. */
@Composable
private fun MekuriFace(
    page: Int?,
    arrangement: MekuriArrangement,
    content: @Composable (Int) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        if (page != null) MekuriPage(page, MekuriPageMode.Turning, arrangement, content)
    }
}

/** The outgoing pages of a jump, fading out over the ones that replaced them. */
@Composable
private fun MekuriCrossfadeOverlay(
    controller: MekuriPagerController,
    content: @Composable (Int) -> Unit,
) {
    val from = controller.crossfadeFrom ?: return
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = controller.crossfadeAlpha.value.coerceIn(0f, 1f) }) {
        content(from)
    }
}

/**
 * Nothing outside the book is ever built. The page index is the content's
 * identity: a slot that changes page must tear the old page's state down rather
 * than hand it to its successor.
 */
@Composable
private fun MekuriPage(
    page: Int,
    mode: MekuriPageMode,
    arrangement: MekuriArrangement,
    content: @Composable (Int) -> Unit,
) {
    val pageCount = when (arrangement) {
        is MekuriArrangement.Single -> arrangement.pageCount
        is MekuriArrangement.Spread -> arrangement.layout.pageCount
    }
    if (page !in 0 until pageCount) return
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalMekuriPageMode provides mode) {
            key(page) { content(page) }
        }
    }
}
