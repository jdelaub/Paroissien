package app.paroissien.library

import app.paroissien.core.model.text.Inline
import app.paroissien.core.model.text.Inlines
import app.paroissien.core.model.text.Mark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Un texte ou un epub devient un livre : chapitres, titres, paragraphes, italiques ; le reste est ignoré. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BookImportTest {

    @Test
    fun texte() {
        val text = """
            The Project Gutenberg eBook
            Title: Introduction à la vie dévote
            Author: François de Sales

            *** START OF THE PROJECT GUTENBERG EBOOK ***

            PRÉFACE

            Mon cher lecteur, je te prie de lire cette préface
            pour ta satisfaction et la mienne.

            CHAPITRE PREMIER. DESCRIPTION DE LA VRAIE DÉVOTION

            Vous aspirez à la dévotion, très chère Philothée.

            Chapitre II

            La dévotion est propre à toutes sortes de vocations.

            *** END OF THE PROJECT GUTENBERG EBOOK ***
            licence...
        """.trimIndent()
        val book = requireNotNull(BookImport.parse(text.toByteArray(), "philothee.txt"))
        assertEquals("Introduction à la vie dévote", book.title)
        assertEquals("François de Sales", book.author)
        assertEquals("fr", book.language)
        assertEquals(listOf("PRÉFACE", "CHAPITRE PREMIER. DESCRIPTION DE LA VRAIE DÉVOTION", "Chapitre II"), book.units.map { it.title })
        val preface = book.units.first().blocks
        assertEquals("heading", preface[0].type)
        assertEquals("Mon cher lecteur, je te prie de lire cette préface pour ta satisfaction et la mienne.", Inlines.plainText(preface[1].text))
        assertTrue(book.units.none { u -> u.blocks.any { Inlines.plainText(it.text).contains("licence") } })
        assertTrue(book.publicationId.startsWith("perso.introduction-a-la-vie-devote"))
    }

    @Test
    fun windows1252() {
        val bytes = "Prière du matin\n\nÔ mon Dieu, je vous offre cette journée.".toByteArray(charset("windows-1252"))
        val book = requireNotNull(BookImport.parse(bytes, "priere.txt"))
        assertEquals("Ô mon Dieu, je vous offre cette journée.", Inlines.plainText(book.units.single().blocks.last().text))
    }

    private fun epub(): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { z ->
            fun put(name: String, text: String) { z.putNextEntry(ZipEntry(name)); z.write(text.toByteArray()); z.closeEntry() }
            put("mimetype", "application/epub+zip")
            put("META-INF/container.xml", """<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""")
            put("OEBPS/content.opf", """<?xml version="1.0"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Histoire d'une âme</dc:title><dc:creator>Thérèse de Lisieux</dc:creator><dc:language>fr-FR</dc:language></metadata><manifest><item id="c1" href="texte/chap%201.xhtml" media-type="application/xhtml+xml"/><item id="c2" href="texte/chap2.xhtml" media-type="application/xhtml+xml"/><item id="css" href="style.css" media-type="text/css"/></manifest><spine><itemref idref="c1"/><itemref idref="c2"/></spine></package>""")
            put("OEBPS/texte/chap 1.xhtml", """<?xml version="1.0" encoding="utf-8"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"><head><title>x</title><style>p{}</style></head><body><h1>Manuscrit A</h1><p>C'est à vous, ma Mère&nbsp;bien-aimée, que je vais <i>confier</i> l'histoire de mon âme.</p><p>Je me sens<br/>pressée.</p><script>alert(1)</script></body></html>""")
            put("OEBPS/texte/chap2.xhtml", """<html xmlns="http://www.w3.org/1999/xhtml"><body><h2>Manuscrit B</h2><div><p>Ô Jésus, mon <b>Bien-Aimé</b>&hellip;</p></div></body></html>""")
        }
    }.toByteArray()

    @Test
    fun epubLu() {
        val book = requireNotNull(BookImport.parse(epub(), "therese.epub"))
        assertEquals("Histoire d'une âme", book.title)
        assertEquals("Thérèse de Lisieux", book.author)
        assertEquals("fr", book.language)
        assertEquals(listOf("Manuscrit A", "Manuscrit B"), book.units.map { it.title })
        val first = book.units[0].blocks
        assertEquals(listOf("heading", "para", "para"), first.map { it.type })
        assertEquals("C'est à vous, ma Mère bien-aimée, que je vais confier l'histoire de mon âme.", Inlines.plainText(first[1].text))
        assertTrue(first[1].text.any { it is Inline.Marked && Mark.ITALIC in it.marks })
        assertTrue(first[2].text.contains(Inline.Break))
        assertTrue("le script est ignoré", book.units.none { u -> u.blocks.any { Inlines.plainText(it.text).contains("alert") } })
        assertEquals("Ô Jésus, mon Bien-Aimé…", Inlines.plainText(book.units[1].blocks.last().text))
        assertEquals("2", book.units[1].blocks.first().attrs["level"])
    }

    @Test
    fun rienDeLisible() {
        assertNull(BookImport.parse(ByteArray(0), "vide.txt"))
        assertNull(BookImport.parse("PK\u0003\u0004pas un zip".toByteArray(), "faux.epub"))
    }
}
