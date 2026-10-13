// SPDX-License-Identifier: Apache-2.0
package app.paroissien.core.liturgy

import java.io.File
import java.time.LocalDate
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Le calendrier de 1969 (France) jour par jour, de 2020 à 2045, dans build/calendrier-1969.jsonl : la chaîne du
 * lectionnaire (dépôt privé, sources/aelf) y lit, pour chaque date, la célébration, le jour du temporal et les cycles,
 * pour ranger les lectures de l'AELF sous les clés que l'application calcule elle-même.
 */
class CalendarDumpTest {

    @Test
    fun calendrier1969() {
        val calendars = LiturgicalCalendars.load()
        val config = CalendarConfig(Rite.ROMAN_1969)
        val lines = generateSequence(LocalDate.of(2020, 1, 1)) { it.plusDays(1) }.takeWhile { it.year <= 2045 }.map { date ->
            val day = calendars.day(date, config)
            buildJsonObject {
                put("date", date.toString())
                put("id", day.principal.id.value)
                put("rank", (day.principal.rank as Enum<*>).name)
                put("title", day.principal.title.fr)
                put("temporal", day.temporal?.id?.value)
                put("temporalTitle", day.temporal?.title?.fr)
                put("source", day.principal.source.name)
                put("season", day.season.name)
                put("week", day.week)
                put("optional", buildJsonArray { day.optional.forEach { add(JsonPrimitive(it.id.value)) } })
                put("colors", buildJsonArray { day.colors.all.forEach { add(JsonPrimitive(it.name)) } })
                put("sundayCycle", day.cycles?.sunday?.name)
                put("weekdayCycle", day.cycles?.weekday?.name)
            }.toString()
        }.toList()
        File("build").mkdirs()
        File("build/calendrier-1969.jsonl").writeText(lines.joinToString("\n", postfix = "\n"))
        assertEquals(9497, lines.size)
    }
}
