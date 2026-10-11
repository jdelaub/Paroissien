package app.paroissien.spike

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import app.paroissien.core.designsystem.BookFace
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.loadBookFonts
import app.paroissien.core.designsystem.paperColors
import app.paroissien.core.reading.book.BookTypography
import app.paroissien.core.reading.book.ReadingBlock
import app.paroissien.core.reading.book.paginate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Le Livre I complet de l'Imitation se charge et se pagine (critère de la porte A, mesuré pour de vrai sur le téléphone). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SpikeBookTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun bookOneLoadsAndPaginates() {
        val blocks = loadSpikeBook(context)
        assertEquals(25, blocks.count { it is ReadingBlock.Heading && it.id.matches(Regex("imit\\.1\\.\\d+")) })
        assertEquals("identifiants uniques", blocks.size, blocks.map { it.id }.toSet().size)

        val density = Density(2.8125f)
        val measurer = TextMeasurer(createFontFamilyResolver(context), density, LayoutDirection.Ltr, cacheSize = 0)
        val typography = BookTypography(loadBookFonts(context, BookFace.GARAMOND), paperColors(PaperTheme.CLAIR))
        // Zone de texte du Flip6 en portrait (384 × 939 dp moins les marges du lecteur).
        val book = paginate(blocks, measurer, typography, density, (328 * 2.8125f).toInt(), (851 * 2.8125f).toInt())
        println("Livre d'essai : ${blocks.size} blocs, ${book.pages.size} pages, pagination ${book.elapsedMillis} ms (Robolectric)")
        assertTrue(book.pages.size > 60)
    }
}
