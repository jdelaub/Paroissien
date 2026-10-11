package app.paroissien.library

import android.content.Context
import app.paroissien.core.content.CatalogVerifier
import app.paroissien.core.content.InstalledPack
import app.paroissien.core.content.PackBlock
import app.paroissien.core.content.PackInstaller
import app.paroissien.core.content.PackReader
import app.paroissien.core.content.PublicationInfo
import app.paroissien.core.content.SearchHit
import app.paroissien.core.content.TocEntry
import app.paroissien.core.content.UnitInfo
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/** Clé publique de test du catalogue (content/schema/keys) ; la clé de production viendra avec les téléchargements (V1). */
private const val TEST_KEY = """-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEiVURn0MQFXz2rwjWRi7Dq89o0Sfy
cBLckl6rsW4RjBgyhta5pFsYnrbYvVNE3abCG+bsvQ9fruGYDU+kUmEXdA==
-----END PUBLIC KEY-----"""

/** Une publication installée : son paquet, ses métadonnées et ses unités de lecture. */
data class LibraryEntry(val pack: InstalledPack, val publication: PublicationInfo, val units: List<UnitInfo>) {
    val title: String get() = publication.title["fr"] ?: publication.title.values.firstOrNull() ?: publication.id

    /** Série à laquelle appartient la publication (les lettres de Clairval), ou null. */
    val series: String? get() = publication.meta["series"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull

    val issued: String? get() = publication.meta["issued"]?.jsonPrimitive?.contentOrNull

    fun unit(id: String?): UnitInfo? = units.firstOrNull { it.id == id }

    fun unitTitle(id: String?): String = unit(id)?.title ?: title
}

data class Library(val entries: List<LibraryEntry>, val problems: List<String>) {
    fun entry(publication: String): LibraryEntry? = entries.firstOrNull { it.publication.id == publication }
}

/** Un passage trouvé, avec la publication qui le contient. */
data class SearchResult(val entry: LibraryEntry, val hit: SearchHit)

/**
 * Livres de l'application : installe les paquets publics (assets packs/) et, dans la saveur personnelle, les paquets
 * privés (assets packs-private/), puis les lit à la demande. Toutes les méthodes sont à appeler hors du fil principal.
 */
class PackLibrary(private val context: Context) {

    fun load(): Library {
        val keys = listOf(CatalogVerifier.publicKey(TEST_KEY))
        val entries = ArrayList<LibraryEntry>()
        val problems = ArrayList<String>()
        for (directory in listOf("packs", "packs-private")) {
            if (context.assets.list(directory).isNullOrEmpty()) continue
            val installation = PackInstaller({ context.assets.open(it) }, File(context.noBackupFilesDir, directory), keys, directory).install()
            problems += installation.problems
            for (pack in installation.packs) {
                runCatching {
                    PackReader.open(pack.file).use { reader ->
                        reader.publications().forEach { entries += LibraryEntry(pack, it, reader.units(it.id)) }
                    }
                }.onFailure { problems += "« ${pack.entry.title["fr"] ?: pack.entry.packageId} » ne s'ouvre pas" }
            }
        }
        return Library(entries, problems)
    }

    fun blocks(entry: LibraryEntry, unit: UnitInfo): List<PackBlock> =
        PackReader.open(entry.pack.file).use { it.blocks(entry.publication.id, unit.id) }

    fun toc(entry: LibraryEntry, language: String): List<TocEntry> =
        PackReader.open(entry.pack.file).use { it.toc(entry.publication.id, language) }

    /** Cherche dans tous les livres, paquet par paquet ; les passages restent groupés par publication. */
    fun search(library: Library, query: String, perPack: Int = 60): List<SearchResult> =
        library.entries.groupBy { it.pack.file }.flatMap { (file, entries) ->
            val byId = entries.associateBy { it.publication.id }
            runCatching { PackReader.open(file).use { it.search(query, perPack) } }.getOrDefault(emptyList())
                .mapNotNull { hit -> byId[hit.publication]?.let { SearchResult(it, hit) } }
        }
}
