package app.paroissien.core.content

import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.security.PublicKey

/** Un paquet installé et vérifié, prêt à ouvrir avec [PackReader]. */
data class InstalledPack(val entry: CatalogPack, val file: File)

/** Ce que l'installation a donné : les paquets lisibles et, pour les autres, la raison en français. */
data class Installation(val packs: List<InstalledPack>, val problems: List<String>)

/**
 * Installe les paquets embarqués (ADR 003) : le catalogue doit être signé par une clé connue ; chaque paquet est copié
 * vers `<root>/<packageId>/<packageVersion>.sqlite`, puis vérifié une seule fois (SHA-256, identité, quick_check).
 * Un paquet défectueux part en quarantaine ; jamais de plantage.
 */
class PackInstaller(
    private val open: (String) -> InputStream,
    private val root: File,
    private val keys: List<PublicKey>,
    private val directory: String = "packs",
) {
    fun install(): Installation {
        val catalogBytes = runCatching { open("$directory/catalog.json").use { it.readBytes() } }.getOrNull()
            ?: return Installation(emptyList(), listOf("Aucun catalogue de livres dans l'application"))
        val signature = runCatching { open("$directory/catalog.json.sig").use { it.readBytes() } }.getOrNull()
        if (signature == null || !CatalogVerifier.verify(catalogBytes, signature, keys)) {
            return Installation(emptyList(), listOf("Le catalogue des livres n'est pas signé par une clé reconnue"))
        }
        val catalog = Catalog.parse(catalogBytes)
        val installed = ArrayList<InstalledPack>()
        val problems = ArrayList<String>()
        for (entry in catalog.packs) {
            val title = entry.title["fr"] ?: entry.packageId
            if (!PackFormat.supports(entry.minAppFormat)) {
                problems += "« $title » demande une version plus récente de Paroissien"
                continue
            }
            val target = File(root, "${entry.packageId}/${entry.packageVersion}.sqlite")
            val verified = File(target.path + ".ok")
            if (!(target.isFile && verified.isFile && target.length() == entry.size)) {
                if (!copyAndCheck(entry, target)) {
                    problems += "« $title » est endommagé : il a été mis de côté"
                    continue
                }
                verified.writeText(entry.sha256)
            }
            installed += InstalledPack(entry, target)
            // Les versions précédentes du même paquet ne servent plus.
            target.parentFile.listFiles()?.filter { it.name.substringBefore('.') != entry.packageVersion.toString() }?.forEach { it.delete() }
        }
        return Installation(installed, problems)
    }

    private fun copyAndCheck(entry: CatalogPack, target: File): Boolean {
        target.parentFile.mkdirs()
        val temp = File(target.path + ".part")
        val digest = MessageDigest.getInstance("SHA-256")
        val copied = runCatching {
            open("$directory/${entry.file}").use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        digest.update(buffer, 0, n)
                        output.write(buffer, 0, n)
                    }
                }
            }
        }.isSuccess
        val sha = digest.digest().joinToString("") { "%02x".format(it) }
        if (copied && sha == entry.sha256 && temp.length() == entry.size && temp.renameTo(target) && PackReader.quickCheck(target)) return true
        quarantine(temp.takeIf { it.exists() } ?: target, entry)
        return false
    }

    private fun quarantine(file: File, entry: CatalogPack) {
        if (!file.exists()) return
        val dir = File(root, "quarantine").apply { mkdirs() }
        file.renameTo(File(dir, "${entry.packageId}-${entry.packageVersion}-${System.currentTimeMillis()}.sqlite"))
    }
}
