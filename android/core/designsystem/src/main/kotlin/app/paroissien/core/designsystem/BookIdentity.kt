package app.paroissien.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Matière de la couverture : maroquin (grain serré), veau (peau lisse), toile fine, couverture de papier ; et, pour
 * l'atelier des couvertures (13/10/2026), chagrin (grain de galets), velours (poil écrasé), vélin (parchemin aux fibres
 * et aux marbrures douces), papier marbré (veines peignées), toile de lin (tissage grossier aux fils inégaux).
 */
enum class CoverMaterial { MOROCCO, CALF, CLOTH, WRAPPER, SHAGREEN, VELVET, VELLUM, MARBLED, LINEN }

/** Décor : dorure au fer (dans le [Metal] de la couverture), ou fer à froid (relief sans or), ou impression (papier). */
enum class Tooling { GOLD, BLIND, PRINTED }

/** Motif central du plat ; les fleurs et les symboles s'ajoutent à la croix, au monogramme et au chrisme (13/10/2026). */
enum class CoverMotif { CROSS, IHS, CHI_RHO, NONE, FLEUR_DE_LYS, CROSS_FLEURY, LILY, ROSE, SACRED_HEART, STAR, CHALICE }

/**
 * Roulette du cadre : une frise poussée au fer entre les filets, comme sur les reliures anciennes : fleurs de lys,
 * palmettes et fleurons de lotus, rinceau de feuillages, dentelle de petits fers, perles.
 */
enum class CoverBorder { NONE, LYS, PALMETTES, RINCEAUX, DENTELLE, PERLES }

/** Fers des angles du cadre : quadrilobe, fleur de lys, écoinçon de rinceaux, carré plein, rosace. */
enum class CoverCorner { NONE, QUADRILOBE, FLEUR_DE_LYS, ECOINCON, CARRE, ROSETTE }

/** Encadrement du motif central : médaillon ovale, losange, cartouche aux angles rentrants, gloire rayonnante. */
enum class CoverCenter { NONE, MEDAILLON, LOSANGE, CARTOUCHE, GLOIRE }

/**
 * Tranche : métallique ([Metal] de la tranche : or, argent, bronze, cuivre), rouge sous or, couleurs (rouge, rouge
 * orangé, bleue, verte, jaune), jaspée (mouchetée de rouge), marbrée, ou naturelle.
 */
enum class EdgeFinish { GILT, RED, PLAIN, RED_UNDER_GOLD, RED_ORANGE, BLUE, GREEN, YELLOW, SPRINKLED, MARBLED }

/** Ciselure d'une tranche dorée : pointillés, losanges, rinceaux poussés au fer dans l'or. */
enum class EdgeTooling { NONE, POINTILLE, LOSANGES, RINCEAUX }

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
    /** Le métal de la dorure : fers, titre et motif. */
    val metal: Metal = Metal.GOLD,
    /** Roulette poussée entre les filets. */
    val border: CoverBorder = CoverBorder.NONE,
    /** Fers des angles ; par défaut le quadrilobe quand [corners] est vrai. */
    val cornerStyle: CoverCorner = if (corners) CoverCorner.QUADRILOBE else CoverCorner.NONE,
    val center: CoverCenter = CoverCenter.NONE,
    /** Dos à caissons : filets dorés de part et d'autre des nerfs et un petit fer au milieu de chaque caisson. */
    val spineTooling: Boolean = false,
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
    /** Le métal d'une tranche dorée ou rouge sous or. */
    val edgeMetal: Metal = Metal.GOLD,
    val edgeTooling: EdgeTooling = EdgeTooling.NONE,
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
