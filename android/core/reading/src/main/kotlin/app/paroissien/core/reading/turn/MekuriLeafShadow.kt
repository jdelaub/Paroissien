// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color

/**
 * The shadows a two-sided leaf casts: the contact shadow past the rim and the
 * crease ramp short of the crease. Drawn in a layer of the leaf's own size
 * beneath the leaf and over the revealed page, so each shadow composites once
 * and the leaf's own faces carry none. Nothing is drawn at rest.
 */
@Composable
internal fun MekuriLeafShadow(
    progress: () -> Float,
    direction: MekuriDirection,
    configuration: MekuriConfiguration,
    modifier: Modifier = Modifier,
    liftsFromBottom: Boolean = false,
) {
    val isFolded by remember(progress) { derivedStateOf { progress() > 0f } }
    val shader = rememberMekuriFoldShader()
    Box(modifier) {
        if (!isFolded) return@Box
        Box(
            Modifier
                .fillMaxSize()
                .mekuriFold(
                    shader = shader,
                    mirrorScale = MekuriFoldPass.mirrorScale(direction),
                    verticalScale = MekuriFoldPass.verticalScale(liftsFromBottom),
                    face = MekuriFace.Shadow,
                    isHinged = true,
                    configuration = configuration,
                    progress = { MekuriFoldPass(direction, progress()).shaderProgress },
                )
                // The layer is opaque so it is never culled; the shader replaces
                // every pixel of it and samples none.
                .drawBehind { drawRect(Color.Black) },
        )
    }
}
