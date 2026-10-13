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
 * Seuls les livres nobles ([Dress.NOBLE]) ont un cadre par section ; la Bible est réglée d'un filet rouge, les livres
 * simples et les lettres restent sans cadre.
 */
object BookFrames {

    /** Débuts de mots du titre ou de l'identifiant de l'unité, sans accents, et le cadre qu'ils appellent ; le premier trouvé gagne. */
    private val bySense = listOf(
        "avant la messe" to "avant_messe",
        "litanie" to "litanies",
        "table des matieres" to "candelabres",
        "sommaire" to "candelabres",
        "ordinaire" to "candelabres",
        "matin" to "matin",
        "soir" to "soir",
        "messe" to "pendant_messe",
        "careme" to "passiflores_agneau",
        "passion" to "passiflores_agneau",
        "chemin de croix" to "passiflores_agneau",
        "saint esprit" to "palmes",
        "pentecote" to "palmes",
        "confirmation" to "palmes",
        "saint sacrement" to "vigne_calice",
        "benediction" to "vigne_calice",
        "avent" to "figuier_jacob",
        "noel" to "figuier_jacob",
        "nativite" to "figuier_jacob",
        "evangile" to "lis_vox_dei",
        "ecriture" to "lis_vox_dei",
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

    /**
     * Les cadres courants, dans l'ordre du paroissien, puis les bandeaux gravés du second paroissien, plus légers ; la
     * doublure, le frontispice gravé, le filet rouge et les compositions propres à une page (table, litanies) n'en
     * font pas partie.
     */
    private val cycle = listOf(
        "matin", "soir", "avant_messe", "pendant_messe", "confession", "communion",
        "vepres", "complies", "mariage", "renaissance", "vigne", "gothique",
        "vigne_calice", "figuier_jacob", "lis_vox_dei", "palmes", "passiflores_agneau",
    )

    /**
     * Le cadre d'une unité. [choice] : l'habit choisi dans l'atelier, qui l'emporte sur la règle ; [custom] : les cadres
     * composés par l'utilisateur, qu'on peut choisir comme les autres.
     */
    fun of(
        publication: String,
        entry: LibraryEntry?,
        unitIndex: Int,
        unit: UnitInfo?,
        choice: BookDressChoice? = null,
        custom: List<OrnamentalFrameDef> = emptyList(),
    ): OrnamentalFrameDef? {
        if (entry == null || unit == null) return null
        val frames = (OrnamentalFrames + custom).associateBy { it.name }
        when (choice?.frameMode) {
            FrameMode.NONE -> return null
            FrameMode.SINGLE -> choice.frame?.let { frames[it] }?.let { return it }
            FrameMode.PER_CHAPTER -> choice.chapterFrames[unit.id]?.let { name -> return if (name.isEmpty()) null else frames[name] }
            FrameMode.RANDOM -> {
                val pool = choice.pool.filter { it in frames }.ifEmpty { cycle.filter { it in frames } }
                if (pool.isNotEmpty()) return frames.getValue(pool[("$publication/${unit.id}/${choice.seed}").hashCode().mod(pool.size)])
            }
            FrameMode.AUTO, null -> Unit
        }
        return auto(publication, entry, unitIndex, unit, frames)
    }

    private fun auto(publication: String, entry: LibraryEntry, unitIndex: Int, unit: UnitInfo, frames: Map<String, OrnamentalFrameDef>): OrnamentalFrameDef? {
        when (BookDresses.of(publication, entry)) {
            Dress.NOBLE -> Unit
            Dress.BIBLE -> return frames["filets_rouges"]
            Dress.SIMPLE, Dress.PLAIN -> return null
        }
        val words = plain("${unit.id} ${unit.title.orEmpty()}")
        bySense.firstOrNull { (word, _) -> startsWord(words, word) }?.let { (_, name) -> frames[name]?.let { return it } }
        val available = cycle.filter { it in frames }
        return available.takeIf { it.isNotEmpty() }?.let { frames.getValue(it[unitIndex.mod(it.size)]) }
    }

    /** Le mot commence un mot du titre (« litanie » trouve « litanies », « messe » ne trouve pas « promesse »). */
    private fun startsWord(words: String, word: String): Boolean {
        var i = words.indexOf(word)
        while (i >= 0) {
            if (i == 0 || !words[i - 1].isLetter()) return true
            i = words.indexOf(word, i + 1)
        }
        return false
    }

    private fun plain(s: String) = Normalizer.normalize(s.lowercase().replace('-', ' '), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
}
