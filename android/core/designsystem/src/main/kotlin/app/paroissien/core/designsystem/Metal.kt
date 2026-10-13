package app.paroissien.core.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode

/**
 * Le métal d'une dorure (atelier, demande du 13/10/2026) : fers de la couverture, lettres du titre, tranche, ors des
 * cadres. Quatre tons du plus sombre au plus clair : l'ombre portée du fer, le creux, le plein, le reflet.
 */
enum class Metal(val shadow: Color, val deep: Color, val mid: Color, val bright: Color) {
    GOLD(Color(0xFF4A3608), Color(0xFF8A6A1C), Color(0xFFD2AE52), Color(0xFFF5E2A0)),
    SILVER(Color(0xFF34373C), Color(0xFF7A7F87), Color(0xFFC3C7CD), Color(0xFFF5F7F9)),
    BRONZE(Color(0xFF33260F), Color(0xFF6B5126), Color(0xFFA88B55), Color(0xFFDCC690)),
    COPPER(Color(0xFF431C0E), Color(0xFF8A4526), Color(0xFFC9784A), Color(0xFFF3BE96)),
    ;

    /** Le reflet d'un fer : creux, reflet, plein, creux, de [start] à [end]. */
    fun brush(start: Offset, end: Offset, tile: TileMode = TileMode.Clamp): Brush =
        Brush.linearGradient(listOf(deep, bright, mid, deep), start = start, end = end, tileMode = tile)

    /** Une tranche dorée : le bloc des pages arrondi renvoie la lumière en bandes le long de la tranche. */
    fun edge(start: Offset, end: Offset): Brush =
        Brush.linearGradient(listOf(deep, bright, mid, deep, mid), start = start, end = end)
}
