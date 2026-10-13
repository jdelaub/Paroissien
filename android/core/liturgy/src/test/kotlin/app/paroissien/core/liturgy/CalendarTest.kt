// SPDX-License-Identifier: Apache-2.0
package app.paroissien.core.liturgy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.Year

/** Tests G1, G2 et G4 du rapport 05 (§3.1) : invariants, 70 types d'année, cas sourcés écrits à la main. */
class CalendarTest {

    private val c1969 by lazy { Calendar1969(SanctoralData.load(Rite.ROMAN_1969)) }
    private val c1962 by lazy {
        assumeTrue("sanctoral 1962 absent", javaClass.classLoader.getResource("calendar/1962/general.json") != null)
        Calendar1962(SanctoralData.load(Rite.ROMAN_1962), FrenchExternalSolemnities)
    }
    private val calendars by lazy {
        assumeTrue("sanctoral 1962 absent", javaClass.classLoader.getResource("calendar/1962/general.json") != null)
        LiturgicalCalendars.load()
    }

    private fun d(iso: String) = LocalDate.parse(iso)

    /** Une année par type : date de Pâques (35) × bissextile ou non (2) = 70 (rapport 05, §2.5). */
    private val yearTypes: List<Int> by lazy {
        val seen = LinkedHashMap<Pair<MonthDay, Boolean>, Int>()
        for (y in 1583..9999) {
            seen.putIfAbsent(MonthDay.from(Computus.easter(y)) to Year.isLeap(y.toLong()), y)
            if (seen.size == 70) break
        }
        assertEquals("70 types d'année", 70, seen.size)
        seen.values.toList()
    }

    // ── G1 et G2 : invariants sur les 70 types d'année ──────────────────────

    @Test
    fun invariants1969OnTheSeventyYearTypes() {
        for (y in yearTypes) {
            val days = daysOf(y).map { c1969.day(it) }.toList()
            assertEquals(Year.of(y).length(), days.size)
            days.forEach { day ->
                val label = "${day.date} ${day.principal.id}"
                assertTrue("un dimanche n'a jamais une férie : $label", !day.date.isSunday() || day.principal.rank != Rank.R1969.WEEKDAY)
                assertTrue("une mémoire facultative n'est jamais célébration principale : $label", day.principal.rank != Rank.R1969.OPTIONAL_MEMORIAL)
                assertTrue("semaine du psautier : $label", day.cycles!!.psalterWeek in 1..4)
            }
            // Chaque solennité du sanctoral est célébrée, à sa date ou transférée : aucune n'est omise (NU 60).
            val solemnities = c1969Solemnities(y)
            val celebrated = days.map { it.principal.id.value }.toSet()
            solemnities.forEach { assertTrue("solennité absente en $y : $it", it in celebrated) }
            assertEquals("Pâques $y", "easter_sunday", c1969.day(Computus.easter(y)).principal.id.value)
            assertEquals("Noël $y", "nativity_of_the_lord", c1969.day(LocalDate.of(y, 12, 25)).principal.id.value)
        }
    }

    private fun c1969Solemnities(y: Int) = SanctoralData.load(Rite.ROMAN_1969).year(y).values.flatten()
        .filter { it.entry.rank == "SOLEMNITY" && it.entry.id != "nativity_of_the_lord" }.map { it.entry.id }

    @Test
    fun invariants1962OnTheSeventyYearTypes() {
        for (y in yearTypes) {
            val days = daysOf(y).map { c1962.day(it) }.toList()
            days.forEach { day ->
                val label = "${day.date} ${day.principal.id}"
                assertTrue("un dimanche reste un dimanche ou une fête de Iʳᵉ ou IIᵉ classe : $label", !day.date.isSunday() || day.principal.rank in setOf(Rank.R1962.CLASS_I, Rank.R1962.CLASS_II))
                assertTrue("au plus deux commémoraisons : $label", day.commemorations.size <= 2)
            }
            // 12 jours de Quatre-Temps par an : Avent, Carême, Pentecôte, septembre.
            assertEquals("Quatre-Temps $y", 12, days.count { DayFlag.EMBER_DAY in it.flags })
            assertEquals("Pâques $y", "easter_sunday", c1962.day(Computus.easter(y)).principal.id.value)
            assertEquals("Christ-Roi $y", "our_lord_jesus_christ_king_of_the_universe", c1962.day(LocalDate.of(y, 10, 31).sundayOnOrBefore()).principal.id.value)
        }
    }

    // ── G4 : cas sourcés, 1969 (France) ─────────────────────────────────────

    @Test
    fun johnTheBaptistAnticipatedIn2022() {
        // Le Sacré-Cœur tombe le 24 juin 2022 : la Nativité de saint Jean-Baptiste est avancée au 23 (Dicastère, AELF).
        assertEquals("most_sacred_heart_of_jesus", c1969.day(d("2022-06-24")).principal.id.value)
        val john = c1969.day(d("2022-06-23")).principal
        assertEquals("nativity_of_john_the_baptist", john.id.value)
        assertEquals(d("2022-06-24"), john.transferredFrom)
    }

    @Test
    fun immaculateConceptionMovedToMondayIn2024() {
        assertEquals("advent_2_sunday", c1969.day(d("2024-12-08")).principal.id.value)
        assertEquals("immaculate_conception_of_the_blessed_virgin_mary", c1969.day(d("2024-12-09")).principal.id.value)
    }

    @Test
    fun josephAndAnnunciationIn2008() {
        // Pâques le 23 mars 2008 : saint Joseph au samedi 15 mars, l'Annonciation au lundi 31 mars.
        assertEquals("joseph_spouse_of_mary", c1969.day(d("2008-03-15")).principal.id.value)
        assertEquals("annunciation_of_the_lord", c1969.day(d("2008-03-31")).principal.id.value)
    }

    @Test
    fun epiphanyOnSundayInFranceAndBaptismOnMonday() {
        assertEquals("epiphany_of_the_lord", c1969.day(d("2027-01-03")).principal.id.value)
        // Épiphanie le 7 janvier 2024 : Baptême le lundi 8.
        assertEquals("baptism_of_the_lord", c1969.day(d("2024-01-08")).principal.id.value)
    }

    @Test
    fun holyFamilyOnDecember30WhenChristmasIsASunday() {
        assertEquals("holy_family_of_jesus_mary_and_joseph", c1969.day(d("2022-12-30")).principal.id.value)
    }

    @Test
    fun newmanFrom2026Only() {
        assertFalse(c1969.day(d("2025-10-09")).optional.any { it.id.value == "john_henry_newman_priest" })
        assertTrue(c1969.day(d("2026-10-09")).optional.any { it.id.value == "john_henry_newman_priest" })
    }

    @Test
    fun october11th2026() {
        val day = c1969.day(d("2026-10-11"))
        assertEquals("ordinary_time_28_sunday", day.principal.id.value)
        assertEquals(Cycles(SundayCycle.A, WeekdayCycle.II, 4), day.cycles)
        assertEquals(LiturgicalColor.GREEN, day.colors.primary)
    }

    @Test
    fun allSoulsAreVioletOrBlack() {
        val day = c1969.day(d("2026-11-02"))
        assertTrue(day.principal.kind == CelebrationKind.ALL_SOULS)
        assertEquals(LiturgicalColor.VIOLET, day.colors.primary)
    }

    // ── G4 : cas sourcés, 1962 ──────────────────────────────────────────────

    @Test
    fun matthiasOnFebruary25InLeapYears() {
        // 2028 : le 25 février est un vendredi de la Septuagésime (en 2024, c'était un dimanche de Carême).
        assertEquals("Sancti/02-24", c1962.day(d("2028-02-25")).principal.mass?.properId?.take(12))
        assertEquals("Sancti/02-24", c1962.day(d("2025-02-24")).principal.mass?.properId?.take(12))
    }

    @Test
    fun allSoulsOnMondayWhenNovember2IsASunday() {
        assertEquals(CelebrationKind.SUNDAY, c1962.day(d("2025-11-02")).principal.kind)
        assertEquals(CelebrationKind.ALL_SOULS, c1962.day(d("2025-11-03")).principal.kind)
    }

    @Test
    fun christmasEveTakesTheFourthSundayOfAdvent() {
        val day = c1962.day(d("2023-12-24"))
        assertEquals("vigil_of_the_nativity_of_the_lord", day.principal.id.value)
        assertTrue(day.commemorations.isEmpty())
    }

    @Test
    fun immaculateConceptionOnAdventSundayCommemoratesTheSunday() {
        val day = c1962.day(d("2024-12-08"))
        assertEquals("Sancti/12-08", day.principal.mass?.properId?.take(12))
        assertEquals("advent_2_sunday", day.commemorations.first().id.value)
    }

    @Test
    fun externalSolemnitiesAreVotiveAndOmittedOnFirstClassDays() {
        // 2026 : Épiphanie le dimanche 4 janvier (Saint Nom de Jésus), Rosaire le 4 octobre.
        assertEquals("epiphany_of_the_lord", c1962.day(d("2026-01-04")).externalSolemnity?.id?.value)
        assertEquals("most_holy_name_of_jesus", c1962.day(d("2026-01-04")).principal.id.value)
        assertEquals("our_lady_of_the_rosary", c1962.day(d("2026-10-04")).externalSolemnity?.id?.value)
        // Pentecôte le 13 mai 2035, 2e dimanche de mai : pas de solennité de Jeanne d'Arc.
        assertNull(c1962.day(d("2035-05-13")).externalSolemnity)
    }

    // ── Droit en vigueur (France), commun aux deux rites ────────────────────

    @Test
    fun obligationsAndPenanceInFrance() {
        val config = CalendarConfig(Rite.ROMAN_1962)
        assertNotNull(calendars.day(d("2026-08-15"), config).obligation)
        assertNotNull(calendars.day(d("2026-05-14"), config).obligation) // Ascension
        assertNull(calendars.day(d("2026-12-08"), config).obligation) // pas en France
        val ash = calendars.day(d("2026-02-18"), config).penance
        assertTrue(ash.fast && ash.abstinence && !ash.substitutable)
        val friday = calendars.day(d("2026-10-16"), config).penance
        assertTrue(friday.abstinence && friday.substitutable && !friday.fast)
        // Vendredi de l'octave de Pâques : jour de solennité, pas d'abstinence (c. 1251).
        assertFalse(calendars.day(d("2026-04-10"), config).penance.abstinence)
        assertNull(calendars.day(d("2026-10-16"), config).penance.traditional)
    }

    @Test
    fun traditionalDisciplineIsOptional() {
        val config = CalendarConfig(Rite.ROMAN_1962, traditionalPenance = true)
        val emberSaturday = calendars.day(d("2026-09-26"), config) // Quatre-Temps de septembre
        assertEquals(true, emberSaturday.penance.traditional?.fast)
        val lentTuesday = calendars.day(d("2026-03-03"), config).penance.traditional!!
        assertTrue(lentTuesday.fast && !lentTuesday.abstinence)
        assertNull(calendars.day(d("2026-03-01"), config).penance.traditional) // dimanche
        assertEquals(DayOfWeek.SATURDAY, d("2026-09-26").dayOfWeek)
    }
}
