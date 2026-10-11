package app.paroissien.spike

import android.content.Context
import app.paroissien.core.content.CatalogVerifier
import app.paroissien.core.content.InstalledPack
import app.paroissien.core.content.PackBlock
import app.paroissien.core.content.PackInstaller
import app.paroissien.core.content.PackReader
import app.paroissien.core.content.PublicationInfo
import app.paroissien.core.content.UnitInfo
import java.io.File

/** Clé publique de test du catalogue (content/schema/keys) ; la clé de production viendra avec les téléchargements (V1). */
private const val TEST_KEY = """-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEiVURn0MQFXz2rwjWRi7Dq89o0Sfy
cBLckl6rsW4RjBgyhta5pFsYnrbYvVNE3abCG+bsvQ9fruGYDU+kUmEXdA==
-----END PUBLIC KEY-----"""

data class LibraryEntry(val pack: InstalledPack, val publication: PublicationInfo, val units: List<UnitInfo>) {
    val title: String get() = publication.title["fr"] ?: publication.title.values.firstOrNull() ?: publication.id
}

data class Library(val entries: List<LibraryEntry>, val problems: List<String>)

/**
 * Bibliothèque du spike de la phase 2 : installe les paquets publics (assets packs/) et privés de la saveur
 * personnelle (assets packs-private/), puis liste les publications et leurs unités. À appeler hors du fil principal.
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
}
