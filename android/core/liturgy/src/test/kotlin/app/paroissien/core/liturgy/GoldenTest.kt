// SPDX-License-Identifier: Apache-2.0
package app.paroissien.core.liturgy

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Tests G3 (rapport 05, §3.1) : notre moteur contre les oracles, champ par champ. Toute divergence doit figurer dans
 * `content/golden/calendar/known-divergences.md`, justifiée : aucune n'est acceptée en silence.
 */
class GoldenTest {

    private val dir = File(System.getProperty("calendar.golden") ?: "../../../content/golden/calendar")

    /** Lignes du tableau des divergences connues : | rite | date | champ | … */
    private val known: Set<Triple<String, String, String>> by lazy {
        val file = File(dir, "known-divergences.md")
        if (!file.exists()) return@lazy emptySet()
        file.readLines().filter { it.startsWith("| 19") }.map { line ->
            val cells = line.split('|').map { it.trim() }
            Triple(cells[1], cells[2], cells[3])
        }.toSet()
    }

    private fun lines(name: String): List<JsonObject> {
        val file = File(dir, name)
        assumeTrue("oracle absent : $file", file.exists())
        return file.readLines().filter { it.isNotBlank() }.map { Json.parseToJsonElement(it).jsonObject }
    }

    private fun JsonObject.str(key: String) = get(key)?.jsonPrimitive?.content
    private fun JsonObject.list(key: String) = get(key)?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()

    private fun report(rite: String, mismatches: List<Triple<String, String, String>>, total: Int) {
        // « *-MM-JJ » vaut pour toutes les années : un défaut de l'oracle sur une fête précise.
        val unexplained = mismatches.filter { (date, field, _) -> Triple(rite, date, field) !in known && Triple(rite, "*" + date.drop(4), field) !in known }
        val byField = unexplained.groupBy { it.second }.mapValues { it.value.size }
        // Le détail complet va dans build/ : la sortie de Gradle ne montre que l'échec.
        File("build/golden-$rite.txt").writeText(
            "$rite : ${mismatches.size} écarts sur $total jours, ${unexplained.size} inexpliqués $byField\n" +
                unexplained.joinToString("\n") { (date, field, detail) -> "$date $field $detail" },
        )
        assertTrue("${unexplained.size} écarts inexpliqués avec l'oracle $rite (voir la sortie du test)", unexplained.isEmpty())
    }

    @Test
    fun romcalFrance1969() {
        val calendar = Calendar1969(SanctoralData.load(Rite.ROMAN_1969))
        val oracle = lines("1969-fr-romcal.jsonl")
        val out = mutableListOf<Triple<String, String, String>>()
        for (o in oracle) {
            val date = o.str("date")!!
            val d = calendar.day(LocalDate.parse(date))
            fun check(field: String, expected: Any?, actual: Any?) {
                if (expected != actual) out += Triple(date, field, "oracle=$expected nous=$actual")
            }
            check("id", o.str("id"), d.principal.id.value)
            check("rank", o.str("rank"), (d.principal.rank as Rank.R1969).name)
            check("color", o.list("colors").firstOrNull(), d.colors.primary.name)
            // La dédicace des églises dont on ignore la date est une solennité facultative propre à chaque église.
            check("optional", o.list("optional").filter { it != "dedication_of_consecrated_churches" }.toSet(), d.optional.map { it.id.value }.toSet())
            check("sundayCycle", o.str("sundayCycle"), d.cycles?.sunday?.name)
            check("weekdayCycle", o.str("weekdayCycle"), d.cycles?.weekday?.name)
            check("psalterWeek", o.str("psalterWeek")?.toInt(), d.cycles?.psalterWeek)
        }
        report("1969", out, oracle.size)
    }

    @Test
    fun missalemeum1962() {
        val oracle = lines("1962-missalemeum.jsonl")
        val calendar = Calendar1962(SanctoralData.load(Rite.ROMAN_1962, country = null))
        val out = mutableListOf<Triple<String, String, String>>()
        for (o in oracle) {
            val date = o.str("date")!!
            val d = calendar.day(LocalDate.parse(date))
            fun check(field: String, expected: Any?, actual: Any?) {
                if (expected != actual) out += Triple(date, field, "oracle=$expected nous=$actual")
            }
            check("proper", normalize(o.str("proper")), normalize(d.principal.mass?.properId))
            check("rank", o.str("rank"), classOf(d.principal.rank))
            check("color", o.list("colors").firstOrNull(), d.colors.primary.name)
            // Missalemeum ne garde qu'une commémoraison quand les rubriques en admettent deux (n. 111) : la sienne doit
            // figurer parmi les nôtres.
            val ours = d.commemorations.map { normalize(it.mass?.properId) }
            val theirs = o.list("commemorations").map(::normalize)
            if (!ours.containsAll(theirs) || (theirs.isEmpty() != ours.isEmpty() && ours.size != 2)) check("commemorations", theirs, ours)
        }
        report("1962", out, oracle.size)
    }

    /** `Tempora/Pent01-0r`, `Pasc2-3Feria` : on ne compare que la clé du jour, sans les variantes de fichier. */
    private fun normalize(key: String?): String? = key?.replace("Tempora/PentEpi", "Tempora/Epi")?.replace(Regex("^(Tempora|Sancti|Commune)/([A-Za-z]*\\d+-\\d+|\\d{2}-\\d{2}|\\d{2}-DU|C10)[A-Za-z]*.*$"), "$1/$2")

    private fun classOf(rank: Rank) = when (rank) {
        Rank.R1962.CLASS_I -> "I"
        Rank.R1962.CLASS_II -> "II"
        Rank.R1962.CLASS_III -> "III"
        Rank.R1962.CLASS_IV -> "IV"
        else -> "COMMEMORATION"
    }
}
