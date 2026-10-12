package app.paroissien.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Une sauvegarde ne déplie que ses propres fichiers : rien hors du dossier de restauration, rien d'inattendu. */
class BackupFormatTest {

    @Test
    fun fichiersAttendus() {
        listOf(
            "manifeste.json", "reglages.json", "donnees.json",
            "images/3f2a-9c-front.webp", "images/3f2a-9c-back.webp", "images/3f2a-9c-thumb.webp",
            "livres/packs/catalog.json", "livres/packs/catalog.json.sig", "livres/packs-private/clairval-fr.sqlite",
            "vignettes/catalog.json", "vignettes/bapteme-du-christ.webp",
        ).forEach { assertTrue(it, BackupFormat.isSafe(it)) }
    }

    @Test
    fun cheminsRefuses() {
        listOf(
            "../evil", "/data/data/app.paroissien/files/x", "livres/../reglages.json", "livres/packs/../../x",
            "livres/packs/.hidden", "livres/Packs/catalog.json", "livres/packs/sous/dossier.sqlite",
            "images/x.webp", "images/../x-front.webp", "images/a/b-front.webp", "autre.json", "livres\\packs\\x",
            "vignettes/../x.webp", "vignettes/x.png", "vignettes/sous/x.webp",
        ).forEach { assertFalse(it, BackupFormat.isSafe(it)) }
    }
}
