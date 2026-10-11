package app.paroissien

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.PaperSample
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.ParoissienTheme
import app.paroissien.core.liturgy.Computus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Écran témoin de la phase 0 : vérifie l'assemblage des modules sur le téléphone. Remplacé par « Aujourd'hui » en phase 4. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var theme by rememberSaveable { mutableStateOf(PaperTheme.CLAIR) }
            ParoissienTheme(theme) {
                val paper = LocalPaperColors.current
                val year = LocalDate.now().year
                val easter = Computus.easter(year)
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.FRANCE))
                Column(
                    Modifier.fillMaxSize().background(paper.background).safeDrawingPadding().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.skeleton_subtitle), color = paper.inkSecondary)
                    Text(stringResource(R.string.skeleton_distribution, BuildConfig.FLAVOR))
                    Text(stringResource(R.string.skeleton_easter, year, easter))
                    PaperSample(stringResource(R.string.skeleton_sample_title, theme.name.lowercase()))
                    OutlinedButton(onClick = { theme = PaperTheme.entries[(theme.ordinal + 1) % PaperTheme.entries.size] }) {
                        Text(stringResource(R.string.skeleton_next_theme))
                    }
                }
            }
        }
    }
}
