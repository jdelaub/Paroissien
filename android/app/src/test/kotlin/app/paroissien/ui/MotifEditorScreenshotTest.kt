package app.paroissien.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.sqlite.driver.AndroidSQLiteDriver
import app.paroissien.AppGraph
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.ParoissienTheme
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** L'atelier des motifs : la réserve ouverte (outil « Motifs »), puis le plein écran. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class MotifEditorScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun reserveEtPleinEcran() {
        val graph = AppGraph(RuntimeEnvironment.getApplication(), AndroidSQLiteDriver())
        compose.setContent { ParoissienTheme(PaperTheme.CLAIR) { MotifEditorScreen(graph, MotifEditorKey(), onBack = {}) } }
        compose.onNodeWithText("Motifs").performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/app.paroissien.ui.MotifEditorScreenshotTest.reserve.png")
        compose.onNodeWithText("Plein écran").performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/app.paroissien.ui.MotifEditorScreenshotTest.pleinEcran.png")
    }

    /** L'aperçu en plein écran d'une couverture de l'atelier (une fenêtre par-dessus l'écran). */
    @Test
    fun apercuCouverture() {
        val graph = AppGraph(RuntimeEnvironment.getApplication(), AndroidSQLiteDriver())
        compose.setContent { ParoissienTheme(PaperTheme.CLAIR) { CoverEditorScreen(graph, CoverEditorKey(), onBack = {}) } }
        compose.onNodeWithText("Voir en grand").performClick()
        compose.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/app.paroissien.ui.MotifEditorScreenshotTest.apercuCouverture.png")
    }
}
