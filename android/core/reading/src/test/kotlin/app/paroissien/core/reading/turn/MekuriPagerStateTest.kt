// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MekuriPagerStateTest {
    @Test
    fun `the initial page is clamped on read into the book`() {
        assertEquals(3, MekuriPagerState(pageCount = { 6 }, initialPage = 3).currentPage)
        assertEquals(5, MekuriPagerState(pageCount = { 6 }, initialPage = 99).currentPage)
        assertEquals(0, MekuriPagerState(pageCount = { 6 }, initialPage = -1).currentPage)
        assertEquals(0, MekuriPagerState(pageCount = { 0 }, initialPage = 4).currentPage)
    }

    @Test
    fun `shrinking the book clamps the current page`() {
        val state = MekuriPagerState(pageCount = { 6 }, initialPage = 5)
        state.pageCountProvider = { 2 }
        assertEquals(1, state.currentPage)
        state.pageCountProvider = { 0 }
        assertEquals(0, state.currentPage)
    }

    @Test
    fun `a book that is still loading does not discard the page`() {
        val state = MekuriPagerState(pageCount = { 6 }, initialPage = 5)
        state.pageCountProvider = { 0 }
        assertEquals(0, state.currentPage)
        state.pageCountProvider = { 6 }
        assertEquals(5, state.currentPage)
    }

    @Test
    fun `a scroll cuts to the page without a driver`() = runBlocking {
        val state = MekuriPagerState(pageCount = { 6 }, initialPage = 0)
        state.scrollToPage(4)
        assertEquals(4, state.currentPage)
        state.scrollToPage(40)
        assertEquals(5, state.currentPage)
    }

    @Test
    fun `a scroll drops a turn in flight`() = runBlocking {
        val driver = RecordingDriver(settlesOn = 1)
        val state = MekuriPagerState(pageCount = { 6 }, initialPage = 0)
        state.attach(driver)
        state.scrollToPage(3)
        assertEquals(1, driver.drops)
        assertEquals(3, state.currentPage)
    }

    @Test
    fun `an animated turn settles on the page the driver reports`() = runBlocking {
        val driver = RecordingDriver(settlesOn = 2)
        val state = MekuriPagerState(pageCount = { 6 }, initialPage = 1)
        state.attach(driver)
        state.animateToPage(2)
        assertEquals(listOf(2), driver.requests)
        assertEquals(2, state.currentPage)
    }

    @Test
    fun `a reverted turn returns normally and leaves the settled page on screen`() = runBlocking {
        val driver = RecordingDriver(settlesOn = 1)
        val state = MekuriPagerState(pageCount = { 6 }, initialPage = 1)
        state.attach(driver)
        state.animateToPage(2)
        assertEquals(1, state.currentPage)
    }

    @Test
    fun `an animated turn coerces its target before the driver sees it`() = runBlocking {
        val driver = RecordingDriver(settlesOn = 5)
        val state = MekuriPagerState(pageCount = { 6 }, initialPage = 0)
        state.attach(driver)
        state.animateToPage(99)
        assertEquals(listOf(5), driver.requests)
    }

    @Test
    fun `a detached pager leaves no driver behind`() = runBlocking {
        val driver = RecordingDriver(settlesOn = 3)
        val state = MekuriPagerState(pageCount = { 6 }, initialPage = 0)
        state.attach(driver)
        state.detach(driver)
        state.scrollToPage(2)
        assertEquals(0, driver.drops)
        assertEquals(2, state.currentPage)
    }

    @Test
    fun `an animation requested before the pager attaches waits for it`() = runBlocking {
        val state = MekuriPagerState(pageCount = { 6 }, initialPage = 0)
        val driver = RecordingDriver(settlesOn = 1)
        val turn = async { state.animateToPage(1) }
        yieldUntil { false }
        assertTrue(turn.isActive)
        assertEquals(0, state.currentPage)
        state.attach(driver)
        turn.await()
        assertEquals(1, state.currentPage)
    }

    @Test
    fun `a state built while the book loads keeps its initial page`() {
        val state = MekuriPagerState(pageCount = { 0 }, initialPage = 5)
        assertEquals(0, state.currentPage)
        state.pageCountProvider = { 6 }
        assertEquals(5, state.currentPage)
    }

    @Test
    fun `a scroll while the book loads keeps its page`() = runBlocking {
        val state = MekuriPagerState(pageCount = { 0 }, initialPage = 0)
        state.scrollToPage(4)
        assertEquals(0, state.currentPage)
        state.pageCountProvider = { 6 }
        assertEquals(4, state.currentPage)
    }

    @Test
    fun `an animation clamps its target only once the pager attaches`() = runBlocking {
        val state = MekuriPagerState(pageCount = { 0 }, initialPage = 0)
        val driver = RecordingDriver(settlesOn = 4)
        val turn = async { state.animateToPage(4) }
        yieldUntil { false }
        state.setPageCount { 6 }
        state.attach(driver)
        turn.await()
        assertEquals(listOf(4), driver.requests)
        assertEquals(4, state.currentPage)
    }

    @Test
    fun `saving while the book loads keeps the page`() {
        val state = MekuriPagerState(pageCount = { 6 }, initialPage = 5)
        state.pageCountProvider = { 0 }
        val saved = with(MekuriPagerState.Saver) { SaverScope { true }.save(state) }
        assertEquals(5, saved)
        val restored = MekuriPagerState.Saver.restore(saved!!)!!
        assertEquals(0, restored.currentPage)
        restored.pageCountProvider = { 6 }
        assertEquals(5, restored.currentPage)
    }

    @Test
    fun `a scroll that drops a turn is not clobbered by the dropped turn`() = runBlocking {
        val driver = SuspendingDriver()
        val state = MekuriPagerState(pageCount = { 6 }, initialPage = 0)
        state.attach(driver)
        val turn = async { state.animateToPage(5) }
        yieldUntil { driver.isParked }
        assertTrue(driver.isParked)
        state.scrollToPage(2)
        val outcome = runCatching { turn.await() }
        assertTrue(outcome.exceptionOrNull() is CancellationException)
        assertEquals(2, state.currentPage)
    }

    @Test
    fun `a dropped turn that returns instead of cancelling does not clobber the scroll`() = runBlocking {
        val driver = ReturningDriver(returnsOn = 5)
        val state = MekuriPagerState(pageCount = { 6 }, initialPage = 0)
        state.attach(driver)
        val turn = async { state.animateToPage(5) }
        yieldUntil { driver.isParked }
        assertTrue(driver.isParked)
        state.scrollToPage(2)
        turn.await()
        assertEquals(2, state.currentPage)
    }

    @Test
    fun `an animation waits for a book with pages in it`() = runBlocking {
        val state = MekuriPagerState(pageCount = { 0 }, initialPage = 0)
        val driver = RecordingDriver(settlesOn = 5)
        state.attach(driver)
        val turn = async { state.animateToPage(5) }
        yieldUntil { false }
        assertTrue(turn.isActive)
        assertEquals(emptyList<Int>(), driver.requests)
        state.setPageCount { 10 }
        turn.await()
        assertEquals(listOf(5), driver.requests)
        assertEquals(5, state.currentPage)
    }

    /**
     * Writes the page count and publishes the write. Outside a recomposer,
     * nothing else sends the apply notification a snapshot flow waits on.
     */
    private fun MekuriPagerState.setPageCount(provider: () -> Int) {
        this.pageCountProvider = provider
        Snapshot.sendApplyNotifications()
    }

    /** Runs the scheduler until [ready], or [YIELDS] times. */
    private suspend fun yieldUntil(ready: () -> Boolean) {
        repeat(YIELDS) {
            if (ready()) return
            yield()
        }
    }

    /** Returns from a dropped turn rather than cancelling, against the contract. */
    private class ReturningDriver(private val returnsOn: Int) : MekuriTurnDriver {
        private val parked = CompletableDeferred<Int>()

        var isParked = false
            private set

        override suspend fun turnTo(page: Int): Int {
            this.isParked = true
            return this.parked.await()
        }

        override fun dropTurn() {
            this.parked.complete(this.returnsOn)
        }
    }

    private class SuspendingDriver : MekuriTurnDriver {
        private var parked: CancellableContinuation<Int>? = null

        val isParked: Boolean get() = this.parked != null

        override suspend fun turnTo(page: Int): Int =
            suspendCancellableCoroutine { continuation -> this.parked = continuation }

        override fun dropTurn() {
            this.parked?.cancel()
            this.parked = null
        }
    }

    private companion object {
        private const val YIELDS = 32
    }

    private class RecordingDriver(private val settlesOn: Int) : MekuriTurnDriver {
        val requests = mutableListOf<Int>()
        var drops = 0

        override suspend fun turnTo(page: Int): Int {
            this.requests += page
            return this.settlesOn
        }

        override fun dropTurn() {
            this.drops += 1
        }
    }
}
