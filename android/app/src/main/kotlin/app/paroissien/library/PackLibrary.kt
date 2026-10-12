package app.paroissien.library

import android.content.Context
import app.paroissien.core.content.Catalog
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
import java.io.InputStream

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

/**
 * Un dossier de paquets (« packs », « packs-private ») : son catalogue signé, tel quel, et les paquets installés à
 * partir de lui. [imported] : venu d'une sauvegarde (`files/livres/`) et non des assets de l'APK. Sert à la sauvegarde
 * complète, qui recopie catalogue, signature et paquets pour qu'une app vide les réinstalle en les vérifiant.
 */
class PackSource(val name: String, val imported: Boolean, val catalog: ByteArray, val signature: ByteArray, val packs: List<InstalledPack>)

data class Library(val entries: List<LibraryEntry>, val problems: List<String>, val sources: List<PackSource> = emptyList()) {
    fun entry(publication: String): LibraryEntry? = entries.firstOrNull { it.publication.id == publication }
}

/** Un passage trouvé, avec la publication qui le contient. */
data class SearchResult(val entry: LibraryEntry, val hit: SearchHit)

/**
 * Livres de l'application : installe les paquets publics (assets packs/) et, dans la saveur personnelle, les paquets
 * privés (assets packs-private/), puis ceux qu'une sauvegarde a apportés (`files/livres/<dossier>/`), et les lit à la
 * demande. Un même paquet présent deux fois ne compte qu'une fois, dans sa version la plus récente. Toutes les
 * méthodes sont à appeler hors du fil principal.
 */
class PackLibrary(private val context: Context) {

    private val keys by lazy { listOf(CatalogVerifier.publicKey(TEST_KEY)) }

    /** Dossier des livres apportés par une sauvegarde : un sous-dossier par catalogue. */
    val importedRoot: File get() = File(context.filesDir, "livres")

    fun load(): Library {
        val problems = ArrayList<String>()
        val sources = ArrayList<PackSource>()
        fun install(name: String, imported: Boolean, open: (String) -> InputStream, root: File) {
            val installation = PackInstaller(open, root, keys, name).install()
            problems += installation.problems
            val catalog = runCatching { open("$name/catalog.json").use { it.readBytes() } }.getOrNull()
            val signature = runCatching { open("$name/catalog.json.sig").use { it.readBytes() } }.getOrNull()
            if (catalog != null && signature != null) sources += PackSource(name, imported, catalog, signature, installation.packs)
        }
        for (directory in listOf("packs", "packs-private")) {
            if (context.assets.list(directory).isNullOrEmpty()) continue
            install(directory, imported = false, { context.assets.open(it) }, File(context.noBackupFilesDir, directory))
        }
        importedRoot.listFiles()?.filter { File(it, "catalog.json").isFile }?.sortedBy { it.name }?.forEach { dir ->
            install(dir.name, imported = true, { File(importedRoot, it).inputStream() }, dir)
        }
        // Un paquet présent dans l'APK et dans une sauvegarde : la version la plus récente, celle de l'APK à égalité.
        val kept = LinkedHashMap<String, InstalledPack>()
        for (pack in sources.flatMap { it.packs }) {
            val other = kept[pack.entry.packageId]
            if (other == null || pack.entry.packageVersion > other.entry.packageVersion) kept[pack.entry.packageId] = pack
        }
        val entries = ArrayList<LibraryEntry>()
        for (pack in kept.values) {
            runCatching {
                PackReader.open(pack.file).use { reader ->
                    reader.publications().forEach { entries += LibraryEntry(pack, it, reader.units(it.id)) }
                }
            }.onFailure { problems += "« ${pack.entry.title["fr"] ?: pack.entry.packageId} » ne s'ouvre pas" }
        }
        return Library(entries, problems, sources)
    }

    /**
     * Installe les catalogues d'une sauvegarde dépliée dans [staged] (`<staged>/<dossier>/catalog.json`, sa signature et
     * ses paquets) vers [importedRoot] : signature du catalogue, SHA-256 et intégrité de chaque paquet sont vérifiés
     * comme pour les assets. Un catalogue dont tous les paquets sont déjà là, même version, est sauté. Renvoie le
     * nombre de paquets installés et les problèmes.
     */
    fun importPacks(staged: File, current: Library?): Pair<Int, List<String>> {
        val have = current?.sources?.flatMap { it.packs }?.map { it.entry.packageId to it.entry.packageVersion }?.toSet().orEmpty()
        var count = 0
        val problems = ArrayList<String>()
        staged.listFiles()?.filter { File(it, "catalog.json").isFile && File(it, "catalog.json.sig").isFile }?.forEach { dir ->
            val catalog = runCatching { Catalog.parse(File(dir, "catalog.json").readBytes()) }.getOrNull()
            if (catalog != null && catalog.packs.all { (it.packageId to it.packageVersion) in have }) return@forEach
            val root = File(importedRoot, dir.name)
            val installation = PackInstaller({ File(staged, it).inputStream() }, root, keys, dir.name).install()
            problems += installation.problems
            if (installation.packs.isNotEmpty()) {
                // Le catalogue et sa signature restent à côté des paquets installés : ils seront revérifiés à chaque démarrage.
                File(dir, "catalog.json").copyTo(File(root, "catalog.json"), overwrite = true)
                File(dir, "catalog.json.sig").copyTo(File(root, "catalog.json.sig"), overwrite = true)
                count += installation.packs.size
            }
        }
        return count to problems
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
