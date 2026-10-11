package app.paroissien.core.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.paroissien.core.liturgy.LiturgicalColor

/**
 * Les six rubans de tissu d'un livre (rapport 09). Chacun a sa couleur liturgique ET une forme de queue :
 * en deutéranopie, le vert et le rouge se confondent (rapport 08), la forme les distingue.
 */
enum class RibbonTail { V, STRAIGHT, POINT, DOUBLE_V, ROUND, NOTCH }

data class RibbonStyle(val color: LiturgicalColor, val tail: RibbonTail, val label: String) {
    companion object {
        val slots = listOf(
            RibbonStyle(LiturgicalColor.RED, RibbonTail.V, "rouge"),
            RibbonStyle(LiturgicalColor.GREEN, RibbonTail.STRAIGHT, "vert"),
            RibbonStyle(LiturgicalColor.VIOLET, RibbonTail.POINT, "violet"),
            RibbonStyle(LiturgicalColor.WHITE, RibbonTail.DOUBLE_V, "blanc"),
            RibbonStyle(LiturgicalColor.ROSE, RibbonTail.ROUND, "rose"),
            RibbonStyle(LiturgicalColor.BLACK, RibbonTail.NOTCH, "noir"),
        )

        fun of(slot: Int) = slots[slot.coerceIn(0, slots.lastIndex)]
    }
}

/** Un ruban vertical de [size] posé en [topLeft], terminé par sa queue ; un fin gros-grain le fait tissu. */
fun DrawScope.drawRibbon(style: RibbonStyle, dark: Boolean, topLeft: Offset, size: Size) {
    val tokens = liturgicalTokens(style.color, dark)
    val w = size.width
    val h = size.height
    val cut = w * 0.8f
    val path = Path().apply {
        moveTo(topLeft.x, topLeft.y)
        lineTo(topLeft.x + w, topLeft.y)
        val bottom = topLeft.y + h
        when (style.tail) {
            RibbonTail.V -> { lineTo(topLeft.x + w, bottom); lineTo(topLeft.x + w / 2, bottom - cut); lineTo(topLeft.x, bottom) }
            RibbonTail.STRAIGHT -> { lineTo(topLeft.x + w, bottom - cut / 3); lineTo(topLeft.x, bottom - cut / 3) }
            RibbonTail.POINT -> { lineTo(topLeft.x + w, bottom - cut); lineTo(topLeft.x + w / 2, bottom); lineTo(topLeft.x, bottom - cut) }
            RibbonTail.DOUBLE_V -> {
                lineTo(topLeft.x + w, bottom); lineTo(topLeft.x + w * 0.75f, bottom - cut / 2)
                lineTo(topLeft.x + w / 2, bottom); lineTo(topLeft.x + w * 0.25f, bottom - cut / 2); lineTo(topLeft.x, bottom)
            }
            RibbonTail.ROUND -> {
                lineTo(topLeft.x + w, bottom - w / 2)
                quadraticTo(topLeft.x + w, bottom, topLeft.x + w / 2, bottom)
                quadraticTo(topLeft.x, bottom, topLeft.x, bottom - w / 2)
            }
            RibbonTail.NOTCH -> {
                lineTo(topLeft.x + w, bottom); lineTo(topLeft.x + w * 0.62f, bottom)
                lineTo(topLeft.x + w * 0.62f, bottom - cut / 2); lineTo(topLeft.x + w * 0.38f, bottom - cut / 2)
                lineTo(topLeft.x + w * 0.38f, bottom); lineTo(topLeft.x, bottom)
            }
        }
        close()
    }
    drawPath(path, Color.Black.copy(alpha = 0.18f), style = Stroke(width = 2f))
    drawPath(path, tokens.fabric)
    // Gros-grain : fines côtes horizontales, un peu plus sombres.
    val rib = Color.Black.copy(alpha = if (dark) 0.18f else 0.07f)
    var y = topLeft.y + 2f
    while (y < topLeft.y + h - cut) {
        drawLine(rib, Offset(topLeft.x, y), Offset(topLeft.x + w, y), strokeWidth = 1f)
        y += 3f
    }
    tokens.trim?.let { drawPath(path, it, style = Stroke(width = 1.5f)) }
}
