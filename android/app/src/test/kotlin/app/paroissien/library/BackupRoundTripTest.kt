package app.paroissien.library

import androidx.sqlite.driver.AndroidSQLiteDriver
import app.paroissien.AppGraph
import app.paroissien.core.data.PersonalCard
import app.paroissien.core.data.Ribbon
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.zip.ZipFile

/**
 * Sauvegarde complète, aller et retour, pour les données de l'utilisateur : on écrit le fichier, l'appareil oublie
 * image, ruban et réglage, on restaure. Le SQLite embarqué de l'AAR Android n'a pas de bibliothèque pour la machine
 * hôte : la base de l'utilisateur prend ici le SQLite du système, et les livres ne s'ouvrent pas (ils sont mis de
 * côté). Leur installation depuis le fichier est celle de [app.paroissien.core.content.PackInstaller], testée dans
 * core:content.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupRoundTripTest {

    @Test
    fun sauvegardePuisRestauration() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val graph = AppGraph(app, AndroidSQLiteDriver())
        val cards = File(app.filesDir, "cards").apply { mkdirs() }
        File(cards, "essai-front.webp").writeBytes(byteArrayOf(1, 2, 3))
        File(cards, "essai-back.webp").writeBytes(byteArrayOf(4, 5))
        graph.database.personalCards().save(PersonalCard("essai", "Essai", hasBack = true, createdAt = 1L))
        graph.database.ribbons().place(Ribbon("prieres", 0, "u", "b", 0, null, 5L))
        graph.settings.update { it.copy(theme = "SEPIA", vignettes = false) }

        val file = File(app.cacheDir, "sauvegarde.zip")
        val written = file.outputStream().use { graph.backup.export(it) }
        assertEquals(1, written.images)
        val names = ZipFile(file).use { zip -> zip.entries().asSequence().map { it.name }.toSet() }
        assertTrue(names.containsAll(listOf("manifeste.json", "reglages.json", "donnees.json", "images/essai-front.webp", "images/essai-back.webp")))
        assertTrue(names.all(BackupFormat::isSafe))

        // L'appareil oublie tout.
        cards.deleteRecursively()
        graph.database.personalCards().delete("essai")
        graph.database.ribbons().remove("prieres", 0)
        graph.settings.update { it.copy(theme = "CLAIR", vignettes = true) }

        val restored = file.inputStream().use { graph.backup.import(it) }
        assertEquals(1, restored.images)
        assertTrue(restored.settings)
        assertTrue(File(cards, "essai-back.webp").isFile)
        val settings = graph.settings.settings.first()
        assertEquals("SEPIA", settings.theme)
        assertEquals(false, settings.vignettes)
        assertNotNull(graph.database.ribbons().all().firstOrNull { it.publication == "prieres" })
        assertEquals(true, graph.database.personalCards().all().firstOrNull { it.id == "essai" }?.hasBack)

        // Deuxième restauration : rien n'est remplacé ni dupliqué.
        val again = file.inputStream().use { graph.backup.import(it) }
        assertEquals(0, again.images)
    }
}
