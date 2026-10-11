// SPDX-License-Identifier: Apache-2.0
package app.paroissien.core.liturgy

import java.time.LocalDate

/** Les deux rites du MVP (ADR 009). */
enum class Rite(val code: String) { ROMAN_1962("1962"), ROMAN_1969("1969") }

/** Slug stable d'une célébration, commun aux deux rites quand la fête est la même (`joseph_spouse_of_mary`). */
@JvmInline value class CelebrationId(val value: String) {
    override fun toString() = value
}

data class LocalizedText(val fr: String, val la: String? = null)

/** Rang : classes de 1960 ou degrés de 1969. */
sealed interface Rank {
    enum class R1962 : Rank { CLASS_I, CLASS_II, CLASS_III, CLASS_IV, COMMEMORATION }
    enum class R1969 : Rank { SOLEMNITY, FEAST, MEMORIAL, OPTIONAL_MEMORIAL, SUNDAY, WEEKDAY }
}

enum class CelebrationKind { SUNDAY, FERIA, FEAST, VIGIL, OCTAVE_DAY, EMBER_DAY, ALL_SOULS, EXTERNAL_SOLEMNITY }

enum class CelebrationSource { TEMPORAL, GENERAL, NATIONAL, OVERRIDE }

enum class Season {
    ADVENT, CHRISTMAS, LENT, PASCHAL_TRIDUUM, EASTER, ORDINARY_TIME,
    // 1962 seulement
    TIME_AFTER_EPIPHANY, SEPTUAGESIMA, PASSIONTIDE, TIME_AFTER_PENTECOST,
}

/** Une couleur principale, et celles que les rubriques permettent aussi (violet ou noir pour les défunts en 1969). */
data class LiturgicalColors(val primary: LiturgicalColor, val alternatives: List<LiturgicalColor> = emptyList()) {
    val all: List<LiturgicalColor> get() = listOf(primary) + alternatives

    companion object {
        fun of(colors: List<LiturgicalColor>) = LiturgicalColors(colors.first(), colors.drop(1))
        fun of(vararg colors: LiturgicalColor) = of(colors.toList())
    }
}

enum class SundayCycle { A, B, C }
enum class WeekdayCycle { I, II }
data class Cycles(val sunday: SundayCycle, val weekday: WeekdayCycle, val psalterWeek: Int)

/** Précepte dominical ou de fête d'obligation (CIC 1983, c. 1246-1247 ; liste française). */
data class Obligation(val basis: String)

/**
 * Pénitence selon le droit en vigueur (c. 1249-1253, CEF), plus en option la discipline traditionnelle
 * du CIC de 1917 (can. 1252), toujours présentée comme facultative (ADR 009).
 */
data class Penance(
    val fast: Boolean = false,
    val abstinence: Boolean = false,
    /** France : hors Carême, l'abstinence du vendredi peut être remplacée par un autre acte de pénitence. */
    val substitutable: Boolean = false,
    val traditional: TraditionalPenance? = null,
) {
    companion object { val NONE = Penance() }
}

data class TraditionalPenance(val fast: Boolean, val abstinence: Boolean, val reason: String)

enum class DayFlag { EMBER_DAY, ROGATION, VIGIL, OCTAVE, TRANSFERRED, PRIVILEGED, HOLY_WEEK, GAUDETE_LAETARE, EXTERNAL_SOLEMNITY }

/** Pour le missel (phase 5) : clé du propre de messe (Divinum Officium pour 1962, slug pour 1969). */
data class MassRef(val properId: String, val commonId: String? = null)

/** Pour les lectures : clé de lectionnaire (1969) ou renvoi au propre (1962). */
data class ReadingsRef(val lectionaryKey: String?, val fromProper: Boolean = false)

data class Celebration(
    val id: CelebrationId,
    val title: LocalizedText,
    val kind: CelebrationKind,
    val rank: Rank,
    /** Rang dans la table de préséance du rite : plus petit = plus haut (NU 1 à 13 ; 1962 : n. 91, numérotation interne). */
    val precedence: Int,
    val source: CelebrationSource,
    val colors: LiturgicalColors,
    val transferredFrom: LocalDate? = null,
    val mass: MassRef? = null,
    val readings: ReadingsRef? = null,
)

data class LiturgicalDay(
    val date: LocalDate,
    val rite: Rite,
    val season: Season,
    /** Numéro de semaine dans le temps (« 28ᵉ semaine du temps ordinaire », « 19ᵉ dimanche après la Pentecôte »). */
    val week: Int?,
    val principal: Celebration,
    /** 1962 : commémoraisons dans l'ordre des oraisons. */
    val commemorations: List<Celebration> = emptyList(),
    /** 1969 : mémoires facultatives au choix du célébrant ; la férie reste la célébration principale. */
    val optional: List<Celebration> = emptyList(),
    val cycles: Cycles? = null,
    val obligation: Obligation? = null,
    val penance: Penance = Penance.NONE,
    val flags: Set<DayFlag> = emptySet(),
    /**
     * 1962 : solennité extérieure (n. 356-361) ; le jour garde sa célébration, sa classe et sa couleur, et l'on peut y
     * dire la messe votive de IIᵉ classe de la fête.
     */
    val externalSolemnity: Celebration? = null,
) {
    val colors: LiturgicalColors get() = principal.colors
}

/** Réglages du calendrier (rapport 05, §3.4). */
data class CalendarConfig(
    val rite: Rite,
    /** 1962, France : solennités extérieures dominicales (ADR 009 : activées par défaut). */
    val externalSolemnities: Boolean = true,
    /** Affiche aussi la discipline traditionnelle de jeûne et d'abstinence, comme facultative. */
    val traditionalPenance: Boolean = false,
)
