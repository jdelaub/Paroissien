package app.paroissien.library

import androidx.compose.ui.graphics.Color
import app.paroissien.core.designsystem.RibbonEmblem
import app.paroissien.core.designsystem.RibbonEnd
import app.paroissien.core.designsystem.RibbonFabric
import app.paroissien.core.designsystem.RibbonStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Les rubans de l'atelier : gardés en JSON, appliqués par emplacement, et sans effet tant qu'on n'a rien retouché. */
class RibbonChoicesTest {

    @Test
    fun aucuneRetoucheLesRubansLiturgiques() {
        assertEquals("", RibbonChoices.encode(List(6) { RibbonChoice() }))
        assertEquals(RibbonStyle.slots, RibbonChoices.styles(""))
    }

    @Test
    fun unRubanRetouche() {
        val text = RibbonChoices.encode(listOf(RibbonChoice(), RibbonChoice(argb = 0xFF2F5D9E, label = "bleu marial", fabric = "MOIRE", end = "FORKED", emblem = "NONE")))
        val styles = RibbonChoices.styles(text)
        assertEquals(RibbonStyle.slots[0], styles[0])
        assertEquals("bleu marial", styles[1].label)
        assertEquals(Color(0xFF2F5D9E), styles[1].custom)
        assertEquals(RibbonFabric.MOIRE, styles[1].fabric)
        assertEquals(RibbonEnd.FORKED, styles[1].end)
        assertEquals(RibbonEmblem.NONE, styles[1].emblem)
        assertEquals(RibbonStyle.slots[2], styles[2])
    }

    /** Un nom inconnu (version future) est ignoré : le ruban garde sa valeur d'origine. */
    @Test
    fun nomInconnu() {
        val style = RibbonStyle.slots[3].with(RibbonChoice(fabric = "SOIE_SAUVAGE", end = "TRIPLE"))
        assertNull(style.fabric)
        assertEquals(RibbonEnd.BIAS, style.end)
    }
}
