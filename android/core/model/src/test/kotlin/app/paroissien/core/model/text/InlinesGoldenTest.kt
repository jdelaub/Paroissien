package app.paroissien.core.model.text

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Parité Python/Kotlin du texte canonique et de la `rev` (ADR 004), sur les blocs du paquet golden. */
class InlinesGoldenTest {

    private val expected = Json.parseToJsonElement(File(System.getProperty("golden.expected")).readText()).jsonObject

    @Test
    fun plainTextAndRevMatchThePipeline() {
        val blocks = expected.getValue("blocks").jsonArray
        assertEquals(true, blocks.size > 10)
        blocks.forEach {
            val b = it.jsonObject
            val plain = Inlines.plainText(Inlines.parse(b.getValue("inlines")))
            val where = "${b.getValue("id").jsonPrimitive.content}/${b.getValue("lang").jsonPrimitive.content}"
            assertEquals(where, b.getValue("plain").jsonPrimitive.content, plain)
            assertEquals(where, b.getValue("rev").jsonPrimitive.content, Inlines.rev(plain))
        }
    }

    @Test
    fun markupShortcutKeepsItalicAndSymbols() {
        val text = Inlines.fromMarkup("_Qui sequitur me_, dicit Dominus ✠ ℣.")
        assertEquals("Qui sequitur me, dicit Dominus ✠ ℣.", Inlines.plainText(text))
        assertEquals(Inline.Marked(setOf(Mark.ITALIC), listOf(Inline.Text("Qui sequitur me"))), text.first())
    }
}
