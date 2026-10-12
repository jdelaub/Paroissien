package app.paroissien.library

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paroissien.core.designsystem.BookIdentity
import app.paroissien.core.designsystem.CoverMaterial
import app.paroissien.core.designsystem.CoverMotif
import app.paroissien.core.designsystem.EdgeFinish
import app.paroissien.core.designsystem.Tooling
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
import kotlin.math.abs

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
    /** Couleurs des cadres : null (celles du livre), « jour » (la couleur liturgique du jour) ou une couleur liturgique. */
    val colors: String? = null,
    /** Reliure retouchée dans l'atelier ; null garde celle du livre. */
    val binding: BindingChoice? = null,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
        fun decode(text: String?): BookDressChoice? = text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
        fun encode(choice: BookDressChoice): String = json.encodeToString(serializer(), choice)
    }
}

/**
 * La reliure choisie dans l'atelier (ATELIER §9) : chaque champ null garde la valeur du livre. Les énumérations sont
 * gardées par leur nom ; un nom inconnu (version future) est ignoré.
 */
@Serializable
data class BindingChoice(
    /** Couleur de la peau ou de la toile (ARGB). */
    val leather: Long? = null,
    val material: String? = null,
    val tooling: String? = null,
    val motif: String? = null,
    val edge: String? = null,
    /** « soie » (doublure), le nom d'un papier à motif, ou « aucune ». */
    val endpaper: String? = null,
    /** Les rubans, par emplacement de `RibbonStyle.slots`, dans l'ordre. */
    val ribbons: List<Int>? = null,
) {
    val isEmpty: Boolean get() = this == BindingChoice()

    companion object {
        const val SILK = "soie"
        const val NO_ENDPAPER = "aucune"

        /** Les peaux et toiles proposées : celles des livres de la bibliothèque, puis quelques classiques. */
        val leathers: List<Long> = listOf(
            0xFF16130F, 0xFF241416, 0xFF2B1A14, 0xFF5C3A22, 0xFF5E1519, 0xFF7A2A1E,
            0xFF1E3A2B, 0xFF1C2B40, 0xFF3B2347, 0xFF1D1D20, 0xFF6B5A3A, 0xFFEDE3CB,
        )
    }
}

private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? = name?.let { n -> enumValues<E>().firstOrNull { it.name == n } }

/** Le livre tel que l'atelier l'a relié : la reliure d'origine, retouchée champ par champ. */
fun BookIdentity.dressed(binding: BindingChoice?): BookIdentity {
    if (binding == null || binding.isEmpty) return this
    val material = enumOrNull<CoverMaterial>(binding.material) ?: cover.material
    val tooling = enumOrNull<Tooling>(binding.tooling) ?: cover.tooling
    return copy(
        cover = cover.copy(
            leather = binding.leather?.let { Color(it) } ?: cover.leather,
            material = material,
            tooling = tooling,
            motifTooling = if (binding.tooling != null) tooling else cover.motifTooling,
            motif = enumOrNull<CoverMotif>(binding.motif) ?: cover.motif,
        ),
        edges = enumOrNull<EdgeFinish>(binding.edge) ?: edges,
        ribbons = binding.ribbons?.takeIf { it.isNotEmpty() } ?: ribbons,
    )
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
    /** Couleurs du cadre : celles des cadres d'origine (null) ou leur variante pour une couleur liturgique. */
    val palette: String? = null,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        fun decode(text: String): CustomFrameSpec? = runCatching { json.decodeFromString(serializer(), text) }.getOrNull()
        fun encode(spec: CustomFrameSpec): String = json.encodeToString(serializer(), spec)
    }
}

/** Un trait d'un motif : ses points en unités de frise (x le long de la bande, y de 0 au bord à 100 côté texte). */
@Serializable
data class MotifStroke(val points: List<Float>, val width: Float, val color: Long, val dot: Boolean = false)

/**
 * Un motif dessiné par l'utilisateur : une cellule de frise de [period] × 100 unités, ses traits (lissés en courbes à
 * l'affichage), répétés en miroir selon [mirrorU] (de part et d'autre du milieu de la cellule) et [mirrorV] (du bord
 * extérieur vers le texte). Il devient une frise qu'on pose dans un cadre composé comme celles de la bibliothèque.
 */
@Serializable
data class CustomMotif(
    val id: String,
    val name: String,
    val period: Float = 160f,
    val strokes: List<MotifStroke> = emptyList(),
    val mirrorU: Boolean = true,
    val mirrorV: Boolean = false,
    /** Fond de la bande (ARGB), ou papier nu. */
    val ground: Long? = null,
) {
    companion object {
        const val PREFIX = "motif-"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        fun decode(text: String): CustomMotif? = runCatching { json.decodeFromString(serializer(), text) }.getOrNull()
        fun encode(motif: CustomMotif): String = json.encodeToString(serializer(), motif)
    }
}

/**
 * Assemble les cadres composés. Chaque module garde ses encres, préfixées du nom de son cadre d'origine (« matin.gold ») :
 * deux modules de cadres différents ne se volent pas leurs couleurs ; les encres du thème (paper, text, rubric) restent
 * communes.
 */
object FrameComposer {
    private val library by lazy { OrnamentalFrames.associateBy { it.name } }

    /** Les motifs de l'utilisateur, tenus à jour par l'application (table custom_frame, identifiants « motif-… »). */
    @Volatile
    var motifs: Map<String, CustomMotif> = emptyMap()
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
    fun modules(kind: String): List<ModuleRef> = (if (kind == "frise") motifs.keys.map { ModuleRef(it, "top") } else emptyList()) +
        OrnamentalFrames.filter { it.name != "doublure" }.flatMap { def ->
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

    fun title(frame: String): String = library[frame]?.title ?: motifs[frame]?.name ?: frame

    fun build(c: CustomFrameSpec): OrnamentalFrameDef? {
        val sources = listOfNotNull(c.top, c.side, c.bottom, c.corner, c.center, c.sideCenter).map { it.frame } + listOfNotNull(c.ground)
        if (sources.none { it in library || it in motifs }) return null
        val top = edge(c.top) ?: empty
        val bands = dispositions[c.disposition] ?: FrameBands.Full
        val ground = c.ground?.let { g -> library[g]?.spec?.ground?.let { ink -> "$g.$ink" } ?: motifs[g]?.ground?.let { "$g.ground" } }
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
        val chosen = c.palette?.let { name -> LiturgicalColor.entries.firstOrNull { it.name == name } }
        val style = FrameStyle(
            // Couleurs du livre, ou, si le cadre composé en a choisi une, sa variante liturgique ; puis celles des motifs.
            base = used.flatMap { n ->
                val st = library.getValue(n).style
                (st.base + (chosen?.let { st.palettes[it] }.orEmpty())).map { (k, v) -> "$n.$k" to v }
            }.toMap() + sources.mapNotNull { motifs[it] }.distinct().flatMap { m -> motifInks(m).toList() },
            palettes = LiturgicalColor.entries.associateWith { color ->
                used.flatMap { n -> library.getValue(n).style.palettes[color].orEmpty().map { (k, v) -> "$n.$k" to v } }.toMap()
            }.filterValues { it.isNotEmpty() },
            dimmed = used.flatMap { n -> library.getValue(n).style.dimmed.map { "$n.$it" } }.toSet() +
                sources.mapNotNull { motifs[it] }.flatMap { motifInks(it).keys },
        )
        return OrnamentalFrameDef(c.id, c.name, spec, style)
    }

    /** Les encres d'un motif : une par couleur de trait, plus son fond. */
    private fun motifInks(m: CustomMotif): Map<String, Color> =
        m.strokes.map { it.color }.distinct().mapIndexed { i, argb -> "${m.id}.c$i" to Color(argb) }.toMap() +
            (m.ground?.let { mapOf("${m.id}.ground" to Color(it)) } ?: emptyMap())

    /** La frise d'un motif : chaque trait lissé (Catmull-Rom), et ses reflets selon la symétrie choisie. */
    private fun motifEdge(m: CustomMotif): EdgeModule {
        val colors = m.strokes.map { it.color }.distinct()
        val ops = m.strokes.flatMap { st ->
            val ink = "${m.id}.c${colors.indexOf(st.color)}"
            val xs = st.points.filterIndexed { i, _ -> i % 2 == 0 }
            val ys = st.points.filterIndexed { i, _ -> i % 2 == 1 }
            val variants = buildList {
                add(xs to ys)
                if (m.mirrorU) add(xs.map { m.period - it } to ys)
                if (m.mirrorV) add(xs to ys.map { 100f - it })
                if (m.mirrorU && m.mirrorV) add(xs.map { m.period - it } to ys.map { 100f - it })
            }
            variants.map { (vx, vy) -> Op(ink, smoothPath(vx, vy), st.width, round = true) }
        }
        return EdgeModule(m.period, 0f, ops, emptyList())
    }

    /** Une courbe lisse qui passe par tous les points (Catmull-Rom en cubiques) ; un point seul fait un rond. */
    fun smoothPath(xs: List<Float>, ys: List<Float>): String {
        if (xs.isEmpty()) return ""
        fun f(v: Float) = "%.1f".format(java.util.Locale.ROOT, v)
        if (xs.size == 1) return "M${f(xs[0])} ${f(ys[0])}L${f(xs[0])} ${f(ys[0])}"
        val sb = StringBuilder("M${f(xs[0])} ${f(ys[0])}")
        val n = xs.size
        for (i in 0 until n - 1) {
            val i0 = maxOf(0, i - 1)
            val i3 = minOf(n - 1, i + 2)
            val c1x = xs[i] + (xs[i + 1] - xs[i0]) / 6f
            val c1y = ys[i] + (ys[i + 1] - ys[i0]) / 6f
            val c2x = xs[i + 1] - (xs[i3] - xs[i]) / 6f
            val c2y = ys[i + 1] - (ys[i3] - ys[i]) / 6f
            sb.append("C${f(c1x)} ${f(c1y)} ${f(c2x)} ${f(c2y)} ${f(xs[i + 1])} ${f(ys[i + 1])}")
        }
        return sb.toString()
    }

    /**
     * Un cadre composé au hasard, mais d'un même esprit : un cadre principal donne le fond, les coins et le haut ; un
     * second, les côtés ou le pied ; un médaillon une fois sur deux ; disposition le plus souvent pleine.
     */
    fun random(seed: Long, id: String, name: String): CustomFrameSpec {
        val rnd = kotlin.random.Random(seed)
        val frames = OrnamentalFrames.map { it.name }.filter { it != "doublure" && it != "evangeliste" }
        val main = frames.random(rnd)
        val second = frames.random(rnd)
        val friezes = modules("frise")
        fun frieze(of: String) = friezes.filter { it.frame == of }.randomOrNull(rnd) ?: friezes.random(rnd)
        val corners = modules("coin")
        val centers = modules("medaillon")
        return CustomFrameSpec(
            id = id, name = name,
            top = frieze(main),
            side = if (rnd.nextBoolean()) frieze(second) else null,
            bottom = if (rnd.nextInt(3) == 0) frieze(second) else null,
            corner = corners.filter { it.frame == main }.randomOrNull(rnd) ?: corners.randomOrNull(rnd)?.takeIf { rnd.nextBoolean() },
            center = centers.takeIf { rnd.nextBoolean() }?.randomOrNull(rnd),
            disposition = if (rnd.nextInt(5) == 0) dispositions.keys.random(rnd) else "pleine",
            depth = listOf(18, 20, 22, 24, 26).random(rnd),
            bottomDepth = listOf(24, 28, 32, 36).random(rnd),
            ground = main.takeIf { library[main]?.spec?.ground != null },
            fillets = listOf("aucun", "cadre", "cadre", "rouge").random(rnd),
            palette = if (rnd.nextInt(3) == 0) LiturgicalColor.entries.random(rnd).name else null,
        )
    }

    private fun ink(frame: String, ink: String) = if (ink in theme) ink else "$frame.$ink"
    private fun ops(frame: String, list: List<Op>) = list.map { Op(ink(frame, it.ink), it.pathData, it.stroke, it.round) }

    fun edge(r: ModuleRef?): EdgeModule? {
        r?.let { motifs[it.frame] }?.let { return motifEdge(it) }
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

    /** L'aperçu d'un motif en cours de dessin : un cadre plein fait de ce seul motif, sans l'enregistrer. */
    fun preview(m: CustomMotif): OrnamentalFrameDef {
        val edge = motifEdge(m)
        val spec = FrameSpec(
            name = m.id, title = m.name, top = edge, side = edge, bottom = edge,
            topCenter = null, sideCenter = null, bottomCenter = null, corner = null, bottomCorner = null, cornerFill = null,
            outerCorner = null, outerBottomCorner = null, outerMid = null, outerBottomMid = null, outerSideMid = null,
            bars = null, rules = emptyList(), ground = m.ground?.let { "${m.id}.ground" },
            margin = 12.dp, topDepth = 22.dp, sideDepth = 22.dp, bottomDepth = 22.dp, textPadding = 12.dp,
        )
        return OrnamentalFrameDef(m.id, m.name, spec, FrameStyle(motifInks(m), emptyMap(), motifInks(m).keys))
    }

    /**
     * L'éventail des couleurs des cadres : toutes les encres et toutes leurs variantes liturgiques, sans doublon proche,
     * rangées par teinte puis par clarté (les gris et les bruns à la fin). Un motif de l'utilisateur y prend ses couleurs :
     * il reste dans l'esprit des cadres déjà là.
     */
    val fan: List<Color> by lazy {
        val all = OrnamentalFrames.flatMap { def -> def.style.base.values + def.style.palettes.values.flatMap { it.values } }
        val kept = ArrayList<Color>()
        for (c in all) {
            if (kept.none { k -> abs(k.red - c.red) + abs(k.green - c.green) + abs(k.blue - c.blue) < 0.09f }) kept += c
        }
        fun hsv(c: Color): Triple<Float, Float, Float> {
            val hsv = FloatArray(3)
            android.graphics.Color.RGBToHSV((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt(), hsv)
            return Triple(hsv[0], hsv[1], hsv[2])
        }
        kept.sortedWith(compareBy({ if (hsv(it).second < 0.18f) 1 else 0 }, { (hsv(it).first / 30f).toInt() }, { -hsv(it).third }))
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
