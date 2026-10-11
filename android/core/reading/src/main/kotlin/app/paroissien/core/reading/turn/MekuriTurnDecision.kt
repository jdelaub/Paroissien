// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

/** Outcome of releasing a drag. */
internal enum class MekuriTurnDecision {
    Commit,
    Revert,

    ;

    internal companion object {
        /**
         * `velocity` is measured along the turn, in dp per second, and compared
         * signed: a fling against the turn never commits.
         */
        internal fun resolve(
            progress: Float,
            velocity: Float,
            configuration: MekuriConfiguration,
        ): MekuriTurnDecision =
            if (progress >= configuration.snapThreshold || velocity >= configuration.flingVelocity) {
                Commit
            } else {
                Revert
            }
    }
}
