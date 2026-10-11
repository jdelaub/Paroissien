package app.paroissien.core.model.text

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FoldTest {

    private val contract = Json.parseToJsonElement(File(System.getProperty("fold.vectors")).readText()).jsonObject

    @Test
    fun specVersionMatchesContract() {
        assertEquals(Fold.SPEC, contract.getValue("spec").jsonPrimitive.content)
    }

    @Test
    fun everySharedVectorFoldsLikeThePipeline() {
        val vectors = contract.getValue("vectors").jsonArray
        assertTrue("au moins 40 vecteurs attendus", vectors.size >= 40)
        val failures = vectors.mapNotNull { v ->
            val o = v.jsonObject
            val input = o.getValue("in").jsonPrimitive.content
            val expected = o.getValue("out").jsonPrimitive.content
            val actual = Fold.fold(input)
            if (actual == expected) null else "${escape(input)} → ${escape(actual)} (attendu ${escape(expected)})"
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun foldIsIdempotentAndDropsSoftHyphens() {
        for (s in listOf("Dó\u00ADmi\u00ADnus", "Cœur de Jésus", "ÆTERNUS", "l’Église")) {
            val once = Fold.fold(s)
            assertEquals(once, Fold.fold(once))
            assertFalse(once.contains('\u00AD'))
        }
    }

    private fun escape(s: String) = s.map { if (it.code in 0x20..0x7E) "$it" else "\\u%04X".format(it.code) }.joinToString("")
}
