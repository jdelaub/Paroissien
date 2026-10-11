package app.paroissien.core.model.text

import java.text.Normalizer
import java.util.Locale

/**
 * Pliage de recherche fold-v1 (ADR 004), appliqué au texte indexé comme à la requête.
 * Doit rester identique à content/pipeline/paroissien_content/fold.py : les deux implémentations
 * sont vérifiées contre content/schema/fold-vectors.json.
 */
object Fold {
    const val SPEC = "fold-v1"

    // Césure conditionnelle, espace sans chasse, gluon : invisibles, ils ne doivent pas couper les mots.
    private val invisible = setOf('­', '​', '⁠')

    fun fold(text: String): String {
        val visible = text.filterNot { it in invisible }
        val lower = Normalizer.normalize(visible, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
        val decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD)
        val bare = buildString(decomposed.length) {
            var i = 0
            while (i < decomposed.length) {
                val cp = decomposed.codePointAt(i)
                if (Character.getType(cp) != Character.NON_SPACING_MARK.toInt()) appendCodePoint(cp)
                i += Character.charCount(cp)
            }
        }
        return bare
            .replace("œ", "oe").replace("æ", "ae").replace("ĳ", "ij")
            .replace('j', 'i').replace('v', 'u')
    }
}
