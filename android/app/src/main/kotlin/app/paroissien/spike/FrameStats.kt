package app.paroissien.spike

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.FrameDataApi24
import androidx.metrics.performance.FrameDataApi31
import androidx.metrics.performance.JankStats
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Mesure de la porte A sans adb (ADR 005) : durée totale des images (CPU et GPU dès l'API 31) sur les
 * dernières images dessinées. Compose ne dessine rien au repos : la fenêtre reflète surtout les tours de page.
 * Toucher le bandeau remet les compteurs à zéro.
 */
@Composable
fun FrameStatsHud(modifier: Modifier = Modifier) {
    val view = LocalView.current
    val collector = remember { FrameCollector(capacity = 600) }
    var summary by remember { mutableStateOf("Mesure en cours…") }
    val refreshHz = view.display?.refreshRate ?: 60f

    DisposableEffect(view) {
        val stats = JankStats.createAndTrack(view.context.findActivity().window) { collector.add(it) }
        onDispose { stats.isTrackingEnabled = false }
    }
    LaunchedEffect(collector, refreshHz) {
        while (true) {
            summary = collector.summary(refreshHz)
            delay(500)
        }
    }
    Text(
        summary,
        color = Color.White,
        fontSize = 12.sp,
        modifier = modifier
            .background(Color(0xCC000000), RoundedCornerShape(6.dp))
            .clickable { collector.reset() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

private class FrameCollector(private val capacity: Int) {
    private val durationsMs = FloatArray(capacity)
    private var count = 0
    private var next = 0

    @Synchronized
    fun add(frame: FrameData) {
        val nanos = when (frame) {
            is FrameDataApi31 -> frame.frameDurationTotalNanos
            is FrameDataApi24 -> frame.frameDurationCpuNanos
            else -> frame.frameDurationUiNanos
        }
        durationsMs[next] = nanos / 1_000_000f
        next = (next + 1) % capacity
        if (count < capacity) count++
    }

    @Synchronized
    fun reset() {
        count = 0
        next = 0
    }

    @Synchronized
    fun summary(refreshHz: Float): String {
        val budget = 1000f / refreshHz
        if (count == 0) return String.format(Locale.FRANCE, "Aucune image · budget %.1f ms (%.0f Hz)", budget, refreshHz)
        val sorted = durationsMs.copyOf(count).sorted()
        val p95 = sorted[((count - 1) * 0.95f).toInt()]
        val over = sorted.count { it > budget } * 100f / count
        val verdict = if (p95 <= budget && over < 1f) "✓" else "✗"
        return String.format(Locale.FRANCE, "%s P95 %.1f ms · %.1f %% > %.1f ms · %d images · %.0f Hz", verdict, p95, over, budget, count, refreshHz)
    }
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Aucune activité pour mesurer les images")
}
