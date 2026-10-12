package app.paroissien.library

import androidx.compose.ui.graphics.Color
import app.paroissien.core.designsystem.CoverMaterial
import app.paroissien.core.designsystem.EdgeFinish
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** La reliure retouchée dans l'atelier : chaque champ choisi remplace celui du livre, les autres restent. */
class BindingChoiceTest {

    private val book = BookIdentities.letter(pages = 40)

    @Test
    fun emptyChoiceKeepsTheBook() {
        assertSame(book, book.dressed(null))
        assertSame(book, book.dressed(BindingChoice()))
    }

    @Test
    fun chosenFieldsReplaceTheBook() {
        val dressed = book.dressed(BindingChoice(leather = 0xFF5E1519, material = "MOROCCO", edge = "GILT", ribbons = listOf(0, 3)))
        assertEquals(Color(0xFF5E1519), dressed.cover.leather)
        assertEquals(CoverMaterial.MOROCCO, dressed.cover.material)
        assertEquals(EdgeFinish.GILT, dressed.edges)
        assertEquals(listOf(0, 3), dressed.ribbons)
        assertEquals(book.cover.motif, dressed.cover.motif)
        assertEquals(book.paper, dressed.paper)
    }

    @Test
    fun unknownNamesAreIgnored() {
        val dressed = book.dressed(BindingChoice(material = "PARCHEMIN", edge = "ARGENT"))
        assertEquals(book.cover.material, dressed.cover.material)
        assertEquals(book.edges, dressed.edges)
    }

    @Test
    fun choiceSurvivesItsJson() {
        val choice = BookDressChoice(binding = BindingChoice(motif = "IHS", endpaper = BindingChoice.SILK))
        assertEquals(choice, BookDressChoice.decode(BookDressChoice.encode(choice)))
    }
}
