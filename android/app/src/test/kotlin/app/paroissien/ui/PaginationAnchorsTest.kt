package app.paroissien.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import app.paroissien.core.designsystem.BookFace
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.ParoissienTheme
import app.paroissien.core.designsystem.rememberBookFonts
import app.paroissien.core.model.text.Inlines
import app.paroissien.core.reading.book.BlockLang
import app.paroissien.core.reading.book.BookReader
import app.paroissien.core.reading.book.BookTypography
import app.paroissien.core.reading.book.PagedBook
import app.paroissien.core.reading.book.ReadingBlock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Chaque page se retrouve par son ancre : sinon, tourner la page fait sauter le livre ailleurs et l'on ne peut plus
 * revenir en arrière. Cas difficile : un long texte en regard (un Évangile du missel dans un livret) qui court sur
 * plusieurs pages, la colonne de droite plus longue que celle de gauche, puis une image seule.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PaginationAnchorsTest {

    @get:Rule
    val compose = createComposeRule()

    private fun words(seed: String, n: Int) = (0 until n).joinToString(" ") { seed.split(' ')[it % seed.split(' ').size] }

    private fun book(): List<ReadingBlock> {
        val la = "In princípio erat Verbum et Verbum erat apud Deum et Deus erat Verbum"
        val fr = "Au commencement était le Verbe et le Verbe était auprès de Dieu et le Verbe était Dieu, et la vie était la lumière des hommes"
        return listOf(ReadingBlock.Heading("h", Inlines.fromMarkup("Évangile"))) + (1..3).flatMap { k ->
            listOf(
                ReadingBlock.Facing(
                    "long$k",
                    ReadingBlock.Paragraph("long$k", Inlines.fromMarkup(words(la, 220 * k)), BlockLang.LA),
                    ReadingBlock.Paragraph("long$k", Inlines.fromMarkup(words(fr, 420 * k)), BlockLang.FR),
                ),
                ReadingBlock.Facing(
                    "court$k",
                    ReadingBlock.Paragraph("court$k", Inlines.fromMarkup(words(la, 500)), BlockLang.LA),
                    ReadingBlock.Paragraph("court$k", Inlines.fromMarkup(words(fr, 60)), BlockLang.FR),
                ),
                ReadingBlock.Slot("slot$k", Inlines.fromMarkup("Offertoire — texte de la messe du jour")),
            )
        }
    }

    /**
     * Revenir en arrière depuis la première page ouvre l'unité précédente, comme avancer depuis la dernière ouvre la
     * suivante : sans feuillet à soulever, le geste ne commençait aucun tour et se perdait (livret, 12/10/2026).
     */
    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun swipingBackFromTheFirstPageOpensThePreviousUnit() {
        var before = 0
        var after = 0
        var paged by mutableStateOf<PagedBook?>(null)
        compose.setContent {
            ParoissienTheme(PaperTheme.CLAIR) {
                val fonts = rememberBookFonts(BookFace.GARAMOND)
                val paper = LocalPaperColors.current
                BookReader(
                    book().take(3), remember(fonts, paper) { BookTypography(fonts, paper) }, null, {},
                    onPaginated = { paged = it }, paginationContext = EmptyCoroutineContext,
                    onTurnPastStart = { before++ }, onTurnPastEnd = { after++ },
                )
            }
        }
        compose.waitUntil(120_000) { paged != null }
        compose.onRoot().performTouchInput { swipeRight() }
        compose.waitForIdle()
        assertEquals("unité précédente", 1, before)
        repeat(paged!!.pages.size) { compose.onRoot().performTouchInput { swipeLeft() }; compose.waitForIdle() }
        assertEquals("unité suivante", 1, after)
    }

    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun everyPageIsFoundByItsAnchor() {
        val blocks = book()
        var paged by mutableStateOf<PagedBook?>(null)
        compose.setContent {
            ParoissienTheme(PaperTheme.CLAIR) {
                val fonts = rememberBookFonts(BookFace.GARAMOND)
                val paper = LocalPaperColors.current
                BookReader(blocks, remember(fonts, paper) { BookTypography(fonts, paper) }, null, {}, onPaginated = { paged = it }, paginationContext = EmptyCoroutineContext)
            }
        }
        compose.waitUntil(120_000) { paged != null }
        val p = paged!!
        val wrong = p.pages.indices.filter { p.pageOf(p.pages[it].anchor) != it }
        assertEquals("pages mal retrouvées sur ${p.pages.size} : " + wrong.joinToString { "$it→${p.pageOf(p.pages[it].anchor)} ${p.pages[it].anchor}" }, emptyList<Int>(), wrong)
    }
}
