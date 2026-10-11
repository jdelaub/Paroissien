package app.paroissien.library

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.paroissien.core.liturgy.LiturgicalDay
import app.paroissien.core.liturgy.Season
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Une image pieuse du catalogue (rapport 09, §3.2) : œuvre CC0 recadrée au format d'une carte, avec son crédit. */
@Serializable
data class PiousImage(
    val id: String,
    val title: Title,
    val work: String = "",
    val artist: String = "",
    val date: String = "",
    val institution: String = "",
    val licence: String = "",
    val credit: String = "",
    val themes: List<String> = emptyList(),
    val seasons: List<String> = emptyList(),
    val feasts: List<String> = emptyList(),
    val prayer: String? = null,
) {
    @Serializable
    data class Title(val fr: String)
}

@Serializable
private data class Catalog(val format: Int = 1, val cards: List<PiousImage>)

/**
 * La boîte à images, embarquée dans `assets/images/` (copiée depuis `content/data/images/` au build) :
 * aucun appel réseau, jamais (rapport 09, §3.2.3).
 */
class PiousImages(private val context: Context, val all: List<PiousImage>) {
    private val byId = all.associateBy { it.id }
    private val thumbs = LruCache<String, ImageBitmap>(48)
    private val full = LruCache<String, ImageBitmap>(3)

    operator fun get(id: String): PiousImage? = byId[id]

    fun thumbnail(id: String): ImageBitmap? = load(thumbs, id, "images/thumbs/$id.webp")

    fun picture(id: String): ImageBitmap? = load(full, id, "images/$id.webp")

    private fun load(cache: LruCache<String, ImageBitmap>, id: String, path: String): ImageBitmap? =
        cache[id] ?: runCatching { context.assets.open(path).use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }
            .getOrNull()?.also { cache.put(id, it) }

    /**
     * L'image du jour : une carte de la fête si le catalogue en a une, sinon du temps liturgique, choisie de façon
     * stable pour la journée (la même tout le jour, une autre demain).
     */
    fun forDay(day: LiturgicalDay): PiousImage? {
        val ids = listOf(day.principal.id.value) + day.commemorations.map { it.id.value } + day.optional.map { it.id.value }
        all.firstOrNull { img -> img.feasts.any { it in ids } }?.let { return it }
        val season = when (day.season) {
            Season.ADVENT -> "ADVENT"
            Season.CHRISTMAS -> "CHRISTMAS"
            Season.LENT, Season.SEPTUAGESIMA -> "LENT"
            Season.PASSIONTIDE, Season.PASCHAL_TRIDUUM -> "PASSIONTIDE"
            Season.EASTER -> if (day.principal.id.value.startsWith("pentecost")) "PENTECOST" else "EASTER"
            else -> "ORDINARY"
        }
        val pool = all.filter { season in it.seasons }.ifEmpty { all }
        return pool.getOrNull(day.date.toEpochDay().mod(pool.size.coerceAtLeast(1)))
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun load(context: Context): PiousImages {
            val text = runCatching { context.assets.open("images/catalog.json").use { it.readBytes().decodeToString() } }.getOrNull()
                ?: return PiousImages(context, emptyList())
            val images = runCatching { json.decodeFromString<Catalog>(text).cards }.getOrDefault(emptyList())
            return PiousImages(context, images.sortedBy { it.id })
        }
    }
}
