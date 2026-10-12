package app.paroissien.core.reading.book

import androidx.compose.ui.graphics.ImageBitmap

import app.paroissien.core.model.text.RichText

/**
 * Blocs lus par le moteur de livre, issus des paquets (ADR 003) ou écrits à la main. Chaque bloc porte l'identifiant
 * stable du paquet : les ancres (ADR 004) le désignent, jamais un numéro de page. Le texte est enrichi (inlines) ;
 * ℣ ℟ ✠ et les numéros de versets en font partie.
 */
enum class BlockLang(val tag: String) {
    FR("fr"), LA("la");

    companion object {
        fun of(tag: String) = entries.firstOrNull { it.tag == tag } ?: FR
    }
}

enum class TextRole { BODY, SMALL, CENTERED }

sealed interface ReadingBlock {
    val id: String

    /** Titre de livre ou de page de titre, centré. */
    data class Title(override val id: String, val text: RichText, val subtitle: RichText? = null) : ReadingBlock

    /** Titre de section en petites capitales. [runningHead] non nul remplace le titre courant des pages suivantes. */
    data class Heading(
        override val id: String,
        val text: RichText,
        val runningHead: String? = null,
        val minor: Boolean = false,
        val lang: BlockLang = BlockLang.FR,
    ) : ReadingBlock

    /** Paragraphe ; [tight] colle le bloc au précédent (versets d'un même paragraphe biblique). */
    data class Paragraph(
        override val id: String,
        val text: RichText,
        val lang: BlockLang,
        val dropCap: Boolean = false,
        val role: TextRole = TextRole.BODY,
        val tight: Boolean = false,
    ) : ReadingBlock

    data class Rubric(override val id: String, val text: RichText) : ReadingBlock

    /** ℣ / ℟ : le symbole fait partie du texte ; la suite des lignes est en retrait. */
    data class Versicle(override val id: String, val text: RichText, val lang: BlockLang = BlockLang.LA) : ReadingBlock

    data class Ornament(override val id: String) : ReadingBlock

    /** Le bloc suivant commence une nouvelle page (début de chapitre, par exemple). */
    data class PageBreak(override val id: String) : ReadingBlock

    /**
     * Vignette gravée en tête de section : l'encre seule, en masque alpha, que la page teinte de l'encre du thème
     * comme une impression. Elle reste avec le début du bloc suivant.
     */
    data class Vignette(override val id: String, val image: ImageBitmap) : ReadingBlock
}

/** Position de lecture ancrée sur le contenu (ADR 004) : bloc et décalage en points de code du texte canonique. */
data class Anchor(val blockId: String, val charOffset: Int)
