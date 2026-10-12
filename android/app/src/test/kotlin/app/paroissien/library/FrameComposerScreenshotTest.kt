package app.paroissien.library

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.ParoissienTheme
import app.paroissien.core.designsystem.frame.FramedPageSample
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Un cadre composé dans l'atelier : frise du matin, côtés de la vigne au calice, coins gothiques, fond du soir. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class FrameComposerScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun cadreCompose() {
        val def = requireNotNull(
            FrameComposer.build(
                CustomFrameSpec(
                    id = "perso-essai", name = "Mon cadre",
                    top = ModuleRef("matin", "top"), side = ModuleRef("mariage", "side"), bottom = ModuleRef("soir", "bottom"),
                    corner = ModuleRef("gothique", "corner"), center = ModuleRef("soir", "topCenter"),
                    ground = "soir", fillets = "cadre", depth = 22, bottomDepth = 32,
                ),
            ),
        )
        compose.setContent { ParoissienTheme(PaperTheme.CLAIR) { FramedPageSample(spec = def.spec, style = def.style) } }
        compose.onRoot().captureRoboImage()
    }
}
