// SPDX-License-Identifier: Apache-2.0
package app.paroissien.core.liturgy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay

class ComputusTest {

    @Test
    fun knownEasterDates() {
        val known = mapOf(
            1818 to "1818-03-22", // date la plus précoce possible
            2000 to "2000-04-23",
            2019 to "2019-04-21",
            2024 to "2024-03-31",
            2025 to "2025-04-20",
            2026 to "2026-04-05",
            2027 to "2027-03-28",
            2038 to "2038-04-25", // date la plus tardive possible
        )
        known.forEach { (year, date) -> assertEquals("Pâques $year", LocalDate.parse(date), Computus.easter(year)) }
    }

    @Test
    fun easterIsAlwaysASundayBetweenMarch22AndApril25() {
        for (year in 1583..4099) {
            val easter = Computus.easter(year)
            assertEquals(DayOfWeek.SUNDAY, easter.dayOfWeek)
            val md = MonthDay.from(easter)
            assertTrue("$year : $easter", md >= MonthDay.of(3, 22) && md <= MonthDay.of(4, 25))
        }
    }
}
