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

    /** La base créée par la 0.0.1 (schéma 1, exporté par Room) passe en version 2 sans rien casser (ADR 008). */
    @Test
    fun migratesFromVersionOne() = runBlocking {
        val schema = Json.parseToJsonElement(
            File(System.getProperty("room.schemas"), "app.paroissien.core.data.UserDatabase/1.json").readText(),
        ).jsonObject.getValue("database").jsonObject
        val file = context.getDatabasePath("migration.db").apply { parentFile?.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            schema.getValue("entities").jsonArray.forEach {
                db.execSQL(it.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", it.jsonObject.getValue("tableName").jsonPrimitive.content))
            }
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '${schema.getValue("identityHash").jsonPrimitive.content}')")
            db.version = 1
        }
        val migrated = UserDatabase.open(context, "migration.db", AndroidSQLiteDriver())
        migrated.ribbons().place(Ribbon("imitation.fr", 0, "l1", "l1.c1.p1", 0, null, 1))
        assertEquals(1, migrated.ribbons().all().size)
        migrated.close()
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null).use { c ->
                val tables = buildList { while (c.moveToNext()) add(c.getString(0)) }
                assertTrue(tables.containsAll(listOf("book_position", "ribbon")))
                assertFalse("l'ancienne table d'essai est retirée", "reading_position" in tables)
            }
        }
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
        val file = UserDataTransfer(source).export(now = 1000)
        source.close()

        val target = UserDatabase.open(context, "target.db", AndroidSQLiteDriver())
        target.positions().save(BookPosition("imitation.fr", "l3", "l3.c1.p1", 0, null, 200)) // plus récente : gardée
        target.ribbons().place(Ribbon("bible.crampon", 1, "jhn", "jhn.1.1", 0, null, 50)) // absente de l'export : gardée
        assertEquals(1, UserDataTransfer(target).import(file))
        assertEquals("l3", target.positions().get("imitation.fr")!!.unit)
        assertEquals(2, target.ribbons().all().size)
        target.close()
    }
}
