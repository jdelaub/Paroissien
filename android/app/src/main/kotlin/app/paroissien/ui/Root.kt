package app.paroissien.ui

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.graphics.drawscope.rotate
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
import app.paroissien.core.designsystem.LocalRibbonFabric
import app.paroissien.core.designsystem.LocalRibbonSet
import app.paroissien.core.designsystem.of
import app.paroissien.library.RibbonChoices
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import app.paroissien.core.designsystem.drawRibbon
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * Écrans de l'application (rapport 08) : cinq entrées de premier niveau (Aujourd'hui, Bibliothèque, Images, Atelier,
 * Signets ; la bibliothèque ne montre que des livres, chaque autre fonction a sa section, demande du 13/10/2026), puis
 * le lecteur, l'Ordo, la recherche (loupe de la bibliothèque), le temps de prière et les réglages.
 */
@Serializable data object TodayKey : NavKey
@Serializable data object LibraryKey : NavKey
@Serializable data object BookmarksKey : NavKey
@Serializable data object SearchKey : NavKey
@Serializable data class SeriesKey(val series: String, val title: String) : NavKey
/** [opening] : le livre est pris sur le rayon ; il s'ouvre par sa couverture et sa doublure avant la page. */
@Serializable data class ReaderKey(val publication: String, val unit: String? = null, val block: String? = null, val offset: Int = 0, val opening: Boolean = false) : NavKey
@Serializable data class DayKey(val date: String) : NavKey
@Serializable data class OrdoKey(val year: Int, val month: Int) : NavKey
@Serializable data class PrayerKey(val publication: String? = null, val unit: String? = null, val block: String? = null, val title: String? = null) : NavKey
@Serializable data class CardKey(val id: String) : NavKey
@Serializable data object ImageBoxKey : NavKey
@Serializable data class ScanKey(val backOf: String? = null) : NavKey
@Serializable data object SettingsKey : NavKey
@Serializable data class BookDressKey(val publication: String) : NavKey
@Serializable data class FrameEditorKey(val id: String? = null, val random: Boolean = false) : NavKey
@Serializable data object WorkshopKey : NavKey
@Serializable data class MotifEditorKey(val id: String? = null) : NavKey
@Serializable data object RibbonEditorKey : NavKey
@Serializable data class CoverEditorKey(val id: String? = null) : NavKey

private val tabs = listOf<NavKey>(TodayKey, LibraryKey, ImageBoxKey, WorkshopKey, BookmarksKey)

@Composable
fun ParoissienRoot(graph: AppGraph) {
    val settings by graph.settings.settings.collectAsState(initial = ReaderSettings())
    val backStack = rememberNavBackStack(TodayKey)
    val top = backStack.lastOrNull()
    val open: (NavKey) -> Unit = { key -> backStack.add(key) }
    val selectTab: (NavKey) -> Unit = { key -> backStack.clear(); backStack.add(key) }
    val back: () -> Unit = { backStack.removeLastOrNull() }

    ParoissienTheme(PaperTheme.entries.firstOrNull { it.name == settings.theme } ?: PaperTheme.CLAIR) {
        val ribbonSet = remember(settings.ribbons) { RibbonChoices.styles(settings.ribbons) }
        CompositionLocalProvider(LocalRibbonFabric provides settings.ribbonFabric(), LocalRibbonSet provides ribbonSet) {
            Immersive(enabled = top is ReaderKey || top is PrayerKey)
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = entryProvider {
                            entry<TodayKey> { TodayScreen(graph, settings, date = null, open = open) }
                            entry<DayKey> { key -> TodayScreen(graph, settings, LocalDate.parse(key.date), open, onBack = { backStack.removeLastOrNull() }) }
                            entry<OrdoKey> { key -> OrdoScreen(graph, settings, key, open, onBack = { backStack.removeLastOrNull() }) }
                            entry<PrayerKey> { key ->
                                PrayerScreen(
                                    graph, settings, key,
                                    onOpenText = { backStack.removeLastOrNull(); key.publication?.let { open(ReaderKey(it, key.unit, key.block)) } },
                                    onClose = { backStack.removeLastOrNull() },
                                )
                            }
                            entry<CardKey> { key -> CardScreen(graph, key, open, onBack = { backStack.removeLastOrNull() }) }
                            entry<ImageBoxKey> { ImageBoxScreen(graph, open, onBack = back.takeIf { backStack.size > 1 }) }
                            entry<BookDressKey> { BookDressScreen(graph, it, open, onBack = { backStack.removeLastOrNull() }) }
                            entry<FrameEditorKey> { FrameEditorScreen(graph, it, onBack = { backStack.removeLastOrNull() }) }
                            entry<ScanKey> { key ->
                                ScanScreen(
                                    graph, key,
                                    onDone = { id -> backStack.removeLastOrNull(); if (key.backOf == null) open(CardKey(id)) },
                                    onBack = { backStack.removeLastOrNull() },
                                )
                            }
                            entry<SettingsKey> { SettingsScreen(graph, settings, open, onBack = { backStack.removeLastOrNull() }) }
                            entry<WorkshopKey> { WorkshopScreen(graph, open, onBack = back.takeIf { backStack.size > 1 }) }
                            entry<MotifEditorKey> { MotifEditorScreen(graph, it, onBack = { backStack.removeLastOrNull() }) }
                            entry<RibbonEditorKey> { RibbonEditorScreen(graph, settings, onBack = back) }
                            entry<CoverEditorKey> { CoverEditorScreen(graph, it, onBack = back) }
                            entry<LibraryKey> { LibraryScreen(graph, settings, open) }
                            entry<SeriesKey> { key -> SeriesScreen(graph, settings, key, open, onBack = { backStack.removeLastOrNull() }) }
                            entry<BookmarksKey> { BookmarksScreen(graph, open) }
                            entry<SearchKey> { SearchScreen(graph, open, onBack = back.takeIf { backStack.size > 1 }) }
                            entry<ReaderKey> { key -> ReaderScreen(graph, key, settings, open, onBack = { backStack.removeLastOrNull() }) }
                        },
                    )
                }
                if (top in tabs) {
                    NavigationBar(containerColor = LocalPaperColors.current.surface) {
                        NavigationBarItem(
                            selected = top == TodayKey,
                            onClick = { selectTab(TodayKey) },
                            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_today)) },
                        )
                        NavigationBarItem(
                            selected = top == LibraryKey,
                            onClick = { selectTab(LibraryKey) },
                            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_library)) },
                        )
                        NavigationBarItem(
                            selected = top == ImageBoxKey,
                            onClick = { selectTab(ImageBoxKey) },
                            icon = { CardsIcon() },
                            label = { Text(stringResource(R.string.tab_images)) },
                        )
                        NavigationBarItem(
                            selected = top == WorkshopKey,
                            onClick = { selectTab(WorkshopKey) },
                            icon = { Icon(Icons.Filled.Create, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_workshop)) },
                        )
                        NavigationBarItem(
                            selected = top == BookmarksKey,
                            onClick = { selectTab(BookmarksKey) },
                            icon = { RibbonIcon(slot = 0) },
                            label = { Text(stringResource(R.string.tab_bookmarks)) },
                        )
                    }
                }
            }
        }
    }
}

/** Deux images pieuses l'une sur l'autre, un peu tournées : l'onglet des images. */
@Composable
private fun CardsIcon(modifier: Modifier = Modifier) {
    val ink = androidx.compose.material3.LocalContentColor.current
    Canvas(modifier.size(22.dp)) {
        val w = size.width * 0.52f
        val h = size.height * 0.78f
        val r = androidx.compose.ui.geometry.CornerRadius(w * 0.08f)
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(1.6.dp.toPx())
        rotate(-12f, pivot = Offset(size.width * 0.4f, size.height * 0.6f)) {
            drawRoundRect(ink, Offset(size.width * 0.14f, size.height * 0.14f), androidx.compose.ui.geometry.Size(w, h), r, style = stroke)
        }
        rotate(8f, pivot = Offset(size.width * 0.6f, size.height * 0.6f)) {
            drawRoundRect(ink, Offset(size.width * 0.36f, size.height * 0.1f), androidx.compose.ui.geometry.Size(w, h), r)
        }
    }
}

/** Un petit ruban de tissu, dessiné comme ceux du livre. */
@Composable
fun RibbonIcon(slot: Int, modifier: Modifier = Modifier) {
    val dark = LocalPaperColors.current.isDark
    val fabric = LocalRibbonFabric.current
    val style = LocalRibbonSet.current.of(slot)
    Canvas(modifier.size(width = 10.dp, height = 22.dp)) { drawRibbon(style, dark, Offset.Zero, size, fabric) }
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
