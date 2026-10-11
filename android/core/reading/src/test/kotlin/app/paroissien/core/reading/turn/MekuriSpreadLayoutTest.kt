// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MekuriSpreadLayoutTest {
    private val coverAlone = MekuriSpreadLayout(pageCount = 6, coverStandsAlone = true)
    private val paired = MekuriSpreadLayout(pageCount = 6, coverStandsAlone = false)

    @Test
    fun `slots pair from the cover`() {
        assertEquals(MekuriSpreadPages(null, 0), coverAlone.pages(0))
        assertEquals(MekuriSpreadPages(1, 2), coverAlone.pages(1))
        assertEquals(MekuriSpreadPages(3, 4), coverAlone.pages(2))
        assertEquals(MekuriSpreadPages(5, null), coverAlone.pages(3))
        assertEquals(4, coverAlone.spreadCount)
    }

    @Test
    fun `slots pair from the first page`() {
        assertEquals(MekuriSpreadPages(0, 1), paired.pages(0))
        assertEquals(MekuriSpreadPages(4, 5), paired.pages(2))
        assertEquals(3, paired.spreadCount)
    }

    @Test
    fun `a page is found in its spread`() {
        assertEquals(0, coverAlone.spreadIndex(0))
        assertEquals(2, coverAlone.spreadIndex(4))
        assertEquals(2, paired.spreadIndex(4))
    }

    @Test
    fun `a forward turn carries the trailing page over`() {
        val leaf = coverAlone.leaf(MekuriTurn.Forward, spreadIndex = 1)
        assertEquals(2, leaf?.front)
        assertEquals(3, leaf?.back)
        assertEquals(4, leaf?.revealed)
        assertEquals(MekuriSlot.Leading, leaf?.landsIn)
        assertEquals(3, coverAlone.selection(MekuriTurn.Forward, spreadIndex = 1))
    }

    @Test
    fun `a forward turn from the cover opens the book`() {
        val leaf = coverAlone.leaf(MekuriTurn.Forward, spreadIndex = 0)
        assertEquals(0, leaf?.front)
        assertEquals(1, leaf?.back)
        assertEquals(2, leaf?.revealed)
    }

    @Test
    fun `the last leaf has nothing behind it`() {
        val leaf = coverAlone.leaf(MekuriTurn.Forward, spreadIndex = 2)
        assertEquals(4, leaf?.front)
        assertEquals(5, leaf?.back)
        assertNull(leaf?.revealed)
        assertEquals(5, coverAlone.selection(MekuriTurn.Forward, spreadIndex = 2))
    }

    @Test
    fun `turns refuse at both ends`() {
        assertNull(coverAlone.leaf(MekuriTurn.Forward, spreadIndex = 3))
        assertNull(coverAlone.leaf(MekuriTurn.Backward, spreadIndex = 0))
        assertNull(coverAlone.selection(MekuriTurn.Forward, spreadIndex = 3))
        assertNull(coverAlone.selection(MekuriTurn.Backward, spreadIndex = 0))
    }

    @Test
    fun `a backward turn carries the leading page back`() {
        val leaf = coverAlone.leaf(MekuriTurn.Backward, spreadIndex = 2)
        assertEquals(3, leaf?.front)
        assertEquals(2, leaf?.back)
        assertEquals(1, leaf?.revealed)
        assertEquals(MekuriSlot.Trailing, leaf?.landsIn)
        assertEquals(1, coverAlone.selection(MekuriTurn.Backward, spreadIndex = 2))
    }

    @Test
    fun `a backward turn into the cover selects the only page there`() {
        val leaf = coverAlone.leaf(MekuriTurn.Backward, spreadIndex = 1)
        assertEquals(1, leaf?.front)
        assertEquals(0, leaf?.back)
        assertNull(leaf?.revealed)
        assertEquals(0, coverAlone.selection(MekuriTurn.Backward, spreadIndex = 1))
    }

    @Test
    fun `paired turns move one spread too`() {
        assertEquals(1, paired.leaf(MekuriTurn.Forward, spreadIndex = 0)?.front)
        assertEquals(2, paired.leaf(MekuriTurn.Forward, spreadIndex = 0)?.back)
        assertEquals(2, paired.selection(MekuriTurn.Forward, spreadIndex = 0))
        assertEquals(1, paired.leaf(MekuriTurn.Backward, spreadIndex = 1)?.back)
        assertEquals(0, paired.selection(MekuriTurn.Backward, spreadIndex = 1))
    }

    @Test
    fun `a lone cover has nowhere to turn`() {
        val loneCover = MekuriSpreadLayout(pageCount = 1, coverStandsAlone = true)
        assertEquals(1, loneCover.spreadCount)
        assertEquals(MekuriSpreadPages(null, 0), loneCover.pages(0))
        assertNull(loneCover.leaf(MekuriTurn.Forward, spreadIndex = 0))
        assertNull(loneCover.leaf(MekuriTurn.Backward, spreadIndex = 0))
    }
}
