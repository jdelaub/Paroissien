package app.paroissien.core.reading.book

/**
 * Blocs lus par le moteur de livre. Modèle provisoire du spike 1b, à aligner sur le paquet v1 (ADR 003, phase 2).
 * Chaque bloc porte un identifiant stable : les ancres (ADR 004) le désignent, jamais un numéro de page.
 * Dans le texte, _…_ marque l'italique ; ℣ ℟ ✠ ✝ sont dessinés en rouge.
 */
enum class BlockLang(val tag: String) { FR("fr"), LA("la") }

enum class TextRole { BODY, SMALL, CENTERED }

sealed interface ReadingBlock {
    val id: String

    /** Titre de livre ou de page de titre, centré. */
    data class Title(override val id: String, val text: String, val subtitle: String? = null) : ReadingBlock

    /** Titre de section en petites capitales. [runningHead] non nul remplace le titre courant des pages suivantes. */
    data class Heading(override val id: String, val text: String, val runningHead: String? = null, val minor: Boolean = false) : ReadingBlock

    data class Paragraph(
        override val id: String,
        val text: String,
        val lang: BlockLang,
        val dropCap: Boolean = false,
        val verse: Int? = null,
        val role: TextRole = TextRole.BODY,
    ) : ReadingBlock

    data class Rubric(override val id: String, val text: String) : ReadingBlock

    data class Versicle(override val id: String, val mark: Char, val text: String, val lang: BlockLang = BlockLang.LA) : ReadingBlock

    data class Ornament(override val id: String) : ReadingBlock

    /** Le bloc suivant commence une nouvelle page (début de chapitre, par exemple). */
    data class PageBreak(override val id: String) : ReadingBlock
}

/** Position de lecture ancrée sur le contenu (ADR 004) : bloc et décalage dans son texte affiché sans césures. */
data class Anchor(val blockId: String, val charOffset: Int)
