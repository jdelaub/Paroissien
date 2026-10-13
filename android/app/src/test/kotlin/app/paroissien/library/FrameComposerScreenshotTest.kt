package app.paroissien.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
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

    /** La réserve de l'atelier des motifs : toutes les silhouettes, puis les formes au trait. */
    @Test
    fun reserveDeMotifs() {
        val colors = listOf(Color(0xFFB0332B), Color(0xFF4F7A57), Color(0xFF2F4E80), Color(0xFF8A6A1C))
        compose.setContent {
            ParoissienTheme(PaperTheme.CLAIR) {
                Column(Modifier.background(Color(0xFFFBF8F1)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val tiles = MotifStamps.all.map { it.figure to it.line } + MotifShape.entries.map { it.figure to true }
                    tiles.chunked(7).forEachIndexed { row, chunk ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            chunk.forEachIndexed { i, (figure, line) ->
                                val path = PathParser().parsePathString(figure.normalized().svg()).toPath()
                                val color = colors[(row + i) % colors.size]
                                Canvas(Modifier.size(44.dp)) {
                                    val k = size.minDimension
                                    translate(size.width / 2, size.height / 2) {
                                        scale(k, k, pivot = Offset.Zero) {
                                            if (line) drawPath(path, color, style = Stroke(0.06f)) else drawPath(path, color)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage()
    }

    /** Un motif composé : fraise et feuille de la réserve, losange et volute d'or, un trait à main levée ; en miroir. */
    @Test
    fun motifCompose() {
        val motif = CustomMotif(
            id = "motif-compose", name = "Fraisier", period = 150f, mirrorU = true,
            strokes = listOf(
                MotifStroke(listOf(10f, 70f, 40f, 50f, 75f, 62f), width = 2.5f, color = 0xFF5C4A3A),
                MotifStroke(listOf(38f, 34f), width = 4f, color = 0xFF4F7A57, stamp = "feuille", sx = 13f, sy = 13f, rotation = -20f),
                MotifStroke(listOf(30f, 66f), width = 4f, color = 0xFFB0332B, stamp = "fraise", sx = 15f, sy = 15f, rotation = 10f),
                MotifStroke(listOf(75f, 30f), width = 3f, color = 0xFFC9A24A, shape = "LOSANGE", sx = 7f, sy = 11f, metal = "GOLD"),
                MotifStroke(listOf(60f, 80f), width = 2.4f, color = 0xFFC9A24A, stamp = "volute", sx = 9f, sy = 9f, metal = "GOLD"),
                MotifStroke(listOf(12f, 30f), width = 2f, color = 0xFF2F4E80, shape = "CERCLE", sx = 6f, sy = 6f, filled = false),
            ),
        )
        FrameComposer.motifs = mapOf(motif.id to motif)
        try {
            val def = requireNotNull(FrameComposer.build(CustomFrameSpec("perso-c", "Fraisier", top = ModuleRef(motif.id, "top"), depth = 30, bottomDepth = 30)))
            compose.setContent { ParoissienTheme(PaperTheme.CLAIR) { FramedPageSample(spec = def.spec, style = def.style) } }
            compose.onRoot().captureRoboImage()
        } finally {
            FrameComposer.motifs = emptyMap()
        }
    }
}
