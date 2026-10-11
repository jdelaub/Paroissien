// SPDX-License-Identifier: Apache-2.0
package app.paroissien.core.liturgy

import app.paroissien.core.liturgy.LiturgicalColor.GREEN
import app.paroissien.core.liturgy.LiturgicalColor.RED
import app.paroissien.core.liturgy.LiturgicalColor.ROSE
import app.paroissien.core.liturgy.LiturgicalColor.VIOLET
import app.paroissien.core.liturgy.LiturgicalColor.WHITE
import app.paroissien.core.liturgy.Rank.R1969
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate

/**
 * Calendrier romain général d'après 1969, avec le propre de France (Épiphanie et Saint-Sacrement au dimanche,
 * Ascension le jeudi). Table de préséance des Normes universelles (NU 59), transferts (NU 5 et 60).
 * Les identifiants du temporal sont ceux de romcal (MIT), qui sert d'oracle (rapport 05).
 */
internal class Calendar1969(private val sanctoral: SanctoralData) {

    private val years = object : LinkedHashMap<Int, Map<LocalDate, LiturgicalDay>>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Map<LocalDate, LiturgicalDay>>) = size > 4
    }

    @Synchronized
    fun day(date: LocalDate): LiturgicalDay = years.getOrPut(date.year) { build(date.year) }.getValue(date)

    // ── Temporal ────────────────────────────────────────────────────────────

    private class Year(val y: Int) {
        val easter: LocalDate = Computus.easter(y)
        val ashWednesday: LocalDate = easter.minusDays(46)
        val palmSunday: LocalDate = easter.minusDays(7)
        val pentecost: LocalDate = easter.plusDays(49)
        val ascension: LocalDate = easter.plusDays(39)
        val advent1: LocalDate = firstSundayOfAdvent(y)
        val christTheKing: LocalDate = advent1.minusWeeks(1)
        /** France : dimanche entre le 2 et le 8 janvier. */
        val epiphany: LocalDate = LocalDate.of(y, 1, 1).next(SUNDAY)
        /** Dimanche après l'Épiphanie, ou lundi si l'Épiphanie tombe le 7 ou le 8 (NU 38). */
        val baptism: LocalDate = if (epiphany.dayOfMonth >= 7) epiphany.plusDays(1) else epiphany.plusWeeks(1)
        /** Dimanche dans l'octave de Noël, ou le 30 décembre si Noël est un dimanche. */
        val holyFamily: LocalDate = LocalDate.of(y, 12, 25).let { if (it.isSunday()) LocalDate.of(y, 12, 30) else it.next(SUNDAY) }

        /** Année liturgique (année civile de sa fin) d'une date. */
        fun liturgicalYear(d: LocalDate) = if (d >= advent1) y + 1 else y
    }

    /** Une célébration candidate avec son degré dans la table des NU (1 = Triduum … 13 = férie ordinaire). */
    private data class Candidate(val celebration: Celebration, val degree: Int, val solemnity: Boolean, val memorial: Boolean, val optional: Boolean)

    private data class Base(val celebration: Celebration, val degree: Int, val season: Season, val week: Int?, val psalterWeek: Int?, val flags: Set<DayFlag> = emptySet())

    private fun temporal(id: String, fr: String, la: String?, rank: R1969, degree: Int, vararg colors: LiturgicalColor, kind: CelebrationKind) =
        Celebration(
            id = CelebrationId(id), title = LocalizedText(fr, la), kind = kind, rank = rank, precedence = degree,
            source = CelebrationSource.TEMPORAL, colors = LiturgicalColors.of(*colors),
            mass = MassRef(id), readings = ReadingsRef(id),
        )

    private fun psalter(week: Int) = (week - 1).mod(4) + 1

    /** Temps de Noël : le psautier continue celui de l'Avent (PGLH 133), sauf si Noël est un dimanche. */
    private fun christmasPsalter(d: LocalDate): Int {
        val christmas = LocalDate.of(if (d.monthValue == 12) d.year else d.year - 1, 12, 25)
        val n = daysBetween(christmas, d) + 1
        val s = christmas.dayOfWeek.value % 7
        val w = (n + s + 6) / 7
        return if (s != 0) (w + 2).mod(4) + 1 else (w - 1).mod(4) + 1
    }

    private fun base(t: Year, d: LocalDate): Base {
        val wd = d.dayOfWeek
        return when {
            // Temps de Noël (début d'année) : jusqu'au Baptême inclus.
            d <= t.baptism -> when {
                d == t.epiphany -> Base(temporal("epiphany_of_the_lord", "Épiphanie du Seigneur", "In Epiphania Domini", R1969.SOLEMNITY, 2, WHITE, kind = CelebrationKind.FEAST), 2, Season.CHRISTMAS, null, christmasPsalter(d))
                d == t.baptism -> Base(temporal("baptism_of_the_lord", "Baptême du Seigneur", "In Baptismate Domini", R1969.FEAST, 5, WHITE, kind = CelebrationKind.FEAST), 5, Season.CHRISTMAS, null, christmasPsalter(d))
                d.dayOfMonth == 1 -> Base(temporal("mary_mother_of_god", "Sainte Marie, Mère de Dieu", "Sollemnitas S. Dei Genetricis Mariae", R1969.SOLEMNITY, 3, WHITE, kind = CelebrationKind.FEAST), 3, Season.CHRISTMAS, null, christmasPsalter(d), setOf(DayFlag.OCTAVE))
                d < t.epiphany -> Base(temporal("christmas_time_january_${d.dayOfMonth}", "${d.dayOfMonth} janvier, temps de Noël", null, R1969.WEEKDAY, 13, WHITE, kind = CelebrationKind.FERIA), 13, Season.CHRISTMAS, null, christmasPsalter(d))
                else -> Base(temporal("${wd.id()}_after_epiphany", "${French.capitalizedWeekday(wd)} après l'Épiphanie", null, R1969.WEEKDAY, 13, WHITE, kind = CelebrationKind.FERIA), 13, Season.CHRISTMAS, null, christmasPsalter(d))
            }
            d < t.ashWednesday -> ordinaryTime(d, ((daysBetween(t.baptism.sundayOnOrBefore(), d)) / 7) + 1)
            d < t.palmSunday -> lent(t, d)
            d < t.easter -> holyWeek(t, d)
            d <= t.pentecost -> easterTime(t, d)
            d < t.advent1 -> ordinaryTime(d, 34 - daysBetween(d.sundayOnOrBefore(), t.christTheKing) / 7)
            d < LocalDate.of(t.y, 12, 25) -> advent(t, d)
            else -> christmasOctave(d)
        }
    }

    private fun ordinaryTime(d: LocalDate, week: Int): Base {
        val wd = d.dayOfWeek
        return if (wd == SUNDAY) {
            // Le 3e dimanche est celui de la Parole de Dieu (*Aperuit illis*, 2019).
            val id = if (week == 3) "sunday_of_the_word_of_god" else "ordinary_time_${week}_sunday"
            val fr = if (week == 3) "3e dimanche du temps ordinaire, dimanche de la Parole de Dieu" else "${French.ordinal(week)} dimanche du temps ordinaire"
            Base(temporal(id, fr, "Dominica ${Latin.roman(week)} per annum", R1969.SUNDAY, 6, GREEN, kind = CelebrationKind.SUNDAY), 6, Season.ORDINARY_TIME, week, psalter(week))
        } else {
            Base(temporal("ordinary_time_${week}_${wd.id()}", "${French.capitalizedWeekday(wd)} de la ${French.ordinalFem(week)} semaine du temps ordinaire", null, R1969.WEEKDAY, 13, GREEN, kind = CelebrationKind.FERIA), 13, Season.ORDINARY_TIME, week, psalter(week))
        }
    }

    private fun lent(t: Year, d: LocalDate): Base {
        val wd = d.dayOfWeek
        val firstSunday = t.ashWednesday.next(SUNDAY)
        if (d < firstSunday) {
            return if (d == t.ashWednesday) {
                Base(temporal("ash_wednesday", "Mercredi des Cendres", "Feria IV Cinerum", R1969.WEEKDAY, 2, VIOLET, kind = CelebrationKind.FERIA), 2, Season.LENT, 0, 4, setOf(DayFlag.PRIVILEGED))
            } else {
                Base(temporal("${wd.id()}_after_ash_wednesday", "${French.capitalizedWeekday(wd)} après les Cendres", null, R1969.WEEKDAY, 9, VIOLET, kind = CelebrationKind.FERIA), 9, Season.LENT, 0, 4)
            }
        }
        val week = daysBetween(firstSunday, d) / 7 + 1
        return if (wd == SUNDAY) {
            val laetare = week == 4
            Base(
                temporal("lent_${week}_sunday", "${French.ordinal(week)} dimanche de Carême", "Dominica ${Latin.roman(week)} in Quadragesima", R1969.SUNDAY, 2, *(if (laetare) arrayOf(ROSE, VIOLET) else arrayOf(VIOLET)), kind = CelebrationKind.SUNDAY),
                2, Season.LENT, week, psalter(week), if (laetare) setOf(DayFlag.GAUDETE_LAETARE) else emptySet(),
            )
        } else {
            Base(temporal("lent_${week}_${wd.id()}", "${French.capitalizedWeekday(wd)} de la ${French.ordinalFem(week)} semaine de Carême", null, R1969.WEEKDAY, 9, VIOLET, kind = CelebrationKind.FERIA), 9, Season.LENT, week, psalter(week))
        }
    }

    private fun holyWeek(t: Year, d: LocalDate): Base = when (daysBetween(t.palmSunday, d)) {
        0 -> Base(temporal("palm_sunday_of_the_passion_of_the_lord", "Dimanche des Rameaux et de la Passion du Seigneur", "Dominica in Palmis de Passione Domini", R1969.SUNDAY, 2, RED, kind = CelebrationKind.SUNDAY), 2, Season.LENT, 6, 2, setOf(DayFlag.HOLY_WEEK))
        1 -> Base(temporal("holy_monday", "Lundi saint", "Feria II Hebdomadae Sanctae", R1969.WEEKDAY, 2, VIOLET, kind = CelebrationKind.FERIA), 2, Season.LENT, 6, 2, setOf(DayFlag.HOLY_WEEK))
        2 -> Base(temporal("holy_tuesday", "Mardi saint", "Feria III Hebdomadae Sanctae", R1969.WEEKDAY, 2, VIOLET, kind = CelebrationKind.FERIA), 2, Season.LENT, 6, 2, setOf(DayFlag.HOLY_WEEK))
        3 -> Base(temporal("holy_wednesday", "Mercredi saint", "Feria IV Hebdomadae Sanctae", R1969.WEEKDAY, 2, VIOLET, kind = CelebrationKind.FERIA), 2, Season.LENT, 6, 2, setOf(DayFlag.HOLY_WEEK))
        4 -> Base(temporal("holy_thursday", "Jeudi saint", "Feria V in Cena Domini", R1969.WEEKDAY, 1, WHITE, kind = CelebrationKind.FERIA), 1, Season.PASCHAL_TRIDUUM, 6, 2, setOf(DayFlag.HOLY_WEEK))
        5 -> Base(temporal("friday_of_the_passion_of_the_lord", "Vendredi saint, la Passion du Seigneur", "Feria VI in Passione Domini", R1969.WEEKDAY, 1, RED, kind = CelebrationKind.FERIA), 1, Season.PASCHAL_TRIDUUM, 6, 1, setOf(DayFlag.HOLY_WEEK))
        else -> Base(temporal("holy_saturday", "Samedi saint", "Sabbato Sancto", R1969.WEEKDAY, 1, VIOLET, kind = CelebrationKind.FERIA), 1, Season.PASCHAL_TRIDUUM, 6, 1, setOf(DayFlag.HOLY_WEEK))
    }

    private fun easterTime(t: Year, d: LocalDate): Base {
        val wd = d.dayOfWeek
        val days = daysBetween(t.easter, d)
        val week = days / 7 + 1
        return when {
            days == 0 -> Base(temporal("easter_sunday", "Dimanche de Pâques, la Résurrection du Seigneur", "Dominica Resurrectionis", R1969.SOLEMNITY, 1, WHITE, kind = CelebrationKind.SUNDAY), 1, Season.EASTER, 1, 1)
            days < 7 -> Base(temporal("easter_${wd.id()}", "${French.capitalizedWeekday(wd)} dans l'octave de Pâques", null, R1969.SOLEMNITY, 2, WHITE, kind = CelebrationKind.OCTAVE_DAY), 2, Season.EASTER, 1, 1, setOf(DayFlag.OCTAVE))
            days == 7 -> Base(temporal("divine_mercy_sunday", "2e dimanche de Pâques, de la divine miséricorde", "Dominica II Paschae", R1969.SUNDAY, 2, WHITE, kind = CelebrationKind.SUNDAY), 2, Season.EASTER, 2, 2)
            d == t.ascension -> Base(temporal("ascension_of_the_lord", "Ascension du Seigneur", "In Ascensione Domini", R1969.SOLEMNITY, 2, WHITE, kind = CelebrationKind.FEAST), 2, Season.EASTER, week, psalter(week))
            d == t.pentecost -> Base(temporal("pentecost_sunday", "Dimanche de la Pentecôte", "Dominica Pentecostes", R1969.SOLEMNITY, 2, RED, kind = CelebrationKind.SUNDAY), 2, Season.EASTER, 8, psalter(8))
            wd == SUNDAY -> Base(temporal("easter_time_${week}_sunday", "${French.ordinal(week)} dimanche de Pâques", "Dominica ${Latin.roman(week)} Paschae", R1969.SUNDAY, 2, WHITE, kind = CelebrationKind.SUNDAY), 2, Season.EASTER, week, psalter(week))
            else -> Base(temporal("easter_time_${week}_${wd.id()}", "${French.capitalizedWeekday(wd)} de la ${French.ordinalFem(week)} semaine de Pâques", null, R1969.WEEKDAY, 13, WHITE, kind = CelebrationKind.FERIA), 13, Season.EASTER, week, psalter(week))
        }
    }

    private fun advent(t: Year, d: LocalDate): Base {
        val wd = d.dayOfWeek
        val week = daysBetween(t.advent1, d) / 7 + 1
        return when {
            wd == SUNDAY -> {
                val gaudete = week == 3
                Base(
                    temporal("advent_${week}_sunday", "${French.ordinal(week)} dimanche de l'Avent", "Dominica ${Latin.roman(week)} Adventus", R1969.SUNDAY, 2, *(if (gaudete) arrayOf(ROSE, VIOLET) else arrayOf(VIOLET)), kind = CelebrationKind.SUNDAY),
                    2, Season.ADVENT, week, psalter(week), if (gaudete) setOf(DayFlag.GAUDETE_LAETARE) else emptySet(),
                )
            }
            d.monthValue == 12 && d.dayOfMonth >= 17 -> Base(temporal("advent_december_${d.dayOfMonth}", "${d.dayOfMonth} décembre, férie de l'Avent", null, R1969.WEEKDAY, 9, VIOLET, kind = CelebrationKind.FERIA), 9, Season.ADVENT, week, psalter(week))
            else -> Base(temporal("advent_${week}_${wd.id()}", "${French.capitalizedWeekday(wd)} de la ${French.ordinalFem(week)} semaine de l'Avent", null, R1969.WEEKDAY, 13, VIOLET, kind = CelebrationKind.FERIA), 13, Season.ADVENT, week, psalter(week))
        }
    }

    private fun christmasOctave(d: LocalDate): Base =
        if (d.dayOfMonth == 25) {
            Base(temporal("nativity_of_the_lord", "Nativité du Seigneur", "In Nativitate Domini", R1969.SOLEMNITY, 2, WHITE, kind = CelebrationKind.FEAST), 2, Season.CHRISTMAS, null, christmasPsalter(d))
        } else {
            val n = d.dayOfMonth - 24
            Base(temporal("christmas_octave_day_$n", "${French.ordinal(n)} jour dans l'octave de Noël", null, R1969.WEEKDAY, 9, WHITE, kind = CelebrationKind.OCTAVE_DAY), 9, Season.CHRISTMAS, null, christmasPsalter(d), setOf(DayFlag.OCTAVE))
        }

    /** Fêtes mobiles du temporal qui entrent en concurrence avec le jour de base. */
    private fun movables(t: Year): Map<LocalDate, List<Candidate>> {
        val out = HashMap<LocalDate, MutableList<Candidate>>()
        fun add(date: LocalDate, c: Celebration, degree: Int) {
            val r = c.rank as R1969
            out.getOrPut(date) { mutableListOf() } += Candidate(c, degree, r == R1969.SOLEMNITY, r == R1969.MEMORIAL, r == R1969.OPTIONAL_MEMORIAL)
        }
        add(t.holyFamily, temporal("holy_family_of_jesus_mary_and_joseph", "Sainte Famille de Jésus, Marie et Joseph", "S. Familiae Iesu, Mariae et Ioseph", R1969.FEAST, 5, WHITE, kind = CelebrationKind.FEAST), 5)
        add(t.pentecost.plusDays(7), temporal("most_holy_trinity", "La Sainte Trinité", "Sanctissimae Trinitatis", R1969.SOLEMNITY, 3, WHITE, kind = CelebrationKind.FEAST), 3)
        add(t.pentecost.plusDays(14), temporal("most_holy_body_and_blood_of_christ", "Le Saint-Sacrement du Corps et du Sang du Christ", "Sanctissimi Corporis et Sanguinis Christi", R1969.SOLEMNITY, 3, WHITE, kind = CelebrationKind.FEAST), 3)
        add(t.pentecost.plusDays(19), temporal("most_sacred_heart_of_jesus", "Le Sacré-Cœur de Jésus", "Sacratissimi Cordis Iesu", R1969.SOLEMNITY, 3, WHITE, kind = CelebrationKind.FEAST), 3)
        add(t.pentecost.plusDays(20), temporal("immaculate_heart_of_mary", "Cœur immaculé de Marie", "Immaculati Cordis B. M. V.", R1969.MEMORIAL, 10, WHITE, kind = CelebrationKind.FEAST), 10)
        if (t.y >= 2018) add(t.pentecost.plusDays(1), temporal("mary_mother_of_the_church", "Bienheureuse Vierge Marie, Mère de l'Église", "B. M. V. Ecclesiae Matris", R1969.MEMORIAL, 10, WHITE, kind = CelebrationKind.FEAST), 10)
        add(t.christTheKing, temporal("our_lord_jesus_christ_king_of_the_universe", "Notre-Seigneur Jésus-Christ, Roi de l'univers", "D. N. Iesu Christi universorum Regis", R1969.SOLEMNITY, 3, WHITE, kind = CelebrationKind.FEAST), 3)
        return out
    }

    // ── Sanctoral ───────────────────────────────────────────────────────────

    private fun candidate(s: Sanctoral, date: LocalDate): Candidate {
        val e = s.entry
        val national = s.source == CelebrationSource.NATIONAL && !s.overridesGeneral
        val rank = when (e.rank) {
            "SOLEMNITY" -> R1969.SOLEMNITY
            "FEAST" -> R1969.FEAST
            "MEMORIAL" -> R1969.MEMORIAL
            else -> R1969.OPTIONAL_MEMORIAL
        }
        // NU 59 : 3 solennités générales, 4 propres ; 5 fêtes du Seigneur ; 7 fêtes générales, 8 propres ;
        // 10 mémoires générales, 11 propres ; 12 mémoires facultatives.
        val degree = when (rank) {
            R1969.SOLEMNITY -> if (national) 4 else 3
            R1969.FEAST -> if (e.lord) 5 else if (national) 8 else 7
            R1969.MEMORIAL -> if (national) 11 else 10
            else -> 12
        }
        val allSouls = e.id.contains("faithful_departed")
        val c = Celebration(
            id = CelebrationId(e.id),
            title = e.title?.text() ?: LocalizedText(e.id),
            kind = if (allSouls) CelebrationKind.ALL_SOULS else CelebrationKind.FEAST,
            rank = rank, precedence = degree, source = s.source,
            colors = if (e.colors.isEmpty()) LiturgicalColors(WHITE) else LiturgicalColors.of(e.colors),
            mass = MassRef(e.id), readings = ReadingsRef(e.id),
        )
        return Candidate(c, degree, rank == R1969.SOLEMNITY, rank == R1969.MEMORIAL, rank == R1969.OPTIONAL_MEMORIAL)
    }

    // ── Résolution de l'année ───────────────────────────────────────────────

    private fun build(y: Int): Map<LocalDate, LiturgicalDay> {
        val t = Year(y)
        val bases = daysOf(y).associateWith { base(t, it) }
        val sanct = sanctoral.year(y)
        val movable = movables(t)
        val cands = bases.mapValues { (d, b) ->
            val temporalIds = setOf(b.celebration.id) + movable[d].orEmpty().map { it.celebration.id }
            val fromData = sanct[d].orEmpty().filter { CelebrationId(it.entry.id) !in temporalIds }.map { candidate(it, d) }
            (listOf(Candidate(b.celebration, b.degree, false, false, false)) + movable[d].orEmpty() + fromData)
                .sortedWith(compareBy<Candidate> { it.degree }.thenBy { if (it.celebration.source == CelebrationSource.TEMPORAL) 0 else 1 })
                .toMutableList()
        }

        // NU 60 : une solennité empêchée est transférée au jour le plus proche hors des degrés 1 à 8 ; les autres
        // célébrations empêchées sont omises cette année-là.
        val impeded = cands.flatMap { (d, list) -> list.drop(1).filter { it.solemnity }.map { d to it } }
        impeded.forEach { (d, c) -> cands.getValue(d).remove(c) }
        impeded.sortedBy { it.first }.forEach { (d, c) ->
            val target = transferTarget(t, d, c, cands)
            val moved = c.copy(celebration = c.celebration.copy(transferredFrom = d))
            cands.getValue(target).add(0, moved)
            cands.getValue(target).sortWith(compareBy { it.degree })
        }

        return bases.mapValues { (d, b) -> resolve(t, d, b, cands.getValue(d)) }
    }

    private fun transferTarget(t: Year, from: LocalDate, c: Candidate, cands: Map<LocalDate, List<Candidate>>): LocalDate {
        fun free(d: LocalDate) = d.year == t.y && cands.getValue(d).first().degree > 8
        // Annonciation en Semaine sainte ou dans l'octave de Pâques : lundi après le 2e dimanche de Pâques (NU 60).
        if (c.celebration.id.value == "annunciation_of_the_lord" && from >= t.palmSunday && from <= t.easter.plusDays(7)) return t.easter.plusDays(8)
        // NU 5 : une solennité qui tombe un dimanche de l'Avent, du Carême ou de Pâques passe au lundi.
        if (from.isSunday() && from != t.palmSunday && from != t.easter && free(from.plusDays(1))) return from.plusDays(1)
        for (k in 1..60) {
            from.minusDays(k.toLong()).takeIf(::free)?.let { return it }
            from.plusDays(k.toLong()).takeIf(::free)?.let { return it }
        }
        error("Aucun jour libre pour ${c.celebration.id} ($from)")
    }

    private fun resolve(t: Year, d: LocalDate, b: Base, list: List<Candidate>): LiturgicalDay {
        // Une mémoire facultative n'est jamais la célébration du jour : elle est proposée au choix, sur une férie.
        val top = list.first { !it.optional }
        val weekdayDegree = b.degree.takeIf { b.celebration.rank == R1969.WEEKDAY }
        // Mercredi des Cendres, Semaine sainte, Triduum (degrés 1 et 2) : aucune mémoire, même facultative.
        val onWeekday = top.celebration === b.celebration && weekdayDegree != null && weekdayDegree > 2
        val memorials = list.filter { it.memorial }
        var principal = top.celebration
        val optional = mutableListOf<Celebration>()
        when {
            // Féries privilégiées (degré 9 : Carême, 17-24 décembre, octave de Noël) : les mémoires deviennent facultatives (NU 14).
            onWeekday && weekdayDegree == 9 ->
                list.filter { it.memorial || it.optional }.forEach { optional += it.celebration.asOptional() }
            // Marie, Mère de l'Église, l'emporte sur une autre mémoire obligatoire (notification de 2018).
            top.memorial && memorials.any { it.celebration.id.value == MMC } ->
                principal = memorials.first { it.celebration.id.value == MMC }.celebration
            top.memorial && memorials.size > 1 && weekdayDegree != null -> {
                // Deux mémoires obligatoires le même jour : toutes deux facultatives, la férie demeure (notification de 1998).
                principal = b.celebration
                list.filter { it.memorial || it.optional }.forEach { optional += it.celebration.asOptional() }
            }
            onWeekday -> list.filter { it.optional }.forEach { optional += it.celebration }
        }
        val season = b.season
        val ly = t.liturgicalYear(d)
        val cycles = Cycles(
            sunday = when (ly % 3) { 1 -> SundayCycle.A; 2 -> SundayCycle.B; else -> SundayCycle.C },
            weekday = if (ly % 2 == 1) WeekdayCycle.I else WeekdayCycle.II,
            psalterWeek = b.psalterWeek ?: 0,
        )
        val flags = b.flags + listOfNotNull(DayFlag.TRANSFERRED.takeIf { principal.transferredFrom != null })
        return LiturgicalDay(
            date = d, rite = Rite.ROMAN_1969, season = season, week = b.week, principal = principal,
            optional = optional, cycles = cycles, flags = flags,
        )
    }

    private val MMC = "mary_mother_of_the_church"

    private fun Celebration.asOptional() = if (rank == R1969.MEMORIAL) copy(rank = R1969.OPTIONAL_MEMORIAL, precedence = 12) else this
}
