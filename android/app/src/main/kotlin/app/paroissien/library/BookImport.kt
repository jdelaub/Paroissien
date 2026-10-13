package app.paroissien.library

import android.util.Xml
import app.paroissien.core.content.DraftBlock
import app.paroissien.core.content.DraftBook
import app.paroissien.core.content.DraftUnit
import app.paroissien.core.model.text.Inline
import app.paroissien.core.model.text.Mark
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Livres importés par l'utilisateur (ATELIER §7) : un fichier texte ou un epub devient un [DraftBook], écrit ensuite en
 * paquet comme les autres. Les fichiers reçus sont des données : rien n'y est exécuté, les tailles sont bornées, les
 * noms des entrées d'un epub ne sortent jamais du livre.
 */
object BookImport {

    /** Au-delà, une unité se coupe : un chapitre trop long ralentirait la mise en pages. */
    private const val MAX_BLOCKS = 400
    private const val MAX_ENTRY = 30L * 1024 * 1024
    private const val MAX_TOTAL = 200L * 1024 * 1024

    /** Lit [bytes] selon leur nature : un zip (epub), sinon du texte. Null si rien de lisible n'en sort. */
    fun parse(bytes: ByteArray, fileName: String): DraftBook? {
        val zip = bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
        return runCatching { if (zip) epub(bytes, fileName) else text(bytes, fileName) }.getOrNull()
    }

    // ── Texte ────────────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Un fichier texte : paragraphes séparés par une ligne vide, lignes d'un même paragraphe réunies ; une ligne courte
     * en capitales, ou qui commence par « Chapitre », « Livre », « Partie »…, ouvre un chapitre. Encodage : UTF-8, sinon
     * Windows-1252. L'en-tête et la licence du projet Gutenberg sont laissés de côté.
     */
    fun text(bytes: ByteArray, fileName: String): DraftBook? {
        var content = decode(bytes).replace("\r\n", "\n").replace('\r', '\n').removePrefix("﻿")
        content = Normalizer.normalize(content, Normalizer.Form.NFC)
        var title = baseName(fileName)
        var author: String? = null
        gutenberg(content)?.let { (body, t, a) -> content = body; t?.let { title = it }; author = a }
        val paragraphs = content.split(Regex("\n[ \t]*\n+")).map { p -> p.lines().joinToString(" ") { it.trim() }.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotEmpty() }
        if (paragraphs.isEmpty()) return null
        val chapters = ArrayList<Pair<String?, MutableList<Pair<String, List<Inline>>>>>()
        for (p in paragraphs) {
            if (isHeading(p)) {
                chapters += p to mutableListOf("heading" to listOf<Inline>(Inline.Text(p)))
            } else {
                if (chapters.isEmpty()) chapters += null to mutableListOf()
                chapters.last().second += "para" to listOf<Inline>(Inline.Text(p))
            }
        }
        val drafts = chapters.mapIndexed { i, (heading, blocks) -> (heading ?: if (i == 0) title else "Suite") to blocks }
        return book(title, author, language(content), fileName, drafts)
    }

    private fun decode(bytes: ByteArray): String = try {
        Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()
    } catch (_: CharacterCodingException) {
        String(bytes, Charset.forName("windows-1252"))
    }

    /** Le corps d'un texte du projet Gutenberg, son titre et son auteur ; null si ce n'en est pas un. */
    private fun gutenberg(text: String): Triple<String, String?, String?>? {
        val start = Regex("\\*\\*\\* ?START OF (THE|THIS) PROJECT GUTENBERG[^\n]*\n").find(text) ?: return null
        val end = Regex("\\*\\*\\* ?END OF (THE|THIS) PROJECT GUTENBERG").find(text, start.range.last)
        val header = text.substring(0, start.range.first)
        fun field(name: String) = Regex("^$name: *(.+)$", RegexOption.MULTILINE).find(header)?.groupValues?.get(1)?.trim()
        return Triple(text.substring(start.range.last + 1, end?.range?.first ?: text.length), field("Title"), field("Author"))
    }

    private val headingStart = Regex("^(chapitre|livre|partie|section|article|lettre|psaume|chapter|book|part|caput|liber)\\b.*", RegexOption.IGNORE_CASE)
    private val roman = Regex("^[IVXLC]+\\.?$")

    private fun isHeading(p: String): Boolean {
        if (p.length > 90 || p.isEmpty()) return false
        if (p.last() in ".,;:" && !roman.matches(p)) return false
        if (headingStart.matches(p) || roman.matches(p)) return true
        val letters = p.filter { it.isLetter() }
        return letters.length >= 3 && letters.count { it.isUpperCase() } >= letters.length * 0.8
    }

    /** Français, latin ou anglais, d'après les petits mots les plus courants ; le français par défaut. */
    private fun language(text: String): String {
        val words = text.take(200_000).lowercase().split(Regex("[^\\p{L}]+"))
        val counts = mapOf(
            "fr" to setOf("le", "la", "les", "des", "est", "que", "qui", "dans", "une", "pour"),
            "la" to setOf("et", "in", "est", "ad", "non", "cum", "quod", "sed", "enim", "autem"),
            "en" to setOf("the", "and", "of", "to", "is", "that", "which", "with", "for", "his"),
        ).mapValues { (_, stop) -> words.count { it in stop } }
        return counts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key ?: "fr"
    }

    // ── Epub ─────────────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Un epub : le manifeste et l'ordre de lecture (`spine`) donnent les chapitres, le XHTML les blocs (titres,
     * paragraphes, italiques, gras, exposants) ; titre, auteur et langue viennent des métadonnées. Les images sont
     * laissées de côté.
     */
    fun epub(bytes: ByteArray, fileName: String): DraftBook? {
        val files = HashMap<String, ByteArray>()
        var total = 0L
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (entry.isDirectory || name.startsWith("/") || ".." in name.split('/')) continue
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val n = zip.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    total += n
                    require(out.size() <= MAX_ENTRY && total <= MAX_TOTAL) { "epub trop gros" }
                }
                files[name] = out.toByteArray()
            }
        }
        val container = files["META-INF/container.xml"] ?: return null
        val opfPath = attributeOf(container, "rootfile", "full-path") ?: return null
        val opf = files[opfPath] ?: return null
        val base = opfPath.substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }
        val package_ = readOpf(opf)
        val chapters = ArrayList<Pair<String, MutableList<Pair<String, List<Inline>>>>>()
        package_.spine.forEachIndexed { index, idref ->
            val item = package_.manifest[idref] ?: return@forEachIndexed
            if (!item.second.contains("html")) return@forEachIndexed
            val path = resolve(base, item.first)
            val doc = files[path] ?: return@forEachIndexed
            val blocks = xhtmlBlocks(doc)
            if (blocks.isEmpty()) return@forEachIndexed
            val title = blocks.firstOrNull { it.first.startsWith("heading") }?.second?.let(::plain) ?: "Chapitre ${index + 1}"
            chapters += title to blocks.toMutableList()
        }
        if (chapters.isEmpty()) return null
        val title = package_.title ?: baseName(fileName)
        return book(title, package_.author, package_.language?.take(2)?.lowercase() ?: "fr", fileName, chapters)
    }

    private class Opf(val title: String?, val author: String?, val language: String?, val manifest: Map<String, Pair<String, String>>, val spine: List<String>)

    private fun parser(bytes: ByteArray): XmlPullParser = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        runCatching { setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true) }
        setInput(ByteArrayInputStream(bytes), null)
        ENTITIES.forEach { (name, value) -> runCatching { defineEntityReplacementText(name, value) } }
    }

    private fun attributeOf(xml: ByteArray, tag: String, attribute: String): String? {
        val p = parser(xml)
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            if (p.eventType == XmlPullParser.START_TAG && p.name.substringAfter(':') == tag) return p.getAttributeValue(null, attribute)
        }
        return null
    }

    private fun readOpf(xml: ByteArray): Opf {
        val p = parser(xml)
        var title: String? = null
        var author: String? = null
        var language: String? = null
        val manifest = HashMap<String, Pair<String, String>>()
        val spine = ArrayList<String>()
        var field: String? = null
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name.substringAfter(':')) {
                    "title", "creator", "language" -> field = p.name.substringAfter(':')
                    "item" -> {
                        val id = p.getAttributeValue(null, "id")
                        val href = p.getAttributeValue(null, "href")
                        if (id != null && href != null) manifest[id] = href to (p.getAttributeValue(null, "media-type") ?: "")
                    }
                    "itemref" -> if (p.getAttributeValue(null, "linear") != "no") p.getAttributeValue(null, "idref")?.let(spine::add)
                }
                XmlPullParser.TEXT -> {
                    val text = p.text.trim()
                    if (text.isNotEmpty()) when (field) {
                        "title" -> if (title == null) title = text
                        "creator" -> if (author == null) author = text
                        "language" -> if (language == null) language = text
                    }
                }
                XmlPullParser.END_TAG -> field = null
            }
        }
        return Opf(title, author, language, manifest, spine)
    }

    private fun resolve(base: String, href: String): String {
        val path = java.net.URLDecoder.decode(href.substringBefore('#'), "UTF-8")
        val parts = ArrayList<String>()
        for (segment in (base + path).split('/')) when (segment) {
            "", "." -> Unit
            ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
            else -> parts += segment
        }
        return parts.joinToString("/")
    }

    private val blockTags = setOf("p", "li", "dd", "dt", "blockquote", "pre", "figcaption", "td", "div", "section", "article", "aside", "header", "footer")
    private val skipTags = setOf("script", "style", "head", "title", "svg", "math", "nav")

    /** Les blocs d'un document XHTML : (type, inlines), le type « heading1 » à « heading3 » pour les titres. */
    private fun xhtmlBlocks(xml: ByteArray): List<Pair<String, List<Inline>>> {
        val p = parser(xml)
        val blocks = ArrayList<Pair<String, List<Inline>>>()
        var current: MutableList<Inline>? = null
        var type = "para"
        val marks = ArrayList<Mark>()
        var skip = 0
        fun flush() {
            val inlines = current ?: return
            current = null
            val trimmed = trim(inlines)
            if (trimmed.isNotEmpty() && plain(trimmed).isNotBlank()) blocks += type to trimmed
        }
        fun open(t: String) { flush(); type = t; current = ArrayList() }
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> {
                    val name = p.name.substringAfter(':').lowercase()
                    when {
                        skip > 0 || name in skipTags -> skip++
                        name.length == 2 && name[0] == 'h' && name[1] in '1'..'6' -> open("heading" + (name[1] - '0').coerceAtMost(3))
                        name in blockTags -> open("para")
                        name == "br" -> current?.add(Inline.Break)
                        name == "i" || name == "em" || name == "cite" -> marks += Mark.ITALIC
                        name == "b" || name == "strong" -> marks += Mark.BOLD
                        name == "sup" -> marks += Mark.SUP
                        name == "sub" -> marks += Mark.SUB
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = p.name.substringAfter(':').lowercase()
                    when {
                        skip > 0 -> skip--
                        name.length == 2 && name[0] == 'h' && name[1] in '1'..'6' -> flush()
                        name in blockTags -> flush()
                        name == "i" || name == "em" || name == "cite" -> marks.remove(Mark.ITALIC)
                        name == "b" || name == "strong" -> marks.remove(Mark.BOLD)
                        name == "sup" -> marks.remove(Mark.SUP)
                        name == "sub" -> marks.remove(Mark.SUB)
                    }
                }
                XmlPullParser.TEXT, XmlPullParser.ENTITY_REF -> if (skip == 0) {
                    // Les entités HTML que l'analyseur laisse telles quelles (« &nbsp; ») sont décodées ici.
                    val text = (p.text ?: "").replace(ENTITY) { ENTITIES[it.groupValues[1]] ?: it.value }.replace(Regex("\\s+"), " ")
                    if (text.isNotEmpty()) {
                        if (current == null) { type = "para"; current = ArrayList() }
                        current?.add(if (marks.isEmpty()) Inline.Text(text) else Inline.Marked(marks.toSet(), listOf(Inline.Text(text))))
                    }
                }
            }
        }
        flush()
        return blocks
    }

    /** Ôte les espaces au début et à la fin d'un bloc, et les sauts de ligne qui le bordent. */
    private fun trim(inlines: List<Inline>): List<Inline> {
        val list = inlines.dropWhile { it == Inline.Break }.dropLastWhile { it == Inline.Break }.toMutableList()
        fun edit(i: Int, f: (String) -> String) {
            when (val n = list.getOrNull(i)) {
                is Inline.Text -> list[i] = Inline.Text(f(n.value))
                is Inline.Marked -> (n.children.singleOrNull() as? Inline.Text)?.let { list[i] = Inline.Marked(n.marks, listOf(Inline.Text(f(it.value)))) }
                else -> Unit
            }
        }
        edit(0) { it.trimStart() }
        edit(list.lastIndex) { it.trimEnd() }
        return list.filterNot { it is Inline.Text && it.value.isEmpty() }
    }

    private fun plain(inlines: List<Inline>): String = app.paroissien.core.model.text.Inlines.plainText(inlines).trim()

    // ── Livre ────────────────────────────────────────────────────────────────────────────────────────────────────────

    /** Les chapitres deviennent des unités (coupées au-delà de [MAX_BLOCKS] blocs), avec des identifiants stables. */
    private fun book(title: String, author: String?, language: String, fileName: String, chapters: List<Pair<String, List<Pair<String, List<Inline>>>>>): DraftBook {
        val slug = slug(title)
        val suffix = UUID.randomUUID().toString().take(6)
        val units = ArrayList<DraftUnit>()
        for ((chapterTitle, blocks) in chapters) {
            blocks.chunked(MAX_BLOCKS).forEachIndexed { part, chunk ->
                val unitId = "u${units.size + 1}"
                val drafts = chunk.mapIndexed { i, (kind, inlines) ->
                    val heading = kind.startsWith("heading")
                    DraftBlock(
                        "$unitId.${i + 1}", if (heading) "heading" else "para", inlines,
                        if (heading) mapOf("level" to (kind.removePrefix("heading").toIntOrNull() ?: 1).toString()) else emptyMap(),
                    )
                }
                units += DraftUnit(unitId, if (part == 0) chapterTitle else "$chapterTitle (suite)", drafts)
            }
        }
        val version = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE).toLong() * 100 + 1
        return DraftBook(
            packageId = "perso-$slug-$suffix", packageVersion = version, publicationId = "perso.$slug-$suffix",
            title = title, language = language.takeIf { it in setOf("fr", "la", "en") } ?: "fr", units = units, author = author, source = fileName,
        )
    }

    private fun baseName(fileName: String) = fileName.substringAfterLast('/').substringBeforeLast('.').replace('_', ' ').trim().ifEmpty { "Livre importé" }

    private fun slug(title: String): String =
        Normalizer.normalize(title, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()
            .replace(Regex("[^a-z0-9]+"), "-").trim('-').take(32).trim('-').ifEmpty { "livre" }

    private val ENTITY = Regex("&([A-Za-z]+);")

    /** Entités HTML courantes, que le XHTML des epubs emploie sans les déclarer. */
    private val ENTITIES = mapOf(
        "nbsp" to " ", "thinsp" to " ", "ensp" to " ", "emsp" to " ", "shy" to "­",
        "mdash" to "—", "ndash" to "–", "hellip" to "…", "laquo" to "«", "raquo" to "»", "lsquo" to "‘", "rsquo" to "’",
        "ldquo" to "“", "rdquo" to "”", "copy" to "©", "deg" to "°", "middot" to "·", "oelig" to "œ", "OElig" to "Œ",
        "aelig" to "æ", "AElig" to "Æ", "eacute" to "é", "egrave" to "è", "ecirc" to "ê", "agrave" to "à", "acirc" to "â",
        "ccedil" to "ç", "icirc" to "î", "ocirc" to "ô", "ucirc" to "û", "ugrave" to "ù", "Eacute" to "É",
    )
}
