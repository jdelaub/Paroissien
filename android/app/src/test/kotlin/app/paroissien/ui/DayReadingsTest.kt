package app.paroissien.ui

import app.paroissien.core.content.PackBlock
import app.paroissien.core.liturgy.CalendarConfig
import app.paroissien.core.liturgy.LiturgicalCalendars
import app.paroissien.core.liturgy.Rite
import app.paroissien.core.model.text.Inlines
import app.paroissien.core.reading.book.ReadingBlock
import java.time.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Les lectures du jour (1969) : les clés du lectionnaire que l'on essaie, et le début de l'Évangile. */
class DayReadingsTest {

    private val calendars = LiturgicalCalendars.load()
    private fun day(date: String) = calendars.day(LocalDate.parse(date), CalendarConfig(Rite.ROMAN_1969))

    @Test
    fun dimancheDuTempsOrdinaire() = assertEquals(
        listOf("jour-2026-10-18", "ordinary_time_29_sunday-a", "ordinary_time_29_sunday-ii", "ordinary_time_29_sunday"),
        lectionaryUnits(day("2026-10-18")),
    )

    /** Une mémoire : sa clé d'abord (lectures propres), puis celles de la férie qu'elle recouvre. */
    @Test
    fun memoireSurUneFerie() = assertEquals(
        listOf(
            "jour-2026-10-15",
            "teresa_of_jesus_of_avila_virgin-a", "teresa_of_jesus_of_avila_virgin-ii", "teresa_of_jesus_of_avila_virgin",
            "ordinary_time_28_thursday-a", "ordinary_time_28_thursday-ii", "ordinary_time_28_thursday",
        ),
        lectionaryUnits(day("2026-10-15")),
    )

    /** Une clé trop longue pour un identifiant : le même raccourci que la chaîne (CRC32, sources/aelf/build.py). */
    @Test
    fun cleRaccourcie() {
        assertEquals("ordinary_time_29_sunday-a", lectionaryUnitId("ordinary_time_29_sunday-a"))
        assertEquals(
            "cyril_constantine_the_philosoph-563d3b95",
            lectionaryUnitId("cyril_constantine_the_philosopher_monk_and_methodius_michael_of_thessaloniki_bishop"),
        )
    }

    private fun block(id: String, type: String, fr: String, style: String? = null) =
        PackBlock(id, type, JsonObject(listOfNotNull(style?.let { "style" to JsonPrimitive(it) }).toMap()), mapOf("fr" to Inlines.fromMarkup(fr)))

    private val ordinaire = listOf(
        ReadingBlock.Heading("premiere-lecture", Inlines.fromMarkup("Première lecture")),
        ReadingBlock.Slot("premiere-lecture.1", Inlines.fromMarkup("Première lecture — lectures du jour"), "lectionarium.lectio-1"),
        ReadingBlock.Heading("deuxieme-lecture", Inlines.fromMarkup("Deuxième lecture")),
        ReadingBlock.Slot("deuxieme-lecture.1", Inlines.fromMarkup("Deuxième lecture — lectures du jour"), "lectionarium.lectio-2", omit = listOf("deuxieme-lecture")),
        ReadingBlock.Versicle("deuxieme-lecture.2", Inlines.fromMarkup("℟ Nous rendons grâce à Dieu.")),
        ReadingBlock.Slot("evangile.1", Inlines.fromMarkup("Évangile — lectures du jour"), "lectionarium.evangelium"),
        ReadingBlock.Slot("collecte.1", Inlines.fromMarkup("Prière d'ouverture — dans le missel"), "missale.collecta"),
    )

    /** Une férie : la première lecture et l'Évangile à leur place ; pas de deuxième lecture, sa section disparaît. */
    @Test
    fun ordinaireRempliUnJourDeSemaine() {
        val u = "ordinary_time_28_thursday-ii"
        val lectures = listOf(
            block("$u.l1", "heading", "Première lecture"),
            block("$u.l1.1", "para", "Annonce de la première lecture (référence)", "small"),
            block("$u.l1.2", "para", "Texte de la première lecture."),
            block("$u.ev", "heading", "Évangile"),
            block("$u.ev.1", "para", "Texte de l'Évangile."),
        )
        val out = DayReadings.fill(ordinaire, u, "Jeudi de la 28e semaine", lectures)
        val ids = out.map { it.id }
        assertTrue(ids.toString(), "deuxieme-lecture" !in ids && "deuxieme-lecture.2" !in ids)
        assertEquals(listOf("premiere-lecture", "premiere-lecture.1.jour", "$u.l1.1", "$u.l1.2", "$u.ev.1", "collecte.1"), ids)
    }

    /** Noël : quatre messes ; ce sont les lectures de la dernière (messe du jour) qui prennent place. */
    @Test
    fun ordinaireAvecLaDerniereMesse() {
        val u = "nativity_of_the_lord"
        val lectures = listOf(
            block("$u.m1.l1", "para", "Is 62 (veille au soir)"),
            block("$u.m2.l1", "para", "Is 9 (nuit)"),
            block("$u.m4.l1", "para", "Is 52 (jour)"),
            block("$u.m4.l2", "para", "He 1 (jour)"),
            block("$u.m4.ev", "para", "Jn 1 (jour)"),
        )
        val out = DayReadings.fill(ordinaire, u, "Nativité du Seigneur", lectures)
        assertEquals(listOf("$u.m4.l1", "$u.m4.l2", "deuxieme-lecture.2", "$u.m4.ev"), out.map { it.id }.filter { it.startsWith(u) || it == "deuxieme-lecture.2" })
    }

    /** Deux messes (procession et Passion) : l'Évangile de la dernière, sa ligne d'annonce, sa phrase-titre et son début. */
    @Test
    fun evangileDeLaDerniereMesse() {
        val u = "palm_sunday_of_the_passion_of_the_lord-b"
        val blocks = listOf(
            block(u, "heading", "Rameaux"),
            block("$u.m1", "heading", "Procession des Rameaux"),
            block("$u.m1.ev", "heading", "Évangile"),
            block("$u.m1.ev.1", "para", "Annonce de l'Évangile de la procession (référence)", "small"),
            block("$u.m1.ev.2", "para", "Premier paragraphe de l'Évangile de la procession."),
            block("$u.m2", "heading", "Messe de la Passion"),
            block("$u.m2.ev", "heading", "Évangile"),
            block("$u.m2.ev.1", "para", "Annonce de la Passion (référence)", "small"),
            block("$u.m2.ev.2", "para", "« Phrase-titre de la Passion »", "centered"),
            block("$u.m2.ev.3", "para", "Premier paragraphe de la Passion."),
            block("$u.m2.ev2", "heading", "Évangile (au choix)"),
        )
        val g = lectionaryGospel(blocks, u)!!
        assertEquals("$u.m2.ev", g.block)
        assertEquals("Annonce de la Passion (référence)", g.incipit)
        assertEquals("« Phrase-titre de la Passion » Premier paragraphe de la Passion.", g.opening)
    }
}
