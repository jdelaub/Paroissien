package app.paroissien.core.content

import app.paroissien.core.model.text.Fold

/**
 * Recherche plein texte (ADR 004). La saisie n'entre jamais telle quelle dans MATCH : on la plie comme l'index,
 * on la découpe en mots, chacun entre guillemets, le dernier en préfixe (« glori » trouve « glória »).
 * Le surlignage se fait sur le texte original, mot par mot, avec le même pliage.
 */
object SearchQuery {

    /** Mots pliés de la saisie, découpés comme unicode61 : tout ce qui n'est ni lettre ni chiffre sépare. */
    fun tokens(input: String): List<String> =
        Fold.fold(input).split(SEPARATORS).filter { it.isNotEmpty() }.take(MAX_TOKENS)

    /** Expression MATCH de FTS5, ou null si la saisie ne contient aucun mot. */
    fun match(input: String): String? {
        val tokens = tokens(input)
        if (tokens.isEmpty()) return null
        return tokens.mapIndexed { i, t -> if (i == tokens.lastIndex) "\"$t\"*" else "\"$t\"" }.joinToString(" ")
    }

    /** Plages du texte original à surligner : mots dont le pliage égale un mot cherché (préfixe pour le dernier). */
    fun highlights(text: String, input: String): List<IntRange> {
        val tokens = tokens(input)
        if (tokens.isEmpty()) return emptyList()
        val exact = tokens.dropLast(1).toSet()
        val prefix = tokens.last()
        return words(text).filter { range ->
            val folded = Fold.fold(text.substring(range.first, range.last + 1))
            folded in exact || folded.startsWith(prefix)
        }
    }

    /** Mots du texte original : suites de lettres, chiffres et marques combinantes. */
    fun words(text: String): List<IntRange> {
        val out = ArrayList<IntRange>()
        var start = -1
        for (i in text.indices) {
            val inWord = text[i].isLetterOrDigit() || Character.getType(text[i]) == Character.NON_SPACING_MARK.toInt()
            if (inWord && start < 0) start = i
            if (!inWord && start >= 0) {
                out += start until i
                start = -1
            }
        }
        if (start >= 0) out += start until text.length
        return out
    }

    private val SEPARATORS = Regex("[^\\p{L}\\p{N}]+")
    private const val MAX_TOKENS = 8
}

/** Un passage trouvé : le bloc, sa langue, son texte canonique et les mots à surligner. */
data class SearchHit(
    val publication: String,
    val unit: String,
    val blockId: String,
    val lang: String,
    val heading: Boolean,
    val text: String,
    val highlights: List<IntRange>,
)

/** Une entrée du sommaire : une unité, ou un titre de niveau 1 ou 2 dans une unité. */
data class TocEntry(val unit: String, val blockId: String?, val level: Int, val title: String)
