// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

/**
 * The two-sided page a turn moves. [front] and [back] are geometric roles, not
 * story roles: [front] idles in the slot the leaf departs from and [back] lands
 * in [landsIn]. [revealed] is the page uncovered in the departed slot, null when
 * that slot ends up absent.
 */
internal data class MekuriLeaf(
    val front: Int,
    val back: Int?,
    val revealed: Int?,
    val landsIn: MekuriSlot,
)
