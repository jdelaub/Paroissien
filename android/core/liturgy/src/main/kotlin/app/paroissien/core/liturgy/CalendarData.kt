// SPDX-License-Identifier: Apache-2.0
package app.paroissien.core.liturgy

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** Une entrée du sanctoral à date fixe (`content/schema/calendar-v1.md`). */
@Serializable
internal data class SanctoralEntry(
    val id: String,
    val date: String,
    val rank: String? = null,
    val colors: List<LiturgicalColor> = emptyList(),
    val title: Title? = null,
    val lord: Boolean = false,
    val validFrom: Int? = null,
    val validUntil: Int? = null,
    val proper: String? = null,
    val commemorations: List<Commemoration> = emptyList(),
    val saints: List<String> = emptyList(),
    val remove: Boolean = false,
    val obligation: Boolean = false,
    val patron: String? = null,
) {
    val month: Int get() = date.substring(0, 2).toInt()
    val day: Int get() = date.substring(3, 5).toInt()

    fun validIn(year: Int) = (validFrom == null || year >= validFrom) && (validUntil == null || year <= validUntil)

    /** La date dans l'année civile, ou `null` (29 février d'une année commune). */
    fun on(year: Int): LocalDate? = runCatching { LocalDate.of(year, month, day) }.getOrNull()
}

@Serializable
internal data class Title(val fr: String, val la: String? = null) {
    fun text() = LocalizedText(fr, la)
}

@Serializable
internal data class Commemoration(val id: String, val title: Title? = null, val proper: String? = null)

@Serializable
internal data class CalendarFile(
    val format: Int,
    val rite: String,
    val calendar: String,
    val sources: List<String> = emptyList(),
    val entries: List<SanctoralEntry>,
)

/** Une entrée fusionnée, avec son origine ; [overridesGeneral] : une entrée nationale qui remplace une générale. */
internal data class Sanctoral(val entry: SanctoralEntry, val source: CelebrationSource, val overridesGeneral: Boolean = false)

/** Sanctoral d'un rite : calendrier général, puis propre national par-dessus (`remove`, remplacement, ajout). */
internal class SanctoralData(val entries: List<Sanctoral>, val sources: List<String>) {

    /** Les entrées de l'année, groupées par date. */
    fun year(year: Int): Map<LocalDate, List<Sanctoral>> =
        entries.filter { it.entry.validIn(year) }
            .mapNotNull { s -> s.entry.on(year)?.let { it to s } }
            .groupBy({ it.first }, { it.second })

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): CalendarFile = json.decodeFromString(CalendarFile.serializer(), text)

        fun load(rite: Rite, country: String? = "fr"): SanctoralData {
            val general = resource("calendar/${rite.code}/general.json")?.let(::parse)
                ?: error("Sanctoral ${rite.code} absent du jar (content/data/calendar)")
            val national = country?.let { resource("calendar/${rite.code}/$it.json") }?.let(::parse)
            return merge(general, national)
        }

        fun merge(general: CalendarFile, national: CalendarFile?): SanctoralData {
            val byId = LinkedHashMap<String, Sanctoral>()
            general.entries.forEach { byId[it.id] = Sanctoral(it, CelebrationSource.GENERAL) }
            national?.entries?.forEach { n ->
                val base = byId[n.id]?.entry
                when {
                    n.remove -> byId.remove(n.id)
                    base != null -> byId[n.id] = Sanctoral(inherit(n, base), CelebrationSource.NATIONAL, overridesGeneral = true)
                    else -> byId[n.id] = Sanctoral(n, CelebrationSource.NATIONAL)
                }
            }
            return SanctoralData(byId.values.toList(), general.sources + national?.sources.orEmpty())
        }

        /** Une entrée nationale de même `id` remplace la générale ; les champs qu'elle omet sont repris. */
        private fun inherit(n: SanctoralEntry, base: SanctoralEntry) = n.copy(
            rank = n.rank ?: base.rank,
            colors = n.colors.ifEmpty { base.colors },
            title = n.title ?: base.title,
            lord = n.lord || base.lord,
            validFrom = n.validFrom ?: base.validFrom,
            validUntil = n.validUntil ?: base.validUntil,
            proper = n.proper ?: base.proper,
            commemorations = n.commemorations.ifEmpty { base.commemorations },
            saints = n.saints.ifEmpty { base.saints },
        )

        private fun resource(path: String): String? =
            SanctoralData::class.java.classLoader?.getResourceAsStream(path)?.use { it.readBytes().decodeToString() }
    }
}
