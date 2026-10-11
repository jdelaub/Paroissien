package app.paroissien.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Matière de la couverture. */
enum class CoverMaterial { MOROCCO, CALF, CLOTH, WRAPPER }

/** Décor : dorure au fer, ou fer à froid (relief sans or), ou impression (couverture de papier). */
enum class Tooling { GOLD, BLIND, PRINTED }

enum class CoverMotif { CROSS, IHS, CHI_RHO, NONE }

/** Tranche : dorée (missels, bibles), rouge (livres de piété), ou naturelle. */
enum class EdgeFinish { GILT, RED, PLAIN }

/** Grammage : un gros livre a des pages très fines (papier bible), un livret des pages épaisses. */
enum class PaperWeight(val stepDp: Float, val foldRadius: Float) {
    BIBLE(0.7f, 0.18f),
    FINE(1.0f, 0.22f),
    REGULAR(1.4f, 0.25f),
    THICK(2.0f, 0.30f),
}

@Immutable
data class CoverStyle(
    val leather: Color,
    val material: CoverMaterial,
    val tooling: Tooling,
    val motif: CoverMotif,
    /** Filets du cadre (0 à 3). */
    val fillets: Int,
    val corners: Boolean,
    /** Nerfs au dos : le relief des coutures d'un livre relié à l'ancienne. */
    val bands: Int,
    /** Couleur des lettres et du décor imprimé, pour une couverture de papier. */
    val ink: Color = Color(0xFF7A1E1A),
    /** Le fer du motif central, qui peut différer du cadre (un IHS doré sur une toile poussée à froid). */
    val motifTooling: Tooling = tooling,
)

@Immutable
data class PaperStock(
    val tint: Color,
    val ink: Color,
    val inkSecondary: Color,
    val rubric: Color,
    val weight: PaperWeight,
) {
    /** Les couleurs de page de ce papier, à partir du thème clair : surface, filets et surlignage suivent la teinte. */
    fun colors(base: PaperColors): PaperColors = base.copy(
        background = tint,
        surface = lerp(tint, Color.White, 0.45f),
        inkPrimary = ink,
        inkSecondary = inkSecondary,
        inkTertiary = lerp(inkSecondary, tint, 0.2f),
        rubric = rubric,
        hairline = lerp(tint, ink, 0.14f),
    )
}

/**
 * L'identité d'un livre (demande de l'utilisateur, 11/10/2026) : sa reliure, son papier, sa tranche, ses signets.
 * Une bible épaisse a une reliure de maroquin dorée, des pages fines et ivoire, une tranche dorée et quatre rubans ;
 * des exercices spirituels, une toile sobre, un papier blanc et une encre noire. [importance] règle la richesse du
 * décor et le poids apparent ; [pages] l'épaisseur du livre.
 */
@Immutable
data class BookIdentity(
    val cover: CoverStyle,
    val paper: PaperStock,
    val edges: EdgeFinish,
    /** Emplacements de rubans cousus dans ce livre (indices de [RibbonStyle.slots]). */
    val ribbons: List<Int>,
    val importance: Int,
    val pages: Int,
) {
    companion object {
        /** Tous les livres pareils : reliure de veau, papier du thème, tranche naturelle, six rubans. */
        fun uniform(pages: Int) = BookIdentity(
            cover = CoverStyle(Color(0xFF4A3020), CoverMaterial.CALF, Tooling.GOLD, CoverMotif.CROSS, fillets = 1, corners = false, bands = 4),
            paper = PaperStock(Color(0xFFFBF8F1), Color(0xFF1F1B16), Color(0xFF5E554A), Color(0xFFA31F1A), PaperWeight.REGULAR),
            edges = EdgeFinish.PLAIN,
            ribbons = RibbonStyle.slots.indices.toList(),
            importance = 1,
            pages = pages,
        )
    }
}

internal fun lerp(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)
