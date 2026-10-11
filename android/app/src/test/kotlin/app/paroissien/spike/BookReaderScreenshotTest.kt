package app.paroissien.spike

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
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext

/** Le lecteur sur le Galaxy Z Flip6 : chapitre I en portrait, double page en paysage, thème sombre. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BookReaderScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val book = loadSpikeBook(RuntimeEnvironment.getApplication())

    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun chapitrePremierPortrait() = capture(Anchor("imit.1.1", 0), BookFace.GARAMOND, PaperTheme.CLAIR)

    @Test
    @Config(sdk = [35], qualifiers = "w939dp-h384dp-land-xxhdpi")
    fun doublePagePaysage() = capture(Anchor("imit.1.3.p2", 0), BookFace.GARAMOND, PaperTheme.SEPIA)

    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun gloriaLiterataSombre() = capture(Anchor("ord.2", 0), BookFace.LITERATA, PaperTheme.SOMBRE)

    private fun capture(anchor: Anchor, face: BookFace, theme: PaperTheme) {
        var ready by mutableStateOf(false)
        compose.setContent {
            ParoissienTheme(theme) {
                val fonts = rememberBookFonts(face)
                val paper = LocalPaperColors.current
                BookReader(
                    blocks = book,
                    typography = remember(fonts, paper) { BookTypography(fonts, paper) },
                    anchor = anchor,
                    onAnchorChange = {},
                    onPaginated = { ready = true },
                    paginationContext = EmptyCoroutineContext,
                )
            }
        }
        compose.waitUntil(timeoutMillis = 60_000) { ready }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage()
    }
}
