package app.paroissien.core.designsystem

import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import app.paroissien.core.liturgy.LiturgicalColor
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Les six rubans, en tissu mat à côtes ou en satin : en long, en queue, et couchés en travers d'une page. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w384dp-h600dp-xxhdpi")
class RibbonsScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test fun clair() = capture(PaperTheme.CLAIR)
    @Test fun sombre() = capture(PaperTheme.SOMBRE)
    @Test fun satin() = capture(PaperTheme.CLAIR, RibbonFabric.SATIN)

    /** Le ruban de la messe du jour, dans chaque couleur liturgique : coupé droit, serré d'un ferret doré. */
    @Test fun messeDuJour() {
        compose.setContent {
            ParoissienTheme(PaperTheme.CLAIR) {
                val paper = LocalPaperColors.current
                Row(Modifier.background(paper.background).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LiturgicalColor.entries.forEach { color ->
                        Canvas(Modifier.size(width = 30.dp, height = 220.dp)) { drawRibbon(RibbonStyle.mass(color), paper.isDark, Offset.Zero, size) }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage()
    }

    /** Couchés en travers d'une page de texte : ondulations légères, ombre douce, extrémité en biais. */
    @Test fun moire() = capture(PaperTheme.CLAIR, RibbonFabric.MOIRE)
    @Test fun velours() = capture(PaperTheme.CLAIR, RibbonFabric.VELVET)

    /** L'atelier : teintes libres, chaque tissu, chaque extrémité, sans broderie pour l'un. */
    @Test
    fun atelier() {
        val styles = listOf(
            RibbonStyle(LiturgicalColor.WHITE, "bleu marial", 5, custom = Color(0xFF2F5D9E), fabric = RibbonFabric.MOIRE, end = RibbonEnd.FORKED),
            RibbonStyle(LiturgicalColor.RED, "bordeaux", 4, custom = Color(0xFF6E1423), fabric = RibbonFabric.VELVET, end = RibbonEnd.STRAIGHT),
            RibbonStyle(LiturgicalColor.GOLD, "or", 6, custom = Color(0xFFC9A227), fabric = RibbonFabric.SATIN, end = RibbonEnd.FERRET),
            RibbonStyle(LiturgicalColor.WHITE, "ivoire", 7, custom = Color(0xFFF1E6C8), fabric = RibbonFabric.TEXTURED, end = RibbonEnd.BIAS, emblem = RibbonEmblem.NONE),
            RibbonStyle(LiturgicalColor.GREEN, "vert sapin", 3, custom = Color(0xFF1F4D3A), fabric = RibbonFabric.TEXTURED, end = RibbonEnd.FORKED),
            RibbonStyle(LiturgicalColor.BLACK, "nuit", 3, custom = Color(0xFF0F2A44), fabric = RibbonFabric.MOIRE, end = RibbonEnd.BIAS),
        )
        compose.setContent {
            ParoissienTheme(PaperTheme.SEPIA) {
                val paper = LocalPaperColors.current
                Row(Modifier.background(paper.background).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    styles.forEach { style ->
                        Canvas(Modifier.size(width = 34.dp, height = 300.dp)) { drawRibbon(style, paper.isDark, Offset.Zero, size) }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage()
    }

    @Test fun enTravers() = across(RibbonFabric.TEXTURED)
    @Test fun enTraversSatin() = across(RibbonFabric.SATIN)

    private fun across(fabric: RibbonFabric) {
        compose.setContent {
            ParoissienTheme(PaperTheme.CLAIR) {
                val paper = LocalPaperColors.current
                Canvas(Modifier.fillMaxSize().background(paper.background)) {
                    var y = 40f
                    while (y < size.height - 60f) {
                        drawLine(paper.inkTertiary.copy(alpha = 0.5f), Offset(60f, y), Offset(size.width - 60f, y), strokeWidth = 9f)
                        y += 46f
                    }
                    val w = size.width
                    val h = size.height
                    fun curve(x0: Float, x3: Float) = List(49) { k ->
                        val u = k / 48f
                        val v = 1 - u
                        Offset(x0 * (v * v * v) + (x0 + w * 0.03f) * (3 * v * v * u) + (x3 - w * 0.14f) * (3 * v * u * u) + x3 * (u * u * u), -10f * v * v * v + h * 0.32f * 3 * v * v * u + h * 0.68f * 3 * v * u * u + (h - 20f) * u * u * u)
                    }
                    drawRibbonAlong(RibbonStyle.slots[0], false, curve(50f, w * 0.5f), 16.dp.toPx(), fabric = fabric)
                    drawRibbonAlong(RibbonStyle.slots[3], false, curve(100f, w * 0.62f), 16.dp.toPx(), phase = 1f, fabric = fabric)
                    drawRibbonAlong(RibbonStyle.slots[2], false, List(2) { Offset(w - 30f, -10f + (h + 20f) * it) }, 11.dp.toPx(), ripple = 0.45f, phase = 2.4f, fabric = fabric)
                }
            }
        }
        compose.onRoot().captureRoboImage()
    }

    private fun capture(theme: PaperTheme, fabric: RibbonFabric = RibbonFabric.TEXTURED) {
        compose.setContent {
            ParoissienTheme(theme) {
                val paper = LocalPaperColors.current
                Column(Modifier.background(paper.background).padding(24.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        RibbonStyle.slots.forEach { style ->
                            Canvas(Modifier.size(width = 34.dp, height = 300.dp)) { drawRibbon(style, paper.isDark, Offset.Zero, size, fabric) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        RibbonStyle.slots.forEach { style ->
                            Canvas(Modifier.size(width = 22.dp, height = 40.dp)) { drawRibbon(style, paper.isDark, Offset.Zero, size, fabric) }
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage()
    }
}
