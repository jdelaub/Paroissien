package app.paroissien.ui

import app.paroissien.AppGraph
import app.paroissien.core.data.ReaderSettings
import app.paroissien.core.liturgy.Rite
import app.paroissien.core.model.text.Inline
import app.paroissien.core.model.text.Inlines
import app.paroissien.core.reading.book.ReadingBlock
import app.paroissien.core.reading.book.packToReadingBlocks
import java.time.LocalDate

/**
 * Les emplacements d'un livret de messe (« Introït — texte de la messe du jour ») se remplissent avec la messe du jour,
 * prise dans le missel de 1962 s'il est dans la bibliothèque (rapport 11 : blocs `slot` résolus par le moteur
 * liturgique). Les pièces du missel portent des identifiants `<unité>[.m<n>].<pièce>…` (content/sources/missel).
 */
object DayProper {

    /** Pièces du missel pour chaque emplacement ; les Quatre-Temps ont leurs leçons, graduels et oraisons numérotés. */
    private val pieces = mapOf(
        "proprium.introitus" to listOf("introit"),
        "proprium.oratio" to listOf("collecte"),
        "proprium.lectio" to listOf("epitre", "lecon", "lecon-", "oraison-", "graduel-"),
        "proprium.graduale" to listOf("graduel", "alleluia", "trait", "sequence"),
        "proprium.evangelium" to listOf("evangile"),
        "proprium.offertorium" to listOf("offertoire"),
        "proprium.secreta" to listOf("secrete"),
        "proprium.praefatio" to listOf("preface"),
        "proprium.communio" to listOf("communion"),
        "proprium.postcommunio" to listOf("postcommunion", "super-populum"),
    )

    private fun matches(piece: String, wanted: List<String>) = wanted.any { w -> if (w.endsWith("-")) piece.startsWith(w) else piece == w }

    /**
     * [blocks] avec, après chaque emplacement, les pièces de la messe du jour ; l'encadré prend le nom du jour. Sans
     * missel, ou sans messe pour ce jour, les blocs reviennent tels quels.
     */
    fun fill(graph: AppGraph, settings: ReaderSettings, blocks: List<ReadingBlock>, language: String, facing: Boolean): List<ReadingBlock> {
        if (blocks.none { it is ReadingBlock.Slot && it.key != null }) return blocks
        val library = graph.library.value ?: return blocks
        val missal = library.entry(MISSAL_BOOK) ?: return blocks
        val day = graph.calendar.day(LocalDate.now(), settings.calendarConfig().copy(rite = Rite.ROMAN_1962))
        val unit = day.principal.mass?.properId?.let(::missalUnit)?.let(missal::unit) ?: return blocks
        val all = runCatching { graph.packs.blocks(missal, unit) }.getOrNull() ?: return blocks
        // Plusieurs messes le même jour (Noël, Jeudi saint, 2 novembre) : la dernière, celle du jour ou du soir.
        fun rest(id: String) = id.removePrefix(unit.id + ".").split('.')
        val masses = all.mapNotNull { rest(it.id).firstOrNull()?.takeIf { p -> Regex("m\\d+").matches(p) } }.distinct()
        val mass = masses.maxByOrNull { it.drop(1).toInt() }
        val own = all.filter { b -> mass == null || rest(b.id).firstOrNull() == mass }
        fun pieceOf(id: String) = rest(id).let { if (mass != null) it.getOrNull(1) else it.firstOrNull() }.orEmpty()
        val title = day.principal.title.fr
        return blocks.flatMap { block ->
            val wanted = (block as? ReadingBlock.Slot)?.key?.let(pieces::get) ?: return@flatMap listOf(block)
            val chosen = own.filter { matches(pieceOf(it.id), wanted) }
            if (chosen.isEmpty()) return@flatMap listOf(block)
            val label = Inlines.plainText(block.text).substringBefore(" — ").substringBefore(" – ").trim()
            val box = ReadingBlock.Slot(block.id, listOf(Inline.Text("$label — $title")), block.key)
            // Le titre de chaque pièce est déjà dans l'encadré.
            val text = packToReadingBlocks(chosen, unit.title ?: title, language, missal.publication.primaryLanguage, facing)
                .filterNot { it is ReadingBlock.Heading || (it is ReadingBlock.Facing && it.left is ReadingBlock.Heading) }
            listOf(box) + text
        }
    }
}
