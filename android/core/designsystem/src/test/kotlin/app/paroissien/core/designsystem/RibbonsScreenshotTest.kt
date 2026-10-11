package app.paroissien.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Les six rubans de satin : en long (couchés dans le livre) et en queue (dans la bande sous le texte). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w384dp-h600dp-xxhdpi")
class RibbonsScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test fun clair() = capture(PaperTheme.CLAIR)
    @Test fun sombre() = capture(PaperTheme.SOMBRE)

    private fun capture(theme: PaperTheme) {
        compose.setContent {
            ParoissienTheme(theme) {
                val paper = LocalPaperColors.current
                Column(Modifier.background(paper.background).padding(24.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        RibbonStyle.slots.forEach { style ->
                            Canvas(Modifier.size(width = 34.dp, height = 300.dp)) { drawRibbon(style, paper.isDark, Offset.Zero, size) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        RibbonStyle.slots.forEach { style ->
                            Canvas(Modifier.size(width = 22.dp, height = 40.dp)) { drawRibbon(style, paper.isDark, Offset.Zero, size) }
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage()
    }
}
