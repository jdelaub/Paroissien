// Généré par tools/cadres/generate.py : ne pas modifier à la main.
// Source : paroissien chromolithographié, p.81 (Élévation), photo cadre-simple-filets-rouges.
package app.paroissien.core.designsystem.frame

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paroissien.core.liturgy.LiturgicalColor

private val filetsRougesTop = EdgeModule(
    period = 100.0f,
    phase = 0.0f,
    outset = 0.0f,
    inset = 0.0f,
    base = emptyList(),
    over = emptyList(),
)

private val filetsRougesCorner = BoxModule(
    width = 100.0f,
    height = 100.0f,
    outset = 0.0f,
    above = true,
    ops = ops(
        """
paper 0 - M88 50C88 70.99 70.99 88 50 88C29.01 88 12 70.99 12 50C12 29.01 29.01 12 50 12C70.99 12 88 29.01 88 50Z
rule 7 - M72 50C72 62.15 62.15 72 50 72C37.85 72 28 62.15 28 50C28 37.85 37.85 28 50 28C62.15 28 72 37.85 72 50Z
rule 0 - M93 50C93 53.31 90.31 56 87 56C83.69 56 81 53.31 81 50C81 46.69 83.69 44 87 44C90.31 44 93 46.69 93 50Z
rule 0 - M56 87C56 90.31 53.31 93 50 93C46.69 93 44 90.31 44 87C44 83.69 46.69 81 50 81C53.31 81 56 83.69 56 87Z
rule 0 - M19 50C19 53.31 16.31 56 13 56C9.69 56 7 53.31 7 50C7 46.69 9.69 44 13 44C16.31 44 19 46.69 19 50Z
rule 0 - M56 13C56 16.31 53.31 19 50 19C46.69 19 44 16.31 44 13C44 9.69 46.69 7 50 7C53.31 7 56 9.69 56 13Z
""",
    ),
)

/** Cadre « Filets rouges ». */
val FiletsRougesFrame = FrameSpec(
    name = "filets_rouges",
    title = "Filets rouges",
    top = filetsRougesTop,
    side = filetsRougesTop,
    bottom = filetsRougesTop,
    topCenter = null,
    sideCenter = null,
    bottomCenter = null,
    corner = filetsRougesCorner,
    bottomCorner = filetsRougesCorner,
    cornerFill = null,
    outerCorner = null,
    outerBottomCorner = null,
    outerMid = null,
    outerBottomMid = null,
    outerSideMid = null,
    bars = null,
    rules = listOf(Rule("rule", at = 50.0f, width = 8.5f, over = true, from = RuleFrom.OUTER)),
    ground = null,
    margin = 10.dp,
    topDepth = 12.dp,
    sideDepth = 12.dp,
    bottomDepth = 12.dp,
    textPadding = 12.dp,
)

/** Encres du cadre : couleurs du livre, remplacements par couleur liturgique, encres atténuées en thème sombre. */
val FiletsRougesStyle = FrameStyle(
    base = mapOf(
        "rule" to Color(0xFFC4473B),
    ),
    palettes = emptyMap(),
    dimmed = setOf("rule"),
)
