// Généré par tools/cadres/generate.py : ne pas modifier à la main.
// Source : atelier : roulette de reliure (fleurs de lys, carrés d'angle), dessin original.
package app.paroissien.core.designsystem.frame

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paroissien.core.liturgy.LiturgicalColor

private val fleursDeLysTop = EdgeModule(
    period = 64.0f,
    phase = 0.0f,
    outset = 0.0f,
    inset = 0.0f,
    base = ops(
        """
gold 0 - M0 14C7.92 24.08 11.52 38.48 8.64 50C7.20 55.76 5.04 59.36 3.60 61.52L-3.60 61.52C-5.04 59.36 -7.20 55.76 -8.64 50C-11.52 38.48 -7.92 24.08 0 14ZM3.60 60.08C7.20 47.12 18 39.92 22.32 47.84C25.20 52.88 23.76 58.64 18.72 60.08C20.16 55.76 18 52.16 14.40 53.60C10.08 55.76 7.20 60.08 6.48 64.40ZM-3.60 60.08L-6.48 64.40C-7.20 60.08 -10.08 55.76 -14.40 53.60C-18 52.16 -20.16 55.76 -18.72 60.08C-23.76 58.64 -25.20 52.88 -22.32 47.84C-18 39.92 -7.20 47.12 -3.60 60.08ZM-10.80 60.80L10.80 60.80L10.80 67.28L-10.80 67.28ZM-5.04 67.28L5.04 67.28C5.76 74.48 2.88 80.96 0 86C-2.88 80.96 -5.76 74.48 -5.04 67.28Z
gold 0 - M36.20 50C36.20 52.32 34.32 54.20 32 54.20C29.68 54.20 27.80 52.32 27.80 50C27.80 47.68 29.68 45.80 32 45.80C34.32 45.80 36.20 47.68 36.20 50Z
""",
    ),
    over = emptyList(),
)

private val fleursDeLysCorner = BoxModule(
    width = 100.0f,
    height = 100.0f,
    outset = 0.0f,
    above = true,
    ops = ops(
        """
gold 0 - M10 10L90 10L90 90L10 90Z
paper 0 - M50 28L72 50L50 72L28 50Z
gold 0 - M57 50C57 53.87 53.87 57 50 57C46.13 57 43 53.87 43 50C43 46.13 46.13 43 50 43C53.87 43 57 46.13 57 50Z
""",
    ),
)

/** Cadre « Roulette de fleurs de lys ». */
val FleursDeLysFrame = FrameSpec(
    name = "fleurs_de_lys",
    title = "Roulette de fleurs de lys",
    top = fleursDeLysTop,
    side = fleursDeLysTop,
    bottom = fleursDeLysTop,
    topCenter = null,
    sideCenter = null,
    bottomCenter = null,
    corner = fleursDeLysCorner,
    bottomCorner = fleursDeLysCorner,
    cornerFill = null,
    outerCorner = null,
    outerBottomCorner = null,
    outerMid = null,
    outerBottomMid = null,
    outerSideMid = null,
    bars = null,
    rules = listOf(Rule("gold", at = 6.0f, width = 4.5f, over = true), Rule("gold", at = 96.0f, width = 3.0f, over = true)),
    ground = null,
    margin = 10.dp,
    topDepth = 18.dp,
    sideDepth = 18.dp,
    bottomDepth = 18.dp,
    textPadding = 10.dp,
)

/** Encres du cadre : couleurs du livre, remplacements par couleur liturgique, encres atténuées en thème sombre. */
val FleursDeLysStyle = FrameStyle(
    base = mapOf(
        "gold" to Color(0xFFC2A055),
    ),
    palettes = emptyMap(),
    dimmed = setOf("gold"),
)
