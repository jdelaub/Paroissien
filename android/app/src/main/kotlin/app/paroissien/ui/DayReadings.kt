package app.paroissien.ui

import app.paroissien.AppGraph
import app.paroissien.core.content.PackBlock
import app.paroissien.core.data.ReaderSettings
import app.paroissien.core.liturgy.LiturgicalDay
import app.paroissien.core.liturgy.Rite
import app.paroissien.core.model.text.Inline
import app.paroissien.core.model.text.Inlines
import app.paroissien.core.reading.book.ReadingBlock
import app.paroissien.core.reading.book.packToReadingBlocks
import java.time.LocalDate
import kotlinx.serialization.json.contentOrNull
import java.time.DayOfWeek
import java.util.zip.CRC32
import kotlinx.serialization.json.jsonPrimitive

/** Le lectionnaire de la forme ordinaire (paquet privé `lectures-aelf`, saveur personnelle). */
internal const val LECTIONARY_BOOK = "lectionnaire.aelf"

/**
 * Les unités du lectionnaire à essayer pour un jour de 1969, dans l'ordre : le jour daté (un jour que les clés
 * n'expliquent pas), puis la célébration et le jour du temporal, chacun avec le cycle dominical (A, B, C), le cycle
 * férial (I, II), puis sans cycle. La chaîne (sources/aelf/build.py du dépôt privé) range les lectures de l'AELF sous
 * la première de ces clés qui convient à toutes leurs dates : un dimanche sous `ordinary_time_29_sunday-a`, une férie
 * sous `ordinary_time_28_wednesday-ii`, une mémoire sans lectures propres sous sa férie.
 */
fun lectionaryUnits(day: LiturgicalDay): List<String> {
    val sunday = day.cycles?.sunday?.name?.lowercase()
    val weekday = day.cycles?.weekday?.name?.lowercase()
    val bases = listOfNotNull(day.principal.id.value, day.temporal?.id?.value).distinct()
    return listOf("jour-${day.date}") +
        bases.flatMap { base -> listOfNotNull(sunday?.let { "$base-$it" }, weekday?.let { "$base-$it" }, base) }.map(::lectionaryUnitId)
}

/**
 * Lectures proposées en plus de celles du jour : la Dédicace des églises dont on ne connaît pas la date de consécration,
 * que l'AELF donne pour la France le dernier dimanche d'octobre (solennité dans ces églises seulement ; le calendrier
 * garde le dimanche).
 */
fun lectionaryAlternatives(day: LiturgicalDay): List<String> = buildList {
    val d = day.date
    if (d.monthValue == 10 && d.dayOfWeek == DayOfWeek.SUNDAY && d.plusDays(7).monthValue == 11) add("dedication_of_churches_unknown_date")
}

/**
 * Identifiant d'unité d'une clé : la clé, ou au-delà de 40 caractères (certains saints de romcal), ses 31 premiers
 * caractères, un tiret et son CRC32 en hexadécimal, comme `unit_id` de la chaîne.
 */
fun lectionaryUnitId(key: String): String =
    if (key.length <= 40) key else key.take(31) + "-" + "%08x".format(CRC32().apply { update(key.toByteArray()) }.value)

/**
 * L'Évangile d'une unité du lectionnaire : celui de la dernière messe quand le jour en a plusieurs (la messe du jour à
 * Noël, la messe de la Passion aux Rameaux). Les titres des pièces sont `<unité>.ev` ou `<unité>.m<k>.ev` ; sous le
 * titre, la ligne « Évangile de Jésus Christ selon saint Matthieu (Mt 22, 15-21) », la phrase-titre, puis le texte.
 */
fun lectionaryGospel(blocks: List<PackBlock>, unit: String): Gospel? {
    fun piece(b: PackBlock) = b.id.removePrefix("$unit.").split('.')
    val heads = blocks.filter { b -> b.type == "heading" && piece(b).last() == "ev" }
    val head = heads.lastOrNull() ?: return null
    val start = blocks.indexOf(head)
    val body = blocks.drop(start + 1).takeWhile { it.type != "heading" }
    fun plain(b: PackBlock) = b.texts["fr"]?.let(Inlines::plainText)?.trim().orEmpty()
    fun style(b: PackBlock) = b.attrs["style"]?.jsonPrimitive?.contentOrNull
    val incipit = body.firstOrNull { style(it) == "small" } ?: return null
    val title = body.firstOrNull { style(it) == "centered" }
    val text = body.firstOrNull { it.type == "para" && style(it) == null } ?: return null
    return Gospel(head.id, plain(incipit), listOfNotNull(title?.let(::plain), plain(text)).joinToString(" "))
}

/** L'Ordinaire de la messe de 2021 (paquet privé `ordinaire-2021`, saveur personnelle). */
internal const val ORDINARY_2021 = "ordinaire.2021"

/**
 * La messe du jour de 1969 assemblée : les emplacements `lectionarium.*` de l'Ordinaire de 2021 reçoivent les lectures
 * du jour du lectionnaire de l'AELF (la dernière messe du jour s'il y en a plusieurs : la messe du jour à Noël, la messe
 * de la Passion aux Rameaux). Les oraisons et la préface (`missale.*`) n'ont pas de source : leur encadré reste.
 */
object DayReadings {

    /** Pièces du lectionnaire pour chaque emplacement (codes des identifiants, voir sources/aelf/README.md). */
    private val pieces = mapOf(
        "lectionarium.lectio-1" to listOf("l1"),
        "lectionarium.psalmus" to listOf("ps", "ct"),
        "lectionarium.lectio-2" to listOf("l2"),
        "lectionarium.acclamatio" to listOf("seq", "acc"),
        "lectionarium.evangelium" to listOf("ev"),
    )

    fun fill(graph: AppGraph, settings: ReaderSettings, blocks: List<ReadingBlock>): List<ReadingBlock> {
        if (blocks.none { it is ReadingBlock.Slot && it.key?.startsWith("lectionarium.") == true }) return blocks
        val lectionary = graph.library.value?.entry(LECTIONARY_BOOK) ?: return blocks
        val day = graph.calendar.day(LocalDate.now(), settings.calendarConfig().copy(rite = Rite.ROMAN_1969))
        val unit = lectionaryUnits(day).firstNotNullOfOrNull(lectionary::unit) ?: return blocks
        return runCatching { fill(blocks, unit.id, unit.title ?: day.principal.title.fr, graph.packs.blocks(lectionary, unit)) }.getOrDefault(blocks)
    }

    /** Le remplissage lui-même (testable sans l'app) : [readings] sont les blocs de l'unité [unit] du lectionnaire. */
    fun fill(blocks: List<ReadingBlock>, unit: String, title: String, readings: List<PackBlock>): List<ReadingBlock> {
        fun rest(id: String) = id.removePrefix("$unit.").split('.')
        val mass = readings.mapNotNull { rest(it.id).firstOrNull()?.takeIf { p -> Regex("m\\d+").matches(p) } }.distinct()
            .maxByOrNull { it.drop(1).toInt() }
        fun piece(b: PackBlock) = rest(b.id).let { if (mass != null) it.getOrNull(1) else it.firstOrNull() }.orEmpty()
        val own = readings.filter { mass == null || rest(it.id).firstOrNull() == mass }
        val drop = HashSet<String>()
        val fills = HashMap<String, List<ReadingBlock>>()
        var first = true
        for (slot in blocks.filterIsInstance<ReadingBlock.Slot>()) {
            val wanted = pieces[slot.key] ?: continue
            val chosen = own.filter { piece(it) in wanted && it.type != "heading" }
            if (chosen.isEmpty()) {
                // Pas de deuxième lecture en semaine : la section disparaît avec sa réponse.
                drop += slot.omit
                continue
            }
            val text = packToReadingBlocks(chosen, title, "fr", "fr")
            fills[slot.id] = if (first) listOf(ReadingBlock.Rubric("${slot.id}.jour", listOf(Inline.Text("Lectures du jour : $title")))) + text else text
            first = false
        }
        return blocks.flatMap { b ->
            when {
                b.id in drop || drop.any { b.id.startsWith("$it.") } -> emptyList()
                b is ReadingBlock.Slot -> fills[b.id] ?: listOf(b)
                else -> listOf(b)
            }
        }
    }
}
