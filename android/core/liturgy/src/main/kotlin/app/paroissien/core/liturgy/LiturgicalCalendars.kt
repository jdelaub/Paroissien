// SPDX-License-Identifier: Apache-2.0
package app.paroissien.core.liturgy

import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate

/**
 * Point d'entrée du calendrier : `day(date, config)` pour l'un ou l'autre rite, en France.
 * Le précepte et la pénitence relèvent du droit en vigueur, quel que soit le missel suivi (rapport 05, §2.1.1) ;
 * la discipline traditionnelle (CIC de 1917) s'y ajoute sur demande, comme facultative (ADR 009).
 */
class LiturgicalCalendars internal constructor(
    sanctoral1962: SanctoralData,
    sanctoral1969: SanctoralData,
) {
    private val c1969 = Calendar1969(sanctoral1969)
    private val c1962 = Calendar1962(sanctoral1962, FrenchExternalSolemnities)
    private val c1962Plain = Calendar1962(sanctoral1962)

    /** Provenance des données, pour « À propos ». */
    val sources: List<String> = (sanctoral1969.sources + sanctoral1962.sources).distinct()

    fun day(date: LocalDate, config: CalendarConfig): LiturgicalDay {
        val raw = when (config.rite) {
            Rite.ROMAN_1969 -> c1969.day(date)
            Rite.ROMAN_1962 -> (if (config.externalSolemnities) c1962 else c1962Plain).day(date)
        }
        return raw.copy(
            obligation = obligation(date),
            penance = penance(date, if (config.traditionalPenance && config.rite == Rite.ROMAN_1962) raw else null),
        )
    }

    fun month(year: Int, month: Int, config: CalendarConfig): List<LiturgicalDay> {
        val first = LocalDate.of(year, month, 1)
        return (0 until first.lengthOfMonth()).map { day(first.plusDays(it.toLong()), config) }
    }

    // ── Droit en vigueur (France) ───────────────────────────────────────────

    /** Dimanches et quatre fêtes d'obligation en France : Noël, Ascension, Assomption, Toussaint (c. 1246 §2). */
    private fun obligation(date: LocalDate): Obligation? {
        val easter = Computus.easter(date.year)
        return when {
            date.dayOfWeek == SUNDAY -> Obligation("Dimanche (c. 1247)")
            date.monthValue == 12 && date.dayOfMonth == 25 -> Obligation("Noël, fête d'obligation en France (c. 1246)")
            date == easter.plusDays(39) -> Obligation("Ascension, fête d'obligation en France (c. 1246)")
            date.monthValue == 8 && date.dayOfMonth == 15 -> Obligation("Assomption, fête d'obligation en France (c. 1246)")
            date.monthValue == 11 && date.dayOfMonth == 1 -> Obligation("Toussaint, fête d'obligation en France (c. 1246)")
            else -> null
        }
    }

    /**
     * c. 1250-1253 : jeûne et abstinence le mercredi des Cendres et le vendredi saint ; abstinence chaque vendredi,
     * sauf une solennité ; en France, hors Carême, l'abstinence peut être remplacée par un autre acte de pénitence.
     */
    private fun penance(date: LocalDate, traditional1962: LiturgicalDay?): Penance {
        val easter = Computus.easter(date.year)
        val ash = easter.minusDays(46)
        val goodFriday = easter.minusDays(2)
        val lent = date >= ash && date < easter
        val base = when {
            date == ash || date == goodFriday -> Penance(fast = true, abstinence = true)
            date.dayOfWeek == FRIDAY && c1969.day(date).principal.rank != Rank.R1969.SOLEMNITY -> Penance(abstinence = true, substitutable = !lent)
            else -> Penance.NONE
        }
        return if (traditional1962 == null) base else base.copy(traditional = traditional(date, traditional1962, easter))
    }

    /**
     * CIC de 1917, can. 1252 : jeûne et abstinence le mercredi des Cendres, les vendredis et samedis de Carême,
     * aux Quatre-Temps et aux vigiles de la Pentecôte, de l'Assomption, de la Toussaint et de Noël ; jeûne seul
     * les autres jours de Carême ; abstinence seule les autres vendredis. Cessent les dimanches et fêtes d'obligation
     * hors Carême ; une vigile n'est pas anticipée. Le samedi saint, ils cessent à midi.
     */
    private fun traditional(date: LocalDate, day: LiturgicalDay, easter: LocalDate): TraditionalPenance? {
        val wd = date.dayOfWeek
        val ash = easter.minusDays(46)
        val lent = date >= ash && date < easter
        if (wd == SUNDAY) return null
        if (!lent && obligation(date) != null) return null
        val vigil = when {
            date.monthValue == 12 && date.dayOfMonth == 24 -> "vigile de Noël"
            date == easter.plusDays(48) -> "vigile de la Pentecôte"
            date.monthValue == 8 && date.dayOfMonth == 14 -> "vigile de l'Assomption"
            date.monthValue == 10 && date.dayOfMonth == 31 -> "vigile de la Toussaint"
            else -> null
        }
        return when {
            date == ash -> TraditionalPenance(fast = true, abstinence = true, reason = "Mercredi des Cendres")
            DayFlag.EMBER_DAY in day.flags -> TraditionalPenance(fast = true, abstinence = true, reason = "Quatre-Temps")
            vigil != null -> TraditionalPenance(fast = true, abstinence = true, reason = vigil.replaceFirstChar(Char::uppercaseChar))
            lent && (wd == FRIDAY || wd == SATURDAY) -> TraditionalPenance(fast = true, abstinence = true, reason = if (date == easter.minusDays(1)) "Samedi saint, jusqu'à midi" else "Carême")
            lent -> TraditionalPenance(fast = true, abstinence = false, reason = "Carême")
            wd == FRIDAY -> TraditionalPenance(fast = false, abstinence = true, reason = "Vendredi")
            else -> null
        }
    }

    companion object {
        /** Charge le sanctoral embarqué (ressources `calendar/` du jar). */
        fun load(): LiturgicalCalendars = LiturgicalCalendars(SanctoralData.load(Rite.ROMAN_1962), SanctoralData.load(Rite.ROMAN_1969))
    }
}
