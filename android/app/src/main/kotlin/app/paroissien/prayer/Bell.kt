package app.paroissien.prayer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.VibrationEffect
import android.os.Vibrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * La cloche du temps de prière, synthétisée plutôt qu'enregistrée : aucun fichier, aucune licence. Une cloche sonne
 * par partiels inharmoniques (bourdon, fondamentale, tierce mineure, quinte, nominal…) qui s'éteignent chacun à son
 * rythme. Jouée sur le flux média (`USAGE_MEDIA`), que la règle « Ne pas déranger » laisse passer ; si le volume média
 * est nul, une vibration la remplace (rapport 08, §2.10.3).
 */
class Bell(private val context: Context) {

    private class Partial(val ratio: Double, val amplitude: Double, val decay: Double)

    private val partials = listOf(
        Partial(0.5, 0.35, 3.2), // bourdon
        Partial(1.0, 0.55, 2.4), // fondamentale
        Partial(1.183, 0.30, 1.8), // tierce mineure
        Partial(1.506, 0.22, 1.5), // quinte
        Partial(2.0, 0.40, 1.2), // nominal
        Partial(2.514, 0.14, 0.9),
        Partial(2.662, 0.12, 0.8),
        Partial(3.011, 0.10, 0.6),
        Partial(4.166, 0.06, 0.4),
    )

    private val samples: ShortArray by lazy { synthesize(fundamental = 392.0, seconds = 6.0) }

    private fun synthesize(fundamental: Double, seconds: Double): ShortArray {
        val n = (RATE * seconds).toInt()
        val raw = DoubleArray(n)
        for (p in partials) {
            val w = 2 * PI * fundamental * p.ratio / RATE
            for (i in 0 until n) {
                val t = i.toDouble() / RATE
                // Une frappe douce : 4 ms de montée, puis la décroissance propre au partiel.
                raw[i] += p.amplitude * sin(w * i) * exp(-t / p.decay) * (1 - exp(-t / 0.004))
            }
        }
        val peak = raw.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-9)
        return ShortArray(n) { (raw[it] / peak * 0.7 * Short.MAX_VALUE).toInt().toShort() }
    }

    /** Sonne une fois et rend la main quand le son s'est éteint. */
    suspend fun ring() {
        val audio = context.getSystemService(AudioManager::class.java)
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            context.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1))
            return
        }
        val pcm = withContext(Dispatchers.Default) { samples }
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        try {
            track.write(pcm, 0, pcm.size)
            track.play()
            delay(pcm.size * 1000L / RATE)
        } finally {
            track.release()
        }
    }

    private companion object {
        const val RATE = 44_100
    }
}
