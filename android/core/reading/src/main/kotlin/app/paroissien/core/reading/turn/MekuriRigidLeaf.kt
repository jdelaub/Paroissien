package app.paroissien.core.reading.turn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.sin

/**
 * Un plat de reliure (Paroissien) : la couverture et l'intérieur des plats ne se plient pas comme une page, ils
 * pivotent d'un bloc autour du dos, en perspective. [progress] va de 0 (à plat, fermé) à 1 (ouvert) ; [spineAtStart]
 * : le dos est au bord de début de la boîte (à gauche en lecture de gauche à droite). Passé le quart de tour, on voit
 * l'autre face, [back], redressée ; sans elle, le plat sort de la page.
 */
@Composable
internal fun MekuriRigidLeaf(
    progress: () -> Float,
    spineAtStart: Boolean,
    modifier: Modifier = Modifier,
    back: (@Composable () -> Unit)? = null,
    front: @Composable () -> Unit,
) {
    Box(
        modifier.graphicsLayer {
            val p = progress().coerceIn(0f, 1f)
            transformOrigin = TransformOrigin(if (spineAtStart) 0f else 1f, 0.5f)
            rotationY = (if (spineAtStart) -1f else 1f) * 180f * p
            cameraDistance = 28f * density
        },
    ) {
        val p = progress().coerceIn(0f, 1f)
        if (p < 0.5f) {
            front()
        } else if (back != null) {
            // L'autre face, retournée pour se lire à l'endroit une fois le plat couché de l'autre côté du dos.
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) { back() }
        }
        // Un plat qui pivote s'assombrit en approchant de la tranche, comme sous une lumière de face.
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = (0.45f * sin(PI.toFloat() * progress().coerceIn(0f, 1f))) }.background(Color.Black))
    }
}
