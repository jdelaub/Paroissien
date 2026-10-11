// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Renders [content] folded by [progress], 0 (flat) to 1 (turned). The crease and
 * contact shadows are drawn by the shader; no further shadow may be layered on
 * top. Single-page mode: the reverse of the sheet is its own front mirrored.
 */
@Composable
internal fun MekuriFoldedPage(
    progress: () -> Float,
    direction: MekuriDirection,
    configuration: MekuriConfiguration,
    modifier: Modifier = Modifier,
    liftsFromBottom: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isFolded by remember(progress) { derivedStateOf { progress() > 0f } }
    val shader = rememberMekuriFoldShader()
    Box(modifier) {
        if (isFolded) {
            Box(
                Modifier
                    .fillMaxSize()
                    .mekuriFold(
                        shader = shader,
                        mirrorScale = MekuriFoldPass.mirrorScale(direction),
                        verticalScale = MekuriFoldPass.verticalScale(liftsFromBottom),
                        face = MekuriFace.Whole,
                        isHinged = false,
                        configuration = configuration,
                        progress = { MekuriFoldPass(direction, progress()).shaderProgress },
                    ),
            ) {
                content()
            }
        } else {
            content()
        }
    }
}
