// SPDX-License-Identifier: Apache-2.0
package app.paroissien.core.liturgy

import app.paroissien.core.liturgy.LiturgicalColor.BLACK
import app.paroissien.core.liturgy.LiturgicalColor.GREEN
import app.paroissien.core.liturgy.LiturgicalColor.RED
import app.paroissien.core.liturgy.LiturgicalColor.ROSE
import app.paroissien.core.liturgy.LiturgicalColor.VIOLET
import app.paroissien.core.liturgy.LiturgicalColor.WHITE
import app.paroissien.core.liturgy.Rank.R1962
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.Year as JYear

/**
 * Calendrier romain de 1962 (rubriques générales de 1960), propre de France et solennités extérieures.
 *
 * Préséance : table du n. 91, ramenée à un nombre (plus petit = plus haut). Transferts : seules les fêtes de
 * Iʳᵉ classe se transfèrent, au premier jour libre de célébrations de Iʳᵉ et IIᵉ classe (n. 95). Commémoraisons :
 * n. 106-114 (privilégiées seules un jour de Iʳᵉ classe, une un jour de IIᵉ, deux un jour de IIIᵉ ou IVᵉ).
 * Les clés de propre sont celles de Divinum Officium (MIT) ; Missalemeum (MIT) sert d'oracle (rapport 05).
 */
internal class Calendar1962(
    private val sanctoral: SanctoralData,
    private val external: ExternalSolemnities = ExternalSolemnities.NONE,
) {

    private val years = object : LinkedHashMap<Int, Map<LocalDate, LiturgicalDay>>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Map<LocalDate, LiturgicalDay>>) = size > 4
    }

    @Synchronized
    fun day(date: LocalDate): LiturgicalDay = years.getOrPut(date.year) { build(date.year) }.getValue(date)

    private val GREATER_LITANIES = "greater_litanies"

    /** Solennités extérieures : la messe d'une fête reprise un dimanche, par dates (n. 356-361). */
    fun interface ExternalSolemnities {
        fun of(year: Int, sanctoral: (String) -> Celebration?): Map<LocalDate, Celebration>

        companion object { val NONE = ExternalSolemnities { _, _ -> emptyMap() } }
    }

    private class Year(val y: Int) {
        val easter: LocalDate = Computus.easter(y)
        val septuagesima: LocalDate = easter.minusDays(63)
        val ashWednesday: LocalDate = easter.minusDays(46)
        val passionSunday: LocalDate = easter.minusDays(14)
        val palmSunday: LocalDate = easter.minusDays(7)
        val ascension: LocalDate = easter.plusDays(39)
        val pentecost: LocalDate = easter.plusDays(49)
        val advent1: LocalDate = firstSundayOfAdvent(y)
        /** Dernier dimanche avant l'Avent : toujours le 24ᵉ après la Pentecôte. */
        val lastPentecostSunday: LocalDate = advent1.minusWeeks(1)
        val holyFamily: LocalDate = LocalDate.of(y, 1, 6).next(SUNDAY)
        /** Saint Nom de Jésus : dimanche entre le 2 et le 5 janvier, sinon le 2 janvier. */
        val holyName: LocalDate = (2..5).map { LocalDate.of(y, 1, it) }.firstOrNull { it.isSunday() } ?: LocalDate.of(y, 1, 2)
        /** Christ-Roi : dernier dimanche d'octobre. */
        val christTheKing: LocalDate = LocalDate.of(y, 10, 31).sundayOnOrBefore()
        /** Quatre-Temps de septembre : mercredi après le 3ᵉ dimanche de septembre (*Rubricarum instructum*, 1960). */
        val emberSeptember: LocalDate = LocalDate.of(y, 9, 1).let { if (it.isSunday()) it else it.next(SUNDAY) }.plusWeeks(2).plusDays(3)
        val gaudete: LocalDate = advent1.plusWeeks(2)
    }

    /** Candidate du jour : sa préséance (n. 91), sa classe, et ce qui gouverne les commémoraisons. */
    private data class Candidate(
        val celebration: Celebration,
        val precedence: Int,
        val temporal: Boolean,
        /** Commémoraison privilégiée (n. 108) : dimanche, féries de l'Avent, du Carême, de la Passion, Quatre-Temps de septembre. */
        val privileged: Boolean = false,
        val lord: Boolean = false,
        val vigil: Boolean = false,
        val feria4: Boolean = false,
        val sunday: Boolean = false,
        /** Commémoraison attachée à une fête du sanctoral (Divinum Officium) : suit sa fête. */
        val attachedTo: String? = null,
        /** Commémoraison intrinsèque, sans propre (saint Paul avec saint Pierre, l'octave de Noël) : toujours faite. */
        val intrinsic: Boolean = false,
    ) {
        val classOf: Int get() = when {
            precedence < 30 -> 1
            precedence < 40 -> 2
            precedence < 50 -> 3
            else -> 4
        }
    }

    private class Base(val candidate: Candidate, val season: Season, val week: Int?, val flags: Set<DayFlag> = emptySet(), val extra: List<Candidate> = emptyList())

    // ── Temporal ────────────────────────────────────────────────────────────

    private fun tempora(
        key: String, id: String, fr: String, la: String?, precedence: Int, colors: List<LiturgicalColor>,
        kind: CelebrationKind, privileged: Boolean = false, lord: Boolean = false, sunday: Boolean = false, feria4: Boolean = false,
    ): Candidate {
        val c = Celebration(
            id = CelebrationId(id), title = LocalizedText(fr, la), kind = kind, rank = rankOf(precedence), precedence = precedence,
            source = CelebrationSource.TEMPORAL, colors = LiturgicalColors.of(colors),
            mass = MassRef(key), readings = ReadingsRef(null, fromProper = true),
        )
        return Candidate(c, precedence, temporal = true, privileged = privileged, lord = lord, sunday = sunday, feria4 = feria4)
    }

    private fun rankOf(precedence: Int) = when {
        precedence < 30 -> R1962.CLASS_I
        precedence < 40 -> R1962.CLASS_II
        precedence < 50 -> R1962.CLASS_III
        precedence < 60 -> R1962.CLASS_IV
        else -> R1962.COMMEMORATION
    }

    private val weekdaysFr = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi")
    private fun DayOfWeek.n() = value % 7
    private fun feriaFr(d: DayOfWeek) = weekdaysFr[d.value - 1]

    private fun base(t: Year, d: LocalDate): Base {
        val wd = d.dayOfWeek
        val sunday = wd == SUNDAY
        return when {
            d < t.holyFamily && d.dayOfMonth <= 13 && d.monthValue == 1 -> christmasTide(t, d)
            d < t.septuagesima -> afterEpiphany(t, d, (daysBetween(t.holyFamily, d.sundayOnOrBefore()) / 7) + 1)
            d < t.ashWednesday -> {
                val week = daysBetween(t.septuagesima, d) / 7 + 1
                val names = listOf("Septuagésime", "Sexagésime", "Quinquagésime")
                val la = listOf("Septuagesima", "Sexagesima", "Quinquagesima")
                if (sunday) {
                    Base(tempora("Tempora/Quadp$week-0", "${la[week - 1].lowercase()}_sunday", "Dimanche de la ${names[week - 1]}", "Dominica in ${la[week - 1]}", 31, listOf(VIOLET), CelebrationKind.SUNDAY, privileged = true, sunday = true), Season.SEPTUAGESIMA, week)
                } else {
                    Base(tempora("Tempora/Quadp$week-${wd.n()}", "${la[week - 1].lowercase()}_${wd.id()}", "${feriaFr(wd)} après la ${names[week - 1]}", null, 51, listOf(VIOLET), CelebrationKind.FERIA, feria4 = true), Season.SEPTUAGESIMA, week)
                }
            }
            d < t.passionSunday -> lent(t, d)
            d < t.easter -> passiontide(t, d)
            d <= t.pentecost.plusDays(6) -> paschaltide(t, d)
            d < t.advent1 -> afterPentecost(t, d)
            d.monthValue == 12 && d.dayOfMonth >= 25 -> christmasOctave(d)
            else -> advent(t, d)
        }
    }

    /** Du 1ᵉʳ janvier au samedi avant la Sainte Famille (le 13 janvier au plus tard). */
    private fun christmasTide(t: Year, d: LocalDate): Base {
        val n = d.dayOfMonth
        return when {
            d == t.holyName -> Base(tempora("Tempora/Nat2-0", "most_holy_name_of_jesus", "Très saint Nom de Jésus", "Ss.mi Nominis Iesu", 30, listOf(WHITE), CelebrationKind.FEAST, lord = true), Season.CHRISTMAS, null)
            n == 1 -> Base(tempora("Sancti/01-01", "octave_of_the_nativity_of_the_lord", "Octave de la Nativité", "In Octava Nativitatis Domini", 14, listOf(WHITE), CelebrationKind.OCTAVE_DAY, lord = true), Season.CHRISTMAS, null, setOf(DayFlag.OCTAVE))
            n < 6 -> Base(tempora("Tempora/Nat0$n", "christmas_time_january_$n", "$n janvier, férie du temps de Noël", null, 51, listOf(WHITE), CelebrationKind.FERIA, feria4 = true), Season.CHRISTMAS, null)
            n == 6 -> Base(tempora("Sancti/01-06", "epiphany_of_the_lord", "Épiphanie de Notre-Seigneur", "In Epiphania Domini", 12, listOf(WHITE), CelebrationKind.FEAST, lord = true), Season.CHRISTMAS, null)
            else -> Base(tempora("Sancti/01-${n.toString().padStart(2, '0')}", "epiphany_time_january_$n", "$n janvier, férie du temps de l'Épiphanie", null, 51, listOf(WHITE), CelebrationKind.FERIA, feria4 = true), Season.CHRISTMAS, null)
        }
    }

    private fun afterEpiphany(t: Year, d: LocalDate, week: Int): Base {
        val wd = d.dayOfWeek
        val season = if (d.monthValue == 1 && d.dayOfMonth <= 13) Season.CHRISTMAS else Season.TIME_AFTER_EPIPHANY
        if (week == 1) {
            return if (wd == SUNDAY) {
                Base(tempora("Tempora/Epi1-0", "holy_family_of_jesus_mary_and_joseph", "Sainte Famille, Jésus, Marie, Joseph", "S. Familiae Iesu, Mariae, Ioseph", 30, listOf(WHITE), CelebrationKind.FEAST, lord = true), season, 1)
            } else {
                // Blanches jusqu'au 13 janvier, fin du temps de Noël ; vertes ensuite (n. 72, 77, 127 a).
                val color = if (season == Season.CHRISTMAS) WHITE else GREEN
                Base(tempora("Tempora/Epi1-${wd.n()}", "epiphany_1_${wd.id()}", "${feriaFr(wd)} après la Sainte Famille", null, 51, listOf(color), CelebrationKind.FERIA, feria4 = true), season, 1)
            }
        }
        return epiphanyWeek(d, week, season)
    }

    private fun epiphanyWeek(d: LocalDate, week: Int, season: Season): Base {
        val wd = d.dayOfWeek
        return if (wd == SUNDAY) {
            Base(tempora("Tempora/Epi$week-0", "epiphany_${week}_sunday", "${French.ordinal(week)} dimanche après l'Épiphanie", "Dominica ${Latin.roman(week)} post Epiphaniam", 31, listOf(GREEN), CelebrationKind.SUNDAY, privileged = true, sunday = true), season, week)
        } else {
            Base(tempora("Tempora/Epi$week-${wd.n()}", "epiphany_${week}_${wd.id()}", "${feriaFr(wd)} de la ${French.ordinalFem(week)} semaine après l'Épiphanie", null, 51, listOf(GREEN), CelebrationKind.FERIA, feria4 = true), season, week)
        }
    }

    private fun lent(t: Year, d: LocalDate): Base {
        val wd = d.dayOfWeek
        if (d < t.ashWednesday.next(SUNDAY)) {
            return if (d == t.ashWednesday) {
                Base(tempora("Tempora/Quadp3-3", "ash_wednesday", "Mercredi des Cendres", "Feria IV Cinerum", 16, listOf(VIOLET), CelebrationKind.FERIA, privileged = true), Season.LENT, 0, setOf(DayFlag.PRIVILEGED))
            } else {
                Base(tempora("Tempora/Quadp3-${wd.n()}", "${wd.id()}_after_ash_wednesday", "${feriaFr(wd)} après les Cendres", null, 40, listOf(VIOLET), CelebrationKind.FERIA, privileged = true), Season.LENT, 0)
            }
        }
        val week = daysBetween(t.ashWednesday.next(SUNDAY), d) / 7 + 1
        if (wd == SUNDAY) {
            val laetare = week == 4
            return Base(
                tempora("Tempora/Quad$week-0", "lent_${week}_sunday", "${French.ordinal(week)} dimanche de Carême", "Dominica ${Latin.roman(week)} in Quadragesima", 15, if (laetare) listOf(ROSE, VIOLET) else listOf(VIOLET), CelebrationKind.SUNDAY, privileged = true, sunday = true),
                Season.LENT, week, if (laetare) setOf(DayFlag.GAUDETE_LAETARE) else emptySet(),
            )
        }
        val ember = week == 1 && (wd == WEDNESDAY || wd == FRIDAY || wd == SATURDAY)
        return if (ember) {
            Base(tempora("Tempora/Quad1-${wd.n()}", "lent_ember_${wd.id()}", "${feriaFr(wd)} des Quatre-Temps de Carême", null, 34, listOf(VIOLET), CelebrationKind.EMBER_DAY, privileged = true), Season.LENT, week, setOf(DayFlag.EMBER_DAY))
        } else {
            Base(tempora("Tempora/Quad$week-${wd.n()}", "lent_${week}_${wd.id()}", "${feriaFr(wd)} de la ${French.ordinalFem(week)} semaine de Carême", null, 40, listOf(VIOLET), CelebrationKind.FERIA, privileged = true), Season.LENT, week)
        }
    }

    private fun passiontide(t: Year, d: LocalDate): Base {
        val wd = d.dayOfWeek
        val days = daysBetween(t.passionSunday, d)
        if (days < 7) {
            if (wd == SUNDAY) return Base(tempora("Tempora/Quad5-0", "first_sunday_of_the_passion", "1er dimanche de la Passion", "Dominica I Passionis", 15, listOf(VIOLET), CelebrationKind.SUNDAY, privileged = true, sunday = true), Season.PASSIONTIDE, 5)
            val sorrows = if (wd == FRIDAY) listOf(commemoration("Tempora/Quad5-5Feriac", "seven_sorrows_friday", "Sept Douleurs de la Bienheureuse Vierge Marie", "Septem Dolorum B. M. V.", WHITE)) else emptyList()
            return Base(tempora("Tempora/Quad5-${wd.n()}", "passion_week_${wd.id()}", "${feriaFr(wd)} de la semaine de la Passion", null, 40, listOf(VIOLET), CelebrationKind.FERIA, privileged = true), Season.PASSIONTIDE, 5, extra = sorrows)
        }
        val flags = setOf(DayFlag.HOLY_WEEK)
        return when (wd) {
            SUNDAY -> Base(tempora("Tempora/Quad6-0", "palm_sunday_of_the_passion_of_the_lord", "2e dimanche de la Passion ou des Rameaux", "Dominica II Passionis seu in Palmis", 15, listOf(VIOLET, RED), CelebrationKind.SUNDAY, privileged = true, sunday = true), Season.PASSIONTIDE, 6, flags)
            DayOfWeek.THURSDAY -> Base(tempora("Tempora/Quad6-4", "thursday_of_the_lords_supper", "Jeudi saint", "Feria V in Cena Domini", 11, listOf(WHITE), CelebrationKind.FERIA), Season.PASSIONTIDE, 6, flags)
            FRIDAY -> Base(tempora("Tempora/Quad6-5", "friday_of_the_passion_of_the_lord", "Vendredi saint", "Feria VI in Passione et Morte Domini", 11, listOf(BLACK, VIOLET), CelebrationKind.FERIA), Season.PASSIONTIDE, 6, flags)
            SATURDAY -> Base(tempora("Tempora/Quad6-6", "holy_saturday", "Samedi saint", "Sabbato Sancto", 11, listOf(VIOLET, WHITE), CelebrationKind.FERIA), Season.PASSIONTIDE, 6, flags)
            else -> Base(tempora("Tempora/Quad6-${wd.n()}", "holy_${wd.id()}", "${feriaFr(wd)} saint", "${Latin.feria(wd)} Hebdomadae Sanctae", 16, listOf(VIOLET), CelebrationKind.FERIA), Season.PASSIONTIDE, 6, flags)
        }
    }

    private fun paschaltide(t: Year, d: LocalDate): Base {
        val wd = d.dayOfWeek
        val days = daysBetween(t.easter, d)
        val week = days / 7
        return when {
            days == 0 -> Base(tempora("Tempora/Pasc0-0", "easter_sunday", "Dimanche de Pâques", "Dominica Resurrectionis", 10, listOf(WHITE), CelebrationKind.SUNDAY), Season.EASTER, 0)
            days < 7 -> Base(tempora("Tempora/Pasc0-${wd.n()}", "easter_${wd.id()}", "${feriaFr(wd)} de Pâques", null, 17, listOf(WHITE), CelebrationKind.OCTAVE_DAY), Season.EASTER, 0, setOf(DayFlag.OCTAVE))
            days == 7 -> Base(tempora("Tempora/Pasc1-0", "low_sunday", "Dimanche in albis, octave de Pâques", "Dominica in Albis", 15, listOf(WHITE), CelebrationKind.SUNDAY, privileged = true, sunday = true), Season.EASTER, 1)
            d == t.ascension -> Base(tempora("Tempora/Pasc5-4", "ascension_of_the_lord", "Ascension de Notre-Seigneur", "In Ascensione Domini", 12, listOf(WHITE), CelebrationKind.FEAST, lord = true), Season.EASTER, 5)
            d == t.ascension.minusDays(1) -> Base(tempora("Tempora/Pasc5-3", "vigil_of_the_ascension_of_the_lord", "Vigile de l'Ascension", "In Vigilia Ascensionis", 30, listOf(WHITE), CelebrationKind.VIGIL), Season.EASTER, 5, setOf(DayFlag.VIGIL))
            d == t.ascension.minusDays(3) -> Base(tempora("Tempora/Pasc5-1", "rogation_monday", "Lundi des Rogations", "Feria II in Rogationibus", 51, listOf(VIOLET), CelebrationKind.FERIA, feria4 = true), Season.EASTER, 5, setOf(DayFlag.ROGATION))
            d == t.pentecost.minusDays(1) -> Base(tempora("Tempora/Pasc6-6", "vigil_of_pentecost", "Vigile de la Pentecôte", "Sabbato in Vigilia Pentecostes", 18, listOf(RED), CelebrationKind.VIGIL), Season.EASTER, 6, setOf(DayFlag.VIGIL))
            d == t.pentecost -> Base(tempora("Tempora/Pasc7-0", "pentecost_sunday", "Dimanche de la Pentecôte", "Dominica Pentecostes", 10, listOf(RED), CelebrationKind.SUNDAY), Season.EASTER, 7)
            d > t.pentecost -> {
                val ember = wd == WEDNESDAY || wd == FRIDAY || wd == SATURDAY
                Base(
                    tempora("Tempora/Pasc7-${wd.n()}", "pentecost_${wd.id()}", "${feriaFr(wd)} de la Pentecôte" + if (ember) ", Quatre-Temps" else "", null, 17, listOf(RED), if (ember) CelebrationKind.EMBER_DAY else CelebrationKind.OCTAVE_DAY),
                    Season.EASTER, 7, setOfNotNull(DayFlag.OCTAVE, DayFlag.EMBER_DAY.takeIf { ember }),
                )
            }
            wd == SUNDAY -> {
                val ordinal = if (week == 6) "Dimanche après l'Ascension" else "${French.ordinal(week)} dimanche après Pâques"
                Base(tempora("Tempora/Pasc$week-0", if (week == 6) "after_ascension_sunday" else "after_easter_${week}_sunday", ordinal, if (week == 6) "Dominica post Ascensionem" else "Dominica ${Latin.roman(week)} post Pascha", 31, listOf(WHITE), CelebrationKind.SUNDAY, privileged = true, sunday = true), Season.EASTER, week)
            }
            else -> Base(tempora("Tempora/Pasc$week-${wd.n()}", "after_easter_${week}_${wd.id()}", "${feriaFr(wd)} de la ${French.ordinalFem(week)} semaine après Pâques", null, 51, listOf(WHITE), CelebrationKind.FERIA, feria4 = true), Season.EASTER, week)
        }
    }

    private fun afterPentecost(t: Year, d: LocalDate): Base {
        val wd = d.dayOfWeek
        val n = daysBetween(t.pentecost, d.sundayOnOrBefore()) / 7
        val last = daysBetween(t.pentecost, t.lastPentecostSunday) / 7
        // Le 24ᵉ dimanche est toujours le dernier ; s'il y a plus de 24 dimanches, les dimanches après l'Épiphanie
        // omis en janvier sont repris entre le 23ᵉ et le dernier.
        if (d == t.christTheKing) return Base(tempora("Sancti/10-DU", "our_lord_jesus_christ_king_of_the_universe", "Notre-Seigneur Jésus-Christ Roi", "D. N. Iesu Christi Regis", 12, listOf(WHITE), CelebrationKind.FEAST, lord = true), Season.TIME_AFTER_PENTECOST, n)
        if (n in 24 until last) {
            val resumed = epiphanyWeek(d, n - last + 7, Season.TIME_AFTER_PENTECOST)
            val key = resumed.candidate.celebration.mass!!.properId.replace("Tempora/Epi", "Tempora/PentEpi")
            return Base(resumed.candidate.copy(celebration = resumed.candidate.celebration.copy(mass = MassRef(key))), resumed.season, n, resumed.flags)
        }
        val week = if (n == last) 24 else n
        val k = week.toString().padStart(2, '0')
        val season = Season.TIME_AFTER_PENTECOST
        return when {
            n == 1 && wd == SUNDAY -> Base(tempora("Tempora/Pent01-0", "most_holy_trinity", "Très Sainte Trinité", "Sanctissimae Trinitatis", 12, listOf(WHITE), CelebrationKind.FEAST, lord = true), season, 1)
            n == 1 && wd == DayOfWeek.THURSDAY -> Base(tempora("Tempora/Pent01-4", "most_holy_body_and_blood_of_christ", "Fête-Dieu, le Très Saint-Sacrement", "Ss.mi Corporis Christi", 12, listOf(WHITE), CelebrationKind.FEAST, lord = true), season, 1)
            n == 2 && wd == FRIDAY -> Base(tempora("Tempora/Pent02-5", "most_sacred_heart_of_jesus", "Sacré-Cœur de Jésus", "Ss.mi Cordis D. N. Iesu Christi", 12, listOf(WHITE), CelebrationKind.FEAST, lord = true), season, 2)
            d >= t.emberSeptember && d <= t.emberSeptember.plusDays(3) && wd != DayOfWeek.THURSDAY ->
                Base(tempora("Tempora/093-${wd.n()}", "september_ember_${wd.id()}", "${feriaFr(wd)} des Quatre-Temps de septembre", null, 34, listOf(VIOLET), CelebrationKind.EMBER_DAY, privileged = true), season, week, setOf(DayFlag.EMBER_DAY))
            wd == SUNDAY -> Base(tempora("Tempora/Pent$k-0", "after_pentecost_${week}_sunday", "${French.ordinal(week)} dimanche après la Pentecôte", "Dominica ${Latin.roman(week)} post Pentecosten", 31, listOf(GREEN), CelebrationKind.SUNDAY, privileged = true, sunday = true), season, week)
            else -> Base(tempora("Tempora/Pent$k-${wd.n()}", "after_pentecost_${week}_${wd.id()}", "${feriaFr(wd)} de la ${French.ordinalFem(week)} semaine après la Pentecôte", null, 51, listOf(GREEN), CelebrationKind.FERIA, feria4 = true), season, week)
        }
    }

    private fun advent(t: Year, d: LocalDate): Base {
        val wd = d.dayOfWeek
        val week = daysBetween(t.advent1, d) / 7 + 1
        return when {
            d.dayOfMonth == 24 && d.monthValue == 12 -> Base(tempora("Sancti/12-24", "vigil_of_the_nativity_of_the_lord", "Vigile de la Nativité", "In Vigilia Nativitatis Domini", 14, listOf(VIOLET), CelebrationKind.VIGIL), Season.ADVENT, week, setOf(DayFlag.VIGIL))
            wd == SUNDAY -> {
                val gaudete = d == t.gaudete
                Base(
                    tempora("Tempora/Adv$week-0", "advent_${week}_sunday", "${French.ordinal(week)} dimanche de l'Avent", "Dominica ${Latin.roman(week)} Adventus", 15, if (gaudete) listOf(ROSE, VIOLET) else listOf(VIOLET), CelebrationKind.SUNDAY, privileged = true, sunday = true),
                    Season.ADVENT, week, if (gaudete) setOf(DayFlag.GAUDETE_LAETARE) else emptySet(),
                )
            }
            week == 3 && (wd == WEDNESDAY || wd == FRIDAY || wd == SATURDAY) ->
                Base(tempora("Tempora/Adv3-${wd.n()}", "advent_ember_${wd.id()}", "${feriaFr(wd)} des Quatre-Temps de l'Avent", null, 34, listOf(VIOLET), CelebrationKind.EMBER_DAY, privileged = true), Season.ADVENT, week, setOf(DayFlag.EMBER_DAY))
            d.monthValue == 12 && d.dayOfMonth >= 17 -> Base(tempora("Tempora/Adv$week-${wd.n()}", "advent_december_${d.dayOfMonth}", "${feriaFr(wd)} ${d.dayOfMonth} décembre, férie majeure de l'Avent", null, 34, listOf(VIOLET), CelebrationKind.FERIA, privileged = true), Season.ADVENT, week)
            else -> Base(tempora("Tempora/Adv$week-${wd.n()}", "advent_${week}_${wd.id()}", "${feriaFr(wd)} de la ${French.ordinalFem(week)} semaine de l'Avent", null, 43, listOf(VIOLET), CelebrationKind.FERIA, privileged = true), Season.ADVENT, week)
        }
    }

    private fun christmasOctave(d: LocalDate): Base = when (d.dayOfMonth) {
        25 -> Base(tempora("Sancti/12-25", "nativity_of_the_lord", "Nativité de Notre-Seigneur", "In Nativitate Domini", 10, listOf(WHITE), CelebrationKind.FEAST, lord = true), Season.CHRISTMAS, null)
        else -> if (d.isSunday()) {
            Base(tempora("Tempora/Nat1-0", "sunday_within_the_octave_of_christmas", "Dimanche dans l'octave de Noël", "Dominica infra octavam Nativitatis", 31, listOf(WHITE), CelebrationKind.SUNDAY, privileged = true, sunday = true), Season.CHRISTMAS, null, setOf(DayFlag.OCTAVE))
        } else {
            Base(tempora("Tempora/Nat${d.dayOfMonth}", "christmas_octave_day_${d.dayOfMonth - 23}", "${French.ordinal(d.dayOfMonth - 23)} jour dans l'octave de Noël", null, 33, listOf(WHITE), CelebrationKind.OCTAVE_DAY), Season.CHRISTMAS, null, setOf(DayFlag.OCTAVE))
        }
    }

    private fun baptismOfTheLord() = Candidate(
        Celebration(CelebrationId("baptism_of_the_lord"), LocalizedText("Commémoraison du Baptême de Notre-Seigneur", "In Commemoratione Baptismatis D. N. Iesu Christi"), CelebrationKind.FEAST, R1962.CLASS_II, 30, CelebrationSource.TEMPORAL, LiturgicalColors(WHITE), mass = MassRef("Sancti/01-13"), readings = ReadingsRef(null, fromProper = true)),
        30, temporal = false, lord = true,
    )

    private fun commemoration(key: String, id: String, fr: String, la: String?, color: LiturgicalColor, attachedTo: String? = null, intrinsic: Boolean = false) = Candidate(
        Celebration(CelebrationId(id), LocalizedText(fr, la), CelebrationKind.FEAST, R1962.COMMEMORATION, 60, CelebrationSource.TEMPORAL, LiturgicalColors(color), mass = MassRef(key)),
        60, temporal = false, attachedTo = attachedTo, intrinsic = intrinsic,
    )

    // ── Sanctoral ───────────────────────────────────────────────────────────

    private val firstClassMarian = setOf("12-08", "08-15")

    private fun sanctoralCandidates(s: Sanctoral): List<Candidate> {
        val e = s.entry
        val national = s.source == CelebrationSource.NATIONAL
        val precedence = when (e.rank) {
            "I" -> when {
                e.date == "11-02" -> 20
                e.lord -> 12
                e.date in firstClassMarian -> 13
                national -> 21
                else -> 19
            }
            "II" -> when {
                e.lord -> 30
                national -> 35
                else -> 32
            }
            "III" -> if (national) 42 else 41
            "VIGIL_I" -> 14
            "VIGIL_II" -> 36
            "VIGIL_III" -> 44
            else -> 60 // IV, COMMEMORATION
        }
        val color = if (e.colors.isEmpty()) LiturgicalColors(WHITE) else LiturgicalColors.of(e.colors)
        val vigil = e.rank?.startsWith("VIGIL") == true
        val main = Candidate(
            Celebration(
                id = CelebrationId(e.id), title = e.title?.text() ?: LocalizedText(e.id),
                kind = when { vigil -> CelebrationKind.VIGIL; e.date == "11-02" -> CelebrationKind.ALL_SOULS; else -> CelebrationKind.FEAST },
                rank = rankOf(precedence), precedence = precedence, source = s.source, colors = color,
                mass = MassRef(e.proper ?: "Sancti/${e.date}"), readings = ReadingsRef(null, fromProper = true),
            ),
            precedence, temporal = false, lord = e.lord, vigil = vigil, privileged = e.id == GREATER_LITANIES,
        )
        val attached = e.commemorations.map { c ->
            commemoration(c.proper ?: "Sancti/${e.date}", c.id, c.title?.fr ?: c.id, c.title?.la, color.primary, attachedTo = e.id, intrinsic = c.proper == null)
        }
        return listOf(main) + attached
    }

    /** Années bissextiles : les fêtes du 24 au 28 février reculent d'un jour (jour bissextil, saint Matthias). */
    private fun sanctoralOf(y: Int): Map<LocalDate, List<Candidate>> {
        val leap = JYear.isLeap(y.toLong())
        val out = HashMap<LocalDate, MutableList<Candidate>>()
        val easter = Computus.easter(y)
        sanctoral.year(y).forEach { (date, list) ->
            list.forEach { entry ->
                val d = when {
                    leap && date.monthValue == 2 && date.dayOfMonth in 24..28 -> date.plusDays(1)
                    entry.entry.id == GREATER_LITANIES && (date == easter || date == easter.plusDays(1)) -> easter.plusDays(2)
                    else -> date
                }
                out.getOrPut(d) { mutableListOf() } += sanctoralCandidates(entry)
            }
        }
        return out
    }

    // ── Résolution ──────────────────────────────────────────────────────────

    private fun build(y: Int): Map<LocalDate, LiturgicalDay> {
        val t = Year(y)
        val bases = daysOf(y).associateWith { base(t, it) }
        val sanct = sanctoralOf(y)
        val cands: Map<LocalDate, MutableList<Candidate>> = bases.mapValues { (d, b) ->
            // Le temporal porte déjà Noël, l'Octave, l'Épiphanie et la vigile de Noël : leur doublon du sanctoral s'efface.
            val fixed = d.monthValue == 12 && d.dayOfMonth in 24..25 || d.monthValue == 1 && (d.dayOfMonth == 1 || d.dayOfMonth == 6)
            val fromData = sanct[d].orEmpty().filterNot { fixed && it.attachedTo == null && it.precedence < 30 }
            val baptism = if (d.monthValue == 1 && d.dayOfMonth == 13 && fromData.none { it.celebration.mass?.properId == "Sancti/01-13" }) listOf(baptismOfTheLord()) else emptyList()
            sort((listOf(b.candidate) + b.extra + baptism + fromData).toMutableList())
        }

        // Vigiles de IIᵉ et IIIᵉ classe omises le dimanche (n. 30).
        cands.forEach { (d, list) -> if (d.isSunday()) list.removeAll { it.vigil && it.precedence > 30 } }

        // Défunts un dimanche : au lundi 3 novembre (n. 95).
        // Fêtes de Iʳᵉ classe empêchées : au premier jour libre de Iʳᵉ et IIᵉ classe (n. 95).
        val impeded = cands.flatMap { (d, list) ->
            list.drop(1).filter { !it.temporal && it.classOf == 1 && it.attachedTo == null && !it.vigil }.map { d to it }
        } + cands.filter { (d, list) -> d.isSunday() && list.first().celebration.kind == CelebrationKind.ALL_SOULS }.map { (d, list) -> d to list.first() }
        impeded.forEach { (d, c) ->
            cands.getValue(d).remove(c)
            cands.getValue(d).removeAll { it.attachedTo == c.celebration.id.value }
        }
        impeded.sortedWith(compareBy({ it.first }, { it.second.precedence })).forEach { (d, c) ->
            var target = d.plusDays(1)
            while (target.year == y && (cands.getValue(target).first().precedence < 40)) target = target.plusDays(1)
            if (target.year == y) {
                val list = cands.getValue(target)
                list += c.copy(celebration = c.celebration.copy(transferredFrom = d))
                sort(list)
            }
        }

        // Solennités extérieures : de droit (Sacré-Cœur, Rosaire) et usages de France.
        val externals = external.of(y) { id -> sanct.values.flatten().firstOrNull { it.celebration.id.value == id }?.celebration }

        return bases.mapValues { (d, b) -> resolve(t, d, b, cands.getValue(d), externals[d]) }
    }

    private fun sort(list: MutableList<Candidate>): MutableList<Candidate> {
        list.sortWith(compareBy<Candidate> { it.precedence }.thenBy { if (it.temporal) 0 else 1 })
        // Une commémoraison attachée suit sa fête.
        val attached = list.filter { it.attachedTo != null }
        list.removeAll(attached)
        attached.forEach { a ->
            val i = list.indexOfFirst { it.celebration.id.value == a.attachedTo }
            if (i >= 0) list.add(i + 1, a) else list += a
        }
        return list
    }

    private fun resolve(t: Year, d: LocalDate, b: Base, list: List<Candidate>, externalSolemnity: Celebration?): LiturgicalDay {
        var principal = list.first()
        val rest = list.drop(1).toMutableList()

        // Samedi de la Sainte Vierge : un samedi de férie de IVᵉ classe sans fête (n. 237).
        if (d.dayOfWeek == SATURDAY && principal.feria4) {
            val key = when {
                b.season == Season.ADVENT -> "Commune/C10a"
                d.monthValue == 12 || d < LocalDate.of(t.y, 2, 2) -> "Commune/C10b"
                d < t.palmSunday.plusDays(3) -> "Commune/C10c"
                b.season == Season.EASTER -> "Commune/C10Pasc"
                else -> "Commune/C10"
            }
            rest.add(0, principal)
            principal = Candidate(
                Celebration(CelebrationId("saturday_of_our_lady"), LocalizedText("Sainte Marie au samedi", "S. Maria in Sabbato"), CelebrationKind.FEAST, R1962.CLASS_IV, 50, CelebrationSource.TEMPORAL, LiturgicalColors(WHITE), mass = MassRef(key)),
                50, temporal = true,
            )
        }

        val commemorations = mutableListOf<Celebration>()
        val principalClass = principal.classOf
        // La commémoraison intrinsèque de la fête célébrée ne compte pas dans la limite du n. 111.
        val intrinsic = rest.filter { it.intrinsic && it.attachedTo == principal.celebration.id.value }
        rest.removeAll(intrinsic)
        val noCommemoration = principal.celebration.id.value == "vigil_of_the_nativity_of_the_lord" || (principal.lord && principalClass == 1 && d.isSunday())
        if (!noCommemoration) {
            val limit = when (principalClass) { 1 -> 2; 2 -> 1; else -> 2 }
            for (c in rest) {
                if (commemorations.size >= limit) break
                if (c.feria4 || (c.temporal && c.classOf == 4)) continue
                if (c.vigil && c.classOf >= 2) continue
                val admissible = when (principalClass) {
                    // Iʳᵉ classe : commémoraisons privilégiées seulement (n. 109).
                    1 -> c.privileged || (c.sunday)
                    // IIᵉ classe : une seule ; un dimanche, seulement une fête de IIᵉ classe (n. 110).
                    2 -> if (principal.sunday) c.classOf == 2 && !c.temporal else !(c.lord && c.classOf <= 2)
                    else -> true
                }
                // Une fête du Seigneur cède sans commémoraison à une autre fête du Seigneur (Sainte Famille et Baptême) ;
                // un dimanche, elle en tient la place avec tous ses droits : le dimanche n'est pas commémoré (n. 16).
                if (c.lord && principal.lord) continue
                if (c.sunday && principal.lord) continue
                if (admissible) commemorations += c.celebration
            }
        }

        val principalCelebration = principal.celebration
        val flags = b.flags.toMutableSet()
        // Messe votive de IIᵉ classe : interdite un jour de Iʳᵉ classe, où la solennité est omise, pas déplacée (n. 341).
        val solemnity = externalSolemnity?.takeIf { d.isSunday() && principal.classOf >= 2 }
        if (solemnity != null) flags += DayFlag.EXTERNAL_SOLEMNITY
        if (principalCelebration.transferredFrom != null) flags += DayFlag.TRANSFERRED
        if (d == LocalDate.of(t.y, 4, 25) && d > t.easter.plusDays(7) || d in t.ascension.minusDays(3)..t.ascension.minusDays(1)) flags += DayFlag.ROGATION

        return LiturgicalDay(
            date = d, rite = Rite.ROMAN_1962, season = b.season, week = b.week,
            principal = principalCelebration,
            commemorations = (intrinsic.map { it.celebration } + commemorations.sortedBy { it.precedence }).distinctBy { it.id },
            flags = flags,
            externalSolemnity = solemnity,
        )
    }
}
