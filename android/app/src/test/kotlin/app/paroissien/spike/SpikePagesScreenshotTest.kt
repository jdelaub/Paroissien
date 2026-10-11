package app.paroissien.spike

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import app.paroissien.core.designsystem.BookFace
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.ParoissienTheme
import app.paroissien.core.designsystem.rememberBookFonts
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Pages témoins de la phase 1a, aux dimensions du Galaxy Z Flip6 en portrait (environ 384 × 939 dp). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
class SpikePagesScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test fun ordinaireGaramond() = capture(page = 6, BookFace.GARAMOND, PaperTheme.CLAIR)
    @Test fun gloriaLatinGaramond() = capture(page = 7, BookFace.GARAMOND, PaperTheme.SEPIA)
    @Test fun controleGaramond() = capture(page = 9, BookFace.GARAMOND, PaperTheme.CLAIR)
    @Test fun controleLiterata() = capture(page = 9, BookFace.LITERATA, PaperTheme.SOMBRE)

    private fun capture(page: Int, face: BookFace, theme: PaperTheme) {
        compose.setContent {
            ParoissienTheme(theme) { BookPage(spikeBook[page], number = page, fonts = rememberBookFonts(face)) }
        }
        compose.onRoot().captureRoboImage()
    }
}
