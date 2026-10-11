// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether page turns cut instead of folding. [override] wins in either
 * direction; otherwise an animator duration scale of zero means reduce.
 */
@Composable
internal fun rememberMekuriReducesMotion(override: Boolean?): Boolean {
    val context = LocalContext.current
    val system = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    return override ?: system
}
