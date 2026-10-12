package app.paroissien.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.imaging.CardDetector
import app.paroissien.core.imaging.Point
import app.paroissien.core.imaging.Quad
import app.paroissien.core.imaging.RgbImage
import app.paroissien.core.imaging.Tone
import app.paroissien.core.imaging.outputSize
import app.paroissien.library.PiousImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import kotlin.math.hypot

/** Côté long de l'image de travail : assez pour 1 440 px redressés, sans charger une photo de 50 Mpx en mémoire. */
private const val WORK_SIDE = 2400

/** Côté long de l'image donnée au détecteur de bords. */
private const val DETECT_SIDE = 480

private enum class Step { SOURCE, CORNERS, TONE }

/**
 * Numériser une image pieuse (rapport 09, §3.3) : photographier ou choisir une photo, ajuster les quatre coins
 * (détectés d'office), redresser la perspective, régler exposition et contraste (niveaux automatiques), puis
 * compresser en WebP. Une image déjà propre s'importe telle quelle : ni recadrage ni retouche, seulement réduite et
 * compressée. Les métadonnées EXIF disparaissent au réencodage. [key] porte l'image dont on numérise le verso.
 */
@Composable
fun ScanScreen(graph: AppGraph, key: ScanKey, onDone: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(Step.SOURCE) }
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var quad by remember { mutableStateOf(Quad.FULL) }
    var detected by remember { mutableStateOf<Quad?>(null) }
    var straight by remember { mutableStateOf<Bitmap?>(null) }
    var tone by remember { mutableStateOf(Tone()) }
    var auto by remember { mutableStateOf(Tone()) }
    var title by remember { mutableStateOf(resources.getString(R.string.scan_default_title, LocalDate.now().longFrench())) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var asIs by remember { mutableStateOf(false) }

    fun load(uri: Uri) {
        busy = true
        scope.launch {
            val loaded = withContext(Dispatchers.Default) { runCatching { decode(context, uri) }.getOrNull() }
            if (loaded == null) { busy = false; message = resources.getString(R.string.scan_unreadable); return@launch }
            source = loaded
            if (asIs) {
                val out = withContext(Dispatchers.Default) { loaded.scaleToFit(PiousImages.MAX_SIDE) }
                straight = out
                auto = withContext(Dispatchers.Default) { Tone.auto(toRgb(out.scaleToFit(DETECT_SIDE))) }
                tone = Tone()
                message = null
                busy = false
                step = Step.TONE
                return@launch
            }
            busy = false
            val found = withContext(Dispatchers.Default) { CardDetector.detect(toRgb(loaded.scaleToFit(DETECT_SIDE))) }
            detected = found
            quad = found ?: Quad.FULL
            message = if (found == null) resources.getString(R.string.scan_not_found) else null
            step = Step.CORNERS
        }
    }

    val shot = remember { File(context.cacheDir, "scans/photo.jpg") }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) load(Uri.fromFile(shot)) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::load) }

    Surface(Modifier.fillMaxSize(), color = LocalPaperColors.current.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { if (step == Step.SOURCE) onBack() else step = if (step == Step.TONE && !asIs) Step.CORNERS else Step.SOURCE }) { Text("‹") }
                Text(stringResource(if (key.backOf != null) R.string.scan_title_back else R.string.scan_title), style = MaterialTheme.typography.headlineSmall)
            }
            when (step) {
                Step.SOURCE -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.scan_hint), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = {
                        asIs = false
                        shot.parentFile?.mkdirs()
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fichiers", shot)
                        try { camera.launch(uri) } catch (_: ActivityNotFoundException) { message = resources.getString(R.string.scan_no_camera) }
                    }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.scan_camera)) }
                    OutlinedButton(
                        onClick = { asIs = false; gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.scan_gallery)) }
                    OutlinedButton(
                        onClick = { asIs = true; gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.scan_as_is)) }
                    Text(stringResource(R.string.scan_as_is_hint), style = MaterialTheme.typography.bodySmall, color = LocalPaperColors.current.inkSecondary)
                    if (busy) Text(stringResource(R.string.scan_working), style = MaterialTheme.typography.bodySmall)
                    message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
                Step.CORNERS -> source?.let { bitmap ->
                    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(message ?: stringResource(R.string.scan_corners_hint), style = MaterialTheme.typography.bodySmall)
                        CornerEditor(bitmap, quad, { quad = it }, Modifier.weight(1f).fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { quad = Quad.FULL }) { Text(stringResource(R.string.scan_whole)) }
                            detected?.let { d -> OutlinedButton(onClick = { quad = d }) { Text(stringResource(R.string.scan_detected)) } }
                            Button(onClick = {
                                busy = true
                                scope.launch {
                                    val out = withContext(Dispatchers.Default) { straighten(bitmap, quad) }
                                    straight = out
                                    auto = withContext(Dispatchers.Default) { Tone.auto(toRgb(out.scaleToFit(DETECT_SIDE))) }
                                    tone = auto
                                    busy = false
                                    step = Step.TONE
                                }
                            }, enabled = !busy) { Text(stringResource(R.string.scan_next)) }
                        }
                    }
                }
                Step.TONE -> straight?.let { bitmap ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(
                            bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.colorMatrix(ColorMatrix(tone.colorMatrix())),
                            modifier = Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height).padding(8.dp),
                        )
                        Text(stringResource(R.string.scan_exposure), style = MaterialTheme.typography.labelLarge)
                        Slider(value = tone.exposure, onValueChange = { tone = tone.copy(exposure = it) }, valueRange = -1f..1f)
                        Text(stringResource(R.string.scan_contrast), style = MaterialTheme.typography.labelLarge)
                        Slider(value = tone.contrast, onValueChange = { tone = tone.copy(contrast = it) }, valueRange = 0.6f..1.6f)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { tone = Tone() }) { Text(stringResource(R.string.scan_original)) }
                            OutlinedButton(onClick = { tone = auto }) { Text(stringResource(R.string.scan_auto)) }
                            OutlinedButton(onClick = { straight = bitmap.rotated() }) { Text(stringResource(R.string.scan_rotate)) }
                        }
                        if (key.backOf == null) {
                            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.scan_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                        Button(onClick = {
                            busy = true
                            scope.launch {
                                val final = if (tone == Tone()) bitmap else withContext(Dispatchers.Default) { applyTone(bitmap, tone) }
                                val id = key.backOf?.also { graph.images.setBack(it, final) } ?: graph.images.add(final, title.trim().ifEmpty { resources.getString(R.string.scan_untitled) })
                                busy = false
                                onDone(id)
                            }
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.scan_save)) }
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) { File(context.cacheDir, "scans").mkdirs() }
}

/** La photo avec les quatre coins à déplacer au doigt ; le cadre suit. */
@Composable
private fun CornerEditor(bitmap: Bitmap, quad: Quad, onChange: (Quad) -> Unit, modifier: Modifier) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val accent = LocalPaperColors.current.rubric
    val handle = with(LocalDensity.current) { 14.dp.toPx() }
    val reach = with(LocalDensity.current) { 48.dp.toPx() }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val ratio = bitmap.width.toFloat() / bitmap.height
        Box(Modifier.aspectRatio(ratio, matchHeightConstraintsFirst = maxWidth / maxHeight > ratio)) {
            Image(image, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            var dragging by remember { mutableIntStateOf(-1) }
            val current by rememberUpdatedState(quad)
            val update by rememberUpdatedState(onChange)
            Canvas(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { at ->
                            val pts = current.points.map { Offset(it.x * size.width, it.y * size.height) }
                            val nearest = pts.indices.minBy { hypot((pts[it].x - at.x).toDouble(), (pts[it].y - at.y).toDouble()) }
                            dragging = if (hypot((pts[nearest].x - at.x).toDouble(), (pts[nearest].y - at.y).toDouble()) <= reach) nearest else -1
                        },
                        onDragEnd = { dragging = -1 },
                        onDrag = { change, amount ->
                            if (dragging < 0) return@detectDragGestures
                            change.consume()
                            val pts = current.points.toMutableList()
                            val p = pts[dragging]
                            pts[dragging] = Point((p.x + amount.x / size.width).coerceIn(0f, 1f), (p.y + amount.y / size.height).coerceIn(0f, 1f))
                            update(Quad(pts[0], pts[1], pts[2], pts[3]))
                        },
                    )
                },
            ) {
                val pts = quad.points.map { Offset(it.x * size.width, it.y * size.height) }
                val path = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) }; close() }
                drawPath(path, Color.Black.copy(alpha = 0.25f))
                drawPath(path, accent, style = Stroke(width = 3.dp.toPx()))
                pts.forEach {
                    drawCircle(Color.White, radius = handle, center = it)
                    drawCircle(accent, radius = handle, center = it, style = Stroke(width = 3.dp.toPx()))
                }
            }
        }
    }
}

// ── Traitement de l'image ───────────────────────────────────────────────────

/** Décode la photo à une taille de travail raisonnable, orientation EXIF appliquée, en mémoire logicielle. */
private fun decode(context: Context, uri: Uri): Bitmap {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = false
        val side = maxOf(info.size.width, info.size.height)
        if (side > WORK_SIDE) {
            val k = WORK_SIDE.toFloat() / side
            decoder.setTargetSize((info.size.width * k).toInt(), (info.size.height * k).toInt())
        }
    }
}

private fun Bitmap.scaleToFit(side: Int): Bitmap {
    val k = side.toFloat() / maxOf(width, height)
    return if (k >= 1f) this else scale((width * k).toInt().coerceAtLeast(1), (height * k).toInt().coerceAtLeast(1))
}

private fun toRgb(bitmap: Bitmap): RgbImage {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return RgbImage(bitmap.width, bitmap.height, pixels)
}

/** Redresse la perspective : le quadrilatère devient un rectangle, aux proportions de la carte, 1 440 px au plus. */
private fun straighten(bitmap: Bitmap, quad: Quad): Bitmap {
    val (w, h) = outputSize(quad, bitmap.width, bitmap.height, PiousImages.MAX_SIDE)
    val src = quad.points.flatMap { listOf(it.x * bitmap.width, it.y * bitmap.height) }.toFloatArray()
    val dst = floatArrayOf(0f, 0f, w.toFloat(), 0f, w.toFloat(), h.toFloat(), 0f, h.toFloat())
    val matrix = Matrix().apply { setPolyToPoly(src, 0, dst, 0, 4) }
    val out = createBitmap(w, h)
    AndroidCanvas(out).drawBitmap(bitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    return out
}

private fun applyTone(bitmap: Bitmap, tone: Tone): Bitmap {
    val out = createBitmap(bitmap.width, bitmap.height)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(tone.colorMatrix()) }
    AndroidCanvas(out).drawBitmap(bitmap, 0f, 0f, paint)
    return out
}

private fun Bitmap.rotated(): Bitmap = Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(90f) }, true)
