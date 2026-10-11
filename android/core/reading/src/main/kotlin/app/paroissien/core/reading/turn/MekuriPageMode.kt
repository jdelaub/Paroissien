// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

/**
 * How a page is being drawn. A [Turning] page is drawn through the fold shader
 * and must not depend on live playback or interaction.
 */
public enum class MekuriPageMode {
    /** The page is settled on screen; interaction and playback run. */
    Live,

    /** The page is a face of the leaf being turned. */
    Turning,
}
