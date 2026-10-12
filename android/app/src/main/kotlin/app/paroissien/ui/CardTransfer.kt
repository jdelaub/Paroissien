package app.paroissien.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import app.paroissien.AppGraph
import app.paroissien.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Envoie des fichiers du cache partagé (`cache/partage/`) par le partage d'Android : une messagerie, un courriel, un
 * autre téléphone. Le droit de lecture n'est donné qu'à l'app choisie, le temps de l'envoi.
 */
fun shareFiles(context: Context, files: List<File>, mime: String, title: String) {
    if (files.isEmpty()) return
    val uris = files.map { FileProvider.getUriForFile(context, "${context.packageName}.fichiers", it) }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    }
    intent.type = mime
    intent.clipData = ClipData.newRawUri(null, uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, title))
}

/**
 * Importer une image pieuse depuis des fichiers : un `.paroissien-image` reçu d'une autre app, ou une ou deux images
 * ordinaires (la première est le recto, la seconde le verso). [onImported] reçoit l'identifiant de la nouvelle image.
 */
@Composable
fun rememberCardImporter(graph: AppGraph, onImported: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        graph.scope.launch {
            val id = withContext(Dispatchers.IO) { importCard(context, graph, uris) }
            if (id != null) onImported(id) else Toast.makeText(context, R.string.images_import_failed, Toast.LENGTH_LONG).show()
        }
    }
    return { launcher.launch(arrayOf("image/*", "application/zip", "application/octet-stream")) }
}

private suspend fun importCard(context: Context, graph: AppGraph, uris: List<Uri>): String? {
    val resolver = context.contentResolver
    val first = uris.first()
    // Un zip commence par « PK » : c'est un .paroissien-image, quel que soit le type annoncé.
    val isBundle = runCatching { resolver.openInputStream(first)?.use { it.read() == 'P'.code && it.read() == 'K'.code } }.getOrNull() == true
    if (isBundle) return resolver.openInputStream(first)?.use { graph.images.importBundle(it) }
    val front = resolver.openInputStream(first)?.use { it.readBytes() } ?: return null
    val back = uris.getOrNull(1)?.let { u -> resolver.openInputStream(u)?.use { it.readBytes() } }
    return graph.images.importFaces(front, back, displayName(context, first)?.substringBeforeLast('.') ?: "")
}

private fun displayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
}.getOrNull()
