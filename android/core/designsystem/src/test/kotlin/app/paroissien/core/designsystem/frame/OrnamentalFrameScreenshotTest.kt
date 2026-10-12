package app.paroissien.core.designsystem.frame

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.ParoissienTheme
import app.paroissien.core.liturgy.LiturgicalColor
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Le cadre « Pour la confession » au format Galaxy Z Flip6 (environ 384 × 939 dp) : couleurs du livre en clair,
 * palette violette atténuée en sombre, atténuation forte en nuit.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
class OrnamentalFrameScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test fun clair() = capture(PaperTheme.CLAIR, liturgicalColor = null)
    @Test fun sombreViolet() = capture(PaperTheme.SOMBRE, LiturgicalColor.VIOLET)
    @Test fun nuit() = capture(PaperTheme.NUIT, liturgicalColor = null)

    private fun capture(theme: PaperTheme, liturgicalColor: LiturgicalColor?) {
        compose.setContent { ParoissienTheme(theme) { FramedPageSample(liturgicalColor = liturgicalColor) } }
        compose.onRoot().captureRoboImage() // nommée d'après la classe et le test, dans src/test/screenshots
    }
}
