// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.ui.geometry.Size

/**
 * The fold shader's arguments for one pass. Built per frame; the shader object
 * itself is built once. A hinged leaf reaches the shader entirely through these
 * values, never through a new term in the shader.
 */
internal data class MekuriFoldUniforms(
    val size: Size,
    val progress: Float,
    val heldRadius: Float,
    val radiusSlope: Float,
    val shear: Float,
    val bow: Float,
    val backFaceDim: Float,
    val shadowWidth: Float,
    val shadowOpacity: Float,
    val face: Float,
) {
    internal companion object {
        /** A leaf spans a two-slot spread and hinges at its centre. */
        fun hinge(size: Size): Float = size.width / 2f

        /**
         * [hinge] is the distance from the layer's leading edge to the spine a
         * leaf turns on, null for a page folded across the whole layer. Every
         * roll and corner shear flatten as the turn lands, so no roll stands at
         * the leading edge and a leaf's crease meets the hinge.
         */
        fun of(
            size: Size,
            progress: Float,
            configuration: MekuriConfiguration,
            face: MekuriFace,
            hinge: Float? = null,
        ): MekuriFoldUniforms {
            val sweep = hinge?.let {
                MekuriHingedSweep.of(
                    progress = progress,
                    creaseBow = configuration.creaseBow,
                    spine = it,
                    layerWidth = size.width,
                )
            }
            val geometry = MekuriFoldGeometry(size.width - (hinge ?: 0f), configuration)
            val shadow = geometry.creaseShadowRect(progress, size)
            val landing = geometry.landingRadiusScale(progress)
            return MekuriFoldUniforms(
                size = size,
                progress = sweep?.shaderProgress ?: progress,
                heldRadius = geometry.heldRadius * landing,
                radiusSlope = geometry.radiusSlope * landing,
                shear = configuration.cornerShear * landing,
                bow = sweep?.creaseBow ?: configuration.creaseBow,
                backFaceDim = configuration.backFaceDim,
                shadowWidth = shadow.width,
                shadowOpacity = configuration.creaseShadowOpacity,
                face = face.raw,
            )
        }
    }
}
