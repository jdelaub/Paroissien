package app.paroissien.ui

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.data.ReaderSettings
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.ParoissienTheme
import app.paroissien.core.designsystem.RibbonStyle
import app.paroissien.core.designsystem.drawRibbon
import kotlinx.serialization.Serializable

/** Écrans de l'application (rapport 08) : trois entrées de premier niveau, le lecteur et la liste d'une série. */
@Serializable data object LibraryKey : NavKey
@Serializable data object BookmarksKey : NavKey
@Serializable data object SearchKey : NavKey
@Serializable data class SeriesKey(val series: String, val title: String) : NavKey
@Serializable data class ReaderKey(val publication: String, val unit: String? = null, val block: String? = null, val offset: Int = 0) : NavKey

private val tabs = listOf<NavKey>(LibraryKey, BookmarksKey, SearchKey)

@Composable
fun ParoissienRoot(graph: AppGraph) {
    val settings by graph.settings.settings.collectAsState(initial = ReaderSettings())
    val backStack = rememberNavBackStack(LibraryKey)
    val top = backStack.lastOrNull()
    val open: (NavKey) -> Unit = { key -> backStack.add(key) }
    val selectTab: (NavKey) -> Unit = { key -> backStack.clear(); backStack.add(key) }

    ParoissienTheme(PaperTheme.entries.firstOrNull { it.name == settings.theme } ?: PaperTheme.CLAIR) {
        Immersive(enabled = top is ReaderKey)
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        entry<LibraryKey> { LibraryScreen(graph, open) }
                        entry<SeriesKey> { key -> SeriesScreen(graph, key, open, onBack = { backStack.removeLastOrNull() }) }
                        entry<BookmarksKey> { BookmarksScreen(graph, open) }
                        entry<SearchKey> { SearchScreen(graph, open) }
                        entry<ReaderKey> { key -> ReaderScreen(graph, key, settings, onBack = { backStack.removeLastOrNull() }) }
                    },
                )
            }
            if (top in tabs) {
                NavigationBar(containerColor = LocalPaperColors.current.surface) {
                    NavigationBarItem(
                        selected = top == LibraryKey,
                        onClick = { selectTab(LibraryKey) },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_library)) },
                    )
                    NavigationBarItem(
                        selected = top == BookmarksKey,
                        onClick = { selectTab(BookmarksKey) },
                        icon = { RibbonIcon(slot = 0) },
                        label = { Text(stringResource(R.string.tab_bookmarks)) },
                    )
                    NavigationBarItem(
                        selected = top == SearchKey,
                        onClick = { selectTab(SearchKey) },
                        icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_search)) },
                    )
                }
            }
        }
    }
}

/** Un petit ruban de tissu, dessiné comme ceux du livre. */
@Composable
fun RibbonIcon(slot: Int, modifier: Modifier = Modifier) {
    val dark = LocalPaperColors.current.isDark
    Canvas(modifier.size(width = 10.dp, height = 22.dp)) { drawRibbon(RibbonStyle.of(slot), dark, Offset.Zero, size) }
}

/** Lecture sans distraction : barres système masquées dans le lecteur, rappelées d'un glissement depuis le bord. */
@Composable
private fun Immersive(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled, view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (enabled) controller?.hide(WindowInsetsCompat.Type.systemBars()) else controller?.show(WindowInsetsCompat.Type.systemBars())
        onDispose { }
    }
}
