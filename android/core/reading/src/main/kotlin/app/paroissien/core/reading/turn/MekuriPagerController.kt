// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The turn in flight and everything that moves it. Every length here is in dp,
 * matching the tuning constants; the pager converts once, at the pointer and at
 * the placement.
 *
 * [progress] is the presented progress of the turn, 0 (nothing turned) to 1, in
 * the turn's own direction. It is written every frame, so it must be read inside
 * a layer or draw block and never in composition.
 */
internal class MekuriPagerController(
    val state: MekuriPagerState,
    private val scope: CoroutineScope,
) : MekuriTurnDriver {
    /** Written by the pager each composition; never read from composition. */
    var arrangement: MekuriArrangement = MekuriArrangement.Single(pageCount = 0)

    var containerSize: Size = Size.Zero

    var configuration: MekuriConfiguration = MekuriConfiguration.Default

    var reducesMotion: Boolean = false

    /** Plays a cue; written by the pager each composition. */
    var onHaptic: (MekuriHaptic) -> Unit = {}

    /** Paroissien : un tour demandé au-delà de la première ou de la dernière page (passage à l'unité voisine). */
    var onBlockedTurn: (MekuriTurn) -> Unit = {}

    /** Presented progress of the turn in flight. */
    var progress: Float by mutableFloatStateOf(0f)

    var turn: MekuriTurnState? by mutableStateOf(null)
        private set

    /** Page fading out over the new one; null unless a crossfade is running. */
    var crossfadeFrom: Int? by mutableStateOf(null)
        private set

    val crossfadeAlpha: Animatable<Float, AnimationVector1D> = Animatable(0f)

    private val settleProgress = Animatable(0f)

    private var settleJob: Job? = null

    private var pending: CompletableDeferred<Int>? = null

    private var nextTurnId = 0

    private var ignoresCurrentDrag = false

    /**
     * Paroissien : un geste vers une page qui n'existe pas et sans feuillet à soulever (revenir depuis la première
     * page) ne commence aucun tour ; on le retient pour ouvrir l'unité voisine s'il est franc.
     */
    private var leaflessTurn: MekuriTurn? = null

    val direction: MekuriDirection get() = this.state.direction

    val settledPage: Int get() = this.state.currentPage

    /** A turn no finger started tracks the page's midline. */
    private fun edgeTrack(turn: MekuriTurnState): MekuriEdgeTrack {
        val span = this.arrangement.shiftSpan(this.settledPage, turn, this.direction)
        return this.arrangement.edgeTrack(
            turn = turn.turn,
            containerSize = this.containerSize,
            grabY = turn.grabY ?: (this.containerSize.height / 2f),
            liftsFromBottom = turn.liftsFromBottom,
            configuration = this.configuration,
            stackTravel = MekuriDrag.axis(turn.turn, this.direction) * (span.end - span.start),
        )
    }

    fun foldProgress(): Float = this.turn?.turn?.fold(this.progress) ?: 0f

    // Taps and programmatic turns

    /** [x] and [y] are in dp; a tap in the lower half lifts the bottom corner. */
    fun tapped(x: Float, y: Float, onCenterTap: (() -> Unit)?, pagingEnabled: Boolean) {
        val zone = MekuriZone.resolve(x, this.containerSize.width, this.configuration)
        val turn = MekuriTurn.from(zone, this.direction)
        if (turn == null) {
            onCenterTap?.invoke()
            return
        }
        if (!pagingEnabled) return
        this.perform(turn, MekuriDrag.liftsFromBottom(y, this.containerSize.height), grabY = y)
    }

    /** Does nothing at the ends or while another turn is in flight. */
    fun perform(turn: MekuriTurn, liftsFromBottom: Boolean = false, grabY: Float? = null) {
        if (this.turn != null) return
        val begun = this.begin(turn).copy(liftsFromBottom = liftsFromBottom, grabY = grabY, playsHaptics = true)
        val target = begun.targetIndex ?: return this.onBlockedTurn(turn)
        if (this.reducesMotion) {
            this.state.currentPage = target
            this.onHaptic(MekuriHaptic.Land)
        } else {
            this.arm(begun, MekuriTurnDecision.Commit)
            this.onHaptic(MekuriHaptic.Lift)
        }
    }

    // The driver the pager state calls

    /**
     * A curl driven from outside lands on the page the caller asked for, not on
     * the landing spread's leading page.
     */
    override suspend fun turnTo(page: Int): Int {
        val from = this.settledPage
        if (page == from) return page
        return when (val transition = this.arrangement.transition(from, page)) {
            MekuriTransition.None -> page
            MekuriTransition.Crossfade -> {
                this.crossfadeTo(page)
                page
            }
            is MekuriTransition.Curl -> this.curlTo(page, transition.turn)
        }
    }

    override fun dropTurn() {
        this.settleJob?.cancel()
        this.settleJob = null
        if (this.turn?.phase == MekuriTurnPhase.Dragging) this.ignoresCurrentDrag = true
        this.turn = null
        this.progress = 0f
        this.pending?.cancel()
        this.pending = null
    }

    /** Cancelling the caller drops the curl; it may not land the page anyway. */
    private suspend fun curlTo(page: Int, turn: MekuriTurn): Int {
        this.dropTurn()
        val begun = this.begin(turn)
        if (this.reducesMotion || begun.isBlocked || !begun.hasLeaf) return page
        val landing = CompletableDeferred<Int>()
        this.pending = landing
        this.arm(begun.copy(targetIndex = page), MekuriTurnDecision.Commit)
        return try {
            landing.await()
        } catch (cancellation: CancellationException) {
            if (!landing.isCancelled) this.dropTurn()
            throw cancellation
        }
    }

    private suspend fun crossfadeTo(page: Int) {
        if (this.reducesMotion) return
        this.crossfadeFrom = this.settledPage
        this.crossfadeAlpha.snapTo(1f)
        this.state.currentPage = page
        try {
            this.crossfadeAlpha.animateTo(0f, this.configuration.settleAnimation)
        } finally {
            this.crossfadeFrom = null
        }
    }

    // Drags

    /**
     * A drag that is not horizontally dominant neither locks nor takes over a
     * turn; a locked turn follows every later sample. `translation` and
     * [grabY], the height the drag started at, are in dp.
     */
    fun dragChanged(translation: Offset, grabY: Float) {
        if (this.ignoresCurrentDrag || this.reducesMotion) return
        val current = this.turn
        if (current != null) {
            var moving = current
            val before = this.progress
            if (current.isSettling) {
                if (MekuriDrag.turn(translation, this.direction) == null) return
                moving = this.takeOver(current)
            }
            val track = this.edgeTrack(moving)
            this.progress = MekuriDrag.progress(
                start = moving.startProgress,
                translation = translation.x,
                axis = MekuriDrag.axis(moving.turn, this.direction),
                isBlocked = moving.isBlocked,
                track = track,
            )
            val crossed = MekuriHaptic.crossesThreshold(
                from = track.releaseProgress(before),
                to = track.releaseProgress(this.progress),
                threshold = this.configuration.snapThreshold,
            )
            if (!moving.isBlocked && crossed) this.onHaptic(MekuriHaptic.Detent)
            return
        }
        val turn = MekuriDrag.turn(translation, this.direction) ?: return
        val begun = this.begin(turn).copy(
            liftsFromBottom = MekuriDrag.liftsFromBottom(grabY, this.containerSize.height),
            grabY = grabY,
            playsHaptics = true,
        )
        if (!begun.hasLeaf) {
            if (begun.isBlocked) this.leaflessTurn = turn
            return
        }
        this.leaflessTurn = null
        this.turn = begun
        this.progress = MekuriDrag.progress(
            start = 0f,
            translation = translation.x,
            axis = MekuriDrag.axis(turn, this.direction),
            isBlocked = begun.isBlocked,
            track = this.edgeTrack(begun),
        )
        this.onHaptic(MekuriHaptic.Lift)
    }

    /** `velocity` is the horizontal drag velocity in dp per second; [grabY] as for [dragChanged]. */
    fun dragEnded(translation: Offset, velocity: Float, grabY: Float) {
        val ignored = this.ignoresCurrentDrag
        this.ignoresCurrentDrag = false
        val leafless = this.leaflessTurn
        this.leaflessTurn = null
        if (ignored) return
        if (this.reducesMotion) {
            this.commitReducedMotionDrag(translation, velocity, grabY)
            return
        }
        if (this.turn == null && leafless != null) {
            // Aucun feuillet n'a suivi le doigt : la course et l'élan décident, comme pour un tour bloqué.
            val axis = MekuriDrag.axis(leafless, this.direction)
            val travel = translation.x * axis / this.containerSize.width.coerceAtLeast(1f)
            val decision = MekuriTurnDecision.resolve(travel, MekuriDrag.projectedVelocity(velocity, axis), this.configuration)
            if (decision == MekuriTurnDecision.Commit) this.onBlockedTurn(leafless)
            return
        }
        val current = this.turn ?: return
        if (current.phase != MekuriTurnPhase.Dragging) return
        val axis = MekuriDrag.axis(current.turn, this.direction)
        val decision = MekuriTurnDecision.resolve(
            progress = this.edgeTrack(current).releaseProgress(this.progress),
            velocity = MekuriDrag.projectedVelocity(velocity, axis),
            configuration = this.configuration,
        )
        if (current.isBlocked) {
            this.settle(MekuriTurnDecision.Revert)
            // Paroissien : un geste franc au bord du livre ouvre l'unité voisine.
            if (decision == MekuriTurnDecision.Commit) this.onBlockedTurn(current.turn)
            return
        }
        this.settle(decision)
    }

    fun dragCancelled() {
        this.ignoresCurrentDrag = false
        this.leaflessTurn = null
        if (this.turn?.phase == MekuriTurnPhase.Dragging) this.settle(MekuriTurnDecision.Revert)
    }

    private fun commitReducedMotionDrag(translation: Offset, velocity: Float, grabY: Float) {
        val turn = MekuriDrag.turn(translation, this.direction) ?: return
        val target = this.arrangement.turnState(0, turn, this.settledPage).targetIndex ?: return this.onBlockedTurn(turn)
        val axis = MekuriDrag.axis(turn, this.direction)
        val track = this.arrangement.edgeTrack(
            turn = turn,
            containerSize = this.containerSize,
            grabY = grabY,
            liftsFromBottom = MekuriDrag.liftsFromBottom(grabY, this.containerSize.height),
            configuration = this.configuration,
        )
        val decision = MekuriTurnDecision.resolve(
            progress = track.releaseProgress(
                MekuriDrag.progress(0f, translation.x, axis, isBlocked = false, track = track),
            ),
            velocity = MekuriDrag.projectedVelocity(velocity, axis),
            configuration = this.configuration,
        )
        if (decision == MekuriTurnDecision.Commit) {
            this.state.currentPage = target
            this.onHaptic(MekuriHaptic.Land)
        }
    }

    /** Cancels the running settle and continues from the presented progress. */
    private fun takeOver(turn: MekuriTurnState): MekuriTurnState {
        this.settleJob?.cancel()
        this.settleJob = null
        val taken = turn.copy(startProgress = this.progress, phase = MekuriTurnPhase.Dragging, playsHaptics = true)
        this.turn = taken
        return taken
    }

    // The settle

    private fun begin(turn: MekuriTurn): MekuriTurnState {
        this.nextTurnId += 1
        this.progress = 0f
        return this.arrangement.turnState(this.nextTurnId, turn, this.settledPage)
    }

    /** Inserts the fold at rest, never animated, and settles from there. */
    private fun arm(turn: MekuriTurnState, decision: MekuriTurnDecision) {
        this.settleJob?.cancel()
        this.turn = turn.copy(phase = MekuriTurnPhase.Armed(decision))
        this.settleJob = this.scope.launch { runSettle(turn.id, decision) }
    }

    private fun settle(decision: MekuriTurnDecision) {
        val current = this.turn ?: return
        this.settleJob?.cancel()
        this.settleJob = this.scope.launch { runSettle(current.id, decision) }
    }

    private suspend fun runSettle(id: Int, decision: MekuriTurnDecision) {
        val current = this.turn ?: return
        if (current.id != id) return
        this.turn = current.copy(phase = MekuriTurnPhase.Settling(decision))
        this.settleProgress.snapTo(this.progress)
        val target = if (decision == MekuriTurnDecision.Commit) 1f else 0f
        this.settleProgress.animateTo(target, this.configuration.settleAnimation) {
            this@MekuriPagerController.progress = this.value
        }
        this.finish(id, decision)
    }

    private fun finish(id: Int, decision: MekuriTurnDecision) {
        val current = this.turn ?: return
        if (current.id != id || current.phase != MekuriTurnPhase.Settling(decision)) return
        this.turn = null
        this.progress = 0f
        val landed = when (decision) {
            MekuriTurnDecision.Commit -> current.targetIndex ?: current.fromIndex
            MekuriTurnDecision.Revert -> current.fromIndex
        }
        if (decision == MekuriTurnDecision.Commit && current.targetIndex != null && current.playsHaptics) {
            this.onHaptic(MekuriHaptic.Land)
        }
        this.state.currentPage = landed
        this.pending?.complete(landed)
        this.pending = null
    }
}
