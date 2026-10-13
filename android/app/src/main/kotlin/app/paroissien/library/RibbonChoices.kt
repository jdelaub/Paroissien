package app.paroissien.library

import androidx.compose.ui.graphics.Color
import app.paroissien.core.designsystem.RibbonEmblem
import app.paroissien.core.designsystem.RibbonEnd
import app.paroissien.core.designsystem.RibbonFabric
import app.paroissien.core.designsystem.RibbonStyle
import app.paroissien.core.liturgy.LiturgicalColor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Un ruban retouché dans l'atelier (demande du 13/10/2026) : chaque champ null garde le ruban liturgique de son
 * emplacement (rouge, vert, violet, blanc, rose, noir). Les énumérations sont gardées par leur nom ; un nom inconnu
 * (version future) est ignoré.
 */
@Serializable
data class RibbonChoice(
    /** Teinte libre (ARGB) ; null : la couleur liturgique de l'emplacement. */
    val argb: Long? = null,
    /** Le nom du ruban (« bleu marial »), lu par TalkBack et écrit dans le panneau des rubans. */
    val label: String? = null,
    val fabric: String? = null,
    val end: String? = null,
    val emblem: String? = null,
) {
    val isEmpty: Boolean get() = this == RibbonChoice()
}

/** Le jeu de rubans de l'utilisateur, tel que les réglages le gardent (JSON, un par emplacement). */
object RibbonChoices {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    /** Les teintes proposées en plus des couleurs liturgiques : bleu marial, or, bordeaux, vert sapin, gris perle… */
    val palette: List<Long> = listOf(
        0xFF2F5D9E, 0xFF7FA7D9, 0xFFC9A227, 0xFF6E1423, 0xFF1F4D3A, 0xFFB7B2A8,
        0xFF8C5A2B, 0xFFE8C7C8, 0xFF3C2A4D, 0xFFF1E6C8, 0xFF0F2A44, 0xFFA65E2E,
    )

    fun decode(text: String): List<RibbonChoice> =
        if (text.isBlank()) emptyList() else runCatching { json.decodeFromString<List<RibbonChoice>>(text) }.getOrDefault(emptyList())

    fun encode(choices: List<RibbonChoice>): String =
        if (choices.all { it.isEmpty }) "" else json.encodeToString(choices.dropLastWhile { it.isEmpty })

    /** Les six rubans à dessiner : ceux de l'atelier là où il y en a, les rubans liturgiques ailleurs. */
    fun styles(text: String): List<RibbonStyle> {
        val choices = decode(text)
        return RibbonStyle.slots.mapIndexed { slot, base -> choices.getOrNull(slot)?.let { base.with(it) } ?: base }
    }
}

private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? = name?.let { n -> enumValues<E>().firstOrNull { it.name == n } }

/** Le ruban liturgique d'un emplacement, retouché. */
fun RibbonStyle.with(choice: RibbonChoice): RibbonStyle = copy(
    label = choice.label?.takeIf { it.isNotBlank() } ?: label,
    custom = choice.argb?.let { Color(it) },
    fabric = enumOrNull<RibbonFabric>(choice.fabric),
    end = enumOrNull<RibbonEnd>(choice.end) ?: end,
    emblem = enumOrNull<RibbonEmblem>(choice.emblem) ?: emblem,
)

/** Le nom français d'une couleur liturgique, pour les rubans. */
fun LiturgicalColor.ribbonName(): String = when (this) {
    LiturgicalColor.RED -> "rouge"
    LiturgicalColor.GREEN -> "vert"
    LiturgicalColor.VIOLET -> "violet"
    LiturgicalColor.WHITE -> "blanc"
    LiturgicalColor.ROSE -> "rose"
    LiturgicalColor.BLACK -> "noir"
    LiturgicalColor.GOLD -> "or"
}
