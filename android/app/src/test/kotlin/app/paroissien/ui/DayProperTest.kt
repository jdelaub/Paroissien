package app.paroissien.ui

import app.paroissien.core.content.PackBlock
import app.paroissien.core.model.text.Inlines
import app.paroissien.core.reading.book.ReadingBlock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** La messe du jour assemblée dans l'Ordinaire : pièces du propre, règle du Gloria, préface du jour, commémoraison. */
class DayProperTest {

    private fun t(s: String) = Inlines.fromMarkup(s)
    private fun pack(id: String, type: String, la: String? = null, fr: String? = null, vararg attrs: Pair<String, Any>) = PackBlock(
        id, type,
        JsonObject(attrs.associate { (k, v) -> k to if (v is Boolean) JsonPrimitive(v) else JsonPrimitive(v.toString()) }),
        listOfNotNull(la?.let { "la" to t(it) }, fr?.let { "fr" to t(it) }).toMap(),
    )

    private val ordinary = listOf(
        ReadingBlock.Rubric("introit.1", t("Le prêtre lit l'Introït.")),
        ReadingBlock.Slot("introit.propre", t("Introït — texte de la messe du jour"), "proprium.introitus"),
        ReadingBlock.Rubric("gloria.1", t("Puis il dit le Gloria, quand la messe le demande.")),
        ReadingBlock.Slot("gloria.regle", t("Gloria — selon la messe du jour"), "proprium.gloria", omit = listOf("gloria.2")),
        ReadingBlock.Paragraph("gloria.2", t("Glória in excélsis Deo."), app.paroissien.core.reading.book.BlockLang.LA),
        ReadingBlock.Slot("collecte.propre", t("Collecte — texte de la messe du jour"), "proprium.oratio"),
        ReadingBlock.Slot("preface.propre", t("Préface — préface de la messe du jour"), "proprium.praefatio", replaces = listOf("preface-commune")),
        ReadingBlock.Heading("preface-commune", t("Préface commune")),
        ReadingBlock.Paragraph("preface-commune.1", t("Vere dignum…"), app.paroissien.core.reading.book.BlockLang.LA),
        ReadingBlock.Slot("te-igitur.communicantes", t("Communicantes propre du jour"), "proprium.communicantes", replaces = listOf("te-igitur.7")),
        ReadingBlock.Paragraph("te-igitur.7", t("Communicántes…"), app.paroissien.core.reading.book.BlockLang.LA),
    )

    private val day = DayProper.Day(
        mass = DayProper.Formulary(
            "tempora-adv1-0", "Premier dimanche de l'Avent",
            listOf(
                pack("tempora-adv1-0.introit", "heading", "Introitus", "Introït"),
                pack("tempora-adv1-0.introit.1", "para", "Ad te levávi ánimam meam", "Vers vous j'ai élevé mon âme"),
                pack("tempora-adv1-0.gloria", "rubric", null, "On ne dit pas le Gloria in excelsis.", "gloria" to false),
                pack("tempora-adv1-0.collecte", "heading", "Oratio", "Collecte"),
                pack("tempora-adv1-0.collecte.1", "para", "Excita, quǽsumus, Dómine, poténtiam tuam", null),
                pack("tempora-adv1-0.preface", "heading", "Præfatio", "Préface"),
                pack("tempora-adv1-0.preface.1", "rubric", null, "Préface de l'Avent.", "preface" to "adv"),
            ),
        ),
        commemorations = listOf(
            DayProper.Formulary("sancti-12-01cc", "Commémoraison de saint Éloi", listOf(
                pack("sancti-12-01cc.collecte", "heading", "Oratio", "Collecte"),
                pack("sancti-12-01cc.collecte.1", "para", "Deus, qui beátum Elígium…", null),
                pack("sancti-12-01cc.collecte.2", "rubric", null, "Commémoraison d'un autre saint"),
                pack("sancti-12-01cc.collecte.3", "para", "Oraison portée par ce formulaire", null),
            )),
            // Commémoraison attachée à la messe elle-même : déjà dans son formulaire.
            DayProper.Formulary("tempora-adv1-0", "Premier dimanche de l'Avent", emptyList()),
        ),
        prefaces = listOf(
            pack("prefaces.adv", "heading", "Præfatio de Adventu", "Préface de l'Avent"),
            pack("prefaces.adv.1", "para", "Vere dignum et justum est… qui salútem humáni géneris", null),
        ),
        primaryLanguage = "la",
    )

    private fun ids(blocks: List<ReadingBlock>) = blocks.map { it.id }

    @Test
    fun theDaysMassFillsTheOrdinary() {
        val out = DayProper.fill(ordinary, day, "fr", facing = false)
        val text = out.joinToString("\n") { b ->
            when (b) {
                is ReadingBlock.Slot -> "[" + Inlines.plainText(b.text) + "]"
                is ReadingBlock.Paragraph -> Inlines.plainText(b.text)
                is ReadingBlock.Rubric -> Inlines.plainText(b.text)
                else -> b.id
            }
        }
        assertTrue(text, text.contains("[Introït — Premier dimanche de l'Avent]\nVers vous j'ai élevé mon âme"))
        assertTrue("règle du Gloria : $text", text.contains("On ne dit pas le Gloria"))
        assertFalse("le Gloria n'est pas dit ce jour-là", "gloria.2" in ids(out))
        assertTrue("commémoraison après la collecte : $text", text.contains("Excita") && text.indexOf("Commémoraison : Commémoraison de saint Éloi") > text.indexOf("Excita"))
        assertFalse("une messe prise en commémoraison ne donne que sa propre oraison : $text", text.contains("Oraison portée"))
        assertEquals("pas de doublon de la messe elle-même", 1, Regex("Commémoraison : ").findAll(text).count())
        assertTrue("préface du jour en entier : $text", text.contains("[Préface de l'Avent]") && text.contains("salútem humáni"))
        assertFalse("la préface commune s'efface", ids(out).any { it.startsWith("preface-commune") })
        assertTrue("sans Communicantes propre, le texte commun reste", "te-igitur.7" in ids(out))
        assertFalse("l'encadré du Communicantes disparaît", "te-igitur.communicantes" in ids(out))
    }

    @Test
    fun withoutMissalTheBoxesStay() {
        val out = DayProper.fill(ordinary, null, "fr", facing = false)
        assertTrue("introit.propre" in ids(out))
        assertTrue("gloria.2" in ids(out))
        assertFalse("les encadrés facultatifs disparaissent", "gloria.regle" in ids(out) || "te-igitur.communicantes" in ids(out))
        assertEquals(ordinary.size - 2, out.size)
    }
}
