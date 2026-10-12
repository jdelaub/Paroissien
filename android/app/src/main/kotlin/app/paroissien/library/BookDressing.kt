package app.paroissien.library

import androidx.compose.ui.unit.dp
import app.paroissien.core.designsystem.frame.BoxModule
import app.paroissien.core.designsystem.frame.CenterModule
import app.paroissien.core.designsystem.frame.EdgeModule
import app.paroissien.core.designsystem.frame.FrameBands
import app.paroissien.core.designsystem.frame.FrameSpec
import app.paroissien.core.designsystem.frame.FrameStyle
import app.paroissien.core.designsystem.frame.Op
import app.paroissien.core.designsystem.frame.OrnamentalFrameDef
import app.paroissien.core.designsystem.frame.OrnamentalFrames
import app.paroissien.core.designsystem.frame.Rule
import app.paroissien.core.designsystem.frame.RuleFrom
import app.paroissien.core.liturgy.LiturgicalColor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Comment un livre reçoit ses cadres (atelier, demande de l'utilisateur du 12/10/2026). */
enum class FrameMode {
    /** La règle du sommaire : par le sens de chaque section, sinon en boucle (et selon le rang du livre). */
    AUTO,

    /** Un seul cadre pour tout le livre. */
    SINGLE,

    /** Un cadre choisi pour chaque chapitre ; les autres suivent la règle. */
    PER_CHAPTER,

    /** Au hasard parmi une sélection, le même pour un chapitre donné jusqu'au prochain tirage. */
    RANDOM,

    /** Aucun cadre. */
    NONE,
}

/** L'habit d'un livre, tel que l'atelier l'enregistre (table book_look, en JSON). */
@Serializable
data class BookDressChoice(
    val frameMode: FrameMode = FrameMode.AUTO,
    /** [FrameMode.SINGLE] : le cadre. */
    val frame: String? = null,
    /** [FrameMode.PER_CHAPTER] : identifiant d'unité → nom du cadre ; « » pour aucun. */
    val chapterFrames: Map<String, String> = emptyMap(),
    /** [FrameMode.RANDOM] : les cadres du tirage (vide : tous les cadres courants). */
    val pool: List<String> = emptyList(),
    /** [FrameMode.RANDOM] : change à chaque « tirer à nouveau ». */
    val seed: Int = 0,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
        fun decode(text: String?): BookDressChoice? = text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
        fun encode(choice: BookDressChoice): String = json.encodeToString(serializer(), choice)
    }
}

/** Un module d'un cadre de la bibliothèque : [part] = top, side, bottom, topCenter, sideCenter, bottomCenter, corner, bottomCorner. */
@Serializable
data class ModuleRef(val frame: String, val part: String)

/**
 * Un cadre composé par l'utilisateur (table custom_frame, en JSON) : il ne dessine rien lui-même, il assemble des
 * modules de la bibliothèque par leur nom. Les épaisseurs sont en dp.
 */
@Serializable
data class CustomFrameSpec(
    val id: String,
    val name: String,
    val top: ModuleRef? = null,
    val side: ModuleRef? = null,
    val bottom: ModuleRef? = null,
    val corner: ModuleRef? = null,
    /** Médaillon des bandes haute et du pied. */
    val center: ModuleRef? = null,
    val sideCenter: ModuleRef? = null,
    /** pleine, laterale, tete, equerre_tete, equerre_pied, ouverte. */
    val disposition: String = "pleine",
    val depth: Int = 22,
    val bottomDepth: Int = 30,
    /** Cadre dont on reprend le fond des bandes, ou aucun fond. */
    val ground: String? = null,
    /** aucun, cadre (les filets du cadre du fond), rouge (un filet rouge). */
    val fillets: String = "aucun",
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        fun decode(text: String): CustomFrameSpec? = runCatching { json.decodeFromString(serializer(), text) }.getOrNull()
        fun encode(spec: CustomFrameSpec): String = json.encodeToString(serializer(), spec)
    }
}

/**
 * Assemble les cadres composés. Chaque module garde ses encres, préfixées du nom de son cadre d'origine (« matin.gold ») :
 * deux modules de cadres différents ne se volent pas leurs couleurs ; les encres du thème (paper, text, rubric) restent
 * communes.
 */
object FrameComposer {
    private val library by lazy { OrnamentalFrames.associateBy { it.name } }
    private val theme = setOf("paper", "text", "rubric")
    private val empty = EdgeModule(100f, 0f, emptyList(), emptyList())

    val dispositions = mapOf(
        "pleine" to FrameBands.Full,
        "laterale" to FrameBands(top = false, outer = true, bottom = false, inner = false),
        "tete" to FrameBands(top = true, outer = false, bottom = false, inner = false),
        "tete_pied" to FrameBands(top = true, outer = false, bottom = true, inner = false),
        "equerre_tete" to FrameBands(top = true, outer = true, bottom = false, inner = false),
        "equerre_pied" to FrameBands(top = false, outer = true, bottom = true, inner = false),
        "ouverte" to FrameBands(top = true, outer = true, bottom = true, inner = false),
    )

    /** Les modules qu'on peut prendre dans la bibliothèque, par sorte : frises, médaillons, coins. */
    fun modules(kind: String): List<ModuleRef> = OrnamentalFrames.filter { it.name != "doublure" }.flatMap { def ->
        val s = def.spec
        when (kind) {
            "frise" -> listOfNotNull(
                ModuleRef(def.name, "top"),
                ModuleRef(def.name, "side").takeIf { s.side !== s.top },
                ModuleRef(def.name, "bottom").takeIf { s.bottom !== s.top && s.bottom !== s.side },
            ).filter { edge(it)?.base?.isNotEmpty() == true }
            "medaillon" -> listOfNotNull(
                ModuleRef(def.name, "topCenter").takeIf { s.topCenter != null },
                ModuleRef(def.name, "sideCenter").takeIf { s.sideCenter != null && s.sideCenter !== s.topCenter },
                ModuleRef(def.name, "bottomCenter").takeIf { s.bottomCenter != null && s.bottomCenter !== s.topCenter },
            )
            "coin" -> listOfNotNull(
                ModuleRef(def.name, "corner").takeIf { s.corner != null },
                ModuleRef(def.name, "bottomCorner").takeIf { s.bottomCorner != null && s.bottomCorner !== s.corner },
            )
            else -> emptyList()
        }
    }

    fun title(frame: String): String = library[frame]?.title ?: frame

    fun build(c: CustomFrameSpec): OrnamentalFrameDef? {
        val sources = listOfNotNull(c.top, c.side, c.bottom, c.corner, c.center, c.sideCenter).map { it.frame } + listOfNotNull(c.ground)
        if (sources.none { it in library }) return null
        val top = edge(c.top) ?: empty
        val bands = dispositions[c.disposition] ?: FrameBands.Full
        val ground = c.ground?.let { g -> library[g]?.spec?.ground?.let { ink -> "$g.$ink" } }
        val rules = when (c.fillets) {
            "cadre" -> c.ground?.let { g -> library[g]?.spec?.rules?.map { Rule(ink(g, it.ink), it.at, it.width, it.over, it.from) } }.orEmpty()
            "rouge" -> listOf(Rule("rubric", 50f, 7f, over = true, from = RuleFrom.OUTER))
            else -> emptyList()
        }
        val spec = FrameSpec(
            name = c.id,
            title = c.name,
            top = top,
            side = edge(c.side) ?: top,
            bottom = edge(c.bottom) ?: top,
            topCenter = center(c.center),
            sideCenter = center(c.sideCenter),
            bottomCenter = center(c.center),
            corner = box(c.corner),
            bottomCorner = box(c.corner),
            cornerFill = null,
            outerCorner = null,
            outerBottomCorner = null,
            outerMid = null,
            outerBottomMid = null,
            outerSideMid = null,
            bars = null,
            rules = rules,
            ground = ground,
            margin = 12.dp,
            topDepth = c.depth.dp,
            sideDepth = c.depth.dp,
            bottomDepth = c.bottomDepth.dp,
            textPadding = 12.dp,
            bands = bands,
        )
        val used = sources.filter { it in library }.distinct()
        val style = FrameStyle(
            base = used.flatMap { n -> library.getValue(n).style.base.map { (k, v) -> "$n.$k" to v } }.toMap(),
            palettes = LiturgicalColor.entries.associateWith { color ->
                used.flatMap { n -> library.getValue(n).style.palettes[color].orEmpty().map { (k, v) -> "$n.$k" to v } }.toMap()
            }.filterValues { it.isNotEmpty() },
            dimmed = used.flatMap { n -> library.getValue(n).style.dimmed.map { "$n.$it" } }.toSet(),
        )
        return OrnamentalFrameDef(c.id, c.name, spec, style)
    }

    private fun ink(frame: String, ink: String) = if (ink in theme) ink else "$frame.$ink"
    private fun ops(frame: String, list: List<Op>) = list.map { Op(ink(frame, it.ink), it.pathData, it.stroke, it.round) }

    fun edge(r: ModuleRef?): EdgeModule? {
        val s = r?.let { library[it.frame]?.spec } ?: return null
        val m = when (r.part) { "top" -> s.top; "side" -> s.side; "bottom" -> s.bottom; else -> null } ?: return null
        return EdgeModule(m.period, m.phase, ops(r.frame, m.base), ops(r.frame, m.over), m.outset, m.inset, m.mirrored, m.spill)
    }

    private fun center(r: ModuleRef?): CenterModule? {
        val s = r?.let { library[it.frame]?.spec } ?: return null
        val m = when (r.part) { "topCenter" -> s.topCenter; "sideCenter" -> s.sideCenter; "bottomCenter" -> s.bottomCenter; else -> null } ?: return null
        return CenterModule(m.width, ops(r.frame, m.ops), m.outset, m.inset, m.upright, m.above, m.at)
    }

    private fun box(r: ModuleRef?): BoxModule? {
        val s = r?.let { library[it.frame]?.spec } ?: return null
        val m = when (r.part) { "corner" -> s.corner; "bottomCorner" -> s.bottomCorner; else -> null } ?: return null
        return BoxModule(ops(r.frame, m.ops), m.width, m.height, m.outset, m.inset, m.above)
    }

    /** Un cadre d'essai pour la planche des modules : ce module seul, sur un cadre plein. */
    fun sample(r: ModuleRef, kind: String): OrnamentalFrameDef? = build(
        CustomFrameSpec(
            id = "essai", name = title(r.frame),
            top = r.takeIf { kind == "frise" } ?: ModuleRef("filets_rouges", "top"),
            corner = r.takeIf { kind == "coin" },
            center = r.takeIf { kind == "medaillon" },
            sideCenter = r.takeIf { kind == "medaillon" },
            ground = r.frame,
        ),
    )
}
