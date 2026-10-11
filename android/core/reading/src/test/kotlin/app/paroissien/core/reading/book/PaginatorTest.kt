package app.paroissien.core.reading.book

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import app.paroissien.core.designsystem.BookFace
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.loadBookFonts
import app.paroissien.core.designsystem.paperColors
import app.paroissien.core.model.text.Inline
import app.paroissien.core.model.text.Inlines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PaginatorTest {

    private val context = RuntimeEnvironment.getApplication()
    private val density = Density(2.8125f) // Galaxy Z Flip6, environ 450 dpi
    private val measurer = TextMeasurer(createFontFamilyResolver(context), density, LayoutDirection.Ltr, cacheSize = 0)
    private val typography = BookTypography(loadBookFonts(context, BookFace.GARAMOND), paperColors(PaperTheme.CLAIR))

    private val blocks: List<ReadingBlock> = buildList {
        val french = "Que vous sert de raisonner profondément sur la Trinité, si vous n’êtes pas humbles, et que par là vous déplaisiez à la Trinité\u202F? "
        val latin = "Vanitas vanitatum et omnia vanitas, præter amáre Deum et illi soli servíre. Ista est summa sapiéntia, per contémptum mundi téndere ad regna cæléstia. "
        add(ReadingBlock.Title("titre", rt("Livre d’essai"), rt("pour le paginateur")))
        add(ReadingBlock.PageBreak("titre.fin"))
        for (c in 1..12) {
            add(ReadingBlock.Heading("c$c", rt("Chapitre $c"), runningHead = "Chapitre $c"))
            add(ReadingBlock.Rubric("c$c.r", rt("Rubrique du chapitre $c, en rouge et en italique.")))
            for (p in 0..5) {
                val lang = if (p % 3 == 2) BlockLang.LA else BlockLang.FR
                val base = if (lang == BlockLang.LA) latin else french
                val number = if (p > 0) listOf(Inline.Num("${p + 1}")) else emptyList()
                add(ReadingBlock.Paragraph("c$c.p$p", number + rt(base.repeat(1 + (c + p) % 5).trim()), lang, dropCap = p == 0))
            }
            add(ReadingBlock.Versicle("c$c.v", rt("℣ Introíbo ad altáre Dei.")))
            add(ReadingBlock.Heading("c$c.refl", rt("Réflexion"), minor = true))
            add(ReadingBlock.Paragraph("c$c.refl.p", rt(french.repeat(3).trim()), BlockLang.FR, role = TextRole.SMALL))
        }
    }

    private fun rt(text: String) = Inlines.fromMarkup(text)

    private fun paginate(widthDp: Float, heightDp: Float): PagedBook =
        paginate(blocks, measurer, typography, density, (widthDp * density.density).toInt(), (heightDp * density.density).toInt())

    @Test
    fun portraitAndLandscapeLayoutsKeepTheBookRules() {
        for ((name, size) in mapOf("portrait" to (328f to 840f), "paysage, une page de la double page" to (413f to 290f))) {
            val book = paginate(size.first, size.second)
            println("$name : ${book.pages.size} pages en ${book.elapsedMillis} ms")
            assertEveryLineOnceInOrder(book)
            assertNothingOverflows(book, size.second * density.density)
            assertNoHeadingAtPageBottom(book)
            assertNoLonelyLines(book)
            val dropCaps = blocks.count { it is ReadingBlock.Paragraph && it.dropCap }
            assertEquals("une lettrine par paragraphe qui en demande", dropCaps, book.pages.sumOf { page -> page.items.count { it is PageItem.Glyph } })
            book.pages.forEachIndexed { index, page -> assertEquals("ancre de la page $index", index, book.pageOf(page.anchor)) }
        }
    }

    @Test
    fun anchorStaysInsideItsPageAfterRelayout() {
        val portrait = paginate(328f, 840f)
        val landscape = paginate(413f, 290f)
        portrait.pages.forEach { page ->
            val there = landscape.pageOf(page.anchor)
            val next = landscape.pages.getOrNull(there + 1)?.anchor
            // L'ancre retrouvée débute au plus tard l'ancre cherchée, et la page suivante commence après elle.
            assertTrue(next == null || landscape.pageOf(next) > there)
            assertEquals(there, landscape.pageOf(landscape.pages[there].anchor))
        }
    }

    private fun linesByBlock(book: PagedBook) =
        book.pages.flatMap { it.items }.filterIsInstance<PageItem.Lines>().groupBy { it.blockId }

    private fun assertEveryLineOnceInOrder(book: PagedBook) {
        linesByBlock(book).forEach { (block, items) ->
            assertEquals("$block commence à la première ligne", 0, items.first().fromLine)
            items.zipWithNext().forEach { (a, b) ->
                if (a.layout === b.layout) {
                    assertEquals("$block : lignes contiguës", a.toLine, b.fromLine)
                } else {
                    assertEquals("$block : la suite commence en tête", 0, b.fromLine)
                }
                assertEquals("$block : ancres contiguës", a.charStart + canonicalOffset(a.layout.layoutInput.text, a.layout.getLineEnd(a.toLine - 1)) - canonicalOffset(a.layout.layoutInput.text, a.layout.getLineStart(a.fromLine)), b.charStart)
            }
            assertEquals("$block : la dernière ligne est posée", items.last().layout.lineCount, items.last().toLine)
        }
    }

    private fun assertNothingOverflows(book: PagedBook, heightPx: Float) {
        book.pages.forEachIndexed { index, page ->
            page.items.filterIsInstance<PageItem.Lines>().forEach {
                assertTrue("page $index déborde : ${it.y + it.height} > $heightPx", it.y >= -0.5f && it.y + it.height <= heightPx + 1f)
            }
        }
    }

    private fun assertNoHeadingAtPageBottom(book: PagedBook) {
        val headings = blocks.filterIsInstance<ReadingBlock.Heading>().map { it.id }.toSet()
        book.pages.dropLast(1).forEachIndexed { index, page ->
            val last = page.items.filterIsInstance<PageItem.Lines>().last()
            assertTrue("titre seul en bas de la page $index : ${last.blockId}", last.blockId !in headings)
        }
    }

    private fun assertNoLonelyLines(book: PagedBook) {
        val fragments = book.pages.map { page -> page.items.filterIsInstance<PageItem.Lines>().groupBy { it.blockId } }
        val totals = linesByBlock(book).mapValues { (_, items) -> items.sumOf { it.toLine - it.fromLine } }
        fragments.forEach { page ->
            page.forEach { (block, items) ->
                val here = items.sumOf { it.toLine - it.fromLine }
                if (here < totals.getValue(block) && totals.getValue(block) >= 4) {
                    assertTrue("$block : fragment d'une ligne", here >= 2)
                }
            }
        }
    }
}
