// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker

/**
 * One detector for both gestures: a pointer that lifts before the platform's
 * touch slop is a tap, and one that passes it is a drag. Taps and drags are one
 * detector; a second `pointerInput` on this node is not permitted.
 *
 * The down is taken only when no descendant has consumed it, so a control inside
 * a page keeps its touches, and a drag stops the moment a descendant consumes a
 * move. Paging disabled drives no turn and consumes no movement; a centre tap
 * still reports.
 *
 * The pointer loop is keyed on the controller alone; the paging flag is sampled
 * once per gesture and holds for that gesture; a cancelled loop reverts what it
 * was dragging.
 */
@Composable
internal fun Modifier.mekuriGestures(
    controller: MekuriPagerController,
    pagingEnabled: Boolean,
    onCenterTap: (() -> Unit)?,
    tapsTurn: Boolean = true,
): Modifier {
    val paging = rememberUpdatedState(pagingEnabled)
    val centreTap = rememberUpdatedState(onCenterTap)
    val turning = rememberUpdatedState(tapsTurn)
    return this.pointerInput(controller) {
        val slop = this.viewConfiguration.touchSlop
        try {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = true)
                val pagingEnabledForGesture = paging.value
                val tracker = VelocityTracker()
                tracker.addPosition(down.uptimeMillis, down.position)
                var last: PointerInputChange = down
                var dragging = false
                var cancelled = false
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    if (change == null || change.isConsumed) {
                        cancelled = true
                        break
                    }
                    last = change
                    if (!change.pressed) break
                    tracker.addPosition(change.uptimeMillis, change.position)
                    val translation = change.position - down.position
                    if (!dragging && translation.getDistance() > slop) dragging = true
                    if (dragging && pagingEnabledForGesture) {
                        controller.dragChanged(translation / this.density, grabY = down.position.y / this.density)
                        if (controller.turn != null) change.consume()
                    }
                }
                when {
                    cancelled -> controller.dragCancelled()
                    dragging -> if (pagingEnabledForGesture) {
                        controller.dragEnded(
                            translation = (last.position - down.position) / this.density,
                            velocity = tracker.calculateVelocity().x / this.density,
                            grabY = down.position.y / this.density,
                        )
                    }
                    else -> {
                        last.consume()
                        // Menu ouvert : un toucher le referme, sans tourner la page (Paroissien).
                        if (!turning.value) {
                            centreTap.value?.invoke()
                        } else {
                            controller.tapped(
                                x = down.position.x / this.density,
                                y = down.position.y / this.density,
                                onCenterTap = centreTap.value,
                                pagingEnabled = pagingEnabledForGesture,
                            )
                        }
                    }
                }
            }
        } finally {
            controller.dragCancelled()
        }
    }
}
