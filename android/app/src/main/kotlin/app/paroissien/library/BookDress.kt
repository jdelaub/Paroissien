package app.paroissien.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paroissien.core.designsystem.CoverMaterial
import app.paroissien.core.designsystem.DoublurePage
import app.paroissien.core.designsystem.frame.EndpaperPatterns
import app.paroissien.core.designsystem.frame.PatternPaper

/**
 * Le rang d'habillage d'un livre (demande de l'utilisateur, 12/10/2026) : les livres nobles reçoivent un habit
 * travaillé, les livres simples (exercices, lettres de Clairval) un habit sobre. Il se déduit de la nature du livre,
 * pas du réglage « Livres » qui rend les reliures uniformes.
 */
enum class Dress {
    /** Livre de prières relié de cuir : un cadre ornemental par section du sommaire, doublure de soie moirée. */
    NOBLE,

    /** Bible : texte suivi, page réglée d'un filet rouge ; doublure de soie moirée. */
    BIBLE,

    /** Livre relié de toile (exercices) : page nue, garde de papier imprimé d'un motif. */
    SIMPLE,

    /** Lettre ou brochure sous couverture de papier : ni cadre ni garde. */
    PLAIN,
}

object BookDresses {

    fun of(publication: String, entry: LibraryEntry?): Dress {
        if (entry == null) return Dress.PLAIN
        val identity = BookIdentities.of(publication, entry, uniform = false)
        return when {
            entry.series != null || identity.cover.material == CoverMaterial.WRAPPER -> Dress.PLAIN
            publication.startsWith("bible") || entry.publication.kind == "bible" -> Dress.BIBLE
            identity.importance >= 2 -> Dress.NOBLE
            else -> Dress.SIMPLE
        }
    }

    /** La garde du livre, après la couverture : soie pour les livres nobles, papier à motif pour les simples. */
    fun endpaper(publication: String, entry: LibraryEntry?): (@Composable () -> Unit)? = when (of(publication, entry)) {
        Dress.NOBLE, Dress.BIBLE -> { { DoublurePage() } }
        Dress.SIMPLE -> EndpaperPatterns.takeIf { it.isNotEmpty() }?.let { all ->
            // le même motif pour un livre donné, d'un livre à l'autre ils varient
            val pattern = all[publication.hashCode().mod(all.size)]
            { PatternPaper(pattern, Modifier.fillMaxSize()) }
        }
        Dress.PLAIN -> null
    }
}
