package app.paroissien

import android.app.Application
import android.content.Context
import app.paroissien.core.data.BookPosition
import app.paroissien.core.data.ReaderSettingsStore
import app.paroissien.core.data.UserDataTransfer
import app.paroissien.core.data.UserDatabase
import app.paroissien.core.liturgy.LiturgicalCalendars
import app.paroissien.library.Library
import app.paroissien.library.PackLibrary
import app.paroissien.library.PiousImages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ParoissienApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
}

/**
 * Graphe de l'application, écrit à la main (rapport 10) : bibliothèque des paquets, base de l'utilisateur, réglages,
 * calendrier liturgique et boîte à images. Les livres s'installent en tâche de fond dès le démarrage.
 */
class AppGraph(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val packs = PackLibrary(context)
    val database: UserDatabase by lazy { UserDatabase.open(context) }
    val settings = ReaderSettingsStore(context)
    val transfer: UserDataTransfer by lazy { UserDataTransfer(database) }
    val calendar: LiturgicalCalendars by lazy { LiturgicalCalendars.load() }
    val images: PiousImages by lazy { PiousImages.load(context, database, scope) }

    private val libraryState = MutableStateFlow<Library?>(null)
    val library: StateFlow<Library?> = libraryState

    init {
        scope.launch(Dispatchers.IO) { libraryState.value = packs.load() }
    }

    fun savePosition(publication: String, unit: String, block: String, offset: Int) {
        scope.launch(Dispatchers.IO) {
            database.positions().save(BookPosition(publication, unit, block, offset, null, System.currentTimeMillis()))
        }
    }
}
