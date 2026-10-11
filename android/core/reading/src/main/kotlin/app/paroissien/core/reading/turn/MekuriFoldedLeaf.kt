// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * A turning leaf with a different page printed on each side. [front] is the page
 * being read and [back] the page on its reverse, given in reading orientation.
 * Without a [back] the reverse is the front mirrored over one page. With one the
 * leaf spans a two-slot spread: its front fills the trailing slot, hinges at the
 * centre and lands in the leading slot. The leaf draws no shadow of its own; the
 * part of it short of the crease draws the departing page, opaque and undimmed.
 */
@Composable
internal fun MekuriFoldedLeaf(
    progress: () -> Float,
    direction: MekuriDirection,
    configuration: MekuriConfiguration,
    modifier: Modifier = Modifier,
    liftsFromBottom: Boolean = false,
    back: (@Composable () -> Unit)? = null,
    front: @Composable () -> Unit,
) {
    if (back == null) {
        MekuriFoldedPage(progress, direction, configuration, modifier, liftsFromBottom, front)
        return
    }
    val isFolded by remember(progress) { derivedStateOf { progress() > 0f } }
    val mirrorScale = MekuriFoldPass.mirrorScale(direction)
    val verticalScale = MekuriFoldPass.verticalScale(liftsFromBottom)
    val frontShader = rememberMekuriFoldShader()
    val backShader = rememberMekuriFoldShader()
    Box(modifier) {
        if (!isFolded) {
            MekuriLeafSlot(mirrorScale, isMirroredInSlot = false, content = front)
            return@Box
        }
        MekuriLeafFace(frontShader, mirrorScale, verticalScale, MekuriFace.Front, configuration, direction, progress) {
            MekuriLeafSlot(mirrorScale, isMirroredInSlot = false, content = front)
        }
        MekuriLeafFace(backShader, mirrorScale, verticalScale, MekuriFace.Back, configuration, direction, progress) {
            MekuriLeafSlot(mirrorScale, isMirroredInSlot = true, content = back)
        }
    }
}

/**
 * The faces never overlap on screen, so the stacking order carries no meaning.
 */
@Composable
private fun MekuriLeafFace(
    shader: RuntimeShader?,
    mirrorScale: Float,
    verticalScale: Float,
    face: MekuriFace,
    configuration: MekuriConfiguration,
    direction: MekuriDirection,
    progress: () -> Float,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .mekuriFold(
                shader = shader,
                mirrorScale = mirrorScale,
                verticalScale = verticalScale,
                face = face,
                isHinged = true,
                configuration = configuration,
                progress = { MekuriFoldPass(direction, progress()).shaderProgress },
            ),
    ) {
        content()
    }
}

/**
 * Placement is a geometry transform, never layout alignment: alignment follows
 * the layout direction and the leaf must follow the reading direction. The back
 * is mirrored within its own slot so both faces sample the trailing slot.
 */
@Composable
private fun MekuriLeafSlot(
    mirrorScale: Float,
    isMirroredInSlot: Boolean,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth(SLOT_FRACTION)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = mirrorScale * size.width / 2f
                    scaleX = if (isMirroredInSlot) -1f else 1f
                },
        ) {
            content()
        }
    }
}

/** A leaf spans two slots and hinges at its centre. */
private const val SLOT_FRACTION = 0.5f
