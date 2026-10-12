package app.paroissien.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import app.paroissien.core.reading.turn.MekuriPager
import app.paroissien.core.reading.turn.MekuriSpread
import app.paroissien.core.reading.turn.rememberMekuriPagerState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Ouvrir un livre pris sur le rayon : on voit d'abord sa couverture, puis, en la tournant, la doublure du plat ; la
 * feuille suivante est transparente et laisse voir, sous elle, la page où l'on en était. Une fois cette feuille
 * tournée, l'ouverture s'efface. Toucher n'importe où tourne la page, comme glisser.
 */
@Composable
fun BookOpening(cover: @Composable () -> Unit, endpaper: (@Composable () -> Unit)?, onOpened: () -> Unit) {
    val pages = listOfNotNull(cover, endpaper)
    val state = rememberMekuriPagerState(pageCount = pages.size + 1, initialPage = 0)
    val scope = rememberCoroutineScope()
    val opened by rememberUpdatedState(onOpened)
    LaunchedEffect(state) {
        snapshotFlow { state.currentPage }.first { it == pages.size }
        opened()
    }
    MekuriPager(
        state = state,
        modifier = Modifier.fillMaxSize(),
        spread = MekuriSpread.Single,
        onCenterTap = { scope.launch { state.animateToPage((state.currentPage + 1).coerceAtMost(pages.size)) } },
    ) { page ->
        if (page < pages.size) pages[page]() else Spacer(Modifier.fillMaxSize())
    }
}
