package app.paroissien.library

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import app.paroissien.core.designsystem.BookIdentity
import app.paroissien.core.designsystem.CoverMaterial
import app.paroissien.core.designsystem.CoverMotif
import app.paroissien.core.designsystem.CoverStyle
import app.paroissien.core.designsystem.EdgeFinish
import app.paroissien.core.designsystem.PaperStock
import app.paroissien.core.designsystem.PaperWeight
import app.paroissien.core.designsystem.Tooling

/**
 * L'identité de chaque livre : reliure, papier, tranche, rubans, à la mesure de l'ouvrage. Les rubans sont des
 * emplacements de `RibbonStyle.slots` (0 rouge, 1 vert, 2 violet, 3 blanc, 4 rose, 5 noir). Un livre inconnu reçoit
 * une identité déduite de son genre et de sa taille ; [uniform] les rend tous pareils (réglage « Livres »).
 */
object BookIdentities {

    /** Nombre de pages estimé : environ quatre blocs (versets, paragraphes) par page. */
    fun pages(entry: LibraryEntry?): Int = ((entry?.units?.sumOf { it.blockCount } ?: 40) / 4).coerceIn(8, 6000)

    fun of(publication: String, entry: LibraryEntry?, uniform: Boolean): BookIdentity {
        val pages = pages(entry)
        if (uniform) return BookIdentity.uniform(pages)
        return when {
            publication == "bible.crampon" -> BookIdentity(
                CoverStyle(Color(0xFF241416), CoverMaterial.MOROCCO, Tooling.GOLD, CoverMotif.CROSS, fillets = 3, corners = true, bands = 5),
                PaperStock(Color(0xFFF3E8C9), Color(0xFF2A2118), Color(0xFF5E5040), Color(0xFF9E2A1E), PaperWeight.BIBLE),
                EdgeFinish.GILT, ribbons = listOf(0, 1, 2, 3), importance = 3, pages = pages,
            )
            publication == "bible.vulgata" -> BookIdentity(
                CoverStyle(Color(0xFF5E1519), CoverMaterial.MOROCCO, Tooling.GOLD, CoverMotif.CHI_RHO, fillets = 3, corners = true, bands = 5),
                PaperStock(Color(0xFFF1E7CB), Color(0xFF2A2118), Color(0xFF5E5040), Color(0xFF9E2A1E), PaperWeight.BIBLE),
                EdgeFinish.GILT, ribbons = listOf(0, 3, 2), importance = 3, pages = pages,
            )
            publication.startsWith("imitation") -> BookIdentity(
                CoverStyle(Color(0xFF5C3A22), CoverMaterial.CALF, Tooling.BLIND, CoverMotif.CROSS, fillets = 2, corners = true, bands = 4),
                PaperStock(Color(0xFFF8F1DF), Color(0xFF241C14), Color(0xFF5A4E40), Color(0xFFA31F1A), PaperWeight.FINE),
                EdgeFinish.RED, ribbons = listOf(0, 1), importance = 2, pages = pages,
            )
            publication.startsWith("exercices") -> BookIdentity(
                CoverStyle(Color(0xFF1D1D20), CoverMaterial.CLOTH, Tooling.BLIND, CoverMotif.IHS, fillets = 1, corners = false, bands = 0, motifTooling = Tooling.GOLD),
                PaperStock(Color(0xFFFFFEFA), Color(0xFF101010), Color(0xFF444444), Color(0xFF8E1B16), PaperWeight.REGULAR),
                EdgeFinish.PLAIN, ribbons = listOf(0, 5), importance = 1, pages = pages,
            )
            // Le missel quotidien : maroquin rouge sombre, papier bible, tranche dorée, quatre rubans (et celui de la messe du jour).
            publication.startsWith("missel") -> BookIdentity(
                CoverStyle(Color(0xFF4A1014), CoverMaterial.MOROCCO, Tooling.GOLD, CoverMotif.CROSS, fillets = 3, corners = true, bands = 5),
                PaperStock(Color(0xFFF5ECD4), Color(0xFF221A12), Color(0xFF5A4E40), Color(0xFFA31F1A), PaperWeight.BIBLE),
                EdgeFinish.GILT, ribbons = listOf(0, 1, 2, 3), importance = 3, pages = pages,
            )
            publication.startsWith("ordinaire") -> BookIdentity(
                CoverStyle(Color(0xFF16130F), CoverMaterial.MOROCCO, Tooling.GOLD, CoverMotif.CROSS, fillets = 2, corners = true, bands = 4),
                PaperStock(Color(0xFFF7EFDA), Color(0xFF1F1A14), Color(0xFF574C3F), Color(0xFFA31F1A), PaperWeight.FINE),
                EdgeFinish.GILT, ribbons = listOf(0, 3), importance = 2, pages = pages,
            )
            // Le diurnal monastique : chagrin noir, papier bible, tranche rouge, un ruban par heure.
            publication.startsWith("diurnal") -> BookIdentity(
                CoverStyle(Color(0xFF121010), CoverMaterial.MOROCCO, Tooling.GOLD, CoverMotif.CROSS, fillets = 2, corners = true, bands = 4),
                PaperStock(Color(0xFFF6EEDA), Color(0xFF1C1712), Color(0xFF574C3F), Color(0xFFA31F1A), PaperWeight.BIBLE),
                EdgeFinish.RED, ribbons = listOf(0, 1, 2, 3, 5), importance = 2, pages = pages,
            )
            // La synopse : maroquin vert sombre, un ruban par évangéliste.
            publication.startsWith("synopse") -> BookIdentity(
                CoverStyle(Color(0xFF1B2E26), CoverMaterial.MOROCCO, Tooling.GOLD, CoverMotif.CHI_RHO, fillets = 2, corners = true, bands = 5),
                PaperStock(Color(0xFFF4EAD0), Color(0xFF2A2118), Color(0xFF5E5040), Color(0xFF9E2A1E), PaperWeight.BIBLE),
                EdgeFinish.GILT, ribbons = listOf(0, 1, 2, 3), importance = 2, pages = pages,
            )
            publication.startsWith("prieres") -> BookIdentity(
                CoverStyle(Color(0xFF1E3A2B), CoverMaterial.MOROCCO, Tooling.GOLD, CoverMotif.CROSS, fillets = 2, corners = true, bands = 4),
                PaperStock(Color(0xFFFAF3E2), Color(0xFF1F1A14), Color(0xFF574C3F), Color(0xFFA31F1A), PaperWeight.FINE),
                EdgeFinish.GILT, ribbons = listOf(0, 3, 1), importance = 2, pages = pages,
            )
            // Livret de messe agrafé : couverture de papier, filets rouges imprimés comme sur l'original, papier blanc.
            publication.startsWith("livret") -> BookIdentity(
                CoverStyle(Color(0xFFF7F2E8), CoverMaterial.WRAPPER, Tooling.PRINTED, CoverMotif.CROSS, fillets = 2, corners = false, bands = 0, ink = Color(0xFFB0141B)),
                PaperStock(Color(0xFFFFFEFA), Color(0xFF151515), Color(0xFF4A4A4A), Color(0xFFA3171C), PaperWeight.REGULAR),
                EdgeFinish.PLAIN, ribbons = listOf(0), importance = 1, pages = pages,
            )
            entry?.series != null -> letter(pages)
            else -> byKind(entry?.publication?.kind, pages)
        }
    }

    /** Une lettre : couverture de papier imprimée, papier épais et blanc, un seul ruban. */
    fun letter(pages: Int) = BookIdentity(
        CoverStyle(Color(0xFFEDE3CB), CoverMaterial.WRAPPER, Tooling.PRINTED, CoverMotif.CHI_RHO, fillets = 1, corners = false, bands = 0, ink = Color(0xFF7A1E1A)),
        PaperStock(Color(0xFFFFFDF7), Color(0xFF1A1A1A), Color(0xFF555555), Color(0xFF8E1B16), PaperWeight.THICK),
        EdgeFinish.PLAIN, ribbons = listOf(0), importance = 0, pages = pages,
    )

    /** Sans identité propre : d'autant plus riche et fin de papier que le livre est gros. */
    private fun byKind(kind: String?, pages: Int): BookIdentity {
        val weight = when {
            pages > 1500 -> PaperWeight.BIBLE
            pages > 300 -> PaperWeight.FINE
            pages > 60 -> PaperWeight.REGULAR
            else -> PaperWeight.THICK
        }
        val big = pages > 600 || kind == "bible" || kind == "missal"
        return BookIdentity(
            CoverStyle(if (big) Color(0xFF2B1A14) else Color(0xFF4A3020), if (big) CoverMaterial.MOROCCO else CoverMaterial.CALF, Tooling.GOLD, CoverMotif.CROSS, fillets = if (big) 2 else 1, corners = big, bands = if (big) 5 else 3),
            PaperStock(Color(0xFFF8F2E2), Color(0xFF221B14), Color(0xFF5A4E40), Color(0xFFA31F1A), weight),
            if (big) EdgeFinish.GILT else EdgeFinish.PLAIN,
            ribbons = if (big) listOf(0, 1, 2, 3) else listOf(0, 1), importance = if (big) 2 else 1, pages = pages,
        )
    }
}

/** Le sous-titre de la publication (traducteur, édition), s'il y en a un : sous le titre de la couverture. */
fun LibraryEntry.subtitle(): String? = runCatching {
    publication.meta["subtitle"]?.jsonObject?.let { it["fr"] ?: it.values.firstOrNull() }?.jsonPrimitive?.content
}.getOrNull()
