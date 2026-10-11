// SPDX-License-Identifier: Apache-2.0
package app.paroissien.core.liturgy

import java.time.LocalDate

/** Comput pascal grégorien (algorithme anonyme dit de Meeus/Jones/Butcher), valable à partir de 1583. */
object Computus {

    fun easter(year: Int): LocalDate {
        require(year >= 1583) { "Calendrier grégorien seulement (année ≥ 1583) : $year" }
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }
}
