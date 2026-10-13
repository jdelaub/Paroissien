package app.paroissien.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.paroissien.R
import app.paroissien.core.designsystem.BookEdgeSample
import app.paroissien.core.designsystem.BookIdentity
import app.paroissien.core.designsystem.CoverBorder
import app.paroissien.core.designsystem.CoverCenter
import app.paroissien.core.designsystem.CoverCorner
import app.paroissien.core.designsystem.CoverMotif
import app.paroissien.core.designsystem.EdgeFinish
import app.paroissien.core.designsystem.EdgeTooling
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.Metal
import app.paroissien.core.designsystem.Tooling
import app.paroissien.library.BindingChoice

/**
 * Les choix d'une reliure, communs à l'atelier des couvertures et à « Habiller ce livre » (13/10/2026) : le plat
 * (matière, fers et leur métal, motif et son encadrement, roulette, filets, fers d'angle), le dos (nerfs, caissons) et
 * la tranche (teinte, métal, ciselure, sur un aperçu en grand). La couleur de la peau reste à l'écran qui l'emploie.
 */
@Composable
internal fun BindingFields(binding: BindingChoice, identity: BookIdentity, onChange: (BindingChoice) -> Unit) {
    val cover = identity.cover
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Heading(R.string.binding_plate)
        Options(R.string.binding_material, MATERIALS, cover.material) { onChange(binding.copy(material = it.name)) }
        Options(R.string.binding_tooling, TOOLINGS, cover.tooling) { onChange(binding.copy(tooling = it.name)) }
        if (cover.tooling == Tooling.GOLD || cover.motifTooling == Tooling.GOLD) {
            Options(R.string.binding_metal, METALS, cover.metal) { onChange(binding.copy(metal = it.name)) }
        }
        Options(R.string.binding_motif, MOTIFS, cover.motif) { onChange(binding.copy(motif = it.name)) }
        if (cover.motif != CoverMotif.NONE) Options(R.string.binding_center, CENTERS, cover.center) { onChange(binding.copy(center = it.name)) }
        Options(R.string.binding_border, BORDERS, cover.border) { onChange(binding.copy(border = it.name)) }
        Label(R.string.cover_editor_fillets)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..3).forEach { n -> FilterChip(cover.fillets == n, { onChange(binding.copy(fillets = n)) }, { Text("$n") }) }
        }
        Options(R.string.binding_corner, CORNERS, cover.cornerStyle) { onChange(binding.copy(cornerStyle = it.name, corners = it != CoverCorner.NONE)) }

        Heading(R.string.binding_spine)
        Label(R.string.cover_editor_bands)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..6).forEach { n -> FilterChip(cover.bands == n, { onChange(binding.copy(bands = n)) }, { Text("$n") }) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.binding_spine_tooling), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(cover.spineTooling, { onChange(binding.copy(spineTooling = it)) })
        }

        Heading(R.string.binding_edges)
        BookEdgeSample(identity, Modifier.fillMaxWidth().height(54.dp))
        Options(R.string.binding_edge_finish, EDGES, identity.edges) { onChange(binding.copy(edge = it.name)) }
        val gilt = identity.edges == EdgeFinish.GILT || identity.edges == EdgeFinish.RED_UNDER_GOLD
        if (gilt) {
            Options(R.string.binding_edge_metal, METALS, identity.edgeMetal) { onChange(binding.copy(edgeMetal = it.name)) }
            Options(R.string.binding_edge_tooling, EDGE_TOOLINGS, identity.edgeTooling) { onChange(binding.copy(edgeTooling = it.name)) }
        }
    }
}

@Composable
private fun Heading(@StringRes id: Int) =
    Text(stringResource(id), style = MaterialTheme.typography.titleSmall, color = LocalPaperColors.current.rubric)

@Composable
private fun Label(@StringRes id: Int) = Text(stringResource(id), style = MaterialTheme.typography.bodyMedium)

@Composable
private fun <E> Options(@StringRes title: Int, values: List<Pair<E, Int>>, selected: E, onPick: (E) -> Unit) {
    Label(title)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { (v, label) -> FilterChip(v == selected, { onPick(v) }, { Text(stringResource(label)) }) }
    }
}

internal val TOOLINGS = listOf(Tooling.GOLD to R.string.binding_gold, Tooling.BLIND to R.string.binding_blind, Tooling.PRINTED to R.string.binding_printed)

internal val METALS = listOf(Metal.GOLD to R.string.metal_gold, Metal.SILVER to R.string.metal_silver, Metal.BRONZE to R.string.metal_bronze, Metal.COPPER to R.string.metal_copper)

internal val MOTIFS = listOf(
    CoverMotif.CROSS to R.string.binding_cross,
    CoverMotif.CROSS_FLEURY to R.string.binding_cross_fleury,
    CoverMotif.IHS to R.string.binding_ihs,
    CoverMotif.CHI_RHO to R.string.binding_chi_rho,
    CoverMotif.FLEUR_DE_LYS to R.string.binding_fleur_de_lys,
    CoverMotif.LILY to R.string.binding_lily,
    CoverMotif.ROSE to R.string.binding_rose,
    CoverMotif.SACRED_HEART to R.string.binding_sacred_heart,
    CoverMotif.STAR to R.string.binding_star,
    CoverMotif.CHALICE to R.string.binding_chalice,
    CoverMotif.NONE to R.string.binding_none,
)

private val CENTERS = listOf(
    CoverCenter.NONE to R.string.binding_none,
    CoverCenter.MEDAILLON to R.string.center_medaillon,
    CoverCenter.LOSANGE to R.string.center_losange,
    CoverCenter.CARTOUCHE to R.string.center_cartouche,
    CoverCenter.GLOIRE to R.string.center_gloire,
)

private val BORDERS = listOf(
    CoverBorder.NONE to R.string.binding_none_f,
    CoverBorder.LYS to R.string.border_lys,
    CoverBorder.PALMETTES to R.string.border_palmettes,
    CoverBorder.RINCEAUX to R.string.border_rinceaux,
    CoverBorder.DENTELLE to R.string.border_dentelle,
    CoverBorder.PERLES to R.string.border_perles,
)

private val CORNERS = listOf(
    CoverCorner.NONE to R.string.binding_none,
    CoverCorner.QUADRILOBE to R.string.corner_quadrilobe,
    CoverCorner.FLEUR_DE_LYS to R.string.corner_lys,
    CoverCorner.ECOINCON to R.string.corner_ecoincon,
    CoverCorner.CARRE to R.string.corner_carre,
    CoverCorner.ROSETTE to R.string.corner_rosette,
)

private val EDGES = listOf(
    EdgeFinish.GILT to R.string.binding_gilt,
    EdgeFinish.RED_UNDER_GOLD to R.string.edge_red_under_gold,
    EdgeFinish.RED to R.string.binding_red,
    EdgeFinish.RED_ORANGE to R.string.edge_red_orange,
    EdgeFinish.BLUE to R.string.edge_blue,
    EdgeFinish.GREEN to R.string.edge_green,
    EdgeFinish.YELLOW to R.string.edge_yellow,
    EdgeFinish.SPRINKLED to R.string.edge_sprinkled,
    EdgeFinish.MARBLED to R.string.edge_marbled,
    EdgeFinish.PLAIN to R.string.binding_plain,
)

private val EDGE_TOOLINGS = listOf(
    EdgeTooling.NONE to R.string.binding_none_f,
    EdgeTooling.POINTILLE to R.string.edge_pointille,
    EdgeTooling.LOSANGES to R.string.edge_losanges,
    EdgeTooling.RINCEAUX to R.string.edge_rinceaux,
)
