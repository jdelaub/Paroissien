package app.paroissien.core.data

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UserDatabaseTest {

    private val context = RuntimeEnvironment.getApplication()

    /** Crée une base au schéma exporté [version], comme l'aurait laissée l'app de cette époque. */
    private fun createAt(version: Int, name: String): File {
        val schema = Json.parseToJsonElement(
            File(System.getProperty("room.schemas"), "app.paroissien.core.data.UserDatabase/$version.json").readText(),
        ).jsonObject.getValue("database").jsonObject
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            schema.getValue("entities").jsonArray.forEach {
                db.execSQL(it.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", it.jsonObject.getValue("tableName").jsonPrimitive.content))
            }
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '${schema.getValue("identityHash").jsonPrimitive.content}')")
            db.version = version
        }
        return file
    }

    /** La base créée par la 0.0.1 (schéma 1, exporté par Room) passe au schéma courant sans rien casser (ADR 008). */
    @Test
    fun migratesFromVersionOne() = runBlocking {
        val file = createAt(1, "migration.db")
        val migrated = UserDatabase.open(context, "migration.db", AndroidSQLiteDriver())
        migrated.ribbons().place(Ribbon("imitation.fr", 0, "l1", "l1.c1.p1", 0, null, 1))
        assertEquals(1, migrated.ribbons().all().size)
        migrated.close()
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null).use { c ->
                val tables = buildList { while (c.moveToNext()) add(c.getString(0)) }
                assertTrue(tables.containsAll(listOf("book_position", "ribbon", "card_placement")))
                assertFalse("l'ancienne table d'essai est retirée", "reading_position" in tables)
            }
        }
    }

    /** La base de la 0.4.0 (schéma 2) garde ses rubans et gagne les images pieuses. */
    @Test
    fun migratesFromVersionTwo() = runBlocking {
        val file = createAt(2, "migration2.db")
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL("INSERT INTO ribbon (publication, slot, unit, block, charOffset, blockRev, updatedAt) VALUES ('imitation.fr', 3, 'l1', 'l1.c2.p1', 4, NULL, 7)")
        }
        val migrated = UserDatabase.open(context, "migration2.db", AndroidSQLiteDriver())
        assertEquals("l1.c2.p1", migrated.ribbons().all().single().block)
        migrated.cards().place(CardPlacement("memling_vierge", "imitation.fr", "l1", "l1.c2.p1", 0, null, 8))
        assertEquals(1, migrated.cards().all().size)
        migrated.close()
    }

    /** La base de la 0.5 (schéma 3) garde ses images placées et gagne les images personnelles. */
    @Test
    fun migratesFromVersionThree() = runBlocking {
        val file = createAt(3, "migration3.db")
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL("INSERT INTO card_placement (card, publication, unit, block, charOffset, blockRev, updatedAt) VALUES ('perso_1', 'imitation.fr', 'l1', 'l1.c2', 0, NULL, 7)")
        }
        val migrated = UserDatabase.open(context, "migration3.db", AndroidSQLiteDriver())
        assertEquals("perso_1", migrated.cards().all().single().card)
        migrated.personalCards().save(PersonalCard("perso_1", "Souvenir de première communion", hasBack = false, createdAt = 8))
        assertEquals(1, migrated.personalCards().observeAll().first().size)
        migrated.close()
    }

    @Test
    fun aCardIsInOneBookAtATime() = runBlocking {
        val db = UserDatabase.open(context, "cards.db", AndroidSQLiteDriver())
        db.cards().place(CardPlacement("sacre_coeur", "imitation.fr", "l1", "l1.c1.p1", 0, null, 1))
        db.cards().place(CardPlacement("sacre_coeur", "bible.crampon", "jhn", "jhn.19.34", 0, null, 2))
        assertEquals(listOf("bible.crampon"), db.cards().all().map { it.publication })
        db.cards().remove("sacre_coeur")
        assertTrue(db.cards().all().isEmpty())
        db.close()
    }

    @Test
    fun ribbonsArePlacedMovedAndRemovedPerBook() = runBlocking {
        val db = UserDatabase.open(context, "ribbons.db", AndroidSQLiteDriver())
        val dao = db.ribbons()
        dao.place(Ribbon("bible.crampon", 2, "jhn", "jhn.3.16", 0, "abc", 10))
        dao.place(Ribbon("bible.crampon", 2, "gen", "gen.1.1", 0, "def", 20)) // le même ruban, déplacé
        dao.place(Ribbon("imitation.fr", 2, "l1", "l1.c1.p1", 5, null, 30))
        assertEquals(listOf("gen.1.1"), dao.observe("bible.crampon").first().map { it.block })
        dao.remove("bible.crampon", 2)
        assertTrue(dao.observe("bible.crampon").first().isEmpty())
        assertEquals(1, dao.all().size)
        db.close()
    }

    @Test
    fun exportThenImportKeepsTheNewestAndNeverDeletes() = runBlocking {
        val source = UserDatabase.open(context, "source.db", AndroidSQLiteDriver())
        source.positions().save(BookPosition("imitation.fr", "l2", "l2.c4.p3", 12, "r1", 100))
        source.ribbons().place(Ribbon("imitation.fr", 0, "l1", "l1.c1.p1", 0, null, 100))
        source.cards().place(CardPlacement("sacre_coeur", "imitation.fr", "l1", "l1.c1.p1", 0, null, 100))
        val file = UserDataTransfer(source).export(now = 1000)
        source.close()

        val target = UserDatabase.open(context, "target.db", AndroidSQLiteDriver())
        target.positions().save(BookPosition("imitation.fr", "l3", "l3.c1.p1", 0, null, 200)) // plus récente : gardée
        target.ribbons().place(Ribbon("bible.crampon", 1, "jhn", "jhn.1.1", 0, null, 50)) // absente de l'export : gardée
        assertEquals(2, UserDataTransfer(target).import(file))
        assertEquals("l3", target.positions().get("imitation.fr")!!.unit)
        assertEquals(2, target.ribbons().all().size)
        assertEquals("sacre_coeur", target.cards().all().single().card)
        target.close()
    }
}
