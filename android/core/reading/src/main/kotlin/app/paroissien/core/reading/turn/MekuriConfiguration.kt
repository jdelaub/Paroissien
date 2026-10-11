// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

/**
 * Fold and gesture tuning shared by every page turn.
 *
 * The public constructor carries the tuning knobs; every other member keeps its
 * tuned value and is not reachable from outside the module. Every numeric
 * member is held in parity with the iOS type. [settleAnimation] is not.
 */
public class MekuriConfiguration internal constructor(
    internal val cylinderRadiusRatio: Float,
    internal val radiusOpening: Float,
    internal val creaseBow: Float,
    internal val cornerShear: Float,
    internal val backFaceDim: Float,
    internal val creaseShadowWidthRatio: Float,
    internal val creaseShadowOpacity: Float,
    internal val snapThreshold: Float,
    internal val flingVelocity: Float,
    internal val tapZoneRatio: Float,
    internal val settleAnimation: AnimationSpec<Float>,
    internal val reducedMotionOverride: Boolean?,
) {
    /**
     * @param foldRadius cylinder radius at the held end of the fold as a ratio
     *   of the page width.
     * @param cornerLift horizontal travel of the fold line per unit of vertical
     *   distance from the page centre. A ratio, not an angle.
     * @param creaseBow how far the free corner runs ahead of a straight crease,
     *   0..1. The crease is straight again at both ends of the turn.
     * @param tapZone width of each tap-to-turn edge zone as a ratio of the
     *   container width.
     * @param snapThreshold share of the finger's range, 0..1, a drag must cover
     *   before its release completes the turn.
     * @param settleAnimation animation used to settle a released turn.
     * @param reducedMotionOverride overrides the system reduce-motion setting
     *   when non-null.
     */
    public constructor(
        foldRadius: Float = DEFAULT_CYLINDER_RADIUS_RATIO,
        cornerLift: Float = DEFAULT_CORNER_SHEAR,
        creaseBow: Float = DEFAULT_CREASE_BOW,
        tapZone: Float = DEFAULT_TAP_ZONE_RATIO,
        snapThreshold: Float = DEFAULT_SNAP_THRESHOLD,
        settleAnimation: AnimationSpec<Float> = DefaultSettleAnimation,
        reducedMotionOverride: Boolean? = null,
    ) : this(
        cylinderRadiusRatio = foldRadius,
        radiusOpening = DEFAULT_RADIUS_OPENING,
        creaseBow = creaseBow,
        cornerShear = cornerLift,
        backFaceDim = DEFAULT_BACK_FACE_DIM,
        creaseShadowWidthRatio = DEFAULT_CREASE_SHADOW_WIDTH_RATIO,
        creaseShadowOpacity = DEFAULT_CREASE_SHADOW_OPACITY,
        snapThreshold = snapThreshold,
        flingVelocity = DEFAULT_FLING_VELOCITY,
        tapZoneRatio = tapZone,
        settleAnimation = settleAnimation,
        reducedMotionOverride = reducedMotionOverride,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MekuriConfiguration) return false
        return this.cylinderRadiusRatio.equals(other.cylinderRadiusRatio) &&
            this.radiusOpening.equals(other.radiusOpening) &&
            this.creaseBow.equals(other.creaseBow) &&
            this.cornerShear.equals(other.cornerShear) &&
            this.backFaceDim.equals(other.backFaceDim) &&
            this.creaseShadowWidthRatio.equals(other.creaseShadowWidthRatio) &&
            this.creaseShadowOpacity.equals(other.creaseShadowOpacity) &&
            this.snapThreshold.equals(other.snapThreshold) &&
            this.flingVelocity.equals(other.flingVelocity) &&
            this.tapZoneRatio.equals(other.tapZoneRatio) &&
            this.settleAnimation == other.settleAnimation &&
            this.reducedMotionOverride == other.reducedMotionOverride
    }

    override fun hashCode(): Int {
        var result = this.cylinderRadiusRatio.hashCode()
        result = 31 * result + this.radiusOpening.hashCode()
        result = 31 * result + this.creaseBow.hashCode()
        result = 31 * result + this.cornerShear.hashCode()
        result = 31 * result + this.backFaceDim.hashCode()
        result = 31 * result + this.creaseShadowWidthRatio.hashCode()
        result = 31 * result + this.creaseShadowOpacity.hashCode()
        result = 31 * result + this.snapThreshold.hashCode()
        result = 31 * result + this.flingVelocity.hashCode()
        result = 31 * result + this.tapZoneRatio.hashCode()
        result = 31 * result + this.settleAnimation.hashCode()
        result = 31 * result + this.reducedMotionOverride.hashCode()
        return result
    }

    internal companion object {
        private const val DEFAULT_CYLINDER_RADIUS_RATIO = 0.25f
        private const val DEFAULT_RADIUS_OPENING = 0.25f
        private const val DEFAULT_CREASE_BOW = 0.35f
        private const val DEFAULT_CORNER_SHEAR = 0.10f
        private const val DEFAULT_BACK_FACE_DIM = 0.86f
        private const val DEFAULT_CREASE_SHADOW_WIDTH_RATIO = 0.10f
        private const val DEFAULT_CREASE_SHADOW_OPACITY = 0.35f
        private const val DEFAULT_SNAP_THRESHOLD = 0.35f
        private const val DEFAULT_FLING_VELOCITY = 600f
        private const val DEFAULT_TAP_ZONE_RATIO = 0.25f

        /** Matches the iOS settle curve by value; not asserted in parity. */
        internal val DefaultSettleAnimation: AnimationSpec<Float> = tween(
            durationMillis = 420,
            easing = CubicBezierEasing(0.35f, 0.1f, 0.75f, 0.85f),
        )

        internal val Default: MekuriConfiguration = MekuriConfiguration()

        /** A vertical straight crease with a constant radius. */
        internal val StraightCrease: MekuriConfiguration = MekuriConfiguration(
            cylinderRadiusRatio = DEFAULT_CYLINDER_RADIUS_RATIO,
            radiusOpening = 0f,
            creaseBow = 0f,
            cornerShear = 0f,
            backFaceDim = DEFAULT_BACK_FACE_DIM,
            creaseShadowWidthRatio = DEFAULT_CREASE_SHADOW_WIDTH_RATIO,
            creaseShadowOpacity = DEFAULT_CREASE_SHADOW_OPACITY,
            snapThreshold = DEFAULT_SNAP_THRESHOLD,
            flingVelocity = DEFAULT_FLING_VELOCITY,
            tapZoneRatio = DEFAULT_TAP_ZONE_RATIO,
            settleAnimation = DefaultSettleAnimation,
            reducedMotionOverride = null,
        )
    }
}
