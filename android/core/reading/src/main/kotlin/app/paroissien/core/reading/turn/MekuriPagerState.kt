// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/**
 * The pager a [MekuriPagerState] drives. Installed by the pager while it is
 * composed; the state holder owns no animation of its own.
 */
internal interface MekuriTurnDriver {
    /**
     * Turns to [page] and returns the page settled on. A turn the user grabbed
     * and pushed back returns the page it started from rather than failing.
     */
    suspend fun turnTo(page: Int): Int

    /**
     * Drops any turn in flight without animating it. A [turnTo] call in flight
     * must be cancelled, not allowed to return.
     */
    fun dropTurn()
}

/**
 * Selection and turn control for a Mekuri pager. Create one with
 * [rememberMekuriPagerState] and read [currentPage] to follow the page on
 * screen; [animateToPage] and [scrollToPage] are the only ways to move it from
 * outside.
 *
 * [animateToPage] suspends until a pager is composed with this state over a
 * non-empty book, and cancelling the calling coroutine releases it. A turn the
 * user grabs and pushes back returns from [animateToPage] normally with the
 * settled page. A [scrollToPage] arriving mid-turn cancels the pending
 * [animateToPage].
 *
 * @param pageCount number of pages in the book, read on every use rather than
 *   copied, so a book that loads its pages later reports them.
 * @param direction spine placement and sweep direction.
 * @param initialPage page shown first; clamped on read into `0 until pageCount`.
 */
public class MekuriPagerState(
    pageCount: () -> Int,
    direction: MekuriDirection = MekuriDirection.LeftToRight,
    initialPage: Int = 0,
) {
    private val driver = MutableStateFlow<MekuriTurnDriver?>(null)

    /** Replaced, never mutated: an equal provider instance writes nothing. */
    internal var pageCountProvider: () -> Int by mutableStateOf(pageCount)

    /** Bumped whenever a scroll drops a turn; a dropped turn may not settle. */
    private var dropGeneration = 0

    private var currentPageState by mutableIntStateOf(initialPage)

    /** Number of pages in the book. Shrinking it clamps [currentPage]. */
    public val pageCount: Int
        get() = this.pageCountProvider().coerceAtLeast(0)

    /** True while a leaf is lifted, dragged or landing; objects laid over the page hide meanwhile. */
    public var isTurning: Boolean by mutableStateOf(false)
        internal set

    /** Spine placement and sweep direction. */
    public var direction: MekuriDirection = direction
        internal set

    /**
     * Page settled on screen. In a spread this is the leading page of the pair.
     * The page is stored as given and clamped only on read; a page count that
     * momentarily drops to zero must not discard it.
     */
    public var currentPage: Int
        get() = coerce(this.currentPageState, this.pageCount)
        internal set(value) {
            this.currentPageState = value
        }

    /**
     * Turns to [page] with a fold and returns once the turn has settled. Waits
     * for a pager over a non-empty book, then clamps the target against the page
     * count. A turn a [scrollToPage] dropped meanwhile leaves the page alone.
     */
    public suspend fun animateToPage(page: Int) {
        val driver = combine(this.driver, snapshotFlow { this.pageCount }) { driver, count ->
            driver.takeIf { count > 0 }
        }.filterNotNull().first()
        val generation = this.dropGeneration
        val settled = driver.turnTo(coerce(page, this.pageCount))
        if (generation != this.dropGeneration) return
        this.currentPageState = settled
    }

    /** Shows [page] with no fold, dropping any turn in flight. */
    public suspend fun scrollToPage(page: Int) {
        this.dropGeneration += 1
        this.driver.value?.dropTurn()
        this.currentPageState = page
    }

    internal fun attach(driver: MekuriTurnDriver) {
        this.driver.value = driver
    }

    internal fun detach(driver: MekuriTurnDriver) {
        this.driver.compareAndSet(driver, null)
    }

    internal companion object {
        private fun coerce(page: Int, pageCount: Int): Int =
            if (pageCount <= 0) 0 else page.coerceIn(0, pageCount - 1)

        /**
         * Saves the stored page rather than the clamped read; the composable
         * writes the real page count after the state is restored.
         */
        internal val Saver: Saver<MekuriPagerState, Int> = Saver(
            save = { it.currentPageState },
            restore = { MekuriPagerState(pageCount = { 0 }, initialPage = it) },
        )
    }
}

/**
 * Remembers a [MekuriPagerState] across recompositions and configuration
 * changes. [pageCount] and [direction] are written into the remembered state on
 * every composition; [initialPage] is read once. A state held outside
 * composition is built directly, with the page count as a function.
 */
@Composable
public fun rememberMekuriPagerState(
    pageCount: Int,
    direction: MekuriDirection = MekuriDirection.LeftToRight,
    initialPage: Int = 0,
): MekuriPagerState {
    val provider = remember(pageCount) { { pageCount } }
    val state = rememberSaveable(saver = MekuriPagerState.Saver) {
        MekuriPagerState(pageCount = provider, direction = direction, initialPage = initialPage)
    }
    state.pageCountProvider = provider
    state.direction = direction
    return state
}
