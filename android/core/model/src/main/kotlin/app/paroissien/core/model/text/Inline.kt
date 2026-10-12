package app.paroissien.core.model.text

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest

/** Texte enrichi d'un bloc : la grammaire des inlines du rapport 11 (§3.2.3), lue depuis les paquets. */
typealias RichText = List<Inline>

sealed interface Inline {
    data class Text(val value: String) : Inline
    data class Marked(val marks: Set<Mark>, val children: RichText) : Inline
    data class Lang(val lang: String, val children: RichText) : Inline
    data class Link(val ref: String, val children: RichText) : Inline
    data class NoteRef(val noteId: String, val label: String?) : Inline
    data class Sym(val symbol: Symbol) : Inline
    data class Num(val value: String) : Inline
    data object Break : Inline
}

enum class Mark(val code: String) { ITALIC("i"), BOLD("b"), SMALL_CAPS("sc"), RED("red"), SUP("sup"), SUB("sub"), SPACED("sp") }

enum class Symbol(val code: String, val char: Char) {
    VERSICLE("V", '℣'), RESPONSE("R", '℟'), CROSS("cross", '✠'), STAR("star", '*'), DAGGER("dagger", '†'),
}

object Inlines {
    private val marks = Mark.entries.associateBy { it.code }
    private val symbols = Symbol.entries.associateBy { it.code }

    fun parse(json: String): RichText = parse(Json.parseToJsonElement(json))

    fun parse(element: JsonElement): RichText = element.jsonArray.map(::node)

    private fun node(e: JsonElement): Inline {
        if (e is JsonPrimitive) return Inline.Text(e.content)
        val o = e as JsonObject
        val children = { (o["c"] as? JsonArray)?.map(::node).orEmpty() }
        return when {
            "m" in o -> Inline.Marked(o.getValue("m").jsonArray.mapNotNull { marks[it.jsonPrimitive.content] }.toSet(), children())
            "lang" in o -> Inline.Lang(o.getValue("lang").jsonPrimitive.content, children())
            "link" in o -> Inline.Link(o.getValue("link").jsonPrimitive.content, children())
            "note" in o -> Inline.NoteRef(o.getValue("note").jsonPrimitive.content, (o["l"] as? JsonPrimitive)?.content)
            "sym" in o -> symbols[o.getValue("sym").jsonPrimitive.content]?.let(Inline::Sym) ?: Inline.Text("")
            "num" in o -> Inline.Num(o.getValue("num").jsonPrimitive.content)
            "br" in o -> Inline.Break
            else -> Inline.Text("") // nœud d'un format mineur plus récent : ignoré (ADR 003)
        }
    }

    /** L'inverse de [parse] : les inlines en JSON, tels que les paquets les gardent (ADR 003). */
    fun toJson(text: RichText): JsonArray = JsonArray(text.map(::json))

    private fun json(n: Inline): JsonElement = when (n) {
        is Inline.Text -> JsonPrimitive(n.value)
        is Inline.Marked -> JsonObject(mapOf("m" to JsonArray(n.marks.sortedBy { it.ordinal }.map { JsonPrimitive(it.code) }), "c" to toJson(n.children)))
        is Inline.Lang -> JsonObject(mapOf("lang" to JsonPrimitive(n.lang), "c" to toJson(n.children)))
        is Inline.Link -> JsonObject(mapOf("link" to JsonPrimitive(n.ref), "c" to toJson(n.children)))
        is Inline.NoteRef -> JsonObject(listOfNotNull("note" to JsonPrimitive(n.noteId), n.label?.let { "l" to JsonPrimitive(it) }).toMap())
        is Inline.Sym -> JsonObject(mapOf("sym" to JsonPrimitive(n.symbol.code)))
        is Inline.Num -> JsonObject(mapOf("num" to JsonPrimitive(n.value)))
        Inline.Break -> JsonObject(mapOf("br" to JsonPrimitive(1)))
    }

    /** Texte canonique (ADR 004) : sym compte pour son caractère, br pour \n, note et num pour rien. */
    fun plainText(text: RichText): String = buildString { appendPlain(text) }

    private fun StringBuilder.appendPlain(text: RichText) {
        for (n in text) when (n) {
            is Inline.Text -> append(n.value)
            is Inline.Marked -> appendPlain(n.children)
            is Inline.Lang -> appendPlain(n.children)
            is Inline.Link -> appendPlain(n.children)
            is Inline.Sym -> append(n.symbol.char)
            Inline.Break -> append('\n')
            is Inline.NoteRef, is Inline.Num -> Unit
        }
    }

    /** `rev` d'ADR 004 : 16 premiers caractères hexadécimaux du SHA-256 du texte canonique en UTF-8. */
    fun rev(plain: String): String =
        MessageDigest.getInstance("SHA-256").digest(plain.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }.take(16)

    /** Raccourci pour le contenu écrit à la main : _…_ en italique, ℣ ℟ ✠ † reconnus comme symboles. */
    fun fromMarkup(markup: String): RichText {
        val out = ArrayList<Inline>()
        val buffer = StringBuilder()
        var italic: MutableList<Inline>? = null
        fun flush() {
            if (buffer.isEmpty()) return
            (italic ?: out).add(Inline.Text(buffer.toString()))
            buffer.clear()
        }
        for (c in markup) {
            val symbol = Symbol.entries.firstOrNull { it.char == c && it != Symbol.STAR }
            when {
                c == '_' -> {
                    flush()
                    if (italic == null) italic = ArrayList() else {
                        out.add(Inline.Marked(setOf(Mark.ITALIC), italic))
                        italic = null
                    }
                }
                symbol != null -> { flush(); (italic ?: out).add(Inline.Sym(symbol)) }
                else -> buffer.append(c)
            }
        }
        flush()
        italic?.let { out.addAll(it) }
        return out
    }
}
