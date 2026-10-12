package app.paroissien.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import app.paroissien.core.designsystem.BookFace
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.ParoissienTheme
import app.paroissien.core.designsystem.rememberBookFonts
import app.paroissien.core.reading.book.Anchor
import app.paroissien.core.reading.book.BookReader
import app.paroissien.core.reading.book.BookTypography
import app.paroissien.core.reading.book.RibbonMark
import app.paroissien.core.reading.book.CardMark
import app.paroissien.core.reading.book.FrameLook
import app.paroissien.core.designsystem.frame.MatinFrame
import app.paroissien.core.designsystem.frame.MatinStyle
import app.paroissien.core.designsystem.DoublurePage
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import app.paroissien.library.BookIdentities
import app.paroissien.core.reading.book.BookLook
import app.paroissien.core.designsystem.PaperWeight
import app.paroissien.core.designsystem.EdgeFinish
import app.paroissien.core.designsystem.BookIdentity
import app.paroissien.core.designsystem.BookCover
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.fillMaxSize
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext

/** Le lecteur sur le Galaxy Z Flip6 (livre de démonstration) : latin en portrait, Ordinaire en double page, thème sombre. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BookReaderScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val book = demoBook()

    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun latinLettrinePortrait() = capture(Anchor("lat.1", 0), BookFace.GARAMOND, PaperTheme.CLAIR)

    @Test
    @Config(sdk = [35], qualifiers = "w939dp-h384dp-land-xxhdpi")
    fun ordinaireDoublePagePaysage() = capture(Anchor("ord.1", 0), BookFace.GARAMOND, PaperTheme.SEPIA)

    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun gloriaLiterataSombre() = capture(Anchor("ord.2", 0), BookFace.LITERATA, PaperTheme.SOMBRE)

    /** Rubans : l'un couché sur la page, les autres pendant dans la bande réservée sous le texte. */
    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun rubansDansLaBande() = capture(
        Anchor("lat.1", 0), BookFace.GARAMOND, PaperTheme.CLAIR,
        ribbons = listOf(RibbonMark(0, Anchor("lat.1", 0)), RibbonMark(1, null, elsewhere = -1), RibbonMark(2, null, elsewhere = 1), RibbonMark(3, null, elsewhere = 1)),
    )

    /** Le ruban couché en travers de la page, comme dans un vrai livre ; les autres pendent dans la bande. */
    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun rubanEnTravers() = capture(
        Anchor("lat.1", 0), BookFace.GARAMOND, PaperTheme.CLAIR,
        ribbons = listOf(RibbonMark(2, Anchor("lat.1", 0)), RibbonMark(0, null, elsewhere = -1), RibbonMark(3, null, elsewhere = 1)),
        across = true,
    )

    /** Une image pieuse posée sur sa page : la page vaut φ fois l'image. */
    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun imageSurLaPage() = capture(
        Anchor("lat.1", 0), BookFace.GARAMOND, PaperTheme.CLAIR,
        cards = listOf(CardMark("essai", Anchor("lat.1", 0), image = null, label = "Essai", picture = syntheticCard())),
    )

    /** Une carte dessinée pour l'essai : fond dégradé, filet doré, croix. Aucune image réelle dans le dépôt. */
    private fun syntheticCard(): ImageBitmap {
        val bitmap = createBitmap(560, 960)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.shader = android.graphics.LinearGradient(0f, 0f, 0f, 960f, 0xFF2D4A7A.toInt(), 0xFFC9B27C.toInt(), android.graphics.Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 560f, 960f, paint)
        paint.shader = null
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 10f
        paint.color = 0xFFD8B65A.toInt()
        canvas.drawRect(24f, 24f, 536f, 936f, paint)
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawRect(262f, 220f, 298f, 700f, paint)
        canvas.drawRect(170f, 340f, 390f, 376f, paint)
        return bitmap.asImageBitmap()
    }

    /** Une page dans le cadre des prières du matin : titre courant et folio au-dessus, rubans dessous. */
    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun pageEncadree() = capture(
        Anchor("lat.1", 0), BookFace.GARAMOND, PaperTheme.CLAIR,
        ribbons = listOf(RibbonMark(0, null, elsewhere = 1)),
        look = BookLook(frame = FrameLook(MatinFrame, MatinStyle)),
    )

    /** La doublure du plat : soie moirée dans sa roulette dorée. */
    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun doublure() {
        compose.setContent { ParoissienTheme(PaperTheme.CLAIR) { DoublurePage() } }
        compose.onRoot().captureRoboImage()
    }

    private val bible = BookIdentities.of("bible.crampon", null, uniform = false).copy(pages = 2800)

    /** La Bible : la couverture de maroquin, page d'avant la première. */
    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun couvertureDeLaBible() = capture(
        null, BookFace.GARAMOND, PaperTheme.CLAIR, identity = bible,
        look = BookLook(EdgeFinish.GILT, PaperWeight.BIBLE, cover = { BookCover(bible, "La Sainte Bible", "Crampon", Modifier.fillMaxSize(), flat = true) }),
    )

    /** Papier bible : ivoire et fin, tranche dorée épaisse, le verso transparaît à peine. */
    @Test
    @Config(sdk = [35], qualifiers = "w384dp-h939dp-xxhdpi")
    fun papierBible() = capture(
        Anchor("ord.1", 0), BookFace.GARAMOND, PaperTheme.CLAIR, identity = bible,
        look = BookLook(EdgeFinish.GILT, PaperWeight.BIBLE, showThrough = true, progress = 0.2f..0.25f, thickness = 8.dp),
    )

    private fun capture(
        anchor: Anchor?,
        face: BookFace,
        theme: PaperTheme,
        ribbons: List<RibbonMark> = emptyList(),
        identity: BookIdentity? = null,
        look: BookLook = BookLook(),
        across: Boolean = false,
        cards: List<CardMark> = emptyList(),
    ) {
        var ready by mutableStateOf(false)
        compose.setContent {
            ParoissienTheme(theme) {
                val fonts = rememberBookFonts(face)
                val themePaper = LocalPaperColors.current
                val paper = identity?.paper?.colors(themePaper) ?: themePaper
                CompositionLocalProvider(LocalPaperColors provides paper) {
                BookReader(
                    blocks = book,
                    typography = remember(fonts, paper) { BookTypography(fonts, paper) },
                    anchor = anchor,
                    onAnchorChange = {},
                    onPaginated = { ready = true },
                    paginationContext = EmptyCoroutineContext,
                    ribbons = ribbons,
                    ribbonsAcross = across,
                    cards = cards,
                    cardsOnPage = cards.isNotEmpty(),
                    look = look,
                )
                }
            }
        }
        compose.waitUntil(timeoutMillis = 60_000) { ready }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage()
    }
}
