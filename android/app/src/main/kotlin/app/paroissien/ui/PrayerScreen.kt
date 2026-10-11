package app.paroissien.ui

import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.data.ReaderSettings
import app.paroissien.prayer.Bell
import app.paroissien.prayer.PrayerMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Phase { PREPARE, RUNNING, ENDED }

private val DURATIONS = listOf(5, 10, 15, 20, 30, 45)

/** Encre ambre sur noir : la séance ne doit ni éclairer la chapelle ni attirer l'œil (rapport 08, §2.13 F). */
private val AMBER = Color(0xFFD9A441)

/**
 * Le temps de prière (rapport 08, §2.10) : une feuille de préparation, puis l'écran de séance, très sombre, allumé,
 * avec une cloche au début et à la fin et le mode « Ne pas déranger » de l'app. Rien n'est compté ni conservé.
 */
@Composable
fun PrayerScreen(graph: AppGraph, settings: ReaderSettings, key: PrayerKey, onOpenText: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val mode = remember { PrayerMode(context) }
    val bell = remember { Bell(context.applicationContext) }
    var phase by rememberSaveable { mutableStateOf(Phase.PREPARE) }
    var endAt by rememberSaveable { mutableLongStateOf(0L) }
    var pausedLeft by rememberSaveable { mutableLongStateOf(-1L) }
    var ruleId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmEnd by remember { mutableStateOf(false) }
    val library by graph.library.collectAsState()
    val passage = key.title ?: key.publication?.let { pub -> library?.entry(pub)?.unitTitle(key.unit) }

    fun finish(ringBell: Boolean) {
        ruleId?.let(mode::stop)
        PrayerMode.sessionRunning = false
        phase = Phase.ENDED
        if (ringBell && settings.prayerBells) graph.scope.launch { bell.ring() }
    }

    when (phase) {
        Phase.PREPARE -> Preparation(graph, settings, mode, passage, onClose) {
            val id = if (settings.prayerDoNotDisturb) mode.ensureRule(settings.zenRuleId, settings.prayerGrayscale) else null
            if (id != null && id != settings.zenRuleId) graph.scope.launch { graph.settings.update { it.copy(zenRuleId = id) } }
            id?.let(mode::start)
            ruleId = id
            PrayerMode.sessionRunning = true
            endAt = SystemClock.elapsedRealtime() + settings.prayerMinutes * 60_000L
            pausedLeft = -1
            phase = Phase.RUNNING
            if (settings.prayerBells) graph.scope.launch { bell.ring() }
        }
        Phase.RUNNING -> {
            BackHandler { confirmEnd = true }
            Session(
                passage = passage,
                endAt = endAt,
                pausedLeft = pausedLeft,
                onPause = {
                    if (pausedLeft < 0) pausedLeft = endAt - SystemClock.elapsedRealtime()
                    else { endAt = SystemClock.elapsedRealtime() + pausedLeft; pausedLeft = -1 }
                },
                onMore = { if (pausedLeft >= 0) pausedLeft += 5 * 60_000L else endAt += 5 * 60_000L },
                onEnd = { confirmEnd = true },
                onTimeUp = { finish(ringBell = true) },
            )
            if (confirmEnd) {
                AlertDialog(
                    onDismissRequest = { confirmEnd = false },
                    text = { Text(stringResource(R.string.prayer_end_confirm)) },
                    confirmButton = { TextButton(onClick = { confirmEnd = false; finish(ringBell = false) }) { Text(stringResource(R.string.prayer_end)) } },
                    dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text(stringResource(R.string.prayer_continue)) } },
                )
            }
        }
        Phase.ENDED -> Ended(passage != null, onOpenText, onClose)
    }

    // Quitter l'écran, quelle qu'en soit la manière, éteint le mode « Ne pas déranger » ; une rotation ne l'interrompt pas.
    val activity = LocalActivity.current
    DisposableEffect(Unit) {
        onDispose {
            if (PrayerMode.sessionRunning && activity?.isChangingConfigurations != true) {
                ruleId?.let(mode::stop)
                PrayerMode.sessionRunning = false
            }
        }
    }
}

@Composable
private fun Preparation(graph: AppGraph, settings: ReaderSettings, mode: PrayerMode, passage: String?, onClose: () -> Unit, onStart: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(mode.accessGranted) }
    // De retour des réglages Android : l'accès a peut-être été accordé.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) granted = mode.accessGranted }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    fun update(transform: (ReaderSettings) -> ReaderSettings) = graph.scope.launch { graph.settings.update(transform) }

    Screen(stringResource(R.string.prayer_title), onBack = onClose) {
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                passage?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                Text(stringResource(R.string.prayer_duration), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DURATIONS.forEach { m ->
                        FilterChip(selected = settings.prayerMinutes == m, onClick = { update { it.copy(prayerMinutes = m) } }, label = { Text(stringResource(R.string.minutes, m)) })
                    }
                }
                SwitchRow(stringResource(R.string.prayer_bells), settings.prayerBells) { v -> update { it.copy(prayerBells = v) } }
                SwitchRow(stringResource(R.string.prayer_dnd), settings.prayerDoNotDisturb) { v -> update { it.copy(prayerDoNotDisturb = v) } }
                if (settings.prayerDoNotDisturb) {
                    Text(
                        stringResource(if (granted) R.string.prayer_dnd_granted else R.string.prayer_dnd_missing),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (!granted) {
                        mode.accessSettings()?.let { intent ->
                            OutlinedButton(onClick = { context.startActivity(intent) }) { Text(stringResource(R.string.prayer_dnd_allow)) }
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= 35 && settings.prayerDoNotDisturb && granted) {
                    SwitchRow(stringResource(R.string.prayer_grayscale), settings.prayerGrayscale) { v -> update { it.copy(prayerGrayscale = v) } }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.prayer_begin)) }
            }
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Session(passage: String?, endAt: Long, pausedLeft: Long, onPause: () -> Unit, onMore: () -> Unit, onEnd: () -> Unit, onTimeUp: () -> Unit) {
    var controls by remember { mutableStateOf(true) }
    var minutesLeft by remember { mutableIntStateOf(0) }
    var showTime by rememberSaveable { mutableStateOf(true) }

    // Écran allumé, luminosité de la fenêtre au plus bas : rétablie en quittant.
    val view = LocalView.current
    val window = LocalActivity.current?.window
    DisposableEffect(view, window) {
        val before = window?.attributes?.screenBrightness
        view.keepScreenOn = true
        window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = 0.1f } }
        onDispose {
            view.keepScreenOn = false
            window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = before ?: -1f } }
        }
    }
    // Le temps restant n'est mis à jour qu'à la minute : rien ne bouge à l'écran pendant la prière.
    LaunchedEffect(endAt, pausedLeft) {
        while (true) {
            val left = if (pausedLeft >= 0) pausedLeft else endAt - SystemClock.elapsedRealtime()
            if (left <= 0) { onTimeUp(); break }
            minutesLeft = ((left + 59_999) / 60_000).toInt()
            if (pausedLeft >= 0) break
            delay(left % 60_000 + 1)
        }
    }
    LaunchedEffect(controls) { if (controls) { delay(3_000); controls = false } }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { controls = !controls },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(32.dp)) {
            passage?.let { Text(it, color = AMBER.copy(alpha = 0.8f), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center) }
            if (showTime) {
                Text(
                    if (pausedLeft >= 0) stringResource(R.string.prayer_paused, minutesLeft) else stringResource(R.string.prayer_left, minutesLeft),
                    color = AMBER.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable { showTime = false },
                )
            }
        }
        if (controls) {
            Row(Modifier.align(Alignment.BottomCenter).padding(32.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onPause) { Text(stringResource(if (pausedLeft >= 0) R.string.prayer_resume else R.string.prayer_pause), color = AMBER) }
                TextButton(onClick = onMore) { Text(stringResource(R.string.prayer_more), color = AMBER) }
                TextButton(onClick = { showTime = !showTime }) { Text(stringResource(R.string.prayer_time_toggle), color = AMBER) }
                TextButton(onClick = onEnd) { Text(stringResource(R.string.prayer_end), color = AMBER) }
            }
        }
    }
}

@Composable
private fun Ended(hasText: Boolean, onOpenText: () -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.prayer_ended), color = AMBER, style = MaterialTheme.typography.titleLarge)
            if (hasText) TextButton(onClick = onOpenText) { Text(stringResource(R.string.prayer_back_to_text), color = AMBER) }
            TextButton(onClick = onClose) { Text(stringResource(R.string.prayer_close), color = AMBER) }
        }
    }
}
