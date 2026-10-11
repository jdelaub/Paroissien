package app.paroissien.core.designsystem

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Une capture par thème, aux dimensions du Galaxy Z Flip6 en portrait (environ 384 × 939 dp). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
class PaperThemesScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test fun clair() = capture(PaperTheme.CLAIR)
    @Test fun sepia() = capture(PaperTheme.SEPIA)
    @Test fun sombre() = capture(PaperTheme.SOMBRE)
    @Test fun nuit() = capture(PaperTheme.NUIT)

    private fun capture(theme: PaperTheme) {
        compose.setContent { ParoissienTheme(theme) { PaperSample("Thème ${theme.name.lowercase()}") } }
        compose.onRoot().captureRoboImage() // nommée d'après la classe et le test, dans src/test/screenshots
    }
}
