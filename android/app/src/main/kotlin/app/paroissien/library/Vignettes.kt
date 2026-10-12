package app.paroissien.library

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.paroissien.core.content.UnitInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.Normalizer

/** Une vignette gravée du catalogue : masque d'encre (WebP), titre, et mots de section qui l'appellent. */
@Serializable
data class VignetteInfo(
    val id: String,
    val title: String,
    val file: String,
    val width: Int,
    val height: Int,
    val senses: List<String> = emptyList(),
    val page: Int? = null,
    val source: String = "",
)

@Serializable
private data class VignetteCatalog(val format: Int = 1, val vignettes: List<VignetteInfo> = emptyList())

/**
 * Vignettes gravées tirées des scans de l'utilisateur (ATELIER §6) : elles viennent du dépôt privé et n'existent que
 * dans la saveur personnelle (assets `vignettes/`). Sans catalogue, il n'y en a simplement pas.
 *
 * Règle : seuls les livres nobles en reçoivent, une en tête de chaque section, choisie d'abord par le sens du titre de
 * la section, sinon en boucle dans l'ordre du sommaire, comme les cadres.
 */
class Vignettes(private val context: Context) {

    val catalog: List<VignetteInfo> by lazy {
        runCatching {
            context.assets.open("vignettes/catalog.json").use { stream ->
                json.decodeFromString(VignetteCatalog.serializer(), stream.readBytes().decodeToString()).vignettes
            }
        }.getOrDefault(emptyList())
    }

    fun of(publication: String, entry: LibraryEntry?, unitIndex: Int, unit: UnitInfo?): VignetteInfo? {
        if (entry == null || unit == null || catalog.isEmpty()) return null
        if (BookDresses.of(publication, entry) != Dress.NOBLE) return null
        val words = plain("${unit.id} ${unit.title.orEmpty()}")
        catalog.firstOrNull { v -> v.senses.any { sense -> Regex("(^|\\P{L})" + Regex.escape(plain(sense))).containsMatchIn(words) } }?.let { return it }
        return catalog[unitIndex.mod(catalog.size)]
    }

    /** L'image, décodée hors du fil principal. */
    fun image(vignette: VignetteInfo): ImageBitmap? = runCatching {
        context.assets.open("vignettes/${vignette.file}").use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
    }.getOrNull()

    private fun plain(s: String) = Normalizer.normalize(s.lowercase().replace('-', ' '), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
