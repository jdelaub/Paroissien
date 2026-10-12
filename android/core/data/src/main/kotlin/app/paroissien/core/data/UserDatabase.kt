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

/**
 * Une image pieuse glissée dans un livre (rapport 09, §3.2). Comme une vraie carte, elle n'est qu'à un endroit à la
 * fois : la retirer du livre la remet dans la boîte (la ligne disparaît).
 */
@Entity(tableName = "card_placement")
data class CardPlacement(
    @PrimaryKey val card: String,
    val publication: String,
    val unit: String,
    val block: String,
    val charOffset: Int,
    val blockRev: String?,
    val updatedAt: Long,
)

/**
 * Une image pieuse numérisée par l'utilisateur (rapport 09, §3.3). Les images elles-mêmes sont des fichiers WebP dans
 * `files/cards/` (recto, verso facultatif, vignette) ; la base n'en garde que la description.
 */
@Entity(tableName = "personal_card")
data class PersonalCard(
    @PrimaryKey val id: String,
    val title: String,
    val hasBack: Boolean,
    val createdAt: Long,
)

/** Une catégorie créée par l'utilisateur pour ranger ses images (en plus des catégories prédéfinies). */
@Entity(tableName = "card_category")
data class CardCategory(
    @PrimaryKey val id: String,
    val label: String,
    val createdAt: Long,
)

/**
 * Une image rangée dans une catégorie, prédéfinie ou créée, par l'utilisateur. [removed] : il l'en a retirée, alors
 * que la bibliothèque l'y mettait. Le plus récent l'emporte à l'import.
 */
@Entity(tableName = "card_in_category", primaryKeys = ["card", "category"])
data class CardInCategory(
    val card: String,
    val category: String,
    val removed: Boolean,
    val updatedAt: Long,
)

/**
 * L'habit d'un livre, choisi dans l'atelier (ATELIER §9) : description JSON (mode des cadres, cadre unique, cadres par
 * chapitre, tirage au hasard…), lue par l'application.
 */
@Entity(tableName = "book_look")
data class BookLookRow(
    @PrimaryKey val publication: String,
    val json: String,
    val updatedAt: Long,
)

/** Un cadre composé par l'utilisateur à partir des modules de la bibliothèque : description JSON. */
@Entity(tableName = "custom_frame")
data class CustomFrameRow(
    @PrimaryKey val id: String,
    val json: String,
    val updatedAt: Long,
)

@Dao
interface LookDao {
    @Query("SELECT * FROM book_look WHERE publication = :publication")
    fun observe(publication: String): Flow<BookLookRow?>

    @Query("SELECT * FROM book_look")
    suspend fun all(): List<BookLookRow>

    @Upsert
    suspend fun save(look: BookLookRow)

    @Query("SELECT * FROM custom_frame ORDER BY updatedAt")
    fun observeFrames(): Flow<List<CustomFrameRow>>

    @Query("SELECT * FROM custom_frame")
    suspend fun frames(): List<CustomFrameRow>

    @Upsert
    suspend fun saveFrame(frame: CustomFrameRow)

    @Query("DELETE FROM custom_frame WHERE id = :id")
    suspend fun deleteFrame(id: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM card_category ORDER BY label")
    fun observeCategories(): Flow<List<CardCategory>>

    @Query("SELECT * FROM card_category")
    suspend fun categories(): List<CardCategory>

    @Query("SELECT * FROM card_in_category")
    fun observeAssignments(): Flow<List<CardInCategory>>

    @Query("SELECT * FROM card_in_category")
    suspend fun assignments(): List<CardInCategory>

    @Upsert
    suspend fun saveCategory(category: CardCategory)

    @Upsert
    suspend fun assign(assignment: CardInCategory)

    @Query("DELETE FROM card_category WHERE id = :id")
    suspend fun deleteCategory(id: String)

    @Query("DELETE FROM card_in_category WHERE category = :id")
    suspend fun clearCategory(id: String)
}

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

@Dao
interface CardDao {
    @Query("SELECT * FROM card_placement WHERE publication = :publication")
    fun observe(publication: String): Flow<List<CardPlacement>>

    @Query("SELECT * FROM card_placement ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CardPlacement>>

    @Query("SELECT * FROM card_placement")
    suspend fun all(): List<CardPlacement>

    @Upsert
    suspend fun place(card: CardPlacement)

    @Query("DELETE FROM card_placement WHERE card = :card")
    suspend fun remove(card: String)
}

@Dao
interface PersonalCardDao {
    @Query("SELECT * FROM personal_card ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PersonalCard>>

    @Query("SELECT * FROM personal_card")
    suspend fun all(): List<PersonalCard>

    @Upsert
    suspend fun save(card: PersonalCard)

    @Query("DELETE FROM personal_card WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(
    entities = [
        BookPosition::class, Ribbon::class, CardPlacement::class, PersonalCard::class, CardCategory::class, CardInCategory::class,
        BookLookRow::class, CustomFrameRow::class,
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = UserDatabase.V1ToV2::class),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
    ],
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun positions(): PositionDao
    abstract fun ribbons(): RibbonDao
    abstract fun cards(): CardDao
    abstract fun personalCards(): PersonalCardDao
    abstract fun categories(): CategoryDao
    abstract fun looks(): LookDao

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
