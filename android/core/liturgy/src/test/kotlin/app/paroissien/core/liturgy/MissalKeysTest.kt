// SPDX-License-Identifier: Apache-2.0
package app.paroissien.core.liturgy

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Chaque jour de 2020 à 2045 trouve sa messe dans le missel de 1962 : la clé de propre que donne le calendrier, et
 * celles de ses commémoraisons, sont parmi les formulaires du paquet (`content/sources/missel/cles.txt`).
 */
class MissalKeysTest {

    private val file = File(System.getProperty("missal.keys") ?: "../../../content/sources/missel/cles.txt")

    @Test
    fun everyDayHasItsMass() = check { day -> listOf(day.principal) + listOfNotNull(day.externalSolemnity) }

    /** Les oraisons des commémoraisons (fichiers `cc`, `o`, `t` de Divinum Officium) ne sont pas encore dans le missel. */
    @Ignore("à faire : les commémoraisons du missel (collecte, secrète, postcommunion)")
    @Test
    fun everyCommemorationHasItsPrayers() = check { day -> day.commemorations }

    private fun check(celebrations: (LiturgicalDay) -> List<Celebration>) {
        assumeTrue("liste des clés absente : $file", file.exists())
        val keys = file.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toSet()
        val calendars = LiturgicalCalendars.load()
        val missing = sortedMapOf<String, String>()
        for (external in listOf(true, false)) {
            val config = CalendarConfig(Rite.ROMAN_1962, externalSolemnities = external)
            var date = LocalDate.of(2020, 1, 1)
            while (date.year <= 2045) {
                celebrations(calendars.day(date, config)).forEach { c ->
                    val key = c.mass?.properId ?: return@forEach
                    if (key !in keys) missing.putIfAbsent(key, "$date ${c.title.fr}")
                }
                date = date.plusDays(1)
            }
        }
        assertTrue("${missing.size} clé(s) sans formulaire : " + missing.entries.joinToString("\n", "\n") { "${it.key} (${it.value})" }, missing.isEmpty())
    }
}
