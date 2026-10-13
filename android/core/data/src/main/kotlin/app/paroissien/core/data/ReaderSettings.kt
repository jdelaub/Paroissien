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
import kotlinx.serialization.Serializable

/**
 * Réglages, mémorisés sur l'appareil (DataStore) : lecture (noms d'énumérations du lecteur), calendrier (ADR 009)
 * et temps de prière (rapport 08, §2.10). Sérialisables pour la sauvegarde complète.
 */
@Serializable
data class ReaderSettings(
    val theme: String = "CLAIR",
    val face: String = "GARAMOND",
    val scaleIndex: Int = 1,
    val spread: String = "AUTO",
    val turn: String = "PAPIER",
    val protectEdges: Boolean = false,
    val showMeasures: Boolean = false,
    /** « fr », « la », ou « la-fr » : latin et français en regard dans les livres bilingues (par défaut). */
    val language: String = "la-fr",
    /** Chaque livre a sa reliure, son papier, sa tranche et ses signets ; sinon, tous pareils. */
    val bookIdentity: Boolean = true,
    /** Le ruban est couché en travers de la page, comme dans un vrai livre ; sinon, dans le petit fond. */
    val ribbonsAcross: Boolean = true,
    /** Tissu des rubans : "TISSU" (mat, à côtes) ou "SATIN". */
    val ribbonFabric: String = "TISSU",
    /** Les images pieuses sont posées sur leur page ; sinon, on n'en voit que le haut, qui dépasse de la tête. */
    val cardsOnPage: Boolean = true,
    /** Les pages des livres de prières dans les cadres du paroissien chromolithographié, un par section. */
    val ornamentalFrames: Boolean = true,
    /** Vignettes gravées en tête des sections des livres nobles, quand l'app en contient. */
    val vignettes: Boolean = true,
    /** Pincer l'écran à deux doigts change la taille du texte ; désactivé par défaut (Aa). */
    val pinchToZoom: Boolean = false,
    /** Les rubans retouchés dans l'atelier (JSON, un par emplacement) ; vide : les six rubans liturgiques. */
    val ribbons: String = "",
    /** Rite du calendrier : "1962" ou "1969". */
    val rite: String = "1962",
    val externalSolemnities: Boolean = true,
    val traditionalPenance: Boolean = false,
    val prayerMinutes: Int = 15,
    val prayerBells: Boolean = true,
    val prayerDoNotDisturb: Boolean = true,
    val prayerGrayscale: Boolean = false,
    /** Identifiant de la règle « Ne pas déranger » créée pour le temps de prière. */
    val zenRuleId: String? = null,
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
            p[IDENTITY] = next.bookIdentity
            p[RIBBONS_ACROSS] = next.ribbonsAcross
            p[RIBBON_FABRIC] = next.ribbonFabric
            p[CARDS_ON_PAGE] = next.cardsOnPage
            p[FRAMES] = next.ornamentalFrames
            p[VIGNETTES] = next.vignettes
            p[PINCH] = next.pinchToZoom
            p[RIBBONS] = next.ribbons
            p[RITE] = next.rite
            p[EXTERNAL] = next.externalSolemnities
            p[TRADITIONAL] = next.traditionalPenance
            p[PRAYER_MINUTES] = next.prayerMinutes
            p[PRAYER_BELLS] = next.prayerBells
            p[PRAYER_DND] = next.prayerDoNotDisturb
            p[PRAYER_GRAY] = next.prayerGrayscale
            if (next.zenRuleId == null) p.remove(ZEN_RULE) else p[ZEN_RULE] = next.zenRuleId
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
            bookIdentity = p[IDENTITY] ?: d.bookIdentity,
            ribbonsAcross = p[RIBBONS_ACROSS] ?: d.ribbonsAcross,
            ribbonFabric = p[RIBBON_FABRIC] ?: d.ribbonFabric,
            cardsOnPage = p[CARDS_ON_PAGE] ?: d.cardsOnPage,
            ornamentalFrames = p[FRAMES] ?: d.ornamentalFrames,
            vignettes = p[VIGNETTES] ?: d.vignettes,
            pinchToZoom = p[PINCH] ?: d.pinchToZoom,
            ribbons = p[RIBBONS] ?: d.ribbons,
            rite = p[RITE] ?: d.rite,
            externalSolemnities = p[EXTERNAL] ?: d.externalSolemnities,
            traditionalPenance = p[TRADITIONAL] ?: d.traditionalPenance,
            prayerMinutes = p[PRAYER_MINUTES] ?: d.prayerMinutes,
            prayerBells = p[PRAYER_BELLS] ?: d.prayerBells,
            prayerDoNotDisturb = p[PRAYER_DND] ?: d.prayerDoNotDisturb,
            prayerGrayscale = p[PRAYER_GRAY] ?: d.prayerGrayscale,
            zenRuleId = p[ZEN_RULE],
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
        val IDENTITY = booleanPreferencesKey("identite_des_livres")
        val RIBBONS_ACROSS = booleanPreferencesKey("rubans_en_travers")
        val RIBBON_FABRIC = stringPreferencesKey("rubans_tissu")
        val CARDS_ON_PAGE = booleanPreferencesKey("images_sur_la_page")
        val FRAMES = booleanPreferencesKey("cadres_ornementaux")
        val VIGNETTES = booleanPreferencesKey("vignettes")
        val PINCH = booleanPreferencesKey("pincer_taille")
        val RIBBONS = stringPreferencesKey("rubans")
        val RITE = stringPreferencesKey("rite")
        val EXTERNAL = booleanPreferencesKey("solennites_exterieures")
        val TRADITIONAL = booleanPreferencesKey("penitence_traditionnelle")
        val PRAYER_MINUTES = intPreferencesKey("priere_minutes")
        val PRAYER_BELLS = booleanPreferencesKey("priere_cloches")
        val PRAYER_DND = booleanPreferencesKey("priere_npd")
        val PRAYER_GRAY = booleanPreferencesKey("priere_gris")
        val ZEN_RULE = stringPreferencesKey("regle_npd")
    }
}
