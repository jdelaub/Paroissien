package app.paroissien.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paroissien.core.liturgy.LiturgicalColor

/** Échantillon de page : encre, rubrique et rubans. Sert de témoin visuel aux thèmes en attendant le moteur de lecture. */
@Composable
fun PaperSample(title: String, modifier: Modifier = Modifier) {
    val paper = LocalPaperColors.current
    Column(modifier.background(paper.background).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = paper.inkPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text("Introïbo ad altáre Dei.", color = paper.inkPrimary, fontSize = 17.sp)
        Text("Le prêtre, au bas de l’autel, fait le signe de la croix.", color = paper.rubric, fontSize = 14.sp, fontStyle = FontStyle.Italic)
        Text("℣. Adjutórium nostrum in nómine Dómini.  ℟. Qui fecit cælum et terram.", color = paper.inkSecondary, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LiturgicalColor.entries.forEach { c ->
                val t = liturgicalTokens(c, paper.isDark)
                val ribbon = Modifier.size(width = 14.dp, height = 40.dp).background(t.fabric)
                Box(if (t.trim != null) ribbon.border(1.dp, t.trim) else ribbon)
            }
        }
    }
}
