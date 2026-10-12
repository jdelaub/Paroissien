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

    /** « Composer au hasard » : une composition d'un même esprit, ici la graine 7, en vert. */
    @Test
    fun cadreAuHasard() {
        val def = requireNotNull(FrameComposer.build(FrameComposer.random(7, "perso-hasard", "Au hasard").copy(palette = "GREEN")))
        compose.setContent { ParoissienTheme(PaperTheme.CLAIR) { FramedPageSample(spec = def.spec, style = def.style) } }
        compose.onRoot().captureRoboImage()
    }

    /** Un motif dessiné : deux traits et un point, reflétés gauche-droite et haut-bas, lissés, sur un fond bleu nuit. */
    @Test
    fun motifDessine() {
        val motif = CustomMotif(
            id = "motif-essai", name = "Rinceau", period = 140f, mirrorU = true, mirrorV = true, ground = 0xFF2E4865,
            strokes = listOf(
                MotifStroke(listOf(8f, 50f, 22f, 26f, 44f, 18f, 62f, 34f, 58f, 50f), width = 6f, color = 0xFFB89C5A),
                MotifStroke(listOf(30f, 50f, 46f, 42f, 70f, 50f), width = 3f, color = 0xFFD3CEC1),
                MotifStroke(listOf(70f, 22f), width = 11f, color = 0xFFD8745A, dot = true),
            ),
        )
        FrameComposer.motifs = mapOf(motif.id to motif)
        try {
            val def = requireNotNull(FrameComposer.build(CustomFrameSpec("perso-m", "Mon motif", top = ModuleRef(motif.id, "top"), corner = ModuleRef("matin", "corner"), ground = motif.id, fillets = "rouge")))
            compose.setContent { ParoissienTheme(PaperTheme.CLAIR) { FramedPageSample(spec = def.spec, style = def.style) } }
            compose.onRoot().captureRoboImage()
        } finally {
            FrameComposer.motifs = emptyMap()
        }
    }
}
