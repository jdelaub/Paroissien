package app.paroissien.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Réglages de lecture, mémorisés sur l'appareil (DataStore). Les valeurs sont des noms d'énumérations du lecteur. */
data class ReaderSettings(
    val theme: String = "CLAIR",
    val face: String = "GARAMOND",
    val scaleIndex: Int = 1,
    val spread: String = "AUTO",
    val turn: String = "PAPIER",
    val protectEdges: Boolean = false,
    val showMeasures: Boolean = false,
    val language: String = "fr",
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "reglages")

class ReaderSettingsStore(context: Context) {
    private val store = context.applicationContext.settingsStore

    val settings: Flow<ReaderSettings> = store.data.map(::read)

    suspend fun update(transform: (ReaderSettings) -> ReaderSettings) {
        store.edit { p ->
            val next = transform(read(p))
            p[THEME] = next.theme
            p[FACE] = next.face
            p[SCALE] = next.scaleIndex
            p[SPREAD] = next.spread
            p[TURN] = next.turn
            p[EDGES] = next.protectEdges
            p[MEASURES] = next.showMeasures
            p[LANGUAGE] = next.language
        }
    }

    private fun read(p: Preferences): ReaderSettings {
        val d = ReaderSettings()
        return ReaderSettings(
            theme = p[THEME] ?: d.theme,
            face = p[FACE] ?: d.face,
            scaleIndex = p[SCALE] ?: d.scaleIndex,
            spread = p[SPREAD] ?: d.spread,
            turn = p[TURN] ?: d.turn,
            protectEdges = p[EDGES] ?: d.protectEdges,
            showMeasures = p[MEASURES] ?: d.showMeasures,
            language = p[LANGUAGE] ?: d.language,
        )
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val FACE = stringPreferencesKey("police")
        val SCALE = intPreferencesKey("taille")
        val SPREAD = stringPreferencesKey("pages")
        val TURN = stringPreferencesKey("feuille")
        val EDGES = booleanPreferencesKey("bords_proteges")
        val MEASURES = booleanPreferencesKey("mesures")
        val LANGUAGE = stringPreferencesKey("langue")
    }
}
