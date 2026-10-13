package app.paroissien.library

import android.graphics.Bitmap
import androidx.sqlite.driver.AndroidSQLiteDriver
import app.paroissien.AppGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Une image pieuse d'une app à l'autre : le `.paroissien-image` reçu devient une image personnelle qui garde sa
 * description ; renvoyée, elle repart avec. Un zip qui n'en est pas un est refusé, et ses entrées inattendues ignorées.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class CardBundleTest {

    private fun png(w: Int, h: Int): ByteArray = ByteArrayOutputStream().also {
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.rgb(200, 180, 120)) }.compress(Bitmap.CompressFormat.PNG, 100, it)
    }.toByteArray()

    private fun zip(vararg parts: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { z -> parts.forEach { (name, bytes) -> z.putNextEntry(ZipEntry(name)); z.write(bytes); z.closeEntry() } }
    }.toByteArray()

    @Test
    fun allerEtRetour() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val graph = AppGraph(app, AndroidSQLiteDriver())
        val meta = """{"id":"x","title":{"fr":"Souvenir de première communion"},"inscription":"Paris, 1932","people":["Marie"],"categories":["premiere-communion","perso-famille"]}"""
        val received = zip("image.json" to meta.encodeToByteArray(), "recto.webp" to png(300, 480), "verso.webp" to png(300, 480), "autre.bin" to byteArrayOf(9))

        val id = requireNotNull(graph.images.importBundle(received.inputStream()))
        val image = graph.images.all.first { list -> list.any { it.id == id } }.single { it.id == id }
        assertTrue(image.personal)
        assertTrue(image.hasBack)
        assertEquals("Souvenir de première communion", image.title.fr)
        assertEquals("Paris, 1932", image.inscription)
        assertEquals(listOf("Marie"), image.people)

        val file = requireNotNull(graph.images.bundle(id))
        val names = ZipFile(file).use { z -> z.entries().asSequence().map { it.name }.toSet() }
        assertEquals(setOf("image.json", "recto.webp", "verso.webp", "vignette.webp"), names)
        val sent = ZipFile(file).use { z -> z.getInputStream(z.getEntry("image.json")).readBytes().decodeToString() }
        assertTrue(sent.contains("Paris, 1932"))
        assertTrue(sent.contains("premiere-communion"))
        assertFalse("une catégorie de l'utilisateur ne voyage pas", sent.contains("perso-famille"))

        assertEquals(2, graph.images.shareFaces(id).size)
    }

    @Test
    fun refus() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val graph = AppGraph(app, AndroidSQLiteDriver())
        assertNull(graph.images.importBundle(zip("recto.webp" to png(10, 10)).inputStream()))
        assertNull(graph.images.importBundle(zip("image.json" to "{}".encodeToByteArray(), "recto.webp" to byteArrayOf(1, 2, 3)).inputStream()))
        assertNull(graph.images.importBundle("pas un zip".byteInputStream()))
    }
}
