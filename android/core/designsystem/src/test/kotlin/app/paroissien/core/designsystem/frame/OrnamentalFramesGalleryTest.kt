package app.paroissien.core.designsystem.frame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.ParoissienTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tous les cadres du registre, chacun sur une page de téléphone en thème clair, aux couleurs du livre ; les cadres
 * partiels aussi en verso (côté extérieur à gauche). Puis toutes les gardes à motif.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class OrnamentalFramesGalleryTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun galerie() {
        var index by mutableIntStateOf(0)
        var recto by mutableStateOf(true)
        compose.setContent {
            val def = OrnamentalFrames[index]
            ParoissienTheme(PaperTheme.CLAIR) { FramedPageSample(spec = def.spec, style = def.style, outerOnRight = recto) }
        }
        OrnamentalFrames.forEachIndexed { i, def ->
            index = i
            for (side in if (def.spec.bands.full) listOf(true) else listOf(true, false)) {
                recto = side
                compose.waitForIdle()
                val suffix = if (side) "" else ".verso"
                compose.onRoot().captureRoboImage("src/test/screenshots/app.paroissien.core.designsystem.frame.galerie.${def.name}$suffix.png")
            }
        }
    }

    @Test
    fun gardes() {
        var index by mutableIntStateOf(0)
        compose.setContent {
            ParoissienTheme(PaperTheme.CLAIR) { EndpaperPatterns.getOrNull(index)?.let { PatternPaper(it) } }
        }
        EndpaperPatterns.forEachIndexed { i, def ->
            index = i
            compose.waitForIdle()
            compose.onRoot().captureRoboImage("src/test/screenshots/app.paroissien.core.designsystem.frame.garde.${def.name}.png")
        }
    }
}
