// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import app.paroissien.core.reading.R
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * A page-curl pager over any content.
 *
 * Pages are addressed by index in reading order and [state] carries the
 * selection. A tap in an outer zone or a horizontal drag turns one page, or one
 * spread when two pages share the container; the reading direction decides which
 * edge is the spine and never touches the numbering.
 *
 * ```kotlin
 * val state = rememberMekuriPagerState(pageCount = pages.size)
 * MekuriPager(state = state, onCenterTap = { chromeHidden = !chromeHidden }) { page ->
 *     PageContent(pages[page])   // reads LocalMekuriPageMode.current
 * }
 * ```
 *
 * Only the visible page or spread and the two faces of a turning leaf are ever
 * built. A face reads [LocalMekuriPageMode] as [MekuriPageMode.Turning] and must
 * be drawable from what is already in memory. A blocked turn at either end lifts
 * the page over whatever is behind the pager, so give it a background.
 *
 * @param state selection and turn control; see [rememberMekuriPagerState].
 * @param pagingEnabled whether taps and drags turn pages. A centre tap still
 *   reports when they do not. A gesture keeps the value it began under, so a
 *   drag in flight when this turns false still releases its turn.
 * @param onCenterTap called for a tap outside both edge zones.
 * @param rigidPages pages that turn as a rigid board (book covers and their insides), pivoting about the spine
 *   instead of folding.
 * @param tapsTurn false while a menu is open: every tap then reports as a centre tap (it closes the menu) and none
 *   turns a page; drags still turn.
 * @param spread whether one or two pages share the container.
 * @param coverStandsAlone whether page 0 opens alone, like a cover.
 * @param pageAspectRatio width over height of one page.
 * @param haptics whether turns play haptics: a tap as the page lifts, a tick as
 *   a drag crosses the point where releasing completes the turn, and a thump as
 *   the page lands. Follows the system touch-feedback setting.
 * @param slide (Paroissien) the leaf slides instead of folding; always true below API 33.
 * @param onBlockedTurn (Paroissien) a turn asked past the last page (true) or before the first (false).
 * @param content builds the page at an index.
 */
@Composable
public fun MekuriPager(
    state: MekuriPagerState,
    modifier: Modifier = Modifier,
    configuration: MekuriConfiguration = MekuriConfiguration(),
    pagingEnabled: Boolean = true,
    onCenterTap: (() -> Unit)? = null,
    tapsTurn: Boolean = true,
    rigidPages: Set<Int> = emptySet(),
    spread: MekuriSpread = MekuriSpread.Automatic,
    coverStandsAlone: Boolean = true,
    pageAspectRatio: Float = DEFAULT_PAGE_ASPECT_RATIO,
    haptics: Boolean = true,
    slide: Boolean = false,
    onBlockedTurn: (forward: Boolean) -> Unit = {},
    content: @Composable (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val controller = remember(state, scope) { MekuriPagerController(state, scope) }
    controller.configuration = configuration
    controller.reducesMotion = rememberMekuriReducesMotion(configuration.reducedMotionOverride)
    controller.onBlockedTurn = { turn -> onBlockedTurn(turn == MekuriTurn.Forward) }
    val view = LocalView.current
    controller.onHaptic = if (haptics) {
        { haptic -> view.performHapticFeedback(haptic.feedbackConstant) }
    } else {
        {}
    }

    DisposableEffect(state, controller) {
        state.attach(controller)
        onDispose { state.detach(controller) }
    }
    LaunchedEffect(state, controller) {
        snapshotFlow { controller.turn != null }.collect { state.isTurning = it }
    }

    // Paroissien : libellés d'accessibilité en français, lus depuis les ressources.
    val pageDescription = stringResource(R.string.reading_page_of, state.currentPage + 1, state.pageCount)
    val nextLabel = stringResource(R.string.reading_next_page)
    val previousLabel = stringResource(R.string.reading_previous_page)
    BoxWithConstraints(
        modifier
            .mekuriGestures(controller, pagingEnabled, onCenterTap, tapsTurn)
            .semantics {
                stateDescription = pageDescription
                customActions = mekuriPageActions(controller, pagingEnabled, nextLabel, previousLabel)
            },
    ) {
        val size = Size(this.maxWidth.value, this.maxHeight.value)
        controller.containerSize = size
        val lastSize = remember(controller) { mutableStateOf(size) }
        LaunchedEffect(controller, size) {
            if (lastSize.value == size) return@LaunchedEffect
            lastSize.value = size
            controller.dropTurn()
        }
        val arrangement = MekuriArrangement.resolve(
            containerSize = size,
            pageCount = state.pageCount,
            spread = spread,
            coverStandsAlone = coverStandsAlone,
            pageAspectRatio = pageAspectRatio,
        )
        controller.arrangement = arrangement
        CompositionLocalProvider(LocalMekuriSlides provides slide) {
            when (arrangement) {
                is MekuriArrangement.Single -> MekuriSingleLayers(controller, { it in rigidPages }, content)
                is MekuriArrangement.Spread -> MekuriSpreadLayers(controller, arrangement.pageSize, { it in rigidPages }, content)
            }
        }
    }
}

private fun mekuriPageActions(
    controller: MekuriPagerController,
    pagingEnabled: Boolean,
    nextLabel: String,
    previousLabel: String,
): List<CustomAccessibilityAction> {
    if (!pagingEnabled) return emptyList()
    return listOf(
        CustomAccessibilityAction(nextLabel) {
            controller.perform(MekuriTurn.Forward)
            true
        },
        CustomAccessibilityAction(previousLabel) {
            controller.perform(MekuriTurn.Backward)
            true
        },
    )
}

private const val DEFAULT_PAGE_ASPECT_RATIO = 2f / 3f
