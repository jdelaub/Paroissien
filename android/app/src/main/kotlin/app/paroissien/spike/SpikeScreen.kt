package app.paroissien.spike

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.paroissien.R
import app.paroissien.core.designsystem.BookFace
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.ParoissienTheme
import app.paroissien.core.designsystem.rememberBookFonts
import app.paroissien.core.reading.turn.MekuriConfiguration
import app.paroissien.core.reading.turn.MekuriPager
import app.paroissien.core.reading.turn.rememberMekuriPagerState

/** Animation de page (rapport 07) : pli AGSL, feuille qui glisse, ou aucune. */
enum class TurnStyle { PAPIER, GLISSEMENT, AUCUN }

/** Livre d'essai de la phase 1a : tourne-page, papier, polices et césure, réglables au toucher central. */
@Composable
fun SpikeScreen() {
    var theme by rememberSaveable { mutableStateOf(PaperTheme.CLAIR) }
    var face by rememberSaveable { mutableStateOf(BookFace.GARAMOND) }
    var turn by rememberSaveable { mutableStateOf(TurnStyle.PAPIER) }
    var hud by rememberSaveable { mutableStateOf(false) }
    var chrome by rememberSaveable { mutableStateOf(false) }
    val state = rememberMekuriPagerState(pageCount = spikeBook.size)

    ParoissienTheme(theme) {
        val fonts = rememberBookFonts(face)
        val configuration = remember(turn) { MekuriConfiguration(reducedMotionOverride = if (turn == TurnStyle.AUCUN) true else null) }
        Box(Modifier.fillMaxSize().background(LocalPaperColors.current.background)) {
            MekuriPager(
                state = state,
                modifier = Modifier.fillMaxSize(),
                configuration = configuration,
                onCenterTap = { chrome = !chrome },
                slide = turn == TurnStyle.GLISSEMENT,
            ) { index ->
                BookPage(spikeBook[index], number = index, fonts = fonts)
            }
            if (chrome) {
                Surface(
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Setting(stringResource(R.string.spike_theme, themeLabel(theme))) { theme = theme.next() }
                        Setting(stringResource(R.string.spike_font, faceLabel(face))) { face = face.next() }
                        Setting(stringResource(R.string.spike_turn, turnLabel(turn))) { turn = turn.next() }
                        Setting(stringResource(if (hud) R.string.spike_hud_on else R.string.spike_hud_off)) { hud = !hud }
                        Text(
                            stringResource(if (Build.VERSION.SDK_INT >= 33) R.string.spike_agsl_yes else R.string.spike_agsl_no, Build.VERSION.SDK_INT),
                            color = LocalPaperColors.current.inkTertiary,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }
            }
            if (hud) FrameStatsHud(Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(bottom = 36.dp))
        }
    }
}

@Composable
private fun Setting(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) { Text(label) }
}

@Composable
private fun themeLabel(t: PaperTheme) = stringResource(
    when (t) {
        PaperTheme.CLAIR -> R.string.spike_theme_clair
        PaperTheme.SEPIA -> R.string.spike_theme_sepia
        PaperTheme.SOMBRE -> R.string.spike_theme_sombre
        PaperTheme.NUIT -> R.string.spike_theme_nuit
    },
)

@Composable
private fun faceLabel(f: BookFace) = stringResource(if (f == BookFace.GARAMOND) R.string.spike_font_garamond else R.string.spike_font_literata)

@Composable
private fun turnLabel(t: TurnStyle) = stringResource(
    when (t) {
        TurnStyle.PAPIER -> R.string.spike_turn_paper
        TurnStyle.GLISSEMENT -> R.string.spike_turn_slide
        TurnStyle.AUCUN -> R.string.spike_turn_none
    },
)

private inline fun <reified E : Enum<E>> E.next(): E = enumValues<E>().let { it[(ordinal + 1) % it.size] }
