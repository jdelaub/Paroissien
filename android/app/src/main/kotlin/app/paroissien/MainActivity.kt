package app.paroissien

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.paroissien.ui.ParoissienRoot

/** Phase 3 : bibliothèque, lecteur, signets, recherche. « Aujourd'hui » arrive en phase 4. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ParoissienRoot((application as ParoissienApp).graph) }
    }
}
