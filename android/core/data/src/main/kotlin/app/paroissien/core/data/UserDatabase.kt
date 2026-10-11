package app.paroissien.core.data

import android.content.Context
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.Upsert
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/** Position de lecture d'un livre, ancrée sur le contenu et non sur une page (ADR 004). */
@Entity(tableName = "reading_position")
data class ReadingPosition(
    @PrimaryKey val packageId: String,
    val blockId: String,
    /** Décalage en points de code dans le texte canonique du bloc. */
    val charOffset: Int,
    /** Révision du bloc au moment de l'enregistrement : sert au ré-ancrage après mise à jour du paquet. */
    val blockRev: String,
    val updatedAt: Long,
)

@Dao
interface ReadingPositionDao {
    @Query("SELECT * FROM reading_position WHERE packageId = :packageId")
    suspend fun get(packageId: String): ReadingPosition?

    @Upsert
    suspend fun save(position: ReadingPosition)
}

@Database(entities = [ReadingPosition::class], version = 1, exportSchema = true)
abstract class UserDatabase : RoomDatabase() {
    abstract fun readingPositions(): ReadingPositionDao

    companion object {
        fun open(context: Context): UserDatabase =
            Room.databaseBuilder<UserDatabase>(context, "user.db")
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}
