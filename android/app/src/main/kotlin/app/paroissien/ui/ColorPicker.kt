package app.paroissien.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.paroissien.R
import app.paroissien.core.designsystem.LocalPaperColors

/**
 * Une teinte libre (atelier : rubans, couvertures) : des pastilles toutes prêtes, puis la teinte, la saturation et la
 * luminosité au curseur, avec la couleur obtenue à côté.
 */
@Composable
internal fun ColorPicker(palette: List<Long>, selected: Long?, fallback: Color, onPick: (Long) -> Unit) {
    val current = selected?.let { Color(it) } ?: fallback
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(current.toArgb(), it) }
    fun pick(h: Float = hsv[0], s: Float = hsv[1], v: Float = hsv[2]) = onPick(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)).toLong() and 0xFFFFFFFFL)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Swatches(palette.map { Color(it) }, selected, onPick)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(36.dp).background(current, CircleShape).border(1.dp, LocalPaperColors.current.hairline, CircleShape))
            Column(Modifier.fillMaxWidth()) {
                Slider(hsv[0], { pick(h = it) }, valueRange = 0f..360f)
                Slider(hsv[1], { pick(s = it) }, valueRange = 0f..1f)
                Slider(hsv[2], { pick(v = it) }, valueRange = 0.05f..1f)
            }
        }
        Text(stringResource(R.string.color_sliders_hint), style = MaterialTheme.typography.bodySmall, color = LocalPaperColors.current.inkSecondary)
    }
}
