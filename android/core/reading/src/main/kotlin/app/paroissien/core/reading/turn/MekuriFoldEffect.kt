// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Paroissien : vrai quand la feuille glisse au lieu de se plier, par choix de l'utilisateur.
 * Sous l'API 33, RuntimeShader n'existe pas : la feuille glisse toujours.
 */
internal val LocalMekuriSlides = staticCompositionLocalOf { false }

/** One shader object per pass; only its argument block is rebuilt per frame. Null when the leaf slides. */
@Composable
internal fun rememberMekuriFoldShader(): RuntimeShader? {
    if (LocalMekuriSlides.current || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return remember { MekuriFoldShaders.create() }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object MekuriFoldShaders {
    fun create(): RuntimeShader = RuntimeShader(MEKURI_FOLD_AGSL)
}

/**
 * Folds the content this modifier is applied to. The shader folds a flap
 * entering from the right with its free corner at the top only, so a
 * right-to-left pass mirrors the content horizontally into the effect and the
 * result back out, and a bottom lift does the same vertically; the mirror is
 * spatial, never a negated progress.
 *
 * Without a shader (Paroissien), the same pass slides the sheet toward the spine
 * in the fold's own space, so the mirrors keep the direction right; the back
 * face and the shadow pass draw nothing.
 *
 * [progress] must be read inside the layer block, not in composition.
 */
internal fun Modifier.mekuriFold(
    shader: RuntimeShader?,
    mirrorScale: Float,
    verticalScale: Float,
    face: MekuriFace,
    isHinged: Boolean,
    configuration: MekuriConfiguration,
    progress: () -> Float,
): Modifier = this
    .graphicsLayer {
        scaleX = mirrorScale
        scaleY = verticalScale
    }
    .graphicsLayer {
        if (shader == null) {
            when (face) {
                MekuriFace.Whole, MekuriFace.Front -> translationX = -progress() * size.width
                MekuriFace.Back, MekuriFace.Shadow -> alpha = 0f
            }
            return@graphicsLayer
        }
        val uniforms = MekuriFoldUniforms.of(
            size = size,
            progress = progress(),
            configuration = configuration,
            face = face,
            hinge = if (isHinged) MekuriFoldUniforms.hinge(size) else null,
        )
        applyFold(shader, uniforms)
        compositingStrategy = CompositingStrategy.Offscreen
        clip = true
    }
    .graphicsLayer {
        scaleX = mirrorScale
        scaleY = verticalScale
    }

private const val LAYER_UNIFORM = "layer"

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun androidx.compose.ui.graphics.GraphicsLayerScope.applyFold(shader: RuntimeShader, uniforms: MekuriFoldUniforms) {
    shader.setUniforms(uniforms)
    renderEffect = RenderEffect.createRuntimeShaderEffect(shader, LAYER_UNIFORM).asComposeRenderEffect()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun RuntimeShader.setUniforms(uniforms: MekuriFoldUniforms) {
    setFloatUniform("size", uniforms.size.width, uniforms.size.height)
    setFloatUniform("progress", uniforms.progress)
    setFloatUniform("heldRadius", uniforms.heldRadius)
    setFloatUniform("radiusSlope", uniforms.radiusSlope)
    setFloatUniform("shear", uniforms.shear)
    setFloatUniform("bow", uniforms.bow)
    setFloatUniform("backFaceDim", uniforms.backFaceDim)
    setFloatUniform("shadowWidth", uniforms.shadowWidth)
    setFloatUniform("shadowOpacity", uniforms.shadowOpacity)
    setFloatUniform("face", uniforms.face)
}
