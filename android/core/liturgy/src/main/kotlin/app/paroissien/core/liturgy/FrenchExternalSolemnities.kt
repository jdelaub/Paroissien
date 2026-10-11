// SPDX-License-Identifier: Apache-2.0
package app.paroissien.core.liturgy

import app.paroissien.core.liturgy.LiturgicalColor.RED
import app.paroissien.core.liturgy.LiturgicalColor.WHITE
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate

/**
 * Solennités extérieures du rite de 1962 en France (ADR 009 : activées par défaut). Le Sacré-Cœur et le Rosaire sont de
 * droit universel (n. 358) ; l'Épiphanie, le Saint-Sacrement, saints Pierre et Paul, Jeanne d'Arc et Thérèse sont des
 * usages de France (indult de 1802, n. 361). Une solennité empêchée par un jour de Iʳᵉ classe est omise, pas déplacée :
 * le calendrier s'en charge. Règles et sources : `content/sources/calendrier-1962-fr/RECHERCHE.md`, § 6.
 */
internal object FrenchExternalSolemnities : Calendar1962.ExternalSolemnities {

    private fun feast(id: String, key: String, fr: String, la: String, color: LiturgicalColor) = Celebration(
        id = CelebrationId(id), title = LocalizedText(fr, la), kind = CelebrationKind.EXTERNAL_SOLEMNITY,
        rank = Rank.R1962.CLASS_II, precedence = 35, source = CelebrationSource.NATIONAL,
        colors = LiturgicalColors(color), mass = MassRef(key), readings = ReadingsRef(null, fromProper = true),
    )

    private fun sundayFrom(date: LocalDate) = if (date.dayOfWeek == SUNDAY) date else date.next(SUNDAY)

    override fun of(year: Int, sanctoral: (String) -> Celebration?): Map<LocalDate, Celebration> {
        val easter = Computus.easter(year)
        val out = LinkedHashMap<LocalDate, Celebration>()
        fun put(date: LocalDate, id: String, key: String, fr: String, la: String, color: LiturgicalColor) {
            out[date] = feast(id, key, fr, la, sanctoral(id)?.colors?.primary ?: color)
        }
        // Épiphanie : le dimanche du 2 au 8 janvier (règle du Barroux), sauf si le 6 janvier est lui-même un dimanche.
        if (LocalDate.of(year, 1, 6).dayOfWeek != SUNDAY) {
            put(sundayFrom(LocalDate.of(year, 1, 2)), "epiphany_of_the_lord", "Sancti/01-06", "Solennité de l'Épiphanie", "In Epiphania Domini", WHITE)
        }
        // Sainte Jeanne d'Arc : 2e dimanche de mai, jour de la fête nationale (loi du 10 juillet 1920).
        put(sundayFrom(LocalDate.of(year, 5, 1)).plusWeeks(1), "joan_of_arc_virgin", "Sancti/05-30", "Solennité de sainte Jeanne d'Arc", "S. Ioannae Arcensis", WHITE)
        // Saint-Sacrement : 2e dimanche après la Pentecôte, avec la procession.
        put(easter.plusDays(63), "most_holy_body_and_blood_of_christ", "Tempora/Pent01-4", "Solennité du Saint-Sacrement", "Ss.mi Corporis Christi", WHITE)
        // Saints Pierre et Paul : dimanche du 30 juin au 5 juillet, sauf si le 29 juin est un dimanche.
        if (LocalDate.of(year, 6, 29).dayOfWeek != SUNDAY) {
            put(LocalDate.of(year, 6, 29).next(SUNDAY), "peter_and_paul_apostles", "Sancti/06-29", "Solennité des saints Pierre et Paul", "Ss. Petri et Pauli", RED)
        }
        // Sacré-Cœur : 3e dimanche après la Pentecôte (n. 358 a) ; il l'emporte sur saints Pierre et Paul le même dimanche.
        put(easter.plusDays(70), "most_sacred_heart_of_jesus", "Tempora/Pent02-5", "Solennité du Sacré-Cœur", "Ss.mi Cordis D. N. Iesu Christi", WHITE)
        // Sainte Thérèse de l'Enfant-Jésus, patronne secondaire : dernier dimanche de septembre.
        put(LocalDate.of(year, 9, 30).sundayOnOrBefore(), "therese_of_the_child_jesus_and_the_holy_face_of_lisieux_virgin", "Sancti/10-03", "Solennité de sainte Thérèse de l'Enfant-Jésus", "S. Teresiae a Iesu Infante", WHITE)
        // Notre-Dame du Rosaire : 1er dimanche d'octobre (n. 358 b).
        put(sundayFrom(LocalDate.of(year, 10, 1)), "our_lady_of_the_rosary", "Sancti/10-07", "Solennité du Très Saint Rosaire", "Ss.mi Rosarii B. M. V.", WHITE)
        return out
    }
}
