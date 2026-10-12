// Généré par tools/cadres/generate.py : ne pas modifier à la main.
// Source : garde d'un paroissien de la fin du XIXᵉ siècle, photo garde-asanoha-or-et-bleu.
package app.paroissien.core.designsystem.frame

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Garde « Asanoha ». */
val AsanohaPattern = PatternSpec(
    name = "asanoha",
    title = "Asanoha",
    width = 100.0f,
    height = 86.603f,
    tileWidth = 54.dp,
    lattice = Lattice.STAGGERED,
    ground = "bleu",
    ops = ops(
    """
or 0 - M13.70 3L86.30 3L50 23.96Z
or 0 - M86.80 12.53L55.50 66.75L55.50 30.60Z
or 0 - M44.50 66.75L13.20 12.53L44.50 30.60Z
or 0 - M5.50 19.86L36.80 74.08L5.50 56Z
or 0 - M36.30 83.60L-36.30 83.60L0 62.64Z
or 0 - M-36.80 74.08L-5.50 19.86L-5.50 56Z
""",
),
)

/** Encres de la garde. */
val AsanohaPatternStyle = FrameStyle(
    base = mapOf(
        "bleu" to Color(0xFF43536A),
        "or" to Color(0xFFC3AB74),
    ),
    palettes = emptyMap(),
    dimmed = setOf("bleu", "or"),
)
