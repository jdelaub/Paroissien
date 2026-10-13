package app.paroissien.core.content

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_CREATE
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READWRITE
import app.paroissien.core.model.text.Fold
import app.paroissien.core.model.text.Inlines
import app.paroissien.core.model.text.RichText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.security.MessageDigest

/** Un bloc d'un livre à écrire : [type] et [attrs] comme dans le format canonique (heading, para, rubric…). */
data class DraftBlock(val id: String, val type: String, val text: RichText, val attrs: Map<String, String> = emptyMap())

/** Une unité de lecture (un chapitre) : son titre au sommaire et ses blocs. */
data class DraftUnit(val id: String, val title: String, val blocks: List<DraftBlock>)

/**
 * Un livre à écrire en paquet : les livres que l'utilisateur importe (txt, epub) deviennent des paquets comme les
 * autres (ATELIER §7), lus, cherchés et habillés sans changement. Une seule publication, une seule langue.
 */
data class DraftBook(
    val packageId: String,
    val packageVersion: Long,
    val publicationId: String,
    val title: String,
    val language: String,
    val units: List<DraftUnit>,
    val kind: String = "book",
    val author: String? = null,
    /** D'où vient le livre (nom du fichier importé), gardé dans les métadonnées. */
    val source: String? = null,
    val rights: String = RIGHTS,
) {
    companion object {
        /** Statut des livres importés : ils sont à l'utilisateur, pour lui seul. */
        const val RIGHTS = "import-personnel"
    }
}

/**
 * Écrit un paquet au format 1.0 (ADR 003), avec le schéma de `content/schema/pack-v1.sql` et les mêmes règles que la
 * chaîne Python (`package.py`) : texte canonique, `rev` de chaque bloc, pliage de recherche, index plein texte.
 */
object PackWriter {

    private val json = Json

    private val schema: List<String> by lazy {
        val sql = requireNotNull(PackWriter::class.java.getResourceAsStream("/pack-v1.sql")) { "schéma des paquets absent" }
            .use { it.readBytes().decodeToString() }
        sql.lines().joinToString("\n") { it.substringBefore("--") }.split(';').map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** Écrit [book] dans [target] (remplacé s'il existe), en passant par un fichier temporaire. */
    fun write(book: DraftBook, target: File) {
        require(book.units.isNotEmpty() && book.units.any { it.blocks.isNotEmpty() }) { "livre vide" }
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, target.name + ".tmp").apply { delete() }
        val connection = BundledSQLiteDriver().open(tmp.absolutePath, SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE)
        try {
            connection.exec("PRAGMA page_size = 4096")
            connection.exec("PRAGMA journal_mode = DELETE")
            schema.forEach { connection.exec(it) }
            connection.exec("BEGIN")
            val contentHash = insert(connection, book)
            val info = sortedMapOf(
                "formatMajor" to PackFormat.MAJOR.toString(),
                "formatMinor" to PackFormat.MINOR.toString(),
                "packageId" to book.packageId,
                "packageVersion" to book.packageVersion.toString(),
                "minAppFormat" to "${PackFormat.MAJOR}.${PackFormat.MINOR}",
                "rights" to book.rights,
                "builtAt" to book.packageVersion.toString().take(8),
                "contentHash" to "sha256:" + sha256(contentHash),
                "title" to book.title,
            )
            info.forEach { (k, v) -> connection.run("INSERT INTO pack_info(key, value) VALUES (?, ?)", k, v) }
            connection.exec("INSERT INTO search_fts(search_fts) VALUES ('rebuild')")
            connection.exec("COMMIT")
            connection.exec("VACUUM")
        } finally {
            connection.close()
        }
        File(tmp.path + "-journal").delete()
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    private fun insert(db: SQLiteConnection, book: DraftBook): String {
        val lang = book.language
        val blocks = book.units.flatMap { it.blocks }
        val plain = blocks.map { Inlines.plainText(it.text) }
        val meta = buildMap {
            put("license", JsonObject(mapOf("spdx" to JsonPrimitive("LicenseRef-Personal"), "notice" to JsonPrimitive("Livre importé par l'utilisateur."))))
            book.author?.let { put("contributors", JsonArray(listOf(JsonObject(mapOf("name" to JsonPrimitive(it), "role" to JsonPrimitive("author")))))) }
            book.source?.let { put("source", JsonObject(mapOf("name" to JsonPrimitive(it)))) }
            put("x-imported", JsonPrimitive(true))
        }
        val contentHash = sha256(plain.joinToString("\n"))
        db.run(
            "INSERT INTO publication(id, revision, kind, primary_lang, languages, aligned, title, meta, content_hash, char_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            book.publicationId, 1L, book.kind, lang, JsonArray(listOf(JsonPrimitive(lang))).toString(), 0L,
            JsonObject(mapOf(lang to JsonPrimitive(book.title))).toString(), JsonObject(meta).toString(), contentHash, plain.sumOf { it.length }.toLong(),
        )
        val pub = db.lastRowId()
        var seq = 0L
        var charStart = 0L
        book.units.forEachIndexed { ord, unit ->
            val first = seq + 1
            db.run(
                "INSERT INTO division(pub, id, parent, ord, kind, toc_title, is_unit, seq_first, seq_last, attrs) VALUES (?, ?, NULL, ?, 'chapter', ?, 1, 0, 0, NULL)",
                pub, unit.id, ord.toLong(), unit.title,
            )
            val division = db.lastRowId()
            for (block in unit.blocks) {
                seq++
                val text = Inlines.plainText(block.text)
                val attrs = block.attrs.takeIf { it.isNotEmpty() }?.let { a -> JsonObject(a.toSortedMap().mapValues { (k, v) -> if (k == "level") JsonPrimitive(v.toInt()) else JsonPrimitive(v) }).toString() }
                db.run(
                    "INSERT INTO block(pub, id, unit, seq, type, attrs, char_start, char_len) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    pub, block.id, division, seq, block.type, attrs, charStart, text.length.toLong(),
                )
                val row = db.lastRowId()
                charStart += text.length
                db.run("INSERT INTO block_text(block, lang, inlines, rev) VALUES (?, ?, ?, ?)", row, lang, json.encodeToString(JsonArray.serializer(), Inlines.toJson(block.text)), Inlines.rev(text))
                val folded = Fold.fold(text)
                val (head, body) = if (block.type == "heading") folded to "" else "" to folded
                db.run("INSERT INTO search_doc(block, lang, head, body) VALUES (?, ?, ?, ?)", row, lang, head, body)
            }
            db.run("UPDATE division SET seq_first = ?, seq_last = ? WHERE rowid = ?", if (seq >= first) first else 0L, if (seq >= first) seq else 0L, division)
        }
        return contentHash
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun SQLiteConnection.exec(sql: String) = prepare(sql).use { it.step() }

    private fun SQLiteConnection.lastRowId(): Long = prepare("SELECT last_insert_rowid()").use { it.step(); it.getLong(0) }

    private fun SQLiteConnection.run(sql: String, vararg args: Any?) = prepare(sql).use { st ->
        args.forEachIndexed { i, v -> st.bind(i + 1, v) }
        st.step()
    }

    private fun SQLiteStatement.bind(index: Int, value: Any?) = when (value) {
        null -> bindNull(index)
        is Long -> bindLong(index, value)
        is Int -> bindLong(index, value.toLong())
        else -> bindText(index, value.toString())
    }
}
