package app.paroissien.core.content

import app.paroissien.core.model.text.Inline
import app.paroissien.core.model.text.Inlines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

class PackTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val golden = File(System.getProperty("golden.dir"), "golden-test.sqlite")
    private val keys = File(System.getProperty("keys.dir"))
    private val publicKey = CatalogVerifier.publicKey(File(keys, "test-p256.pub.pem").readText())

    @Test
    fun goldenPackReadsLikeThePipelineWroteIt() {
        PackReader.open(golden).use { reader ->
            assertEquals("domaine-public", reader.info()["rights"])
            val pub = reader.publications().single()
            assertEquals("golden.test", pub.id)
            assertEquals("fr", pub.primaryLanguage)
            assertEquals(listOf("chap-1", "chap-2", "notes"), reader.units("golden.test").map { it.id })
            val blocks = reader.blocks("golden.test", "chap-1")
            assertEquals("chap-1.titre", blocks.first().id)
            assertEquals("Caput primum", Inlines.plainText(blocks.first().texts.getValue("la")))
            assertTrue(blocks.single { it.id == "chap-1.p1" }.attrs.containsKey("dropCap"))
            assertTrue("le séparateur n'a pas de texte", blocks.single { it.type == "separator" }.texts.isEmpty())
            val versicle = blocks.single { it.id == "chap-1.v1" }.texts.getValue("la")
            assertEquals(Inline.Sym(app.paroissien.core.model.text.Symbol.VERSICLE), versicle.first())
        }
    }

    @Test
    fun opensslSignatureVerifiesInJava() {
        val message = File(keys, "test-vector.txt").readBytes()
        val signature = File(keys, "test-vector.txt.sig").readBytes()
        assertTrue(CatalogVerifier.verify(message, signature, listOf(publicKey)))
        assertFalse(CatalogVerifier.verify(message + 0, signature, listOf(publicKey)))
    }

    @Test
    fun installsVerifiedPacksAndQuarantinesDamagedOnes() {
        val assets = tmp.newFolder("assets")
        val root = tmp.newFolder("installed")
        val packs = File(assets, "packs").apply { mkdirs() }
        golden.copyTo(File(packs, "golden-test.sqlite"))
        writeCatalog(packs, sha256(golden.readBytes()), golden.length())
        val installer = PackInstaller({ File(assets, it).inputStream() }, root, listOf(publicKey))

        val first = installer.install()
        assertEquals(emptyList<String>(), first.problems)
        assertEquals("golden-test", first.packs.single().entry.packageId)
        PackReader.open(first.packs.single().file).use { assertEquals(3, it.units("golden.test").size) }
        assertEquals("la seconde installation ne recopie rien", first.packs, installer.install().packs)

        // Empreinte fausse : le paquet part en quarantaine, l'app ne plante pas.
        root.deleteRecursively()
        writeCatalog(packs, "0".repeat(64), golden.length())
        val damaged = installer.install()
        assertTrue(damaged.packs.isEmpty())
        assertTrue(damaged.problems.single().contains("endommagé"))
        assertTrue(File(root, "quarantine").listFiles()!!.isNotEmpty())

        // Catalogue non signé : refusé en bloc.
        File(packs, "catalog.json.sig").delete()
        assertTrue(installer.install().problems.single().contains("signé"))
    }

    private fun writeCatalog(dir: File, sha: String, size: Long) {
        val catalog = """{"catalogFormat":1,"generated":2026101101,"packs":[{"file":"golden-test.sqlite","manifest":"golden-test.manifest.json",""" +
            """"minAppFormat":"1.0","packageId":"golden-test","packageVersion":2026101101,"publications":["golden.test"],""" +
            """"rights":"domaine-public","sha256":"$sha","size":$size,"title":{"fr":"Paquet golden"}}]}"""
        File(dir, "catalog.json").writeText(catalog)
        File(dir, "catalog.json.sig").writeBytes(sign(catalog.toByteArray()))
    }

    private fun sign(bytes: ByteArray): ByteArray {
        val pem = File(keys, "test-p256.pk8.pem").readText()
        val der = Base64.getDecoder().decode(pem.lines().filterNot { it.startsWith("-----") }.joinToString(""))
        val key = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(der))
        return Signature.getInstance("SHA256withECDSA").run { initSign(key); update(bytes); sign() }
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
