package app.paroissien.library

import android.content.Context
import app.paroissien.AppGraph
import app.paroissien.BuildConfig
import app.paroissien.core.data.ReaderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Ce qu'une sauvegarde a écrit ou repris, pour le message à l'utilisateur. */
data class BackupSummary(val books: Int, val images: Int, val marks: Int, val settings: Boolean, val problems: List<String> = emptyList())

@Serializable
internal data class BackupManifest(
    val format: Int,
    val app: String,
    val flavor: String,
    val createdAt: Long,
    /** Titres des paquets, pour savoir ce que contient le fichier sans le déplier. */
    val books: List<String> = emptyList(),
    val images: Int = 0,
)

/** Le format du fichier : noms admis, version. Séparé pour les tests. */
internal object BackupFormat {
    const val VERSION = 1
    const val MANIFEST = "manifeste.json"
    const val SETTINGS = "reglages.json"
    const val DATA = "donnees.json"

    /** Fichiers d'une image pieuse personnelle, tels que [PiousImages] les écrit. */
    val IMAGE = Regex("[A-Za-z0-9_-]+-(front|back|thumb)\\.webp")
    private val BOOK = Regex("livres/[a-z0-9_-]+/[A-Za-z0-9_-][A-Za-z0-9._-]*")
    val VIGNETTE = Regex("catalog\\.json|[A-Za-z0-9_-]+\\.webp")

    /** Rien d'autre n'est déplié : ni chemin absolu, ni « .. », ni fichier inattendu. */
    fun isSafe(name: String): Boolean = name == MANIFEST || name == SETTINGS || name == DATA ||
        (name.startsWith("images/") && IMAGE.matches(name.removePrefix("images/"))) ||
        (BOOK.matches(name) && ".." !in name) ||
        (name.startsWith("vignettes/") && VIGNETTE.matches(name.removePrefix("vignettes/")))

    /** Au-delà, la sauvegarde est refusée : les livres les plus gros font une trentaine de mégaoctets. */
    const val MAX_BYTES = 2L * 1024 * 1024 * 1024
}

/**
 * Sauvegarde complète (ATELIER §8) : un seul zip, écrit et relu par le sélecteur de documents, sans réseau.
 *
 * - `manifeste.json` : format, version de l'app, date, titres des livres ;
 * - `reglages.json` : tous les réglages ;
 * - `donnees.json` : positions, rubans, images posées, images pieuses personnelles (export de [UserDataTransfer]) ;
 * - `images/` : recto, verso et vignette des images pieuses personnelles ;
 * - `livres/<dossier>/` : chaque catalogue signé, sa signature et ses paquets, y compris les contenus privés de la
 *   saveur personnelle (décision de l'utilisateur du 12/10/2026 : une app livrée vide se remplit en important ce
 *   fichier, en attendant les autorisations) ;
 * - `vignettes/` : les vignettes gravées et leur catalogue, s'il y en a.
 *
 * À la restauration, les livres sont revérifiés (signature du catalogue, SHA-256, intégrité) avant d'être installés ;
 * signets et images sont fusionnés (le plus récent l'emporte, rien n'est effacé) ; les réglages sont remplacés.
 */
class Backup(private val context: Context, private val graph: AppGraph) {

    private val cards get() = File(context.filesDir, "cards")

    suspend fun export(out: OutputStream): BackupSummary = withContext(Dispatchers.IO) {
        val library = graph.library.filterNotNull().first()
        val settings = graph.settings.settings.first()
        val data = graph.transfer.export()
        val images = cards.listFiles { f -> BackupFormat.IMAGE.matches(f.name) }.orEmpty().sortedBy { it.name }
        // Un même dossier peut venir de l'APK et d'une sauvegarde : le premier suffit (les paquets sont dédoublonnés).
        // Un catalogue sans paquet installé ne servirait qu'à signaler des livres manquants à la restauration.
        val sources = library.sources.filter { it.packs.isNotEmpty() }.distinctBy { it.name }
        val manifest = BackupManifest(
            format = BackupFormat.VERSION,
            app = BuildConfig.VERSION_NAME,
            flavor = BuildConfig.FLAVOR,
            createdAt = System.currentTimeMillis(),
            books = sources.flatMap { s -> s.packs.map { it.entry.title["fr"] ?: it.entry.packageId } },
            images = images.count { it.name.endsWith("-front.webp") },
        )
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            fun put(name: String, bytes: ByteArray) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
            fun put(name: String, file: File) {
                zip.putNextEntry(ZipEntry(name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            put(BackupFormat.MANIFEST, json.encodeToString(BackupManifest.serializer(), manifest).encodeToByteArray())
            put(BackupFormat.SETTINGS, json.encodeToString(ReaderSettings.serializer(), settings).encodeToByteArray())
            put(BackupFormat.DATA, data.encodeToByteArray())
            images.forEach { put("images/${it.name}", it) }
            for (source in sources) {
                put("livres/${source.name}/catalog.json", source.catalog)
                put("livres/${source.name}/catalog.json.sig", source.signature)
                source.packs.forEach { put("livres/${source.name}/${it.entry.file}", it.file) }
            }
            if (graph.vignettes.catalog.isNotEmpty()) {
                for ((name, open) in graph.vignettes.files()) {
                    zip.putNextEntry(ZipEntry("vignettes/$name"))
                    open().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        BackupSummary(books = manifest.books.size, images = manifest.images, marks = 0, settings = true)
    }

    suspend fun import(input: InputStream): BackupSummary = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restauration").apply { deleteRecursively(); mkdirs() }
        try {
            unzip(input, staging)
            val manifest = File(staging, BackupFormat.MANIFEST).takeIf { it.isFile }
                ?.let { json.decodeFromString(BackupManifest.serializer(), it.readText()) }
                ?: error("ce fichier n'est pas une sauvegarde de Paroissien")
            require(manifest.format <= BackupFormat.VERSION) { "cette sauvegarde vient d'une version plus récente de Paroissien" }

            // 1. Les livres, vérifiés comme ceux de l'APK.
            val (books, problems) = File(staging, "livres").takeIf { it.isDirectory }
                ?.let { graph.packs.importPacks(it, graph.library.value) } ?: (0 to emptyList())

            // 2. Les images pieuses : un fichier déjà là n'est jamais remplacé.
            var images = 0
            File(staging, "images").listFiles()?.filter { BackupFormat.IMAGE.matches(it.name) }?.forEach { file ->
                val target = File(cards.apply { mkdirs() }, file.name)
                if (!target.exists()) {
                    file.copyTo(target)
                    if (file.name.endsWith("-front.webp")) images++
                }
            }

            // 2 bis. Les vignettes gravées, si l'app n'en a pas déjà.
            File(staging, "vignettes").takeIf { File(it, "catalog.json").isFile && graph.vignettes.catalog.isEmpty() }?.let { dir ->
                val target = graph.vignettes.importedDir.apply { mkdirs() }
                dir.listFiles()?.filter { BackupFormat.VIGNETTE.matches(it.name) }?.forEach { it.copyTo(File(target, it.name), overwrite = true) }
                graph.vignettes.reload()
            }

            // 3. Signets, rubans, images posées, descriptions des images : fusion, le plus récent l'emporte.
            val marks = File(staging, BackupFormat.DATA).takeIf { it.isFile }?.let { graph.transfer.import(it.readText()) } ?: 0

            // 4. Les réglages remplacent ceux de l'appareil, sauf la règle « Ne pas déranger », propre à l'appareil.
            val settings = File(staging, BackupFormat.SETTINGS).takeIf { it.isFile }?.let { file ->
                val imported = json.decodeFromString(ReaderSettings.serializer(), file.readText())
                graph.settings.update { imported.copy(zenRuleId = it.zenRuleId) }
                true
            } ?: false

            if (books > 0) graph.reloadLibrary()
            BackupSummary(books, images, marks, settings, problems)
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun unzip(input: InputStream, into: File) {
        var total = 0L
        val buffer = ByteArray(64 * 1024)
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                require(BackupFormat.isSafe(entry.name)) { "fichier inattendu dans la sauvegarde : ${entry.name}" }
                val target = File(into, entry.name)
                target.parentFile?.mkdirs()
                target.outputStream().use { out ->
                    while (true) {
                        val n = zip.read(buffer)
                        if (n < 0) break
                        total += n
                        require(total <= BackupFormat.MAX_BYTES) { "sauvegarde trop volumineuse" }
                        out.write(buffer, 0, n)
                    }
                }
            }
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    }
}
