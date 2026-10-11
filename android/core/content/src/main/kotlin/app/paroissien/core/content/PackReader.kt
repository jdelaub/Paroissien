package app.paroissien.core.content

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READONLY
import app.paroissien.core.model.text.Inlines
import app.paroissien.core.model.text.RichText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

data class PublicationInfo(
    val id: String,
    val kind: String,
    val title: Map<String, String>,
    val languages: List<String>,
    val primaryLanguage: String,
    val meta: JsonObject,
)

data class UnitInfo(val id: String, val kind: String, val title: String?, val blockCount: Int)

/** Un bloc tel que le paquet le stocke : son type, ses attributs et un texte par langue. */
data class PackBlock(val id: String, val type: String, val attrs: JsonObject, val texts: Map<String, RichText>)

/**
 * Lecture d'un paquet installé, en lecture seule, avec le SQLite embarqué (3.50.1, FTS5) : le même pilote que l'app
 * sur l'appareil et que les tests sur la JVM. Une connexion n'est pas partagée entre fils.
 */
class PackReader private constructor(private val connection: SQLiteConnection) : AutoCloseable {

    fun info(): Map<String, String> = query("SELECT key, value FROM pack_info") { it.getText(0) to it.getText(1) }.toMap()

    fun publications(): List<PublicationInfo> = query(
        "SELECT id, kind, title, languages, primary_lang, meta FROM publication ORDER BY rowid",
    ) {
        PublicationInfo(
            id = it.getText(0),
            kind = it.getText(1),
            title = json.parseToJsonElement(it.getText(2)).jsonObject.mapValues { (_, v) -> v.jsonPrimitive.content },
            languages = Json.decodeFromString(it.getText(3)),
            primaryLanguage = it.getText(4),
            meta = json.parseToJsonElement(it.getText(5)).jsonObject,
        )
    }

    /** Unités de lecture d'une publication, dans l'ordre du livre (les divisions sont insérées en préordre). */
    fun units(publication: String): List<UnitInfo> = query(
        "SELECT d.id, d.kind, d.toc_title, d.seq_last - d.seq_first + 1 FROM division d JOIN publication p ON d.pub = p.rowid" +
            " WHERE p.id = ? AND d.is_unit = 1 ORDER BY d.rowid",
        publication,
    ) { UnitInfo(it.getText(0), it.getText(1), if (it.isNull(2)) null else it.getText(2), it.getLong(3).toInt()) }

    fun blocks(publication: String, unit: String): List<PackBlock> {
        val rows = query(
            "SELECT b.id, b.type, b.attrs, t.lang, t.inlines FROM block b" +
                " JOIN publication p ON b.pub = p.rowid JOIN division d ON b.unit = d.rowid" +
                " LEFT JOIN block_text t ON t.block = b.rowid" +
                " WHERE p.id = ? AND d.id = ? ORDER BY b.seq, t.lang",
            publication, unit,
        ) { Row(it.getText(0), it.getText(1), if (it.isNull(2)) null else it.getText(2), if (it.isNull(3)) null else it.getText(3), if (it.isNull(4)) null else it.getText(4)) }
        val out = ArrayList<PackBlock>()
        for ((id, group) in rows.groupBy { it.id }) {
            val first = group.first()
            val texts = group.filter { it.lang != null }.associate { it.lang!! to Inlines.parse(it.inlines!!) }
            out += PackBlock(id, first.type, first.attrs?.let { json.parseToJsonElement(it).jsonObject } ?: JsonObject(emptyMap()), texts)
        }
        return out
    }

    /**
     * Sommaire d'une publication : chaque unité, puis ses titres de niveau 1 et 2, dans l'ordre de lecture.
     * Le titre est pris dans [language] s'il existe, sinon dans la langue principale.
     */
    fun toc(publication: String, language: String): List<TocEntry> {
        val pub = publications().firstOrNull { it.id == publication } ?: return emptyList()
        val headings = query(
            "SELECT d.id, b.id, b.attrs, t.lang, t.inlines FROM block b" +
                " JOIN publication p ON b.pub = p.rowid JOIN division d ON b.unit = d.rowid" +
                " JOIN block_text t ON t.block = b.rowid" +
                " WHERE p.id = ? AND b.type = 'heading' ORDER BY b.seq",
            publication,
        ) { HeadingRow(it.getText(0), it.getText(1), if (it.isNull(2)) null else it.getText(2), it.getText(3), it.getText(4)) }
            .groupBy { it.block }
            .values.map { rows -> rows.firstOrNull { it.lang == language } ?: rows.firstOrNull { it.lang == pub.primaryLanguage } ?: rows.first() }
        val byUnit = headings.groupBy { it.unit }
        return units(publication).flatMap { unit ->
            listOf(TocEntry(unit.id, null, 0, unit.title ?: unit.id)) +
                byUnit[unit.id].orEmpty().mapNotNull { row ->
                    val level = row.attrs?.let { json.parseToJsonElement(it).jsonObject["level"]?.jsonPrimitive?.content?.toIntOrNull() } ?: 1
                    if (level > 2) null else TocEntry(unit.id, row.block, level, Inlines.plainText(Inlines.parse(row.inlines)))
                }
        }
    }

    /** Passages qui contiennent tous les mots de [input] (le dernier en préfixe), du plus pertinent au moins pertinent. */
    fun search(input: String, limit: Int = 50): List<SearchHit> {
        val match = SearchQuery.match(input) ?: return emptyList()
        val hits = query(
            "SELECT p.id, d.id, b.id, s.lang, b.type, t.inlines FROM search_fts f" +
                " JOIN search_doc s ON s.rowid = f.rowid JOIN block b ON b.rowid = s.block" +
                " JOIN publication p ON b.pub = p.rowid JOIN division d ON b.unit = d.rowid" +
                " JOIN block_text t ON t.block = b.rowid AND t.lang = s.lang" +
                " WHERE search_fts MATCH ? ORDER BY bm25(search_fts, 4.0, 1.0) LIMIT $limit",
            match,
        ) { Hit(it.getText(0), it.getText(1), it.getText(2), it.getText(3), it.getText(4), it.getText(5)) }
        return hits.map { hit ->
            val text = Inlines.plainText(Inlines.parse(hit.inlines))
            SearchHit(hit.pub, hit.unit, hit.block, hit.lang, hit.type == "heading", text, SearchQuery.highlights(text, input))
        }
    }

    private class HeadingRow(val unit: String, val block: String, val attrs: String?, val lang: String, val inlines: String)

    private class Hit(val pub: String, val unit: String, val block: String, val lang: String, val type: String, val inlines: String)

    private class Row(val id: String, val type: String, val attrs: String?, val lang: String?, val inlines: String?)

    private fun <T> query(sql: String, vararg args: String, map: (SQLiteStatement) -> T): List<T> =
        connection.prepare(sql).use { statement ->
            args.forEachIndexed { i, a -> statement.bindText(i + 1, a) }
            buildList { while (statement.step()) add(map(statement)) }
        }

    override fun close() = connection.close()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val driver = BundledSQLiteDriver()

        /** Ouvre [file] en lecture seule et refuse tout fichier qui n'est pas un paquet d'un format connu. */
        fun open(file: File): PackReader {
            val connection = driver.open(file.absolutePath, SQLITE_OPEN_READONLY)
            try {
                connection.execSQL("PRAGMA trusted_schema = OFF")
                val reader = PackReader(connection)
                val appId = reader.query("PRAGMA application_id") { it.getLong(0).toInt() }.single()
                val version = reader.query("PRAGMA user_version") { it.getLong(0).toInt() }.single()
                require(appId == PackFormat.APPLICATION_ID) { "${file.name} n'est pas un paquet Paroissien" }
                require(version / 100 == PackFormat.MAJOR) { "${file.name} : format ${version / 100}.${version % 100} inconnu" }
                return reader
            } catch (e: Throwable) {
                connection.close()
                throw e
            }
        }

        internal fun quickCheck(file: File): Boolean = runCatching {
            open(file).use { r -> r.query("PRAGMA quick_check") { it.getText(0) }.singleOrNull() == "ok" }
        }.getOrDefault(false)
    }
}

private fun SQLiteConnection.execSQL(sql: String) = prepare(sql).use { it.step() }
