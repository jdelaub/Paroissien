package app.paroissien.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import app.paroissien.core.data.CardCategory
import app.paroissien.core.data.CardInCategory
import app.paroissien.core.data.PersonalCard
import app.paroissien.core.data.UserDatabase
import app.paroissien.core.liturgy.LiturgicalDay
import app.paroissien.core.liturgy.Season
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
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
    /** Catégories de la bibliothèque (identifiants de [CardCategories]) ; l'utilisateur en ajoute ou en retire. */
    val categories: List<String> = emptyList(),
    /** Taille réelle de la carte en millimètres (largeur, hauteur), relevée sur des photos à la même échelle. */
    val sizeMm: List<Int>? = null,
    /** Écriture manuscrite (dédicace, date), transcrite. */
    val inscription: String = "",
    /** Personnes nommées : baptisé, communiant, prêtre ordonné, défunt. */
    val people: List<String> = emptyList(),
    val place: String = "",
    val feastNames: List<String> = emptyList(),
    val notes: String = "",
    /** Photographiée sur une vraie carte (tools/images/cartes.py), et non reproduction d'une œuvre. */
    val photographed: Boolean = false,
) {
    @Serializable
    data class Title(val fr: String)

    /** Une vraie carte (photographiée ou numérisée), avec sa silhouette : à montrer telle quelle, coins arrondis. */
    val physical: Boolean get() = personal || photographed || sizeMm != null
}

/** Une catégorie définie par la bibliothèque (son nom à l'écran). */
@Serializable
data class CategoryLabel(val id: String, val label: String)

@Serializable
internal data class Catalog(val format: Int = 1, val categories: List<CategoryLabel> = emptyList(), val cards: List<PiousImage>)

/** Une catégorie à l'écran : prédéfinie, venue de la bibliothèque, ou créée par l'utilisateur ([custom]). */
data class CardCategoryInfo(val id: String, val label: String, val custom: Boolean)

/** Catégories prédéfinies, par occasion ou par usage (demande de l'utilisateur, 12/10/2026). */
object CardCategories {
    val predefined = listOf(
        "bapteme" to "Baptême",
        "premiere-communion" to "Première communion",
        "communion-solennelle" to "Communion solennelle",
        "confirmation" to "Confirmation",
        "ordination" to "Ordination",
        "vie-religieuse" to "Vie religieuse",
        "mariage" to "Mariage",
        "memento" to "Mementos",
        "priere" to "Prières",
        "pelerinage" to "Pèlerinages",
        "fete" to "Fêtes",
        "art" to "Œuvres d'art",
        "saints" to "Saints",
    )

    /** Souvenirs de famille : jamais proposés comme image du jour. */
    val family = setOf("bapteme", "premiere-communion", "communion-solennelle", "confirmation", "ordination", "vie-religieuse", "mariage", "memento")
}

/**
 * La boîte à images (rapport 09, §3.2-3.3). Aucune image n'est fournie d'office : la bibliothèque éventuelle
 * (`assets/images/catalog.json`) est choisie par l'utilisateur, et ses propres images numérisées vivent dans
 * `files/cards/` (recto, verso, vignette), jamais ailleurs. Aucun appel réseau.
 */
class PiousImages(
    private val context: Context,
    private val source: LibrarySource,
    private val db: UserDatabase,
    scope: CoroutineScope,
) {
    private val dir = File(context.filesDir, "cards")
    private val thumbs = LruCache<String, ImageBitmap>(64)
    private val full = LruCache<String, ImageBitmap>(3)
    private val library = MutableStateFlow(source.catalog())

    /** Les images personnelles, les plus récentes d'abord, puis celles de la bibliothèque, sauf celles qu'on a masquées. */
    val all: StateFlow<List<PiousImage>> = combine(db.personalCards().observeAll(), library, db.categories().observeAssignments()) { personal, lib, marks ->
        val hidden = marks.filter { it.category == HIDDEN && !it.removed }.map { it.card }.toSet()
        (personal.map(::asImage) + lib.cards).filter { it.id !in hidden }
    }.stateIn(scope, SharingStarted.Eagerly, library.value.cards)

    /** Les images de la bibliothèque qu'on a masquées : elles se rendent d'un toucher. */
    val hidden: StateFlow<List<PiousImage>> = combine(library, db.categories().observeAssignments()) { lib, marks ->
        val ids = marks.filter { it.category == HIDDEN && !it.removed }.map { it.card }.toSet()
        lib.cards.filter { it.id in ids }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /**
     * Retire des images de la boîte : une image personnelle est supprimée (fichiers et description) ; une image de la
     * bibliothèque, embarquée dans l'app, est masquée et se rend depuis « Images masquées ».
     */
    suspend fun remove(ids: Collection<String>) = withContext(Dispatchers.IO) {
        val personal = db.personalCards().all().map { it.id }.toSet()
        for (id in ids) {
            if (id in personal) delete(id) else db.categories().assign(CardInCategory(id, HIDDEN, removed = false, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun restore(id: String) = withContext(Dispatchers.IO) {
        db.categories().assign(CardInCategory(id, HIDDEN, removed = true, updatedAt = System.currentTimeMillis()))
    }

    /** Toutes les catégories : prédéfinies, celles de la bibliothèque, puis celles de l'utilisateur. */
    val categories: StateFlow<List<CardCategoryInfo>> = combine(db.categories().observeCategories(), library) { custom, lib ->
        val fixed = (CardCategories.predefined + lib.categories.map { it.id to it.label })
            .distinctBy { it.first }.map { (id, label) -> CardCategoryInfo(id, label, custom = false) }
        fixed + custom.map { CardCategoryInfo(it.id, it.label, custom = true) }
    }.stateIn(scope, SharingStarted.Eagerly, CardCategories.predefined.map { (id, label) -> CardCategoryInfo(id, label, custom = false) })

    /** Après une restauration : la bibliothèque est relue. */
    fun reloadLibrary() {
        thumbs.evictAll()
        full.evictAll()
        library.value = source.catalog()
    }

    /** Catégories de chaque image : celles de la bibliothèque, plus celles que l'utilisateur a ajoutées, moins celles qu'il a retirées. */
    val categoriesOf: StateFlow<Map<String, Set<String>>> = combine(all, db.categories().observeAssignments()) { images, assignments ->
        val changes = assignments.groupBy { it.card }
        images.associate { image ->
            val base = image.categories.toMutableSet()
            changes[image.id]?.forEach { if (it.removed) base -= it.category else base += it.category }
            image.id to base.toSet()
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    suspend fun setCategory(card: String, category: String, member: Boolean) = withContext(Dispatchers.IO) {
        db.categories().assign(CardInCategory(card, category, removed = !member, updatedAt = System.currentTimeMillis()))
    }

    /** Crée une catégorie de l'utilisateur ; renvoie son identifiant. */
    suspend fun createCategory(label: String): String = withContext(Dispatchers.IO) {
        val id = "perso-" + UUID.randomUUID().toString().take(8)
        db.categories().saveCategory(CardCategory(id, label.trim(), System.currentTimeMillis()))
        id
    }

    suspend fun deleteCategory(id: String) = withContext(Dispatchers.IO) {
        db.categories().clearCategory(id)
        db.categories().deleteCategory(id)
    }

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
                val path = when (kind) { "thumb" -> "thumbs/$id.webp"; "back" -> "$id-verso.webp"; else -> "$id.webp" }
                source.open(path).use { BitmapFactory.decodeStream(it) }
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
        // Les souvenirs de famille (mementos, baptêmes, communions…) restent dans la boîte : pas d'image du jour.
        val images = all.value.filter { image -> image.categories.none { it in CardCategories.family } }
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

    /**
     * D'où vient la bibliothèque d'images : les assets `images/` de l'APK (saveur personnelle), sinon
     * `files/bibliotheque-images/`, où une sauvegarde l'a restaurée (app livrée vide).
     */
    class LibrarySource(private val context: Context) {
        val importedDir: File get() = File(context.filesDir, "bibliotheque-images")
        private val fromAssets: Boolean by lazy { runCatching { context.assets.open("images/catalog.json").close() }.isSuccess }

        fun exists(): Boolean = fromAssets || File(importedDir, "catalog.json").isFile

        fun open(path: String): InputStream = if (fromAssets) context.assets.open("images/$path") else File(importedDir, path).inputStream()

        internal fun catalog(): Catalog = runCatching {
            open("catalog.json").use { json.decodeFromString<Catalog>(it.readBytes().decodeToString()) }
        }.getOrNull()?.let { it.copy(cards = it.cards.sortedBy { c -> c.id }) } ?: Catalog(cards = emptyList())

        /** Les fichiers de la bibliothèque, pour la sauvegarde : catalogue, faces, vignettes. */
        fun files(): List<String> {
            if (!exists()) return emptyList()
            return listOf("catalog.json") + catalog().cards.flatMap { c -> listOfNotNull("${c.id}.webp", "thumbs/${c.id}.webp", "${c.id}-verso.webp".takeIf { c.hasBack }) }
        }
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
        /** Pseudo-catégorie des images de la bibliothèque qu'on a retirées de la boîte. */
        const val HIDDEN = "masquee"
        /** Côté long d'une image enregistrée : assez pour la carte en plein écran (rapport 09, §3.2.3). */
        const val MAX_SIDE = 1440
        private const val THUMB_HEIGHT = 360
        private const val QUALITY = 82
        private val json = Json { ignoreUnknownKeys = true }

        fun load(context: Context, db: UserDatabase, scope: CoroutineScope): PiousImages = PiousImages(context, LibrarySource(context), db, scope)
    }
}
