package app.paroissien

import android.app.Application
import android.content.Context
import androidx.sqlite.SQLiteDriver
import app.paroissien.core.data.BookPosition
import app.paroissien.core.data.ReaderSettingsStore
import app.paroissien.core.data.UserDataTransfer
import app.paroissien.core.data.UserDatabase
import app.paroissien.core.liturgy.LiturgicalCalendars
import app.paroissien.library.Library
import app.paroissien.library.PackLibrary
import app.paroissien.library.PiousImages
import app.paroissien.library.Backup
import app.paroissien.library.Vignettes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParoissienApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
}

/**
 * Graphe de l'application, écrit à la main (rapport 10) : bibliothèque des paquets, base de l'utilisateur, réglages,
 * calendrier liturgique et boîte à images. Les livres s'installent en tâche de fond dès le démarrage.
 */
class AppGraph(context: Context, sqliteDriver: SQLiteDriver? = null) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val packs = PackLibrary(context)
    /** [sqliteDriver] : le SQLite embarqué par défaut ; celui du système dans les tests Robolectric. */
    val database: UserDatabase by lazy { sqliteDriver?.let { UserDatabase.open(context, driver = it) } ?: UserDatabase.open(context) }
    val settings = ReaderSettingsStore(context)
    val transfer: UserDataTransfer by lazy { UserDataTransfer(database) }
    val calendar: LiturgicalCalendars by lazy { LiturgicalCalendars.load() }
    val images: PiousImages by lazy { PiousImages.load(context, database, scope) }
    val vignettes = Vignettes(context)
    val backup: Backup by lazy { Backup(context, this) }

    private val libraryState = MutableStateFlow<Library?>(null)
    val library: StateFlow<Library?> = libraryState

    init {
        scope.launch(Dispatchers.IO) { libraryState.value = packs.load() }
    }

    /** Relit les livres après une restauration : ceux de la sauvegarde rejoignent la bibliothèque. */
    suspend fun reloadLibrary() = withContext(Dispatchers.IO) { libraryState.value = packs.load() }

    fun savePosition(publication: String, unit: String, block: String, offset: Int) {
        scope.launch(Dispatchers.IO) {
            database.positions().save(BookPosition(publication, unit, block, offset, null, System.currentTimeMillis()))
        }
    }
}
