// SPDX-License-Identifier: Apache-2.0
package app.paroissien.core.liturgy

import java.time.DayOfWeek
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

internal fun LocalDate.isSunday() = dayOfWeek == SUNDAY

internal fun LocalDate.sundayOnOrBefore(): LocalDate = with(TemporalAdjusters.previousOrSame(SUNDAY))

internal fun LocalDate.next(day: DayOfWeek): LocalDate = with(TemporalAdjusters.next(day))

internal fun LocalDate.previous(day: DayOfWeek): LocalDate = with(TemporalAdjusters.previous(day))

internal fun daysBetween(from: LocalDate, to: LocalDate) = ChronoUnit.DAYS.between(from, to).toInt()

/** 1ᵉʳ dimanche de l'Avent : le 4ᵉ dimanche avant Noël (commun aux deux rites). */
internal fun firstSundayOfAdvent(year: Int): LocalDate = LocalDate.of(year, 12, 25).previous(SUNDAY).minusWeeks(3)

/** Toutes les dates d'une année civile. */
internal fun daysOf(year: Int): Sequence<LocalDate> =
    generateSequence(LocalDate.of(year, 1, 1)) { it.plusDays(1) }.takeWhile { it.year == year }

internal object French {
    private val weekdays = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")

    fun weekday(day: DayOfWeek) = weekdays[day.value - 1]

    fun capitalizedWeekday(day: DayOfWeek) = weekday(day).replaceFirstChar(Char::uppercaseChar)

    /** « 1er », « 2e »… (sans exposant : les polices de lecture n'ont pas toutes U+1D49). */
    fun ordinal(n: Int) = if (n == 1) "1er" else "${n}e"

    fun ordinalFem(n: Int) = if (n == 1) "1re" else "${n}e"
}

internal object Latin {
    private val romans = listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")

    fun roman(n: Int): String {
        var rest = n
        return buildString { romans.forEach { (v, s) -> while (rest >= v) { append(s); rest -= v } } }
    }

    private val feriae = listOf("Feria II", "Feria III", "Feria IV", "Feria V", "Feria VI", "Sabbato", "Dominica")

    fun feria(day: DayOfWeek) = feriae[day.value - 1]
}

/** Nom anglais du jour dans les identifiants (`ordinary_time_28_monday`, comme romcal). */
internal fun java.time.DayOfWeek.id() = name.lowercase()
