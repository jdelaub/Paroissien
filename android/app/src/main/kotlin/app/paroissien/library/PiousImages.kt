package app.paroissien.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import app.paroissien.core.data.PersonalCard
import app.paroissien.core.data.UserDatabase
import app.paroissien.core.liturgy.LiturgicalDay
import app.paroissien.core.liturgy.Season
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** Une image pieuse : d'une bibliothèque embarquée (`catalog.json`), ou numérisée par l'utilisateur. */
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
    val personal: Boolean = false,
    val hasBack: Boolean = false,
) {
    @Serializable
    data class Title(val fr: String)
}

@Serializable
private data class Catalog(val format: Int = 1, val cards: List<PiousImage>)

/**
 * La boîte à images (rapport 09, §3.2-3.3). Aucune image n'est fournie d'office : la bibliothèque éventuelle
 * (`assets/images/catalog.json`) est choisie par l'utilisateur, et ses propres images numérisées vivent dans
 * `files/cards/` (recto, verso, vignette), jamais ailleurs. Aucun appel réseau.
 */
class PiousImages(private val context: Context, private val catalog: List<PiousImage>, private val db: UserDatabase, scope: CoroutineScope) {
    private val dir = File(context.filesDir, "cards")
    private val thumbs = LruCache<String, ImageBitmap>(64)
    private val full = LruCache<String, ImageBitmap>(3)

    /** Les images personnelles, les plus récentes d'abord, puis celles de la bibliothèque. */
    val all: StateFlow<List<PiousImage>> = db.personalCards().observeAll()
        .map { personal -> personal.map(::asImage) + catalog }
        .stateIn(scope, SharingStarted.Eagerly, catalog)

    operator fun get(id: String): PiousImage? = all.value.firstOrNull { it.id == id }

    fun thumbnail(id: String): ImageBitmap? = load(thumbs, "t:$id", id, "thumb")

    fun picture(id: String): ImageBitmap? = load(full, "f:$id", id, "front")

    fun back(id: String): ImageBitmap? = load(full, "b:$id", id, "back")

    private fun load(cache: LruCache<String, ImageBitmap>, key: String, id: String, kind: String): ImageBitmap? {
        cache[key]?.let { return it }
        val personal = this[id]?.personal ?: id.startsWith(PERSONAL)
        val bitmap = runCatching {
            if (personal) {
                File(dir, "$id-$kind.webp").takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
            } else {
                val path = when (kind) { "thumb" -> "images/thumbs/$id.webp"; "back" -> return null; else -> "images/$id.webp" }
                context.assets.open(path).use { BitmapFactory.decodeStream(it) }
            }
        }.getOrNull() ?: return null
        return bitmap.asImageBitmap().also { cache.put(key, it) }
    }

    /** Enregistre une image numérisée (déjà redressée et réglée) ; renvoie son identifiant. */
    suspend fun add(front: Bitmap, title: String): String = withContext(Dispatchers.IO) {
        val id = PERSONAL + UUID.randomUUID().toString().take(8)
        dir.mkdirs()
        write(front, File(dir, "$id-front.webp"))
        write(front.scaleToHeight(THUMB_HEIGHT), File(dir, "$id-thumb.webp"))
        db.personalCards().save(PersonalCard(id, title, hasBack = false, createdAt = System.currentTimeMillis()))
        id
    }

    /** Le verso d'une image personnelle : la photo du dos d'une vraie carte, avec sa prière (rapport 09, §3.2.1). */
    suspend fun setBack(id: String, back: Bitmap) = withContext(Dispatchers.IO) {
        val card = this@PiousImages[id] ?: return@withContext
        write(back, File(dir, "$id-back.webp"))
        full.remove("b:$id")
        db.personalCards().save(PersonalCard(id, card.title.fr, hasBack = true, createdAt = System.currentTimeMillis()))
    }

    suspend fun rename(id: String, title: String) = withContext(Dispatchers.IO) {
        val card = this@PiousImages[id] ?: return@withContext
        db.personalCards().save(PersonalCard(id, title, card.hasBack, System.currentTimeMillis()))
    }

    /** Supprime une image personnelle, ses fichiers, et la retire du livre où elle était glissée. */
    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        listOf("front", "back", "thumb").forEach { File(dir, "$id-$it.webp").delete() }
        listOf("t:", "f:", "b:").forEach { (if (it == "t:") thumbs else full).remove(it + id) }
        db.cards().remove(id)
        db.personalCards().delete(id)
    }

    /**
     * L'image du jour : une carte de la fête si la boîte en a une, sinon du temps liturgique, choisie de façon
     * stable pour la journée. Rien si la boîte est vide.
     */
    fun forDay(day: LiturgicalDay): PiousImage? {
        val images = all.value
        if (images.isEmpty()) return null
        val ids = listOf(day.principal.id.value) + day.commemorations.map { it.id.value } + day.optional.map { it.id.value }
        images.firstOrNull { img -> img.feasts.any { it in ids } }?.let { return it }
        val season = when (day.season) {
            Season.ADVENT -> "ADVENT"
            Season.CHRISTMAS -> "CHRISTMAS"
            Season.LENT, Season.SEPTUAGESIMA -> "LENT"
            Season.PASSIONTIDE, Season.PASCHAL_TRIDUUM -> "PASSIONTIDE"
            Season.EASTER -> if (day.principal.id.value.startsWith("pentecost")) "PENTECOST" else "EASTER"
            else -> "ORDINARY"
        }
        val pool = images.filter { season in it.seasons }.ifEmpty { images }
        return pool[day.date.toEpochDay().mod(pool.size)]
    }

    private fun asImage(card: PersonalCard) = PiousImage(
        id = card.id, title = PiousImage.Title(card.title), credit = "", personal = true, hasBack = card.hasBack,
    )

    private fun write(bitmap: Bitmap, file: File) {
        @Suppress("DEPRECATION")
        val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        file.outputStream().use { bitmap.compress(format, QUALITY, it) }
    }

    private fun Bitmap.scaleToHeight(h: Int): Bitmap = if (height <= h) this else scale((width * h.toFloat() / height).toInt().coerceAtLeast(1), h)

    companion object {
        const val PERSONAL = "perso_"
        /** Côté long d'une image enregistrée : assez pour la carte en plein écran (rapport 09, §3.2.3). */
        const val MAX_SIDE = 1440
        private const val THUMB_HEIGHT = 360
        private const val QUALITY = 82
        private val json = Json { ignoreUnknownKeys = true }

        fun load(context: Context, db: UserDatabase, scope: CoroutineScope): PiousImages {
            val text = runCatching { context.assets.open("images/catalog.json").use { it.readBytes().decodeToString() } }.getOrNull()
            val catalog = text?.let { runCatching { json.decodeFromString<Catalog>(it).cards }.getOrNull() }.orEmpty()
            return PiousImages(context, catalog.sortedBy { it.id }, db, scope)
        }
    }
}
