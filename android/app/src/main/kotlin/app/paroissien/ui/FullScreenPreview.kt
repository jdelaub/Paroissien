package app.paroissien.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.paroissien.R
import app.paroissien.core.designsystem.BookCover
import app.paroissien.core.designsystem.BookEdgeSample
import app.paroissien.core.designsystem.BookIdentity
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.Metal
import app.paroissien.core.designsystem.frame.FramedPageSample
import app.paroissien.core.designsystem.frame.OrnamentalFrameDef
import app.paroissien.core.liturgy.LiturgicalColor

/**
 * Un aperçu en plein écran (atelier, 14/10/2026) : par-dessus tout, barres du système masquées. On pince pour
 * agrandir et l'on déplace l'image agrandie ; un double toucher la remet à sa taille ; « Fermer » ou « retour » ferme.
 * [options] : des choix posés en haut, à côté de « Fermer » (recto ou verso, couverture ou tranche…).
 */
@Composable
internal fun FullScreenPreview(onClose: () -> Unit, options: (@Composable () -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        DisposableEffect(window) {
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            onDispose { }
        }
        val paper = LocalPaperColors.current
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val zoom = rememberTransformableState { change, pan, _ ->
            scale = (scale * change).coerceIn(1f, 5f)
            offset = if (scale <= 1.01f) Offset.Zero else offset + pan
        }
        Box(Modifier.fillMaxSize().background(paper.background).clipToBounds()) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }
                    .transformable(zoom)
                    .pointerInput(Unit) { detectTapGestures(onDoubleTap = { scale = 1f; offset = Offset.Zero }) },
                contentAlignment = Alignment.Center,
                content = content,
            )
            Surface(color = paper.background.copy(alpha = 0.88f), modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(6.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onClose) { Text(stringResource(R.string.motif_exit_full)) }
                    options?.invoke()
                }
            }
        }
    }
}

/** Une couverture en grand : le livre fermé, sa garde, ou sa tranche vue de face. */
@Composable
internal fun CoverPreview(identity: BookIdentity, title: String, subtitle: String?, endpaper: (@Composable () -> Unit)? = null, onClose: () -> Unit) {
    var page by remember { mutableStateOf(0) }
    val pages = listOfNotNull(R.string.preview_cover, R.string.preview_endpaper.takeIf { endpaper != null }, R.string.preview_edge)
    FullScreenPreview(onClose, options = {
        pages.forEachIndexed { i, label -> FilterChip(page == i, { page = i }, { Text(stringResource(label)) }) }
    }) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp), contentAlignment = Alignment.Center) {
            val w = min(maxWidth * 0.92f, maxHeight * 0.86f * 0.70f)
            val edgeHeight = min(maxHeight * 0.2f, 140.dp)
            when (pages.getOrNull(page)) {
                R.string.preview_endpaper -> Box(Modifier.width(w).aspectRatio(0.70f).clipToBounds()) { endpaper?.invoke() }
                R.string.preview_edge -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BookEdgeSample(identity, Modifier.fillMaxWidth().height(edgeHeight))
                }
                else -> BookCover(identity, title, subtitle, Modifier.width(w).aspectRatio(0.70f))
            }
        }
    }
}

/** Un cadre en grand : une page de texte à la taille de l'écran, au recto ou au verso, dans ses couleurs et son métal. */
@Composable
internal fun FramePreview(def: OrnamentalFrameDef, liturgicalColor: LiturgicalColor? = null, metal: Metal? = null, onClose: () -> Unit) {
    var recto by remember { mutableStateOf(true) }
    FullScreenPreview(onClose, options = {
        FilterChip(recto, { recto = true }, { Text(stringResource(R.string.preview_recto)) })
        FilterChip(!recto, { recto = false }, { Text(stringResource(R.string.preview_verso)) })
    }) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 52.dp)) {
            FramedPageSample(spec = def.spec, style = def.style.withMetal(metal ?: def.style.metal), liturgicalColor = liturgicalColor, outerOnRight = recto)
        }
    }
}

/** Toucher : l'action ordinaire ([onClick]) ; appui long : l'aperçu en plein écran. */
@Composable
internal fun Modifier.previewOnLongPress(onPreview: () -> Unit, onClick: () -> Unit): Modifier {
    val label = stringResource(R.string.preview_full)
    return combinedClickable(onLongClickLabel = label, onLongClick = onPreview, onClick = onClick)
}

/** Le libellé et le geste communs pour ouvrir un aperçu : un bouton « Voir en grand ». */
@Composable
internal fun PreviewButton(onClick: () -> Unit) {
    Row { TextButton(onClick = onClick) { Text(stringResource(R.string.preview_full)) } }
}
