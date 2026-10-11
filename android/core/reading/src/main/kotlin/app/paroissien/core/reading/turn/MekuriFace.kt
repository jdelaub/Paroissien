// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

/** Which face of the leaf one fold pass draws. [raw] is the shader argument. */
internal enum class MekuriFace(val raw: Float) {
    /** One layer for both faces, carrying both shadows. */
    Whole(0f),

    /** The rising front on the roll and the flat front short of the crease. */
    Front(1f),

    /** The back on the roll and the landed back. */
    Back(2f),

    /** The contact shadow and the crease ramp, for a layer beneath the leaf. */
    Shadow(3f),
}
