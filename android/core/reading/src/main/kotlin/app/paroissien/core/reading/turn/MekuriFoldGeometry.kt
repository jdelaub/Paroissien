// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Pure fold arithmetic in page space. Progress runs 0 (flat) to 1 (turned); the
 * fold axis travels from the trailing edge to the leading edge. Fold distance is
 * measured from the held end, `y == pageHeight`, toward the free corner at
 * `y == 0`.
 *
 * These formulas are mirrored in the fold shader and are held in numeric parity
 * with the iOS geometry.
 */
internal data class MekuriFoldGeometry(
    val pageWidth: Float,
    val configuration: MekuriConfiguration = MekuriConfiguration.Default,
) {
    /** Cylinder radius at the held end. */
    val heldRadius: Float
        get() = pageWidth * configuration.cylinderRadiusRatio

    /** Radius growth per unit of fold distance. */
    val radiusSlope: Float
        get() = configuration.cylinderRadiusRatio * configuration.radiusOpening

    /**
     * Horizontal position of the fold axis at the page's vertical centre with no
     * bow. Out-of-range progress clamps to 0..1.
     */
    fun foldAxisOffset(progress: Float): Float = pageWidth * (1 - clamped(progress))

    /** Horizontal position of the crease at [y], including shear and bow. */
    fun foldAxisOffset(progress: Float, y: Float, pageHeight: Float): Float {
        val held = 1 - y / pageHeight
        val shear = configuration.cornerShear * (y - pageHeight / 2)
        return foldAxisOffset(progress) + shear - bowLead(progress) * held * held
    }

    /** Distance the free corner runs ahead of the straight crease. */
    fun bowLead(progress: Float): Float {
        val clamped = clamped(progress)
        return configuration.creaseBow * pageWidth * clamped * (1 - clamped)
    }

    /** Cylinder radius at [foldDistance] along the fold from the held end. */
    fun radius(foldDistance: Float): Float = heldRadius + radiusSlope * foldDistance

    fun isFlat(progress: Float): Boolean = progress <= 0f

    /**
     * Scale on the cylinder radius and corner shear: 1 until the landing
     * begins, [LandingFloor] at progress 1.
     */
    fun landingRadiusScale(progress: Float): Float {
        val remaining = 1 - clamped(progress)
        return max(min(remaining / LandingFraction, 1f), LandingFloor)
    }

    /** Full-height band centred on the fold axis. */
    fun creaseShadowRect(progress: Float, pageSize: Size): Rect {
        val width = pageWidth * configuration.creaseShadowWidthRatio
        val axis = foldAxisOffset(progress)
        return Rect(
            left = axis - width / 2,
            top = 0f,
            right = axis + width / 2,
            bottom = pageSize.height,
        )
    }

    /**
     * Horizontal position of the sheet's free edge at row [y] of the shader's
     * frame, as the shader draws it: on the roll while the flap is shorter than
     * half a turn, lying flat past the crest beyond. Radius and shear land with
     * the turn.
     */
    fun freeEdge(progress: Float, y: Float, pageHeight: Float): Float {
        val landing = landingRadiusScale(progress)
        val held = 1 - y / pageHeight
        val shear = configuration.cornerShear * landing * (y - pageHeight / 2)
        val axis = foldAxisOffset(progress) + shear - bowLead(progress) * held * held
        val flap = pageWidth - axis
        val radius = radius(pageHeight - y) * landing
        val halfTurn = PI.toFloat() * radius
        return if (flap <= halfTurn) axis + radius * sin(flap / radius) else axis - (flap - halfTurn)
    }

    private fun clamped(progress: Float): Float = min(max(progress, 0f), 1f)

    companion object {
        /** Share of the turn over which the roll flattens. */
        const val LandingFraction = 0.3f

        /** Smallest radius scale; the contact shadow divides by the radius. */
        const val LandingFloor = 0.01f
    }
}
