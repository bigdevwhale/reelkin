package app.reelkin.ui.bill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.reelkin.data.DAY
import app.reelkin.domain.Cinema
import app.reelkin.domain.Filter
import app.reelkin.domain.Span
import app.reelkin.domain.Stats
import app.reelkin.domain.Words
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Nav
import app.reelkin.ui.Overlay
import app.reelkin.ui.components.Bar
import app.reelkin.ui.components.Bars
import app.reelkin.ui.components.Btn
import app.reelkin.ui.components.Card
import app.reelkin.ui.components.Chip
import app.reelkin.ui.components.Flow
import app.reelkin.ui.components.GenreRing
import app.reelkin.ui.components.Kind
import app.reelkin.ui.components.Page
import app.reelkin.ui.components.Section
import app.reelkin.ui.components.Sheet
import app.reelkin.ui.components.Stat
import app.reelkin.ui.components.Tabs
import app.reelkin.ui.components.TopRow
import app.reelkin.ui.components.t
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun loc(ru: Boolean): Locale = if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH
private fun fmt(ms: Long, pattern: String, ru: Boolean) = SimpleDateFormat(pattern, loc(ru)).format(Date(ms))

/** Minutes as «3 ч 20 мин» or «3 h 20 min». */
fun dur(min: Long, ru: Boolean): String {
    val h = min / 60; val m = min % 60
    return when {
        h == 0L -> if (ru) "$m мин" else "$m min"
        m == 0L -> if (ru) "$h ч" else "$h h"
        else -> if (ru) "$h ч $m мин" else "$h h $m min"
    }
}

/** What was watched, in numbers and on a calendar. */
@Composable
fun StatsPage(c: Cinema, nav: Nav) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Page(t("Статистика", "Stats"), nav::close) {
        Row(Modifier.fillMaxWidth().clip(R8.Md).background(C.goldSoft).clickable { nav.go(Overlay.Cards) }.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t("Открытки для соцсетей", "Postcards to share"), style = T.Strong)
                Text(t("Будка, питомец, итоги, жанры — одной картинкой", "Your booth, pet, numbers and genres as a picture"), style = T.Small)
            }
            Text("›", style = T.Title.copy(color = C.gold))
        }
        Tabs(listOf(t("Цифры", "Numbers"), t("Календарь", "Calendar")), page, { page = it })
        if (page == 0) StatsNumbers(c, nav) else StatsCalendar(c)
    }
}

@Composable
fun StatsNumbers(c: Cinema, nav: Nav) {
    val ru = LocalRu.current
    var f by remember { mutableStateOf(Filter(span = if (c.thisYear.count { !it.season } >= 5) Span.YEAR else Span.ALL)) }
    var more by remember { mutableStateOf(false) }
    var topTab by rememberSaveable { mutableIntStateOf(0) }
    val s = remember(c, f) { Stats(c, f) }
    val spans = Span.entries
    val years = remember(c) { (c.seats.map { c.yearOf(it.at) } + c.episodes.map { c.yearOf(it.at) }).toSet().filter { it < c.year - 1 }.sortedDescending() }
    // One row that scrolls: the spans first, then the years before last, newest first.
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val order = listOf(Span.ALL, Span.YEAR, Span.LAST_YEAR, Span.MONTH, Span.WEEK)
        val names = mapOf(Span.ALL to t("Всё время", "All time"), Span.YEAR to "${c.year}", Span.LAST_YEAR to "${c.year - 1}", Span.MONTH to t("Месяц", "Month"), Span.WEEK to t("Неделя", "Week"))
        for (sp in order) Chip(names.getValue(sp), f.year == null && f.span == sp, { f = f.copy(span = sp, year = null) })
        for (y in years) Chip("$y", f.year == y, { f = f.copy(year = y) })
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val parts = listOfNotNull(f.genre?.let { Words.genre(it, ru) }, if (!f.films) t("только сериалы", "series only") else null, if (!f.series) t("только фильмы", "films only") else null)
        Text(if (parts.isEmpty()) t("всё подряд", "everything") else parts.joinToString(" · "), style = T.Small, modifier = Modifier.weight(1f))
        Btn(if (parts.isEmpty()) t("Фильтры", "Filters") else t("Фильтры · ${parts.size}", "Filters · ${parts.size}"), { more = true }, kind = Kind.SOFT, small = true)
    }
    if (more) Sheet({ more = false }) {
        Text(t("Что считать", "What to count"), style = T.Title)
        Section(t("Что", "What"))
        Flow {
            Chip(t("всё", "everything"), f.films && f.series, { f = f.copy(films = true, series = true) })
            Chip(t("фильмы", "films"), f.films && !f.series, { f = f.copy(films = true, series = false) })
            Chip(t("сериалы", "series"), !f.films && f.series, { f = f.copy(films = false, series = true) })
        }
        Section(t("Жанр", "Genre"))
        Flow { for (g in app.reelkin.data.Genre.entries) Chip(Words.genre(g, ru), g == f.genre, { f = f.copy(genre = if (f.genre == g) null else g) }, color = C.genre(g)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Btn(t("Сбросить", "Clear"), { f = f.copy(genre = null, films = true, series = true) }, Modifier.weight(1f), kind = Kind.GHOST)
            Btn(t("Готово", "Done"), { more = false }, Modifier.weight(1f), kind = Kind.GOLD)
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Stat(Words.fmt(s.hours), t("часов у экрана", "hours watched"), Modifier.weight(1f))
        Stat(dur(s.perDay.toLong(), ru), t("в среднем в день", "a day on average"), Modifier.weight(1f), C.velvet)
        Stat("${s.days}", t("дней с кино", "days with films"), Modifier.weight(1f), C.ink)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Stat("${s.films.size}", t("фильмов", "films"), Modifier.weight(1f), C.ink)
        Stat("${s.episodes.size}", t("серий", "episodes"), Modifier.weight(1f), C.ink)
        Stat("${s.seasons}", t("сезонов досмотрено", "seasons finished"), Modifier.weight(1f), C.ink)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Stat(s.avgPopcorn?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "–", t("попкорна в среднем", "popcorn on average"), Modifier.weight(1f))
        Stat("${s.newCountries}", t("новых стран", "new countries"), Modifier.weight(1f), C.velvet)
        Stat("${s.newDecades}", t("новых десятилетий", "new decades"), Modifier.weight(1f), C.velvet)
    }
    if (s.moments.isEmpty()) {
        Text(t("За это время ничего не отмечено. Загляни в «Всё время».", "Nothing logged in this span. Try «All time»."), style = T.Muted)
        return
    }

    val series = s.series
    if (series.size > 1) Card(Modifier.fillMaxWidth()) {
        Text((if (s.monthly) t("По месяцам", "By month") else t("По дням", "By day")).uppercase(), style = T.Eyebrow)
        val labels = series.mapIndexed { i, (at, _) ->
            when {
                s.monthly -> fmt(at, "LLL", ru).take(1).uppercase().let { m -> if (series.size <= 13) m else if (fmt(at, "M", ru) == "1" || i == 0) "’" + fmt(at, "yy", ru) else "" }
                f.span == Span.WEEK -> fmt(at, "EE", ru)
                i == 0 || i == series.size / 2 || i == series.size - 1 -> fmt(at, "d MMM", ru)
                else -> ""
            }
        }
        Bars(series.map { it.second }, labels, Modifier.padding(top = 8.dp), highlight = if (!s.monthly) series.size - 1 else -1, wide = !s.monthly && f.span != Span.WEEK,
            describe = { i -> val (at, m) = series[i]; (if (s.monthly) fmt(at, "LLLL yyyy", ru) else fmt(at, "d MMMM, EE", ru)).replaceFirstChar { it.uppercase() } + " — " + if (m == 0L) t2(ru, "без кино", "no films") else dur(m, ru) })
    }

    Section(t("Больше всего", "Top"))
    Tabs(listOf(t("Фильмы", "Films"), t("Сериалы", "Series"), t("Жанры", "Genres"), t("Страны", "Countries")), topTab, { topTab = it })
    var all by remember(topTab, f) { mutableStateOf(false) }
    when (topTab) {
        0, 1 -> {
            val rows = if (topTab == 0) s.topFilms else s.topSeries
            if (rows.isEmpty()) Text(t("Пока пусто.", "Nothing yet."), style = T.Muted)
            val top = rows.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
            rows.take(if (all) 50 else 10).forEachIndexed { i, r ->
                val value = if (topTab == 0) (if (r.stars > 0) "🍿".repeat(r.stars) else dur(r.minutes.toLong(), ru)) else if (ru) "${r.count} сер." else "${r.count} ep."
                TopRow(i + 1, r.name, listOf(r.sub, dur(r.minutes.toLong(), ru)).filter { it.isNotEmpty() }.joinToString(" · "), value, r.minutes / top.toFloat(), cover = r.cover, genre = r.genre, poster = true)
            }
            if (rows.size > 10) Btn(if (all) t("Свернуть", "Less") else t("Показать 50", "Show 50"), { all = !all }, Modifier.fillMaxWidth(), kind = Kind.GHOST, small = true)
        }
        2 -> {
            val g = s.genres
            val total = g.sumOf { it.second }.coerceAtLeast(1)
            GenreRing(g)
            g.forEachIndexed { i, (genre, m) ->
                val n = s.genreCounts.firstOrNull { it.first == genre }?.second ?: 0
                TopRow(i + 1, Words.genre(genre, ru), "${m * 100 / total}%" + if (n > 0) " · " + Words.films(n, ru) else "", dur(m.toLong(), ru), m / g.first().second.toFloat(), genre = genre)
            }
        }
        else -> {
            if (s.countries.isEmpty()) Text(t("У фильмов не указаны страны.", "No countries on the films."), style = T.Muted)
            val top = s.countries.firstOrNull()?.second?.coerceAtLeast(1) ?: 1
            s.countries.take(if (all) 50 else 10).forEachIndexed { i, (cn, n) -> TopRow(i + 1, cn, "", Words.films(n, ru), n / top.toFloat()) }
            if (s.countries.size > 10) Btn(if (all) t("Свернуть", "Less") else t("Показать все", "Show all"), { all = !all }, Modifier.fillMaxWidth(), kind = Kind.GHOST, small = true)
        }
    }

    Section(t("Когда ты смотришь", "When you watch"))
    Card(Modifier.fillMaxWidth()) {
        Text(t("По часам", "By hour").uppercase(), style = T.Eyebrow)
        val h = s.hoursOfDay
        Bars(h.toList(), (0 until 24).map { "$it" }, Modifier.padding(top = 8.dp), color = C.velvet.copy(alpha = .8f), every = 3, highlight = s.peakHour ?: -1,
            describe = { i -> "$i:00–${(i + 1) % 24}:00 — " + dur(h[i], ru) })
    }
    Card(Modifier.fillMaxWidth()) {
        Text(t("По дням недели", "By weekday").uppercase(), style = T.Eyebrow)
        val w = s.weekdays
        val names = if (ru) listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс") else listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
        val full = if (ru) listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье") else listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        Bars(w.toList(), names, Modifier.padding(top = 8.dp), highlight = s.favouriteWeekday ?: -1, describe = { i -> full[i] + " — " + dur(w[i], ru) })
    }

    if (s.decades.isNotEmpty()) {
        Section(t("Десятилетия", "Decades"), t("когда фильм вышел", "when the film came out"))
        Card(Modifier.fillMaxWidth()) {
            val d = s.decades
            Bars(d.map { it.second.toLong() }, d.map { "’" + (it.first % 100).toString().padStart(2, '0') }, describe = { i -> (if (ru) "${d[i].first}-е" else "${d[i].first}s") + " — " + Words.films(d[i].second, ru) })
        }
    }

    if (s.films.isNotEmpty()) {
        Section(t("Фильмы", "Films"), t("попкорн, длина, компания", "popcorn, length, company"))
        Card(Modifier.fillMaxWidth()) {
            if (s.rated.isNotEmpty()) {
                val top = s.popcorn.maxOf { it.second }.coerceAtLeast(1).toFloat()
                s.popcorn.forEach { (st, n) -> Line("🍿".repeat(st), n, n / top, C.gold) }
                Spacer(Modifier.height(10.dp))
            }
            val l = Stats.LENGTHS
            val names = listOf(t("до ${l[0]} мин", "under ${l[0]} min"), "${l[0]}–${l[1] - 1} " + t("мин", "min"), "${l[1]}–${l[2] - 1} " + t("мин", "min"), t("${l[2]} мин и больше", "${l[2]} min and more"))
            val topL = s.lengths.max().coerceAtLeast(1).toFloat()
            s.lengths.forEachIndexed { i, n -> Line(names[i], n, n / topL, C.velvet) }
            Spacer(Modifier.height(10.dp))
            val topC = s.company.maxOf { it.second }.coerceAtLeast(1).toFloat()
            s.company.forEach { (co, n) -> Line(companyName(co, ru), n, n / topC, C.muted) }
        }
    }

    Section(t("Рекорды", "Records"))
    Card(Modifier.fillMaxWidth()) {
        val (best, now) = remember(c) { Stats.streaks(c) }
        s.longestFilm?.let { Text(t("Самый длинный фильм: «${it.title}», ${dur(it.minutes.toLong(), true)}", "Longest film: ${it.title}, ${dur(it.minutes.toLong(), false)}"), style = T.Body) }
        s.busiestDay?.let { (at, m) -> Text(t("Самый киношный день: ${fmt(at, "d MMMM yyyy", true)} — ${dur(m, true)}", "Biggest day: ${fmt(at, "MMMM d, yyyy", false)} — ${dur(m, false)}"), style = T.Body) }
        s.binge?.let { (at, n) -> Text(t("Больше всего серий за день: $n, ${fmt(at, "d MMMM yyyy", true)}", "Most episodes in a day: $n, ${fmt(at, "MMMM d, yyyy", false)}"), style = T.Body) }
        s.mostRewatched?.let { (title, n) -> Text(t("Пересмотрен чаще всего: «$title», $n раза", "Watched most often: $title, $n times"), style = T.Body) }
        Text(t("Дней подряд с кино: рекорд $best, сейчас $now", "Days in a row with films: best $best, now $now"), style = T.Body)
        s.peakHour?.let { Text(t("Чаще всего смотришь около $it:00", "You watch most around $it:00"), style = T.Body) }
        if (s.rewatches > 0 || s.scenes > 0) Text(t("Пересмотров: ${s.rewatches} · сцен после титров: ${s.scenes}", "Rewatches: ${s.rewatches} · post-credits scenes: ${s.scenes}"), style = T.Body)
        Text(t("За всё время: ${Words.minutes(c.minutes, true)}, ${Words.films(c.seats.count { !it.season }, true)}", "All time: ${Words.minutes(c.minutes, false)}, ${Words.films(c.seats.count { !it.season }, false)}"), style = T.Small)
    }
}

private fun t2(ru: Boolean, r: String, e: String) = if (ru) r else e

/** A name, a count and a bar as long as its share of the biggest one. */
@Composable
private fun Line(label: String, n: Int, p: Float, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(label, style = T.Body, modifier = Modifier.weight(1f), maxLines = 1)
        Text("$n", style = T.Strong)
    }
    Spacer(Modifier.height(4.dp))
    Bar(p, Modifier.fillMaxWidth(), color = color)
    Spacer(Modifier.height(8.dp))
}

/** The year as rows of little squares, one per day, brighter the more was watched; tap a day to see what. */
@Composable
fun StatsCalendar(c: Cinema) {
    val ru = LocalRu.current
    val byDay = remember(c) { Stats.byDay(c) }
    val cal = Calendar.getInstance()
    fun epochDay(y: Int, m: Int, d: Int): Long { cal.clear(); cal.set(y, m, d); val ms = cal.timeInMillis; return (ms + cal.timeZone.getOffset(ms)).floorDiv(DAY) }
    val years = remember(c) { (byDay.keys.map { d -> Calendar.getInstance().apply { timeInMillis = d * DAY }.get(Calendar.YEAR) } + c.year).toSet().sortedDescending() }
    var year by rememberSaveable { mutableIntStateOf(c.year) }
    var picked by remember { mutableStateOf<Long?>(null) }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (y in years) Chip("$y", y == year, { year = y; picked = null })
    }
    val first = epochDay(year, 0, 1); val last = epochDay(year + 1, 0, 1)
    val inYear = byDay.filterKeys { it in first until last }
    Text(t("За $year: ${dur(inYear.values.sum(), true)}, дней с кино: ${inYear.size}", "In $year: ${dur(inYear.values.sum(), false)}, ${inYear.size} days with films"), style = T.Muted)
    val max = inYear.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val today = (c.now + cal.timeZone.getOffset(c.now)).floorDiv(DAY)
    for (m in 0 until 12) {
        cal.clear(); cal.set(year, m, 1)
        val len = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val start = epochDay(year, m, 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(fmt(cal.timeInMillis, "LLL", ru).trimEnd('.'), style = T.Small, modifier = Modifier.width(38.dp))
            Canvas(Modifier.weight(1f).aspectRatio(31 / 1.25f).pointerInput(year, m) {
                detectTapGestures { o -> val d = (o.x / (size.width / 31f)).toInt(); if (d < len) picked = start + d }
            }) {
                val w = size.width / 31f
                for (d in 0 until len) {
                    val day = start + d
                    val v = inYear[day] ?: 0L
                    val k = (v.toFloat() / max).coerceIn(0f, 1f)
                    val col = if (v == 0L) C.surface2 else C.gold.copy(alpha = .25f + .75f * k)
                    drawRoundRect(col, Offset(d * w + w * .1f, size.height * .1f), Size(w * .8f, size.height * .8f), CornerRadius(w * .2f))
                    if (day == picked || day == today) drawRoundRect(if (day == picked) C.velvet else C.ink, Offset(d * w + w * .1f, size.height * .1f), Size(w * .8f, size.height * .8f), CornerRadius(w * .2f), style = Stroke(1.5f))
                }
            }
        }
    }
    picked?.let { d ->
        val films = c.seats.filter { (it.at + cal.timeZone.getOffset(it.at)).floorDiv(DAY) == d }
        val eps = c.episodes.filter { (it.at + cal.timeZone.getOffset(it.at)).floorDiv(DAY) == d }
        Card(Modifier.fillMaxWidth()) {
            Text(fmt(d * DAY, "d MMMM yyyy, EEEE", ru), style = T.Name)
            if (films.isEmpty() && eps.isEmpty()) Text(t("Без кино.", "No films."), style = T.Muted)
            for (tk in films) Text((if (tk.season) "▣ " else "▶ ") + tk.title + if (tk.stars > 0) "  " + "🍿".repeat(tk.stars) else "", style = T.Body)
            for ((id, l) in eps.groupBy { it.series }) Text("▤ " + (c.title(id)?.title ?: "?") + " · " + (if (ru) "${l.size} сер." else "${l.size} ep."), style = T.Body)
        }
    }
}
