// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import kotlin.math.max
import kotlin.math.min

/**
 * The shader sweeps its axis across the whole layer. A leaf hinged [spine]
 * points from the leading edge sweeps only the page past the hinge, so its
 * progress is scaled by that share of the layer and its bow rescaled so the
 * crease is straight again at the end of the turn. Progress clamps to 0..1;
 * [spine] must be positive, and the bow rescale is safe only because the hinge
 * is half the layer, which keeps [shaderProgress] at or below 0.5.
 */
internal data class MekuriHingedSweep(
    val shaderProgress: Float,
    val creaseBow: Float,
) {
    internal companion object {
        fun of(progress: Float, creaseBow: Float, spine: Float, layerWidth: Float): MekuriHingedSweep {
            val clamped = min(max(progress, 0f), 1f)
            val shaderProgress = clamped * (layerWidth - spine) / layerWidth
            return MekuriHingedSweep(
                shaderProgress = shaderProgress,
                creaseBow = creaseBow * (1 - clamped) / (1 - shaderProgress),
            )
        }
    }
}
