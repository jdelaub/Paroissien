package app.paroissien.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import app.paroissien.AppGraph
import app.paroissien.R
import app.paroissien.core.content.PackBlock
import app.paroissien.core.content.TocEntry
import app.paroissien.core.model.text.Inlines
import app.paroissien.core.data.ReaderSettings
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.liturgicalTokens
import app.paroissien.core.liturgy.DayFlag
import app.paroissien.core.liturgy.LiturgicalColor
import app.paroissien.core.liturgy.LiturgicalDay
import app.paroissien.core.liturgy.Rite
import app.paroissien.library.subtitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Le livre de méditation proposé chaque jour, un chapitre après l'autre. */
private const val MEDITATION_BOOK = "imitation.fr"
internal const val MISSAL_BOOK = "missel.1962"
private const val ORDINARY_BOOK = "ordinaire.1962"

/** Le début de l'Évangile du jour : son titre (« Suite ✠ du saint Évangile selon saint Luc »), ses premières lignes. */
class Gospel(val block: String, val incipit: String, val opening: String)

/** L'Évangile d'un formulaire du missel (la dernière messe s'il y en a plusieurs), en français s'il y en a. */
fun gospelOf(blocks: List<PackBlock>, unit: String): Gospel? {
    val gospel = blocks.filter { b -> b.id.removePrefix("$unit.").split('.').let { it.firstOrNull() == "evangile" || it.getOrNull(1) == "evangile" } }
    val texts = gospel.filter { it.type != "heading" && it.type != "rubric" }
    val head = gospel.firstOrNull { it.type == "heading" } ?: return null
    fun plain(b: PackBlock) = (b.texts["fr"] ?: b.texts["la"])?.let(Inlines::plainText)?.trim().orEmpty()
    val incipit = texts.firstOrNull() ?: return null
    val body = texts.drop(1).firstOrNull() ?: return null
    return Gospel(head.id, plain(incipit), plain(body))
}

/** Unité du missel pour une clé de propre de Divinum Officium : « Tempora/Adv1-0 » → « tempora-adv1-0 ». */
fun missalUnit(properId: String): String = properId.lowercase().replace('/', '-')

/**
 * « Aujourd'hui » (rapport 08, §2.3) : la fête du jour, sa couleur et son rang, le précepte et la pénitence, la lecture
 * à reprendre, l'image pieuse et la méditation du jour. Fonctionne sans aucun paquet : tout vient du calendrier.
 * [date] fixe le jour ouvert depuis l'Ordo ; sinon, c'est aujourd'hui, et l'écran passe minuit tout seul.
 */
@Composable
fun TodayScreen(graph: AppGraph, settings: ReaderSettings, date: LocalDate?, open: (NavKey) -> Unit, onBack: (() -> Unit)? = null) {
    val today by produceState(LocalDate.now()) {
        while (true) {
            val now = LocalDateTime.now()
            delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1_000)
            value = LocalDate.now()
        }
    }
    var shown by rememberSaveable(date) { mutableLongStateOf((date ?: today).toEpochDay()) }
    LaunchedEffect(today) { if (date == null) shown = today.toEpochDay() }
    val day = LocalDate.ofEpochDay(shown)
    val config = settings.calendarConfig()
    val liturgical by produceState<LiturgicalDay?>(null, day, config) { value = withContext(Dispatchers.Default) { graph.calendar.day(day, config) } }
    val scope = rememberCoroutineScope()
    val library by graph.library.collectAsState()
    val positions by remember { graph.database.positions().observeAll() }.collectAsState(initial = emptyList())
    val images by graph.images.all.collectAsState()
    // Méditation du jour : un chapitre de l'Imitation après l'autre, le même toute la journée.
    val meditation by produceState<TocEntry?>(null, library, day) {
        val entry = library?.entry(MEDITATION_BOOK) ?: return@produceState
        val chapters = withContext(Dispatchers.IO) { graph.packs.toc(entry, "fr") }.let { toc -> toc.filter { it.level >= 1 }.ifEmpty { toc } }
        value = chapters.takeIf { it.isNotEmpty() }?.let { it[day.toEpochDay().mod(it.size.toLong()).toInt()] }
    }

    // L'Évangile du jour (1962) : son titre et ses premières lignes, en français s'il y en a.
    val gospel by produceState<Gospel?>(null, library, liturgical) {
        val d = liturgical ?: return@produceState
        val missal = library?.entry(MISSAL_BOOK) ?: return@produceState
        val unit = d.principal.mass?.properId?.let(::missalUnit)?.let(missal::unit) ?: return@produceState
        value = withContext(Dispatchers.IO) { runCatching { gospelOf(graph.packs.blocks(missal, unit), unit.id) }.getOrNull() }
    }

    Surface(Modifier.fillMaxSize(), color = LocalPaperColors.current.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) TextButton(onClick = onBack) { Text("‹") }
                TextButton(onClick = { shown -= 1 }) { Text("‹", style = MaterialTheme.typography.titleLarge) }
                Text(
                    day.longFrench(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).clickable { shown = today.toEpochDay() },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(onClick = { shown += 1 }) { Text("›", style = MaterialTheme.typography.titleLarge) }
                IconButton(onClick = { open(OrdoKey(day.year, day.monthValue)) }) {
                    Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.ordo_open))
                }
                if (onBack == null) {
                    IconButton(onClick = { open(SettingsKey) }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                item { RiteSwitch(settings.rite) { rite -> scope.launch { graph.settings.update { it.copy(rite = rite) } } } }
                val d = liturgical ?: return@LazyColumn
                item(key = "banner") { DayBanner(d) }
                item(key = "law") { LawNotes(d) }

                val resume = positions.firstOrNull()
                val lib = library
                // Messe du jour (1962) : le propre dans le missel, la messe entière (l'Ordinaire où s'insère le propre du
                // jour), et le début de l'Évangile, qui ouvre le missel à l'Évangile.
                if (settings.rite == "1962" && lib != null) {
                    val missal = lib.entry(MISSAL_BOOK)
                    val proper = d.principal.mass?.properId?.let(::missalUnit)?.takeIf { missal?.unit(it) != null }
                    val ordinary = lib.entry(ORDINARY_BOOK)
                    if (proper != null || ordinary != null) {
                        item(key = "mass") {
                            Section(stringResource(R.string.today_mass))
                            if (missal != null && proper != null) BookRow(d.principal.title.fr, missal.title) { open(ReaderKey(MISSAL_BOOK, proper)) }
                            if (ordinary != null) {
                                BookRow(stringResource(R.string.today_mass_whole), stringResource(if (proper != null) R.string.today_mass_whole_detail else R.string.today_mass_ordinary_only)) {
                                    open(ReaderKey(ORDINARY_BOOK))
                                }
                            }
                            gospel?.let { g ->
                                Column(
                                    Modifier.fillMaxWidth().clickable { open(ReaderKey(MISSAL_BOOK, proper, g.block)) }.padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(stringResource(R.string.today_gospel), style = MaterialTheme.typography.labelLarge, color = LocalPaperColors.current.rubric)
                                    Text(g.incipit, style = MaterialTheme.typography.bodyMedium, color = LocalPaperColors.current.inkSecondary)
                                    Text(g.opening, style = MaterialTheme.typography.bodyLarge, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                if (resume != null && lib != null) {
                    lib.entry(resume.publication)?.let { entry ->
                        item(key = "resume") {
                            Section(stringResource(R.string.today_resume))
                            BookRow(entry.title, entry.unitTitle(resume.unit)) { open(ReaderKey(resume.publication, resume.unit, resume.block, resume.charOffset)) }
                        }
                    }
                }

                images.takeIf { it.isNotEmpty() }?.let { graph.images.forDay(d) }?.let { image ->
                    item(key = "image") {
                        Section(stringResource(R.string.today_image))
                        Row(
                            Modifier.fillMaxWidth().clickable { open(CardKey(image.id)) }.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            graph.images.thumbnail(image.id)?.let {
                                Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.width(56.dp).aspectRatio(0.58f).clip(MaterialTheme.shapes.extraSmall))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(image.title.fr, style = MaterialTheme.typography.titleMedium)
                                Text(image.artist, style = MaterialTheme.typography.bodySmall, color = LocalPaperColors.current.inkSecondary)
                            }
                        }
                        HorizontalDivider(color = LocalPaperColors.current.hairline)
                    }
                }

                item(key = "meditation") {
                    Section(stringResource(R.string.today_meditation))
                    val chapter = meditation
                    if (chapter != null) {
                        BookRow(chapter.title, lib?.entry(MEDITATION_BOOK)?.title) { open(ReaderKey(MEDITATION_BOOK, chapter.unit, chapter.blockId)) }
                    }
                    OutlinedButton(
                        onClick = { open(PrayerKey(MEDITATION_BOOK.takeIf { chapter != null }, chapter?.unit, chapter?.blockId, chapter?.title)) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    ) { Text(stringResource(R.string.prayer_start_short, settings.prayerMinutes)) }
                }
                item(key = "settings") {
                    Section(stringResource(R.string.calendar_settings))
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        if (settings.rite == "1962") {
                            SwitchRow(stringResource(R.string.setting_external), settings.externalSolemnities) { v -> scope.launch { graph.settings.update { it.copy(externalSolemnities = v) } } }
                            SwitchRow(stringResource(R.string.setting_traditional), settings.traditionalPenance) { v -> scope.launch { graph.settings.update { it.copy(traditionalPenance = v) } } }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun RiteSwitch(rite: String, onChange: (String) -> Unit) {
    val options = listOf("1962" to R.string.rite_1962, "1969" to R.string.rite_1969)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        options.forEachIndexed { i, (code, label) ->
            SegmentedButton(
                selected = rite == code,
                onClick = { onChange(code) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(stringResource(label)) }
        }
    }
}

/** Bandeau du jour : nom liturgique, rang, couleur écrite en toutes lettres (la couleur n'est jamais le seul signal). */
@Composable
private fun DayBanner(d: LiturgicalDay) {
    val paper = LocalPaperColors.current
    val tokens = liturgicalTokens(d.colors.primary, paper.isDark)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Box(Modifier.width(6.dp).height(96.dp).background(tokens.fabric))
        Column(Modifier.padding(start = 14.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(d.principal.title.fr, style = MaterialTheme.typography.headlineSmall)
            d.principal.title.la?.let { Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = paper.inkSecondary) }
            Text(rankLabel(d.principal), style = MaterialTheme.typography.bodyMedium, color = paper.rubric)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                d.colors.all.forEach { ColorDot(it) }
                Text(
                    d.colors.all.map { stringResource(it.label()) }.joinToString(stringResource(R.string.color_or)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            val details = buildList {
                add(stringResource(d.season.label()))
                d.cycles?.let { add(stringResource(R.string.cycles, it.sunday.name, it.weekday.name, it.psalterWeek)) }
            }
            Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = paper.inkSecondary)
            d.principal.transferredFrom?.let { Text(stringResource(R.string.transferred_from, it.longFrench()), style = MaterialTheme.typography.bodySmall, color = paper.inkSecondary) }
            d.externalSolemnity?.let { Text(stringResource(R.string.external_solemnity_note, it.title.fr), style = MaterialTheme.typography.bodyMedium, color = paper.rubric) }
            d.commemorations.forEach { Text(stringResource(R.string.commemoration_of, it.title.fr), style = MaterialTheme.typography.bodyMedium) }
            if (d.optional.isNotEmpty()) {
                Text(stringResource(R.string.optional_memorials), style = MaterialTheme.typography.labelMedium, color = paper.inkSecondary)
                d.optional.forEach { Text("· ${it.title.fr}", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
fun ColorDot(color: LiturgicalColor, modifier: Modifier = Modifier) {
    val paper = LocalPaperColors.current
    val tokens = liturgicalTokens(color, paper.isDark)
    Canvas(modifier.size(12.dp).clip(CircleShape)) {
        drawCircle(tokens.fabric)
        tokens.trim?.let { drawCircle(it, radius = size.minDimension / 2, center = Offset(size.width / 2, size.height / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(2f)) }
    }
}

/** Précepte et pénitence du droit en vigueur, puis la discipline traditionnelle si elle est demandée (facultative). */
@Composable
private fun LawNotes(d: LiturgicalDay) {
    val paper = LocalPaperColors.current
    val lines = buildList {
        d.obligation?.let { add(stringResource(R.string.obligation, it.basis)) }
        val p = d.penance
        when {
            p.fast && p.abstinence -> add(stringResource(R.string.penance_fast_abstinence))
            p.abstinence && p.substitutable -> add(stringResource(R.string.penance_friday_substitutable))
            p.abstinence -> add(stringResource(R.string.penance_abstinence))
        }
        p.traditional?.let { t ->
            val what = when {
                t.fast && t.abstinence -> stringResource(R.string.tradition_fast_abstinence)
                t.fast -> stringResource(R.string.tradition_fast)
                else -> stringResource(R.string.tradition_abstinence)
            }
            add(stringResource(R.string.tradition_line, what, t.reason))
        }
    }
    if (lines.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        lines.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = paper.inkSecondary, modifier = Modifier.padding(vertical = 2.dp)) }
    }
    HorizontalDivider(color = paper.hairline, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = LocalPaperColors.current.rubric,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 2.dp),
    )
}

/**
 * L'Ordo du mois (rapport 05, §3.5) : une case par jour, un petit ruban à la couleur liturgique, la date et la fête.
 * Un toucher ouvre le jour.
 */
@Composable
fun OrdoScreen(graph: AppGraph, settings: ReaderSettings, key: OrdoKey, open: (NavKey) -> Unit, onBack: () -> Unit) {
    var month by rememberSaveable { mutableLongStateOf(LocalDate.of(key.year, key.month, 1).toEpochDay()) }
    val first = LocalDate.ofEpochDay(month)
    val config = settings.calendarConfig()
    val days by produceState<List<LiturgicalDay>>(emptyList(), first, config) {
        value = withContext(Dispatchers.Default) { graph.calendar.month(first.year, first.monthValue, config) }
    }
    val paper = LocalPaperColors.current
    Screen(stringResource(if (config.rite == Rite.ROMAN_1962) R.string.ordo_title_1962 else R.string.ordo_title_1969), onBack = onBack) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { month = first.minusMonths(1).toEpochDay() }) { Text("‹", style = MaterialTheme.typography.titleLarge) }
                Text(first.monthFrench(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { month = first.plusMonths(1).toEpochDay() }) { Text("›", style = MaterialTheme.typography.titleLarge) }
            }
        }
        items(days.size, key = { days[it].date.toString() }) { i ->
            val d = days[i]
            val tokens = liturgicalTokens(d.colors.primary, paper.isDark)
            val sunday = d.date.dayOfWeek == java.time.DayOfWeek.SUNDAY
            Row(
                Modifier.fillMaxWidth().clickable { open(DayKey(d.date.toString())) }.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(8.dp).height(36.dp).background(tokens.fabric))
                Text(
                    "${d.date.dayOfMonth}",
                    style = if (sunday) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    color = if (sunday) paper.rubric else paper.inkPrimary,
                    modifier = Modifier.width(44.dp).padding(start = 12.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(d.principal.title.fr, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val marks = listOfNotNull(
                        stringResource(R.string.mark_obligation).takeIf { d.obligation != null && !sunday },
                        stringResource(R.string.mark_fast).takeIf { d.penance.fast },
                        stringResource(R.string.mark_abstinence).takeIf { d.penance.abstinence && !d.penance.fast && !d.penance.substitutable },
                        stringResource(R.string.mark_ember).takeIf { DayFlag.EMBER_DAY in d.flags },
                    )
                    Text(
                        (listOf(rankLabel(d.principal)) + marks).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = paper.inkSecondary,
                    )
                }
            }
            if (d.date.dayOfWeek == java.time.DayOfWeek.SUNDAY) HorizontalDivider(color = paper.hairline)
        }
    }
}
