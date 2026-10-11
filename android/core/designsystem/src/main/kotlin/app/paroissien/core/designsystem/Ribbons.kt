package app.paroissien.core.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import app.paroissien.core.liturgy.LiturgicalColor

/**
 * Les six rubans de satin d'un livre (rapport 09), à la couleur liturgique, coupés en biais et brodés d'une croix
 * dorée près de l'extrémité, comme les signets des missels. Le nom de la couleur est toujours écrit à côté
 * (panneau des rubans, signets, TalkBack) : la couleur n'est jamais le seul signal.
 */
data class RibbonStyle(val color: LiturgicalColor, val label: String) {
    companion object {
        val slots = listOf(
            RibbonStyle(LiturgicalColor.RED, "rouge"),
            RibbonStyle(LiturgicalColor.GREEN, "vert"),
            RibbonStyle(LiturgicalColor.VIOLET, "violet"),
            RibbonStyle(LiturgicalColor.WHITE, "blanc"),
            RibbonStyle(LiturgicalColor.ROSE, "rose"),
            RibbonStyle(LiturgicalColor.BLACK, "noir"),
        )

        fun of(slot: Int) = slots[slot.coerceIn(0, slots.lastIndex)]
    }
}

private val GoldDark = Color(0xFF8A6A1C)
private val Gold = Color(0xFFD8B65A)
private val GoldLight = Color(0xFFF3DE96)

/**
 * Un ruban vertical de [size] posé en [topLeft]. Le satin tient à trois choses : un reflet clair un peu décentré,
 * des bords plus sombres là où le tissu tourne, et de longs plis doux. L'extrémité est coupée en biais (le côté
 * gauche descend le plus bas), une croix dorée brodée juste au-dessus.
 */
fun DrawScope.drawRibbon(style: RibbonStyle, dark: Boolean, topLeft: Offset, size: Size) {
    val tokens = liturgicalTokens(style.color, dark)
    val w = size.width
    val h = size.height
    val x = topLeft.x
    val y = topLeft.y
    val bias = (w * 0.9f).coerceAtMost(h * 0.4f)
    val bottom = y + h
    val path = Path().apply {
        moveTo(x, y)
        lineTo(x + w, y)
        lineTo(x + w, bottom - bias)
        lineTo(x, bottom)
        close()
    }
    // Ombre portée très douce sur le papier.
    drawPath(path, Color.Black.copy(alpha = if (dark) 0.35f else 0.14f), style = Stroke(width = 2.5f))
    drawPath(path, tokens.fabric)

    clipPath(path) {
        // Le reflet du satin, en travers du ruban.
        val light = if (style.color == LiturgicalColor.BLACK) 0.22f else if (style.color == LiturgicalColor.WHITE) 0.55f else 0.32f
        drawRect(
            Brush.horizontalGradient(
                0f to Color.Black.copy(alpha = 0.22f),
                0.16f to Color.Transparent,
                0.34f to Color.White.copy(alpha = light),
                0.46f to Color.White.copy(alpha = light * 0.35f),
                0.72f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.26f),
                startX = x, endX = x + w,
            ),
            topLeft = topLeft, size = size,
        )
        // De longs plis doux : le tissu n'est jamais tout à fait plat.
        if (h > w * 3) {
            val fold = Color.Black.copy(alpha = if (dark) 0.16f else 0.10f)
            val shine = Color.White.copy(alpha = if (dark) 0.06f else 0.10f)
            drawRect(
                Brush.verticalGradient(
                    0f to Color.Transparent, 0.22f to shine, 0.38f to fold, 0.55f to Color.Transparent,
                    0.7f to shine, 0.86f to fold, 1f to Color.Transparent,
                    startY = y, endY = bottom,
                ),
                topLeft = topLeft, size = size,
            )
        }
        // Lisières : un fil plus sombre de chaque côté.
        val edge = Color.Black.copy(alpha = if (dark) 0.35f else 0.18f)
        val e = (w * 0.05f).coerceIn(0.6f, 2f)
        drawRect(edge, Offset(x, y), Size(e, h))
        drawRect(edge, Offset(x + w - e, y), Size(e, h))
    }
    tokens.trim?.let { drawPath(path, it, style = Stroke(width = 1.2f)) }
    drawCross(Offset(x + w / 2, bottom - bias - w * 0.62f), w)
}

/** Une croix pattée dorée, brodée : dégradé d'or et fin contour plus sombre. Rien si le ruban est trop étroit. */
private fun DrawScope.drawCross(center: Offset, ribbonWidth: Float) {
    val height = ribbonWidth * 0.78f
    if (height < 9f) return
    val width = height * 0.72f
    val bar = height * 0.16f
    val flare = bar * 0.85f
    val cx = center.x
    val cy = center.y
    val top = cy - height / 2
    val bot = cy + height / 2
    val arm = cy - height * 0.14f // la traverse, un peu au-dessus du milieu
    val path = Path().apply {
        // Branche du haut, évasée.
        moveTo(cx - bar / 2, arm - bar / 2)
        lineTo(cx - bar / 2 - flare / 2, top)
        lineTo(cx + bar / 2 + flare / 2, top)
        lineTo(cx + bar / 2, arm - bar / 2)
        // Bras droit.
        lineTo(cx + width / 2, arm - bar / 2 - flare / 2)
        lineTo(cx + width / 2, arm + bar / 2 + flare / 2)
        lineTo(cx + bar / 2, arm + bar / 2)
        // Pied.
        lineTo(cx + bar / 2 + flare / 2, bot)
        lineTo(cx - bar / 2 - flare / 2, bot)
        lineTo(cx - bar / 2, arm + bar / 2)
        // Bras gauche.
        lineTo(cx - width / 2, arm + bar / 2 + flare / 2)
        lineTo(cx - width / 2, arm - bar / 2 - flare / 2)
        close()
    }
    drawPath(path, Brush.linearGradient(listOf(GoldDark, GoldLight, Gold, GoldDark), start = Offset(cx - width / 2, top), end = Offset(cx + width / 2, bot)))
    drawPath(path, GoldDark.copy(alpha = 0.7f), style = Stroke(width = (height * 0.05f).coerceAtLeast(0.8f)))
}
