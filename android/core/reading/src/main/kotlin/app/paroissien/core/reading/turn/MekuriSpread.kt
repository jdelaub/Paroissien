// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.ui.geometry.Size
import kotlin.math.min

/**
 * Whether one or two pages share the container. [Automatic] shows two when the
 * pair, laid out to fit, stays readable and fills the container.
 */
public enum class MekuriSpread {
    /**
     * Two pages when the fitted page is at least [MinimumDoublePageWidth] wide
     * and the pair covers at least [MinimumDoubleFillFraction] of the
     * container's height; otherwise one.
     */
    Automatic,

    /** One page across the container, whatever its size. */
    Single,

    /** Two pages side by side, scaled to fit the container. */
    Double,

    ;

    /**
     * `pageAspectRatio` is width over height. Under [Automatic], the pages are
     * fitted by [pageSize] and the fitted page must be at least
     * [MinimumDoublePageWidth] wide and at least [MinimumDoubleFillFraction] of
     * the container's height tall.
     */
    internal fun isDouble(containerSize: Size, pageAspectRatio: Float): Boolean = when (this) {
        Single -> false
        Double -> true
        Automatic -> {
            val page = pageSize(containerSize, pageAspectRatio)
            page.width >= MinimumDoublePageWidth &&
                page.height >= containerSize.height * MinimumDoubleFillFraction
        }
    }

    internal companion object {
        /** Narrowest fitted page, in dp, at which two pages stay readable. */
        internal const val MinimumDoublePageWidth = 270f

        /** Smallest share of the container's height a fitted pair may cover. */
        internal const val MinimumDoubleFillFraction = 0.6f

        /**
         * Largest page at `pageAspectRatio` such that two of them, side by side,
         * fit the container. `pageAspectRatio` must be positive.
         */
        internal fun pageSize(containerSize: Size, pageAspectRatio: Float): Size {
            val height = min(containerSize.height, containerSize.width / (2 * pageAspectRatio))
            return Size(width = height * pageAspectRatio, height = height)
        }
    }
}
