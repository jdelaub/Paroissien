package app.paroissien.prayer

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.notification.Condition
import android.service.notification.ZenDeviceEffects
import android.service.notification.ZenPolicy
import androidx.annotation.RequiresApi
import app.paroissien.MainActivity
import app.paroissien.R

/**
 * Le mode « Temps de prière » (rapport 08, §2.10.2) : une `AutomaticZenRule` propre à l'app, créée une fois, puis
 * activée et désactivée. Le système fusionne les règles et rétablit lui-même l'état précédent ; depuis Android 15,
 * une app ne peut plus toucher au « Ne pas déranger » global, et c'est tant mieux.
 * Sans l'accès accordé par l'utilisateur, la séance a lieu quand même : rien n'est jamais bloquant.
 */
class PrayerMode(private val context: Context) {
    private val nm = context.getSystemService(NotificationManager::class.java)
    private val conditionId: Uri = Uri.Builder().scheme("condition").authority(context.packageName).appendPath("temps-de-priere").build()

    val accessGranted: Boolean get() = nm.isNotificationPolicyAccessGranted

    /** L'écran des réglages où l'utilisateur accorde l'accès ; il peut manquer sur certains appareils. */
    fun accessSettings(): Intent? = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        .takeIf { it.resolveActivity(context.packageManager) != null }

    /** Retrouve la règle enregistrée, ou la crée ; renvoie son identifiant, ou `null` sans l'accès. */
    fun ensureRule(savedId: String?, grayscale: Boolean): String? {
        if (!accessGranted) return null
        return runCatching {
            if (savedId != null && nm.getAutomaticZenRule(savedId) != null) {
                updateEffects(savedId, grayscale)
                savedId
            } else {
                nm.addAutomaticZenRule(rule(grayscale))
            }
        }.getOrNull()
    }

    fun start(id: String) = setState(id, Condition.STATE_TRUE)

    fun stop(id: String) = setState(id, Condition.STATE_FALSE)

    /** Filet de sécurité au démarrage : aucune séance n'est en cours, la règle ne doit pas rester active. */
    fun ensureOff(id: String?) {
        if (id == null || !accessGranted) return
        runCatching { if (nm.getAutomaticZenRule(id) != null) stop(id) }
    }

    private fun setState(id: String, state: Int) {
        if (!accessGranted) return
        val summary = context.getString(R.string.prayer_mode_name)
        val condition = if (Build.VERSION.SDK_INT >= 35) Condition(conditionId, summary, state, Condition.SOURCE_USER_ACTION) else Condition(conditionId, summary, state)
        runCatching { nm.setAutomaticZenRuleState(id, condition) }
    }

    private fun policy(): ZenPolicy = ZenPolicy.Builder()
        .disallowAllSounds()
        .allowAlarms(true)
        .allowMedia(true) // la cloche sonne sur le flux média
        .allowCalls(ZenPolicy.PEOPLE_TYPE_STARRED)
        .allowRepeatCallers(true)
        .build()

    private fun rule(grayscale: Boolean): AutomaticZenRule {
        val name = context.getString(R.string.prayer_mode_name)
        val config = ComponentName(context, MainActivity::class.java)
        return if (Build.VERSION.SDK_INT >= 35) {
            AutomaticZenRule.Builder(name, conditionId)
                .setType(AutomaticZenRule.TYPE_THEATER)
                .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(policy())
                .setConfigurationActivity(config)
                .setManualInvocationAllowed(true)
                .setIconResId(R.drawable.ic_prayer)
                .setTriggerDescription(context.getString(R.string.prayer_mode_trigger))
                .setDeviceEffects(effects(grayscale))
                .build()
        } else {
            AutomaticZenRule(name, null, config, conditionId, policy(), NotificationManager.INTERRUPTION_FILTER_PRIORITY, true)
        }
    }

    @RequiresApi(35)
    private fun effects(grayscale: Boolean): ZenDeviceEffects =
        ZenDeviceEffects.Builder().setShouldDisplayGrayscale(grayscale).setShouldDimWallpaper(true).build()

    /** Niveaux de gris (Android 15 et plus) ; si l'utilisateur a personnalisé le mode dans les Réglages, on n'y touche pas. */
    private fun updateEffects(id: String, grayscale: Boolean) {
        if (Build.VERSION.SDK_INT < 35 || nm.areAutomaticZenRulesUserManaged()) return
        val current = nm.getAutomaticZenRule(id) ?: return
        if (current.deviceEffects?.shouldDisplayGrayscale() == grayscale) return
        runCatching { nm.updateAutomaticZenRule(id, AutomaticZenRule.Builder(current).setDeviceEffects(effects(grayscale)).build()) }
    }

    companion object {
        /** Une séance est-elle en cours dans ce processus ? Sinon, la règle est désactivée au démarrage. */
        @Volatile var sessionRunning = false
    }
}
