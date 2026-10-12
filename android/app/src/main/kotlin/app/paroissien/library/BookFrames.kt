package app.paroissien.library

import app.paroissien.core.content.UnitInfo
import app.paroissien.core.designsystem.frame.OrnamentalFrameDef
import app.paroissien.core.designsystem.frame.OrnamentalFrames
import java.text.Normalizer

/**
 * Quel cadre ornemental pour quelle partie d'un livre (demande de l'utilisateur, 12/10/2026) : la règle suit le
 * sommaire. Chaque unité (section de premier niveau) reçoit un cadre, le même sur toutes ses pages, comme dans le
 * paroissien d'origine où chaque section a le sien. D'abord par le sens (le cadre des prières du matin pour les
 * prières du matin…), sinon en boucle dans l'ordre du sommaire, pour que deux sections voisines diffèrent.
 *
 * La Bible et les lettres restent sans cadre : ce ne sont pas des livres de prières.
 */
object BookFrames {

    /** Mots du titre ou de l'identifiant de l'unité, sans accents, et le cadre qu'ils appellent ; le premier trouvé gagne. */
    private val bySense = listOf(
        "avant la messe" to "avant_messe",
        "matin" to "matin",
        "soir" to "soir",
        "litanie" to "soir",
        "messe" to "pendant_messe",
        "ordinaire" to "pendant_messe",
        "confession" to "confession",
        "penitence" to "confession",
        "contrition" to "confession",
        "communion" to "communion",
        "eucharist" to "communion",
        "marial" to "communion",
        "vierge" to "communion",
        "vepres" to "vepres",
        "complies" to "complies",
        "mariage" to "mariage",
    )

    /** Les cadres courants, dans l'ordre du paroissien ; la doublure et le frontispice gravé n'en font pas partie. */
    private val cycle = listOf(
        "matin", "soir", "avant_messe", "pendant_messe", "confession", "communion",
        "vepres", "complies", "mariage", "renaissance", "vigne", "gothique",
    )

    fun of(publication: String, entry: LibraryEntry?, unitIndex: Int, unit: UnitInfo?): OrnamentalFrameDef? {
        if (entry == null || unit == null) return null
        if (publication.startsWith("bible") || entry.publication.kind == "bible" || entry.series != null) return null
        val frames = OrnamentalFrames.associateBy { it.name }
        val words = plain("${unit.id} ${unit.title.orEmpty()}")
        bySense.firstOrNull { (word, _) -> word in words }?.let { (_, name) -> frames[name]?.let { return it } }
        val available = cycle.filter { it in frames }
        return available.takeIf { it.isNotEmpty() }?.let { frames.getValue(it[unitIndex.mod(it.size)]) }
    }

    private fun plain(s: String) = Normalizer.normalize(s.lowercase().replace('-', ' '), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
}
