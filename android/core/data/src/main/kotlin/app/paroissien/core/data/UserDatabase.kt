package app.paroissien.core.data

import android.content.Context
import androidx.room3.AutoMigration
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.DeleteTable
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.Upsert
import androidx.room3.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * Position de lecture d'une publication (ADR 004) : l'unité ouverte et une ancre sur le contenu, jamais une page.
 * [blockRev] sert au ré-ancrage quand le paquet change.
 */
@Entity(tableName = "book_position")
data class BookPosition(
    @PrimaryKey val publication: String,
    val unit: String,
    val block: String,
    val charOffset: Int,
    val blockRev: String?,
    val updatedAt: Long,
)

/** Un ruban de tissu cousu dans un livre (rapport 09) : [slot] 0 à 5 fixe sa couleur et sa forme. */
@Entity(tableName = "ribbon", primaryKeys = ["publication", "slot"])
data class Ribbon(
    val publication: String,
    val slot: Int,
    val unit: String,
    val block: String,
    val charOffset: Int,
    val blockRev: String?,
    val updatedAt: Long,
)

@Dao
interface PositionDao {
    @Query("SELECT * FROM book_position WHERE publication = :publication")
    suspend fun get(publication: String): BookPosition?

    @Query("SELECT * FROM book_position ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<BookPosition>>

    @Query("SELECT * FROM book_position")
    suspend fun all(): List<BookPosition>

    @Upsert
    suspend fun save(position: BookPosition)
}

@Dao
interface RibbonDao {
    @Query("SELECT * FROM ribbon WHERE publication = :publication ORDER BY slot")
    fun observe(publication: String): Flow<List<Ribbon>>

    @Query("SELECT * FROM ribbon ORDER BY publication, slot")
    fun observeAll(): Flow<List<Ribbon>>

    @Query("SELECT * FROM ribbon")
    suspend fun all(): List<Ribbon>

    @Upsert
    suspend fun place(ribbon: Ribbon)

    @Query("DELETE FROM ribbon WHERE publication = :publication AND slot = :slot")
    suspend fun remove(publication: String, slot: Int)
}

@Database(
    entities = [BookPosition::class, Ribbon::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = UserDatabase.V1ToV2::class)],
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun positions(): PositionDao
    abstract fun ribbons(): RibbonDao

    /** La version 1 n'avait qu'une table d'essai, jamais remplie : elle disparaît. */
    @DeleteTable(tableName = "reading_position")
    class V1ToV2 : AutoMigrationSpec

    companion object {
        const val NAME = "user.db"

        fun open(context: Context, name: String = NAME, driver: SQLiteDriver = BundledSQLiteDriver()): UserDatabase =
            Room.databaseBuilder<UserDatabase>(context, name)
                .setDriver(driver)
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}
