package app.paroissien.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import app.paroissien.core.designsystem.BookFace
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.ParoissienTheme
import app.paroissien.core.designsystem.rememberBookFonts
import app.paroissien.core.reading.book.Anchor
import app.paroissien.core.reading.book.BookReader
import app.paroissien.core.reading.book.BookTypography
import app.paroissien.core.reading.book.RibbonMark
import app.paroissien.library.BookIdentities
import app.paroissien.core.reading.book.BookLook
import app.paroissien.core.designsystem.PaperWeight
import app.paroissien.core.designsystem.EdgeFinish
import app.paroissien.core.designsystem.BookIdentity
import app.paroissien.core.designsystem.BookCover
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.fillMaxSize
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext

/** Le lecteur sur le Galaxy Z Flip6 (livre de démonstration) : latin en portrait, Ordinaire en double page, thème sombre. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BookReaderScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val book = demoBook()

    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun latinLettrinePortrait() = capture(Anchor("lat.1", 0), BookFace.GARAMOND, PaperTheme.CLAIR)

    @Test
    @Config(sdk = [35], qualifiers = "w939dp-h384dp-land-xxhdpi")
    fun ordinaireDoublePagePaysage() = capture(Anchor("ord.1", 0), BookFace.GARAMOND, PaperTheme.SEPIA)

    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun gloriaLiterataSombre() = capture(Anchor("ord.2", 0), BookFace.LITERATA, PaperTheme.SOMBRE)

    /** Rubans : l'un couché sur la page, les autres pendant dans la bande réservée sous le texte. */
    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun rubansDansLaBande() = capture(
        Anchor("lat.1", 0), BookFace.GARAMOND, PaperTheme.CLAIR,
        ribbons = listOf(RibbonMark(0, Anchor("lat.1", 0)), RibbonMark(1, null, elsewhere = -1), RibbonMark(2, null, elsewhere = 1), RibbonMark(3, null, elsewhere = 1)),
    )

    private val bible = BookIdentities.of("bible.crampon", null, uniform = false).copy(pages = 2800)

    /** La Bible : la couverture de maroquin, page d'avant la première. */
    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun couvertureDeLaBible() = capture(
        null, BookFace.GARAMOND, PaperTheme.CLAIR, identity = bible,
        look = BookLook(EdgeFinish.GILT, PaperWeight.BIBLE, cover = { BookCover(bible, "La Sainte Bible", "Crampon", Modifier.fillMaxSize(), flat = true) }),
    )

    /** Papier bible : ivoire et fin, tranche dorée épaisse, le verso transparaît à peine. */
    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun papierBible() = capture(
        Anchor("ord.1", 0), BookFace.GARAMOND, PaperTheme.CLAIR, identity = bible,
        look = BookLook(EdgeFinish.GILT, PaperWeight.BIBLE, showThrough = true, progress = 0.2f..0.25f, thickness = 8.dp),
    )

    private fun capture(
        anchor: Anchor?,
        face: BookFace,
        theme: PaperTheme,
        ribbons: List<RibbonMark> = emptyList(),
        identity: BookIdentity? = null,
        look: BookLook = BookLook(),
    ) {
        var ready by mutableStateOf(false)
        compose.setContent {
            ParoissienTheme(theme) {
                val fonts = rememberBookFonts(face)
                val themePaper = LocalPaperColors.current
                val paper = identity?.paper?.colors(themePaper) ?: themePaper
                CompositionLocalProvider(LocalPaperColors provides paper) {
                BookReader(
                    blocks = book,
                    typography = remember(fonts, paper) { BookTypography(fonts, paper) },
                    anchor = anchor,
                    onAnchorChange = {},
                    onPaginated = { ready = true },
                    paginationContext = EmptyCoroutineContext,
                    ribbons = ribbons,
                    look = look,
                )
                }
            }
        }
        compose.waitUntil(timeoutMillis = 60_000) { ready }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage()
    }
}
