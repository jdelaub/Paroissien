package app.paroissien.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

/** Quatre reliures : une bible de maroquin dorée, un livre de piété en veau, des exercices en toile, une lettre brochée. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w384dp-h700dp-xxhdpi")
class BookCoversScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val bible = BookIdentity(
        CoverStyle(Color(0xFF2A1416), CoverMaterial.MOROCCO, Tooling.GOLD, CoverMotif.CROSS, fillets = 3, corners = true, bands = 5),
        PaperStock(Color(0xFFF3E9CC), Color(0xFF2A2118), Color(0xFF5E5040), Color(0xFF9E2A1E), PaperWeight.BIBLE),
        EdgeFinish.GILT, listOf(0, 1, 2, 3), importance = 3, pages = 2800,
    )
    private val imitation = BookIdentity(
        CoverStyle(Color(0xFF5C3A22), CoverMaterial.CALF, Tooling.BLIND, CoverMotif.CROSS, fillets = 2, corners = true, bands = 4),
        PaperStock(Color(0xFFF8F1DF), Color(0xFF241C14), Color(0xFF5A4E40), Color(0xFFA31F1A), PaperWeight.FINE),
        EdgeFinish.RED, listOf(0, 1), importance = 2, pages = 420,
    )
    private val exercices = BookIdentity(
        CoverStyle(Color(0xFF1D1D20), CoverMaterial.CLOTH, Tooling.BLIND, CoverMotif.IHS, fillets = 1, corners = false, bands = 0, motifTooling = Tooling.GOLD),
        PaperStock(Color(0xFFFFFEFA), Color(0xFF101010), Color(0xFF444444), Color(0xFF8E1B16), PaperWeight.REGULAR),
        EdgeFinish.PLAIN, listOf(0, 5), importance = 1, pages = 160,
    )
    private val lettre = BookIdentity(
        CoverStyle(Color(0xFFEDE3CB), CoverMaterial.WRAPPER, Tooling.PRINTED, CoverMotif.CHI_RHO, fillets = 1, corners = false, bands = 0, ink = Color(0xFF7A1E1A)),
        PaperStock(Color(0xFFFFFDF7), Color(0xFF1A1A1A), Color(0xFF555555), Color(0xFF8E1B16), PaperWeight.THICK),
        EdgeFinish.PLAIN, listOf(0), importance = 0, pages = 12,
    )

    /** Les textures de l'atelier : chagrin, vélin, velours, toile de lin, papier marbré, et le maroquin pour comparer. */
    @Test
    fun texturesDeLAtelier() {
        fun book(color: Long, material: CoverMaterial, tooling: Tooling = Tooling.GOLD, motif: CoverMotif = CoverMotif.CROSS) = BookIdentity(
            CoverStyle(Color(color), material, tooling, motif, fillets = 2, corners = true, bands = 4, ink = Color(0xFF5E1519)),
            PaperStock(Color(0xFFF8F1DF), Color(0xFF241C14), Color(0xFF5A4E40), Color(0xFFA31F1A), PaperWeight.FINE),
            EdgeFinish.GILT, listOf(0), importance = 2, pages = 500,
        )
        val covers = listOf(
            book(0xFF1E3A2B, CoverMaterial.SHAGREEN) to "Chagrin",
            book(0xFFEDE3CB, CoverMaterial.VELLUM, Tooling.PRINTED, CoverMotif.CHI_RHO) to "Vélin",
            book(0xFF5E1519, CoverMaterial.VELVET, motif = CoverMotif.IHS) to "Velours",
            book(0xFF6B5A3A, CoverMaterial.LINEN, Tooling.BLIND) to "Toile de lin",
            book(0xFF1C2B40, CoverMaterial.MARBLED, motif = CoverMotif.NONE) to "Papier marbré",
            book(0xFF241416, CoverMaterial.MOROCCO) to "Maroquin",
        )
        compose.setContent {
            ParoissienTheme(PaperTheme.CLAIR) {
                Column(Modifier.background(Color(0xFFE9E1D0)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    covers.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { (identity, title) -> BookCover(identity, title, null, Modifier.size(112.dp, 160.dp)) }
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun reliures() {
        compose.setContent {
            ParoissienTheme(PaperTheme.CLAIR) {
                Column(Modifier.background(Color(0xFFE9E1D0)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        BookCover(bible, "La Sainte Bible", "Crampon", Modifier.size(160.dp, 230.dp))
                        BookCover(imitation, "Imitation de Jésus-Christ", null, Modifier.size(160.dp, 230.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        BookCover(exercices, "Exercices spirituels", "Saint Ignace", Modifier.size(160.dp, 230.dp))
                        BookCover(lettre, "Lettre de Clairval", "Mars 1996", Modifier.size(160.dp, 230.dp))
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage()
    }
}
