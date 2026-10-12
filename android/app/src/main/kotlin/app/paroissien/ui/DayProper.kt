package app.paroissien.ui

import app.paroissien.AppGraph
import app.paroissien.core.content.PackBlock
import app.paroissien.core.data.ReaderSettings
import app.paroissien.core.liturgy.Celebration
import app.paroissien.core.liturgy.Rite
import app.paroissien.core.model.text.Inline
import app.paroissien.core.model.text.Inlines
import app.paroissien.core.reading.book.ReadingBlock
import app.paroissien.core.reading.book.packToReadingBlocks
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate

/**
 * La messe du jour assemblée (rapport 03 §2.8, mode « messe intégrée ») : les emplacements d'un Ordinaire ou d'un
 * livret (« Introït — texte de la messe du jour ») reçoivent les pièces de la messe du jour, prises dans le missel de
 * 1962 s'il est dans la bibliothèque (rapport 11 : blocs `slot` résolus par le moteur liturgique). Les pièces du
 * missel portent des identifiants `<unité>[.m<n>].<pièce>…` (content/sources/missel).
 */
object DayProper {

    /** Un formulaire du missel : la messe du jour ou une commémoraison. */
    class Formulary(val unitId: String, val title: String, val blocks: List<PackBlock>)

    /** Ce qu'il faut pour remplir les emplacements : la messe, ses commémoraisons, les préfaces du missel. */
    class Day(val mass: Formulary, val commemorations: List<Formulary>, val prefaces: List<PackBlock>, val primaryLanguage: String)

    /** Pièces du missel pour chaque emplacement ; les Quatre-Temps ont leurs leçons, graduels et oraisons numérotés. */
    private val pieces = mapOf(
        "proprium.introitus" to listOf("introit"),
        "proprium.oratio" to listOf("collecte"),
        "proprium.lectio" to listOf("epitre", "lecon", "lecon-", "oraison-", "graduel-"),
        "proprium.graduale" to listOf("graduel", "alleluia", "trait", "sequence"),
        "proprium.evangelium" to listOf("evangile"),
        "proprium.offertorium" to listOf("offertoire"),
        "proprium.secreta" to listOf("secrete"),
        "proprium.communio" to listOf("communion"),
        "proprium.postcommunio" to listOf("postcommunion", "super-populum"),
    )

    /** Après ces pièces viennent les oraisons des commémoraisons, dans l'ordre. */
    private val commemorated = mapOf("proprium.oratio" to "collecte", "proprium.secreta" to "secrete", "proprium.postcommunio" to "postcommunion")

    /** Emplacements qui disparaissent quand rien ne les remplit : le texte du livre vaut alors. */
    private val optional = setOf("proprium.gloria", "proprium.credo", "proprium.communicantes", "proprium.hancigitur")

    private fun matches(piece: String, wanted: List<String>) = wanted.any { w -> if (w.endsWith("-")) piece.startsWith(w) else piece == w }

    /** [blocks] avec la messe du jour ; sans missel ni messe pour ce jour, les encadrés restent avec leur mention. */
    fun fill(graph: AppGraph, settings: ReaderSettings, blocks: List<ReadingBlock>, language: String, facing: Boolean): List<ReadingBlock> {
        if (blocks.none { it is ReadingBlock.Slot && it.key != null }) return blocks
        return fill(blocks, runCatching { day(graph, settings) }.getOrNull(), language, facing)
    }

    private fun day(graph: AppGraph, settings: ReaderSettings): Day? {
        val missal = graph.library.value?.entry(MISSAL_BOOK) ?: return null
        val day = graph.calendar.day(LocalDate.now(), settings.calendarConfig().copy(rite = Rite.ROMAN_1962))
        fun formulary(c: Celebration): Formulary? {
            val unit = c.mass?.properId?.let(::missalUnit)?.let(missal::unit) ?: return null
            return Formulary(unit.id, c.title.fr, graph.packs.blocks(missal, unit))
        }
        val mass = formulary(day.principal) ?: return null
        val prefaces = missal.unit("prefaces")?.let { graph.packs.blocks(missal, it) }.orEmpty()
        return Day(mass, day.commemorations.mapNotNull(::formulary), prefaces, missal.publication.primaryLanguage)
    }

    /** Le remplissage lui-même (testable sans l'app). */
    fun fill(blocks: List<ReadingBlock>, day: Day?, language: String, facing: Boolean): List<ReadingBlock> {
        val drop = HashSet<String>()
        val fills = HashMap<String, List<ReadingBlock>>()
        for (slot in blocks.filterIsInstance<ReadingBlock.Slot>()) {
            val key = slot.key ?: continue
            if (day == null) {
                if (key in optional) fills[slot.id] = emptyList()
                continue
            }
            resolve(slot, key, day, language, facing, drop)?.let { fills[slot.id] = it }
        }
        return blocks.flatMap { b ->
            when {
                b.id in drop || drop.any { b.id.startsWith("$it.") } -> emptyList()
                b is ReadingBlock.Slot -> fills[b.id] ?: listOf(b)
                else -> listOf(b)
            }
        }
    }

    /** Ce qui prend la place d'un emplacement, ou null pour le laisser tel quel. */
    private fun resolve(slot: ReadingBlock.Slot, key: String, day: Day, language: String, facing: Boolean, drop: MutableSet<String>): List<ReadingBlock>? {
        val mass = select(day.mass)
        fun convert(list: List<PackBlock>, title: String = day.mass.title) =
            packToReadingBlocks(list, title, language, day.primaryLanguage, facing)
                .filterNot { it is ReadingBlock.Heading || (it is ReadingBlock.Facing && it.left is ReadingBlock.Heading) }
        fun box(text: String) = ReadingBlock.Slot(slot.id, listOf(Inline.Text(text)), slot.key)
        fun label() = Inlines.plainText(slot.text).substringBefore(" — ").substringBefore(" – ").trim()
        fun prefaceSection(section: String) = day.prefaces.filter { it.id.startsWith("prefaces.$section.") }
        fun headingOf(section: String) = day.prefaces.firstOrNull { it.id == "prefaces.$section" }?.let { h ->
            (h.texts[language] ?: h.texts["fr"] ?: h.texts.values.firstOrNull())?.let(Inlines::plainText)
        }
        return when (key) {
            // Le Gloria et le Credo suivent la règle du jour : leur texte est omis quand la messe ne les a pas.
            "proprium.gloria", "proprium.credo" -> {
                val name = key.removePrefix("proprium.")
                val rule = mass.blocks.firstOrNull { mass.piece(it.id) == name && it.type == "rubric" } ?: return emptyList()
                val said = rule.attr(name)?.toBooleanStrictOrNull()
                    ?: !(rule.texts["fr"]?.let(Inlines::plainText).orEmpty().contains("ne dit pas"))
                if (!said) drop += slot.omit
                convert(listOf(rule))
            }
            // La préface du jour, en entier ; elle remplace celles que le livre imprime.
            "proprium.praefatio" -> {
                val rubric = mass.blocks.firstOrNull { mass.piece(it.id) == "preface" && it.type == "rubric" } ?: return null
                val named = "${label()} — ${Inlines.plainText(rubric.texts["fr"] ?: rubric.texts.values.first())}"
                val section = rubric.attr("preface") ?: return listOf(box(named))
                val text = prefaceSection(section)
                if (text.isEmpty()) return listOf(box(named))
                drop += slot.replaces
                listOf(box(headingOf(section) ?: label())) + convert(text)
            }
            // Communicantes et Hanc igitur propres (octaves) : ils remplacent le texte commun ; sinon rien ne change.
            "proprium.communicantes", "proprium.hancigitur" -> {
                val attr = if (key == "proprium.communicantes") "communicantes" else "hancIgitur"
                val section = mass.blocks.firstNotNullOfOrNull { it.attr(attr) } ?: return emptyList()
                val text = prefaceSection(section).ifEmpty { return emptyList() }
                drop += slot.replaces
                listOf(box(headingOf(section) ?: label())) + convert(text)
            }
            else -> {
                val wanted = pieces[key] ?: return null
                val chosen = mass.blocks.filter { matches(mass.piece(it.id), wanted) }
                if (chosen.isEmpty()) return null
                // Les oraisons que la messe imprime déjà (commémoraisons portées par le formulaire, comme l'octave de Noël
                // à la Saint-Étienne) ne se répètent pas.
                val printed = mass.blocks.mapNotNull { b -> b.texts["la"]?.let(Inlines::plainText)?.trim()?.takeIf { it.isNotEmpty() } }.toSet()
                val extra = commemorated[key]?.let { piece ->
                    // Une commémoraison du même formulaire que la messe (saint Paul le 22 février) y est déjà ; une messe
                    // entière prise en commémoraison ne donne que sa propre oraison, pas celles qu'elle porte.
                    day.commemorations.filter { it.unitId != day.mass.unitId }.flatMap { c ->
                        val f = select(c)
                        val prayer = f.blocks.filter { f.piece(it.id) == piece && it.type != "heading" }.takeWhile { it.type != "rubric" }
                        val known = prayer.any { b -> b.texts["la"]?.let(Inlines::plainText)?.trim() in printed }
                        if (prayer.isEmpty() || known) emptyList()
                        else listOf(ReadingBlock.Rubric("${slot.id}.${c.unitId}", listOf(Inline.Text("Commémoraison : ${c.title}")))) + convert(prayer, c.title)
                    }
                }.orEmpty()
                listOf(box("${label()} — ${day.mass.title}")) + convert(chosen) + extra
            }
        }
    }

    /** Les blocs d'un formulaire, et la pièce de chacun ; plusieurs messes le même jour : la dernière (du jour, du soir). */
    private class Selected(val blocks: List<PackBlock>, val piece: (String) -> String)

    private fun select(f: Formulary): Selected {
        fun rest(id: String) = id.removePrefix("${f.unitId}.").split('.')
        val mass = f.blocks.mapNotNull { rest(it.id).firstOrNull()?.takeIf { p -> Regex("m\\d+").matches(p) } }.distinct()
            .maxByOrNull { it.drop(1).toInt() }
        val own = f.blocks.filter { b -> mass == null || rest(b.id).firstOrNull() == mass }
        return Selected(own) { id -> rest(id).let { if (mass != null) it.getOrNull(1) else it.firstOrNull() }.orEmpty() }
    }

    private fun PackBlock.attr(key: String) = attrs[key]?.jsonPrimitive?.contentOrNull
}
