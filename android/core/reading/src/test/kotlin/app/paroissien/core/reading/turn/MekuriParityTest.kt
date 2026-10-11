// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every expected value here is a literal shared with `MekuriParityTests.swift`.
 * A number produced by one platform is never taken from the other.
 */
class MekuriParityTest {
    private val geometry = MekuriFoldGeometry(PAGE_WIDTH, MekuriConfiguration.Default)
    private val straightBow = MekuriFoldGeometry(PAGE_WIDTH, MekuriConfiguration(creaseBow = 0f))
    private val straightCrease = MekuriFoldGeometry(PAGE_WIDTH, MekuriConfiguration.StraightCrease)

    private val coverAlone = MekuriSpreadLayout(pageCount = 6, coverStandsAlone = true)
    private val paired = MekuriSpreadLayout(pageCount = 6, coverStandsAlone = false)

    // The fold axis

    @Test
    fun `the straight axis matches the swift implementation`() {
        assertEquals(400f, geometry.foldAxisOffset(0f), TOLERANCE)
        assertEquals(300f, geometry.foldAxisOffset(0.25f), TOLERANCE)
        assertEquals(200f, geometry.foldAxisOffset(0.5f), TOLERANCE)
        assertEquals(100f, geometry.foldAxisOffset(0.75f), TOLERANCE)
        assertEquals(0f, geometry.foldAxisOffset(1f), TOLERANCE)
        assertEquals(400f, geometry.foldAxisOffset(-0.5f), TOLERANCE)
        assertEquals(0f, geometry.foldAxisOffset(1.5f), TOLERANCE)
    }

    @Test
    fun `the bowed crease matches the swift implementation at every row`() {
        fun axis(progress: Float, y: Float) = geometry.foldAxisOffset(progress, y, PAGE_HEIGHT)
        assertEquals(233.75f, axis(0.25f, 0f), TOLERANCE)
        assertEquals(293.4375f, axis(0.25f, 400f), TOLERANCE)
        assertEquals(318.359375f, axis(0.25f, 600f), TOLERANCE)
        assertEquals(340f, axis(0.25f, 800f), TOLERANCE)
        assertEquals(125f, axis(0.5f, 0f), TOLERANCE)
        assertEquals(191.25f, axis(0.5f, 400f), TOLERANCE)
        assertEquals(217.8125f, axis(0.5f, 600f), TOLERANCE)
        assertEquals(240f, axis(0.5f, 800f), TOLERANCE)
        assertEquals(33.75f, axis(0.75f, 0f), TOLERANCE)
        assertEquals(93.4375f, axis(0.75f, 400f), TOLERANCE)
    }

    @Test
    fun `a zero bow leaves shear alone on both platforms`() {
        fun axis(y: Float) = straightBow.foldAxisOffset(0.25f, y, PAGE_HEIGHT)
        assertEquals(260f, axis(0f), TOLERANCE)
        assertEquals(300f, axis(400f), TOLERANCE)
        assertEquals(340f, axis(800f), TOLERANCE)

        fun straight(y: Float) = straightCrease.foldAxisOffset(0.25f, y, PAGE_HEIGHT)
        assertEquals(300f, straight(0f), TOLERANCE)
        assertEquals(300f, straight(600f), TOLERANCE)
        assertEquals(300f, straight(800f), TOLERANCE)
    }

    @Test
    fun `the bow lead matches the swift implementation`() {
        assertEquals(0f, geometry.bowLead(0f), TOLERANCE)
        assertEquals(26.25f, geometry.bowLead(0.25f), TOLERANCE)
        assertEquals(35f, geometry.bowLead(0.5f), TOLERANCE)
        assertEquals(26.25f, geometry.bowLead(0.75f), TOLERANCE)
        assertEquals(0f, geometry.bowLead(1f), TOLERANCE)
        assertEquals(0f, straightBow.bowLead(0.5f), TOLERANCE)
    }

    // The radius profile

    @Test
    fun `the radius profile matches the swift implementation at both ends`() {
        assertEquals(100f, geometry.heldRadius, TOLERANCE)
        assertEquals(0.0625f, geometry.radiusSlope, TOLERANCE)
        assertEquals(100f, geometry.radius(0f), TOLERANCE)
        assertEquals(125f, geometry.radius(400f), TOLERANCE)
        assertEquals(150f, geometry.radius(800f), TOLERANCE)
        assertEquals(0f, straightCrease.radiusSlope, TOLERANCE)
        assertEquals(100f, straightCrease.radius(800f), TOLERANCE)
    }

    @Test
    fun `the landing scale matches the swift implementation`() {
        assertEquals(1f, geometry.landingRadiusScale(0f), TOLERANCE)
        assertEquals(1f, geometry.landingRadiusScale(0.7f), TOLERANCE)
        assertEquals(0.5f, geometry.landingRadiusScale(0.85f), TOLERANCE)
        assertEquals(0.01f, geometry.landingRadiusScale(1f), TOLERANCE)
    }

    @Test
    fun `the crease shadow band matches the swift implementation`() {
        val rect = geometry.creaseShadowRect(0.25f, Size(PAGE_WIDTH, PAGE_HEIGHT))
        assertEquals(280f, rect.left, TOLERANCE)
        assertEquals(320f, rect.right, TOLERANCE)
        assertEquals(0f, rect.top, TOLERANCE)
        assertEquals(800f, rect.bottom, TOLERANCE)
    }

    // The spread rule

    @Test
    fun `the spread floor matches the swift implementation`() {
        assertEquals(270f, MekuriSpread.MinimumDoublePageWidth, TOLERANCE)
        assertTrue(MekuriSpread.Automatic.isDouble(Size(1200f, 540f), 0.5f))
        assertTrue(MekuriSpread.Automatic.isDouble(Size(1200f, 542f), 0.5f))
        assertFalse(MekuriSpread.Automatic.isDouble(Size(1200f, 538f), 0.5f))
        assertFalse(MekuriSpread.Automatic.isDouble(Size(2000f, 500f), 0.5f))
        assertTrue(MekuriSpread.Automatic.isDouble(Size(956f, 440f), 0.7f))
        assertFalse(MekuriSpread.Automatic.isDouble(Size(440f, 956f), 0.7f))
        assertFalse(MekuriSpread.Automatic.isDouble(Size(1366f, 1024f), 0.25f))
    }

    @Test
    fun `the spread fill fraction matches the swift implementation`() {
        assertEquals(0.6f, MekuriSpread.MinimumDoubleFillFraction, TOLERANCE)
        assertFalse(MekuriSpread.Automatic.isDouble(Size(590f, 1000f), 0.5f))
        assertTrue(MekuriSpread.Automatic.isDouble(Size(610f, 1000f), 0.5f))
        assertTrue(MekuriSpread.Automatic.isDouble(Size(1366f, 1024f), 0.7f))
        assertFalse(MekuriSpread.Automatic.isDouble(Size(1024f, 1366f), 0.7f))
    }

    @Test
    fun `the spread page size matches the swift implementation`() {
        val heightLimited = MekuriSpread.pageSize(Size(1200f, 600f), 2f / 3f)
        assertEquals(400f, heightLimited.width, TOLERANCE)
        assertEquals(600f, heightLimited.height, TOLERANCE)

        val widthLimited = MekuriSpread.pageSize(Size(900f, 800f), 2f / 3f)
        assertEquals(450f, widthLimited.width, TOLERANCE)
        assertEquals(675f, widthLimited.height, TOLERANCE)
    }

    // The six-page matrix

    @Test
    fun `six pages pair the same way on both platforms`() {
        assertEquals(4, coverAlone.spreadCount)
        assertEquals(MekuriSpreadPages(null, 0), coverAlone.pages(0))
        assertEquals(MekuriSpreadPages(1, 2), coverAlone.pages(1))
        assertEquals(MekuriSpreadPages(3, 4), coverAlone.pages(2))
        assertEquals(MekuriSpreadPages(5, null), coverAlone.pages(3))
        assertEquals(0, coverAlone.spreadIndex(0))
        assertEquals(2, coverAlone.spreadIndex(4))

        assertEquals(3, paired.spreadCount)
        assertEquals(MekuriSpreadPages(0, 1), paired.pages(0))
        assertEquals(MekuriSpreadPages(2, 3), paired.pages(1))
        assertEquals(MekuriSpreadPages(4, 5), paired.pages(2))
        assertEquals(2, paired.spreadIndex(4))
    }

    @Test
    fun `the leaf table matches the swift implementation with a cover alone`() {
        assertEquals(
            MekuriLeaf(front = 0, back = 1, revealed = 2, landsIn = MekuriSlot.Leading),
            coverAlone.leaf(MekuriTurn.Forward, 0),
        )
        assertEquals(
            MekuriLeaf(front = 2, back = 3, revealed = 4, landsIn = MekuriSlot.Leading),
            coverAlone.leaf(MekuriTurn.Forward, 1),
        )
        assertEquals(
            MekuriLeaf(front = 4, back = 5, revealed = null, landsIn = MekuriSlot.Leading),
            coverAlone.leaf(MekuriTurn.Forward, 2),
        )
        assertNull(coverAlone.leaf(MekuriTurn.Forward, 3))
        assertNull(coverAlone.leaf(MekuriTurn.Backward, 0))
        assertEquals(
            MekuriLeaf(front = 1, back = 0, revealed = null, landsIn = MekuriSlot.Trailing),
            coverAlone.leaf(MekuriTurn.Backward, 1),
        )
        assertEquals(
            MekuriLeaf(front = 3, back = 2, revealed = 1, landsIn = MekuriSlot.Trailing),
            coverAlone.leaf(MekuriTurn.Backward, 2),
        )
    }

    @Test
    fun `the leaf table matches the swift implementation when pages pair from zero`() {
        assertEquals(
            MekuriLeaf(front = 1, back = 2, revealed = 3, landsIn = MekuriSlot.Leading),
            paired.leaf(MekuriTurn.Forward, 0),
        )
        assertNull(paired.leaf(MekuriTurn.Forward, 2))
        assertEquals(
            MekuriLeaf(front = 2, back = 1, revealed = 0, landsIn = MekuriSlot.Trailing),
            paired.leaf(MekuriTurn.Backward, 1),
        )
        assertNull(paired.leaf(MekuriTurn.Backward, 0))
    }

    // The turn and the selection

    @Test
    fun `the turn arithmetic matches the swift implementation`() {
        assertEquals(1, MekuriTurn.Forward.targetIndex(0, 6))
        assertNull(MekuriTurn.Forward.targetIndex(5, 6))
        assertEquals(4, MekuriTurn.Backward.targetIndex(5, 6))
        assertNull(MekuriTurn.Backward.targetIndex(0, 6))
        assertEquals(MekuriTurn.Forward, MekuriTurn.from(MekuriZone.Trailing, MekuriDirection.LeftToRight))
        assertEquals(MekuriTurn.Backward, MekuriTurn.from(MekuriZone.Leading, MekuriDirection.LeftToRight))
        assertEquals(MekuriTurn.Backward, MekuriTurn.from(MekuriZone.Trailing, MekuriDirection.RightToLeft))
        assertEquals(MekuriTurn.Forward, MekuriTurn.from(MekuriZone.Leading, MekuriDirection.RightToLeft))
        assertNull(MekuriTurn.from(MekuriZone.Center, MekuriDirection.LeftToRight))
    }

    @Test
    fun `the selection rules match the swift implementation`() {
        assertEquals(1, coverAlone.selection(MekuriTurn.Forward, 0))
        assertEquals(3, coverAlone.selection(MekuriTurn.Forward, 1))
        assertEquals(5, coverAlone.selection(MekuriTurn.Forward, 2))
        assertNull(coverAlone.selection(MekuriTurn.Forward, 3))
        assertEquals(0, coverAlone.selection(MekuriTurn.Backward, 1))
        assertEquals(1, coverAlone.selection(MekuriTurn.Backward, 2))
        assertNull(coverAlone.selection(MekuriTurn.Backward, 0))
        assertEquals(2, paired.selection(MekuriTurn.Forward, 0))
        assertEquals(0, paired.selection(MekuriTurn.Backward, 1))
    }

    @Test
    fun `a selection change chooses the same transition on both platforms`() {
        assertEquals(MekuriTransition.None, MekuriTransition.between(2, 2))
        assertEquals(MekuriTransition.Curl(MekuriTurn.Forward), MekuriTransition.between(2, 3))
        assertEquals(MekuriTransition.Curl(MekuriTurn.Backward), MekuriTransition.between(2, 1))
        assertEquals(MekuriTransition.Crossfade, MekuriTransition.between(2, 5))
        assertEquals(MekuriTransition.None, MekuriTransition.between(1, 2, coverAlone))
        assertEquals(MekuriTransition.Curl(MekuriTurn.Forward), MekuriTransition.between(0, 1, coverAlone))
        assertEquals(MekuriTransition.Crossfade, MekuriTransition.between(4, 0, coverAlone))
        assertEquals(MekuriTransition.None, MekuriTransition.between(0, 1, paired))
        assertEquals(MekuriTransition.Curl(MekuriTurn.Forward), MekuriTransition.between(1, 2, paired))
    }

    @Test
    fun `a leaf begins with the same faces on both platforms`() {
        val single = MekuriTurnState.begin(0, MekuriTurn.Forward, from = 2, pageCount = 6)
        assertEquals(3, single.targetIndex)
        assertNull(single.leading)
        assertEquals(3, single.trailing)
        assertEquals(2, single.leafFront)
        assertNull(single.leafBack)

        val blocked = MekuriTurnState.begin(0, MekuriTurn.Forward, from = 5, pageCount = 6)
        assertNull(blocked.targetIndex)
        assertNull(blocked.trailing)
        assertEquals(5, blocked.leafFront)

        val opening = MekuriTurnState.begin(0, MekuriTurn.Forward, from = 0, layout = coverAlone)
        assertEquals(1, opening.targetIndex)
        assertNull(opening.leading)
        assertEquals(2, opening.trailing)
        assertEquals(0, opening.leafFront)
        assertEquals(1, opening.leafBack)

        val back = MekuriTurnState.begin(0, MekuriTurn.Backward, from = 3, layout = coverAlone)
        assertEquals(1, back.targetIndex)
        assertEquals(1, back.leading)
        assertEquals(4, back.trailing)
        assertEquals(2, back.leafFront)
        assertEquals(3, back.leafBack)

        val end = MekuriTurnState.begin(0, MekuriTurn.Forward, from = 5, layout = coverAlone)
        assertNull(end.targetIndex)
        assertEquals(5, end.leading)
        assertNull(end.trailing)
        assertFalse(end.hasLeaf)
    }

    @Test
    fun `the fold progress of a backward turn matches the swift implementation`() {
        val forward = MekuriTurnState.begin(0, MekuriTurn.Forward, from = 2, pageCount = 6)
        assertEquals(0.25f, forward.copy(progress = 0.25f).foldProgress, TOLERANCE)

        val backward = MekuriTurnState.begin(0, MekuriTurn.Backward, from = 2, pageCount = 6)
        assertEquals(0.75f, backward.copy(progress = 0.25f).foldProgress, TOLERANCE)
    }

    // The shift, the sweep and the pass

    @Test
    fun `the spread shift matches the swift implementation`() {
        assertEquals(
            -200f,
            MekuriSpreadShift.atRest(null, 0, 400f, MekuriDirection.LeftToRight),
            TOLERANCE,
        )
        assertEquals(
            200f,
            MekuriSpreadShift.atRest(null, 0, 400f, MekuriDirection.RightToLeft),
            TOLERANCE,
        )
        assertEquals(
            200f,
            MekuriSpreadShift.atRest(5, null, 400f, MekuriDirection.LeftToRight),
            TOLERANCE,
        )
        assertEquals(
            0f,
            MekuriSpreadShift.atRest(1, 2, 400f, MekuriDirection.LeftToRight),
            TOLERANCE,
        )

        val turn = MekuriTurnState.begin(0, MekuriTurn.Forward, from = 0, layout = coverAlone)
        val span = MekuriSpreadShift.span(turn, coverAlone, 400f, MekuriDirection.LeftToRight)
        assertEquals(-200f, span.start, TOLERANCE)
        assertEquals(0f, span.end, TOLERANCE)
        assertEquals(-100f, span.value(0.5f), TOLERANCE)
        assertEquals(0f, span.value(1.5f), TOLERANCE)
    }

    @Test
    fun `the hinged sweep matches the swift implementation`() {
        val half = MekuriHingedSweep.of(0.5f, 0.35f, spine = 400f, layerWidth = 800f)
        assertEquals(0.25f, half.shaderProgress, TOLERANCE)
        assertEquals(0.2333333f, half.creaseBow, TOLERANCE)

        val rest = MekuriHingedSweep.of(0f, 0.35f, spine = 400f, layerWidth = 800f)
        assertEquals(0f, rest.shaderProgress, TOLERANCE)
        assertEquals(0.35f, rest.creaseBow, TOLERANCE)

        val landed = MekuriHingedSweep.of(1f, 0.35f, spine = 400f, layerWidth = 800f)
        assertEquals(0.5f, landed.shaderProgress, TOLERANCE)
        assertEquals(0f, landed.creaseBow, TOLERANCE)
    }

    @Test
    fun `the fold pass matches the swift implementation`() {
        val forward = MekuriFoldPass(MekuriDirection.LeftToRight, 0.4f)
        assertFalse(forward.isMirrored)
        assertEquals(0.4f, forward.shaderProgress, TOLERANCE)
        assertEquals(1f, forward.mirrorScale, TOLERANCE)

        val mirrored = MekuriFoldPass(MekuriDirection.RightToLeft, 0.4f)
        assertTrue(mirrored.isMirrored)
        assertEquals(0.4f, mirrored.shaderProgress, TOLERANCE)
        assertEquals(-1f, mirrored.mirrorScale, TOLERANCE)
        assertEquals(1f, mirrored.verticalScale, TOLERANCE)

        val bottom = MekuriFoldPass(MekuriDirection.RightToLeft, 0.4f, liftsFromBottom = true)
        assertEquals(0.4f, bottom.shaderProgress, TOLERANCE)
        assertEquals(-1f, bottom.mirrorScale, TOLERANCE)
        assertEquals(-1f, bottom.verticalScale, TOLERANCE)
    }

    // The drag and the release

    @Test
    fun `the drag arithmetic matches the swift implementation`() {
        assertEquals(
            -1f,
            MekuriDrag.axis(MekuriTurn.Forward, MekuriDirection.LeftToRight),
            TOLERANCE,
        )
        assertEquals(
            1f,
            MekuriDrag.axis(MekuriTurn.Backward, MekuriDirection.LeftToRight),
            TOLERANCE,
        )
        assertEquals(
            1f,
            MekuriDrag.axis(MekuriTurn.Forward, MekuriDirection.RightToLeft),
            TOLERANCE,
        )
        val track = MekuriEdgeTrack(MekuriTurn.Forward, geometry, 800f, 400f, 402f)
        val back = MekuriEdgeTrack(MekuriTurn.Backward, geometry, 800f, 400f, 402f)
        assertEquals(0.61062f, MekuriDrag.progress(0f, -140f, -1f, isBlocked = false, track = track), TOLERANCE)
        assertEquals(0.40049f, MekuriDrag.progress(0f, -140f, -1f, isBlocked = true, track = track), TOLERANCE)
        assertEquals(0.5049f, MekuriDrag.progress(0.2f, -80f, -1f, isBlocked = false, track = track), TOLERANCE)
        assertEquals(1f, MekuriDrag.progress(0.9f, -2000f, -1f, isBlocked = false, track = track), TOLERANCE)
        assertEquals(0f, MekuriDrag.progress(0.1f, 80f, -1f, isBlocked = false, track = track), TOLERANCE)
        assertEquals(0.07042f, MekuriDrag.progress(0f, 140f, 1f, isBlocked = false, track = back), TOLERANCE)
        assertEquals(0.35f, track.releaseProgress(track.progress(140.7f)), TOLERANCE)
        assertEquals(900f, MekuriDrag.projectedVelocity(-900f, -1f), TOLERANCE)
    }

    @Test
    fun `the free edge matches the swift implementation`() {
        assertEquals(400f, geometry.freeEdge(0f, 400f, 800f), TOLERANCE)
        assertEquals(269.8602f, geometry.freeEdge(0.5f, 0f, 800f), TOLERANCE)
        assertEquals(315.6354f, geometry.freeEdge(0.5f, 400f, 800f), TOLERANCE)
        assertEquals(339.9574f, geometry.freeEdge(0.5f, 800f, 800f), TOLERANCE)
        assertEquals(-396.073f, geometry.freeEdge(1f, 400f, 800f), TOLERANCE)
        val track = MekuriEdgeTrack(MekuriTurn.Forward, geometry, 800f, 400f, 400f)
        assertEquals(0.3f, track.progress(track.travel(0.3f)), TOLERANCE)
        val sliding = MekuriEdgeTrack(MekuriTurn.Forward, geometry, 800f, 400f, 800f, stackTravel = -200f)
        assertEquals(track.travel(0.6f) - 120f, sliding.travel(0.6f), TOLERANCE)
    }

    @Test
    fun `the lifting corner matches the swift implementation`() {
        assertFalse(MekuriDrag.liftsFromBottom(0f, 800f))
        assertFalse(MekuriDrag.liftsFromBottom(399f, 800f))
        assertFalse(MekuriDrag.liftsFromBottom(400f, 800f))
        assertTrue(MekuriDrag.liftsFromBottom(401f, 800f))
        assertTrue(MekuriDrag.liftsFromBottom(800f, 800f))
    }

    @Test
    fun `the drag reach matches the swift implementation`() {
        val single = MekuriArrangement.Single(pageCount = 6)
        val spread = MekuriArrangement.Spread(
            MekuriSpreadLayout(pageCount = 6, coverStandsAlone = true),
            androidx.compose.ui.geometry.Size(556f, 834f),
        )
        assertEquals(402f, single.turnWidth(402f), TOLERANCE)
        assertEquals(1112f, spread.turnWidth(1194f), TOLERANCE)
        val grabbed = single.edgeTrack(
            MekuriTurn.Forward,
            androidx.compose.ui.geometry.Size(402f, 874f),
            grabY = 700f,
            liftsFromBottom = true,
            configuration = MekuriConfiguration.Default,
        )
        assertEquals(174f, grabbed.row, TOLERANCE)
        assertEquals(402f, grabbed.reach, TOLERANCE)
        val slot = spread.edgeTrack(
            MekuriTurn.Forward,
            androidx.compose.ui.geometry.Size(1194f, 900f),
            grabY = 20f,
            liftsFromBottom = false,
            configuration = MekuriConfiguration.Default,
        )
        assertEquals(0f, slot.row, TOLERANCE)
        assertEquals(1112f, slot.reach, TOLERANCE)
    }

    @Test
    fun `the release decision matches the swift implementation`() {
        val configuration = MekuriConfiguration.Default
        assertEquals(MekuriTurnDecision.Commit, MekuriTurnDecision.resolve(0.35f, 0f, configuration))
        assertEquals(MekuriTurnDecision.Revert, MekuriTurnDecision.resolve(0.34f, 0f, configuration))
        assertEquals(MekuriTurnDecision.Commit, MekuriTurnDecision.resolve(0.1f, 600f, configuration))
        assertEquals(MekuriTurnDecision.Revert, MekuriTurnDecision.resolve(0.1f, -900f, configuration))
    }

    @Test
    fun `the tap zones match the swift implementation`() {
        val configuration = MekuriConfiguration.Default
        assertEquals(MekuriZone.Leading, MekuriZone.resolve(0f, 400f, configuration))
        assertEquals(MekuriZone.Leading, MekuriZone.resolve(99f, 400f, configuration))
        assertEquals(MekuriZone.Center, MekuriZone.resolve(100f, 400f, configuration))
        assertEquals(MekuriZone.Center, MekuriZone.resolve(300f, 400f, configuration))
        assertEquals(MekuriZone.Trailing, MekuriZone.resolve(301f, 400f, configuration))
    }

    private companion object {
        const val PAGE_WIDTH = 400f
        const val PAGE_HEIGHT = 800f

        /**
         * Absolute tolerance at page scale. Single precision at a page width of
         * 400 carries about 3e-5 per unit in the last place.
         */
        const val TOLERANCE = 1e-3f
    }
}
