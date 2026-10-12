package app.paroissien.core.content

import app.paroissien.core.model.text.Inline
import app.paroissien.core.model.text.Inlines
import app.paroissien.core.model.text.Mark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Un livre importé, écrit par l'app, se lit comme un paquet de la chaîne : unités, blocs, sommaire, recherche. */
class PackWriterTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val book = DraftBook(
        packageId = "perso-essai", packageVersion = 2026101201, publicationId = "perso.essai", title = "Essai", language = "fr",
        author = "Anonyme", source = "essai.txt",
        units = listOf(
            DraftUnit("u1", "Chapitre premier", listOf(
                DraftBlock("u1.1", "heading", listOf(Inline.Text("Chapitre premier")), mapOf("level" to "1")),
                DraftBlock("u1.2", "para", listOf(Inline.Text("Le Seigneur est mon "), Inline.Marked(setOf(Mark.ITALIC), listOf(Inline.Text("berger"))), Inline.Text("."))),
            )),
            DraftUnit("u2", "Chapitre second", listOf(
                DraftBlock("u2.1", "heading", listOf(Inline.Text("Chapitre second")), mapOf("level" to "1")),
                DraftBlock("u2.2", "para", listOf(Inline.Text("Rien ne saurait me manquer."))),
            )),
        ),
    )

    @Test
    fun writtenBookReadsBack() {
        val file = File(tmp.root, "perso-essai.sqlite")
        PackWriter.write(book, file)
        PackReader.open(file).use { reader ->
            assertEquals("perso-essai", reader.info()["packageId"])
            assertEquals(DraftBook.RIGHTS, reader.info()["rights"])
            val pub = reader.publications().single()
            assertEquals("perso.essai", pub.id)
            assertEquals("Essai", pub.title["fr"])
            assertEquals(listOf("u1", "u2"), reader.units("perso.essai").map { it.id })
            assertEquals(listOf(2, 2), reader.units("perso.essai").map { it.blockCount })
            val blocks = reader.blocks("perso.essai", "u1")
            assertEquals("Le Seigneur est mon berger.", Inlines.plainText(blocks[1].texts.getValue("fr")))
            assertTrue(blocks[1].texts.getValue("fr").any { it is Inline.Marked && Mark.ITALIC in it.marks })
            assertEquals(listOf("Chapitre premier", "Chapitre second"), reader.toc("perso.essai", "fr").map { it.title })
            assertEquals("u2.2", reader.search("manquer").single().blockId)
        }
        assertTrue(PackReader.quickCheck(file))
    }
}
