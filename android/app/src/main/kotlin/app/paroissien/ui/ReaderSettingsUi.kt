package app.paroissien.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.paroissien.R
import app.paroissien.core.data.ReaderSettings
import app.paroissien.core.designsystem.BookFace
import app.paroissien.core.designsystem.PaperTheme
import app.paroissien.core.designsystem.RibbonFabric
import app.paroissien.core.reading.book.SpreadMode

/** Animation de page (rapport 07) : pli AGSL, feuille qui glisse, ou aucune. */
enum class TurnStyle { PAPIER, GLISSEMENT, AUCUN }

val SCALES = floatArrayOf(0.85f, 1f, 1.15f, 1.3f, 1.5f)

fun ReaderSettings.paperTheme() = PaperTheme.entries.firstOrNull { it.name == theme } ?: PaperTheme.CLAIR
fun ReaderSettings.bookFace() = BookFace.entries.firstOrNull { it.name == face } ?: BookFace.GARAMOND
fun ReaderSettings.spreadMode() = SpreadMode.entries.firstOrNull { it.name == spread } ?: SpreadMode.AUTO
fun ReaderSettings.turnStyle() = TurnStyle.entries.firstOrNull { it.name == turn } ?: TurnStyle.PAPIER
fun ReaderSettings.ribbonFabric() = if (ribbonFabric == "SATIN") RibbonFabric.SATIN else RibbonFabric.TEXTURED

/** Les réglages de lecture, chacun un bouton qui passe à la valeur suivante ; [change] les enregistre, [onAll] ouvre l'écran des réglages. */
@Composable
fun ReaderSettingsRow(settings: ReaderSettings, onAll: (() -> Unit)? = null, onDress: (() -> Unit)? = null, change: ((ReaderSettings) -> ReaderSettings) -> Unit) {
    FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Setting(stringResource(R.string.setting_theme, stringResource(themeLabel(settings.paperTheme())))) { change { it.copy(theme = settings.paperTheme().next().name) } }
        Setting(stringResource(R.string.setting_font, stringResource(if (settings.bookFace() == BookFace.GARAMOND) R.string.font_garamond else R.string.font_literata))) {
            change { it.copy(face = settings.bookFace().next().name) }
        }
        Setting(stringResource(R.string.setting_smaller)) { change { it.copy(scaleIndex = (it.scaleIndex - 1).coerceAtLeast(0)) } }
        Setting(stringResource(R.string.setting_larger)) { change { it.copy(scaleIndex = (it.scaleIndex + 1).coerceAtMost(SCALES.lastIndex)) } }
        Setting(stringResource(R.string.setting_spread, stringResource(spreadLabel(settings.spreadMode())))) { change { it.copy(spread = settings.spreadMode().next().name) } }
        Setting(stringResource(R.string.setting_turn, stringResource(turnLabel(settings.turnStyle())))) { change { it.copy(turn = settings.turnStyle().next().name) } }
        // Français, latin, puis les deux en regard.
        Setting(stringResource(R.string.setting_language, stringResource(languageLabel(settings.language)))) {
            change { it.copy(language = when (it.language) { "fr" -> "la"; "la" -> LANGUAGE_FACING; else -> "fr" }) }
        }
        Setting(stringResource(if (settings.protectEdges) R.string.setting_edges_on else R.string.setting_edges_off)) { change { it.copy(protectEdges = !it.protectEdges) } }
        Setting(stringResource(if (settings.bookIdentity) R.string.setting_identity_on else R.string.setting_identity_off)) { change { it.copy(bookIdentity = !it.bookIdentity) } }
        Setting(stringResource(if (settings.ribbonsAcross) R.string.setting_ribbons_across_on else R.string.setting_ribbons_across_off)) { change { it.copy(ribbonsAcross = !it.ribbonsAcross) } }
        Setting(stringResource(if (settings.ribbonFabric() == RibbonFabric.SATIN) R.string.setting_fabric_satin else R.string.setting_fabric_textured)) {
            change { it.copy(ribbonFabric = if (it.ribbonFabric == "SATIN") "TISSU" else "SATIN") }
        }
        Setting(stringResource(if (settings.cardsOnPage) R.string.setting_cards_on_page_on else R.string.setting_cards_on_page_off)) { change { it.copy(cardsOnPage = !it.cardsOnPage) } }
        Setting(stringResource(if (settings.ornamentalFrames) R.string.setting_frames_on else R.string.setting_frames_off)) { change { it.copy(ornamentalFrames = !it.ornamentalFrames) } }
        Setting(stringResource(if (settings.pinchToZoom) R.string.setting_pinch_on else R.string.setting_pinch_off)) { change { it.copy(pinchToZoom = !it.pinchToZoom) } }
        Setting(stringResource(if (settings.showMeasures) R.string.setting_measures_on else R.string.setting_measures_off)) { change { it.copy(showMeasures = !it.showMeasures) } }
        onDress?.let { Setting(stringResource(R.string.setting_dress), it) }
        onAll?.let { Setting(stringResource(R.string.setting_all), it) }
    }
}

@Composable
private fun Setting(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) { Text(label) }
}

internal fun themeLabel(t: PaperTheme) = when (t) {
    PaperTheme.CLAIR -> R.string.theme_clair
    PaperTheme.SEPIA -> R.string.theme_sepia
    PaperTheme.SOMBRE -> R.string.theme_sombre
    PaperTheme.NUIT -> R.string.theme_nuit
}

internal fun spreadLabel(s: SpreadMode) = when (s) {
    SpreadMode.AUTO -> R.string.spread_auto
    SpreadMode.SINGLE -> R.string.spread_single
    SpreadMode.DOUBLE -> R.string.spread_double
}

internal fun turnLabel(t: TurnStyle) = when (t) {
    TurnStyle.PAPIER -> R.string.turn_paper
    TurnStyle.GLISSEMENT -> R.string.turn_slide
    TurnStyle.AUCUN -> R.string.turn_none
}

private inline fun <reified E : Enum<E>> E.next(): E = enumValues<E>().let { it[(ordinal + 1) % it.size] }

/** Le nom d'un réglage de langue : français, latin, ou les deux en regard. */
fun languageLabel(language: String) = when (language) {
    "la" -> R.string.language_la
    LANGUAGE_FACING -> R.string.language_facing
    else -> R.string.language_fr
}
