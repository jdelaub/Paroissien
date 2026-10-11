package app.paroissien

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.paroissien.prayer.PrayerMode
import app.paroissien.ui.ParoissienRoot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as ParoissienApp).graph
        // Filet de sécurité (rapport 08, §2.10.2) : sans séance en cours, le mode « Temps de prière » ne reste pas actif.
        if (!PrayerMode.sessionRunning) {
            val mode = PrayerMode(applicationContext)
            graph.scope.launch { mode.ensureOff(graph.settings.settings.first().zenRuleId) }
        }
        setContent { ParoissienRoot(graph) }
    }
}
