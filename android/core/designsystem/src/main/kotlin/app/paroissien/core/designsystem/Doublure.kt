package app.paroissien.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.paroissien.core.designsystem.frame.OrnamentalFrameDef
import app.paroissien.core.designsystem.frame.OrnamentalFrameLayer
import app.paroissien.core.designsystem.frame.OrnamentalFrames
import kotlin.math.PI
import kotlin.math.sin

/** Bleu roi de la soie, relevé sur la doublure du paroissien. */
val SilkBlue = Color(0xFF3E3C8C)

/** Cuir du contre-plat, sous la roulette dorée, si le cadre « doublure » n'est pas là. */
private val Leather = Color(0xFF1E1B22)

/**
 * Soie moirée. Le moiré naît de deux familles de côtes : les unes droites, sombres, les autres claires et doucement
 * ondulées ; là où elles tombent l'une sur l'autre, la soie s'éclaire, là où elles alternent, elle s'assombrit, d'où
 * les ondes en « bois d'eau ». Un reflet large en diagonale et des bords plus sombres donnent le volume.
 */
@Composable
fun SilkMoire(modifier: Modifier = Modifier, silk: Color = SilkBlue) {
    Spacer(
        modifier.fillMaxSize().clipToBounds().drawWithCache {
            val d = density
            val p = 1.25f * d
            val amp = 7f * p
            val stepX = 4f * d
            val tau = 2f * PI.toFloat()
            fun wave(x: Float, y: Float): Float {
                val xd = x / d
                val yd = y / d
                return amp * (
                    sin(tau * yd / 110f + 1.4f * sin(tau * xd / 230f + yd / 150f)) +
                        0.45f * sin(tau * xd / 150f + yd / 70f + 0.8f) +
                        0.25f * sin(tau * yd / 37f + xd / 90f)
                    )
            }
            val ribs = Path()
            val waves = Path()
            var y = -2 * amp
            while (y < size.height + 2 * amp) {
                ribs.moveTo(0f, y)
                ribs.lineTo(size.width, y)
                var x = 0f
                waves.moveTo(0f, y + wave(0f, y))
                while (x < size.width) {
                    x = (x + stepX).coerceAtMost(size.width)
                    waves.lineTo(x, y + wave(x, y))
                }
                y += p
            }
            val fine = Path()
            var f = 0f
            while (f < size.height) {
                fine.moveTo(0f, f)
                fine.lineTo(size.width, f)
                f += p / 2
            }
            val light = Brush.linearGradient(
                0f to Color(0x4D8C8CE6), 0.45f to Color.Transparent, 1f to Color(0x590A0828),
                start = Offset.Zero, end = Offset(size.width, size.height),
            )
            onDrawBehind {
                drawRect(silk)
                drawRect(light)
                drawPath(ribs, Color(0x470C0A30), style = Stroke(width = p * 0.55f))
                drawPath(waves, Color(0x47AFAFFF), style = Stroke(width = p * 0.55f))
                drawPath(fine, Color(0x1A0A0828), style = Stroke(width = 0.35f * d))
            }
        },
    )
}

/**
 * La doublure du plat, qu'on voit en ouvrant le livre avant la première page : soie moirée dans la roulette dorée du
 * cadre « doublure » (sur cuir sombre), ou sur cuir nu si ce cadre manque.
 */
@Composable
fun DoublurePage(modifier: Modifier = Modifier, frame: OrnamentalFrameDef? = OrnamentalFrames.firstOrNull { it.name == "doublure" }) {
    val leather = frame?.let { def -> def.spec.ground?.let { def.style.base[it] } } ?: Leather
    Box(modifier.fillMaxSize().background(leather)) {
        val spec = frame?.spec
        val inset = spec?.margin ?: 14.dp
        Box(
            Modifier.fillMaxSize().padding(
                start = inset + (spec?.sideDepth ?: 0.dp),
                end = inset + (spec?.sideDepth ?: 0.dp),
                top = inset + (spec?.topDepth ?: 0.dp),
                bottom = inset + (spec?.bottomDepth ?: 0.dp),
            ),
        ) { SilkMoire() }
        if (frame != null) OrnamentalFrameLayer(frame.spec, frame.style)
    }
}
