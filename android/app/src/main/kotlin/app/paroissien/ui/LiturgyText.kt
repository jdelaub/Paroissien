package app.paroissien.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.paroissien.R
import app.paroissien.core.data.ReaderSettings
import app.paroissien.core.liturgy.CalendarConfig
import app.paroissien.core.liturgy.Celebration
import app.paroissien.core.liturgy.CelebrationKind
import app.paroissien.core.liturgy.LiturgicalColor
import app.paroissien.core.liturgy.Rank
import app.paroissien.core.liturgy.Rite
import app.paroissien.core.liturgy.Season
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Libellés français du calendrier : rangs, temps, couleurs (rapport 05, §3.5). */
fun ReaderSettings.calendarConfig() = CalendarConfig(
    rite = if (rite == "1969") Rite.ROMAN_1969 else Rite.ROMAN_1962,
    externalSolemnities = externalSolemnities,
    traditionalPenance = traditionalPenance,
)

private val longDate = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)
private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)

fun LocalDate.longFrench(): String = format(longDate).replaceFirstChar(Char::uppercaseChar).replace(" 1 ", " 1er ")

fun LocalDate.monthFrench(): String = format(monthYear).replaceFirstChar(Char::uppercaseChar)

@StringRes
fun LiturgicalColor.label(): Int = when (this) {
    LiturgicalColor.WHITE -> R.string.color_white
    LiturgicalColor.RED -> R.string.color_red
    LiturgicalColor.GREEN -> R.string.color_green
    LiturgicalColor.VIOLET -> R.string.color_violet
    LiturgicalColor.ROSE -> R.string.color_rose
    LiturgicalColor.BLACK -> R.string.color_black
    LiturgicalColor.GOLD -> R.string.color_gold
}

@StringRes
fun Season.label(): Int = when (this) {
    Season.ADVENT -> R.string.season_advent
    Season.CHRISTMAS -> R.string.season_christmas
    Season.LENT -> R.string.season_lent
    Season.PASCHAL_TRIDUUM -> R.string.season_triduum
    Season.EASTER -> R.string.season_easter
    Season.ORDINARY_TIME -> R.string.season_ordinary
    Season.TIME_AFTER_EPIPHANY -> R.string.season_after_epiphany
    Season.SEPTUAGESIMA -> R.string.season_septuagesima
    Season.PASSIONTIDE -> R.string.season_passiontide
    Season.TIME_AFTER_PENTECOST -> R.string.season_after_pentecost
}

/** « Fête de IIIᵉ classe », « Dimanche de IIᵉ classe », « Mémoire facultative »… */
@Composable
fun rankLabel(c: Celebration): String = when (val r = c.rank) {
    is Rank.R1969 -> stringResource(
        when (r) {
            Rank.R1969.SOLEMNITY -> R.string.rank_solemnity
            Rank.R1969.FEAST -> R.string.rank_feast
            Rank.R1969.MEMORIAL -> R.string.rank_memorial
            Rank.R1969.OPTIONAL_MEMORIAL -> R.string.rank_optional_memorial
            Rank.R1969.SUNDAY -> R.string.rank_sunday
            Rank.R1969.WEEKDAY -> R.string.rank_weekday
        },
    )
    is Rank.R1962 -> {
        if (r == Rank.R1962.COMMEMORATION) return stringResource(R.string.rank_commemoration)
        val classe = stringResource(
            when (r) {
                Rank.R1962.CLASS_I -> R.string.class_1
                Rank.R1962.CLASS_II -> R.string.class_2
                Rank.R1962.CLASS_III -> R.string.class_3
                else -> R.string.class_4
            },
        )
        when (c.kind) {
            CelebrationKind.SUNDAY -> stringResource(R.string.rank_sunday_class, classe)
            CelebrationKind.FERIA, CelebrationKind.EMBER_DAY -> stringResource(R.string.rank_feria_class, classe)
            CelebrationKind.VIGIL -> stringResource(R.string.rank_vigil_class, classe)
            CelebrationKind.EXTERNAL_SOLEMNITY -> stringResource(R.string.rank_external)
            CelebrationKind.OCTAVE_DAY -> stringResource(R.string.rank_octave_class, classe)
            else -> stringResource(R.string.rank_feast_class, classe)
        }
    }
}
