package app.reelkin.ui.saga

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelkin.R
import app.reelkin.domain.Cinema
import app.reelkin.domain.Filter
import app.reelkin.domain.Goals
import app.reelkin.domain.Span
import app.reelkin.domain.Stats
import app.reelkin.domain.Words
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Nav
import app.reelkin.ui.Overlay
import app.reelkin.ui.bill.StatsCalendar
import app.reelkin.ui.bill.companyName
import app.reelkin.ui.bill.dur
import app.reelkin.ui.components.Bar
import app.reelkin.ui.components.Bars
import app.reelkin.ui.components.Btn
import app.reelkin.ui.components.GenreRing
import app.reelkin.ui.components.Kind
import app.reelkin.ui.components.Sheet
import app.reelkin.ui.components.TopRow
import app.reelkin.ui.components.t
import app.reelkin.ui.goals.Glyph
import app.reelkin.ui.goals.GoalsBlock
import app.reelkin.ui.hall.HallScreen
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun fmt(ms: Long, pattern: String, ru: Boolean) = SimpleDateFormat(pattern, if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH).format(Date(ms))
private val DAYS_RU = listOf("понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье")
private val DAYS_EN = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

fun defaultFilter(c: Cinema) = Filter(span = if (c.thisYear.count { !it.season } >= 5) Span.YEAR else Span.ALL)

/**
 * The saga as a quiet dashboard: two pickers on top (when, what), one big number, the chart, a grid of facts that open
 * into their whole story, the goals, and the hall, the calendar, the awards and the postcards as plain rows.
 */
@Composable
fun SagaScreen(c: Cinema, nav: Nav) {
    val ru = LocalRu.current
    val f = nav.sagaFilter ?: defaultFilter(c)
    val s = remember(c, f) { Stats(c, f) }
    val years = remember(c) { (c.seats.map { c.yearOf(it.at) } + c.episodes.map { c.yearOf(it.at) }).toSet().filter { it < c.year - 1 }.sortedDescending() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t("Сага", "Saga"), style = T.Title, modifier = Modifier.weight(1f))
            app.reelkin.ui.cards.ShareButton({ nav.go(Overlay.Cards) })
        }
        app.reelkin.ui.components.FirstVisit(app.reelkin.ui.components.Tip.SAGA, c.s)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val spans = listOf(Span.ALL to t("Всё время", "All time"), Span.YEAR to "${c.year}", Span.LAST_YEAR to "${c.year - 1}", Span.MONTH to t("Последний месяц", "Last month"), Span.WEEK to t("Последняя неделя", "Last week"))
            val spanName = f.year?.toString() ?: spans.first { it.first == f.span }.second
            Picker(spanName, spans.map { it.second } + years.map { "$it" }) { i ->
                nav.sagaFilter = if (i < spans.size) f.copy(span = spans[i].first, year = null) else f.copy(year = years[i - spans.size])
            }
            val kinds = listOf(t("Фильмы и сериалы", "Films and series"), t("Только фильмы", "Films only"), t("Только сериалы", "Series only"))
            Picker(kinds[if (f.films && f.series) 0 else if (f.films) 1 else 2], kinds) { i ->
                nav.sagaFilter = f.copy(films = i != 2, series = i != 1)
            }
        }

        // The one big number, and the rest of the totals in a line under it.
        Column(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).border(1.dp, C.line, R8.Lg).padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Words.fmt(s.hours), style = T.Display.copy(color = C.gold))
                Text(" " + t("часов у экрана", "hours watched"), style = T.Muted, modifier = Modifier.padding(bottom = 6.dp))
            }
            Text(listOf(Words.films(s.films.size, ru), if (ru) "${s.episodes.size} " + Words.plural(s.episodes.size, "серия", "серии", "серий") else "${s.episodes.size} episodes",
                if (ru) "${s.days} " + Words.plural(s.days, "день", "дня", "дней") + " с кино" else "${s.days} days with films").joinToString(" · "), style = T.Body)
            val series = s.series
            if (series.size > 1) {
                Spacer(Modifier.height(10.dp))
                val labels = series.mapIndexed { i, (at, _) ->
                    when {
                        s.monthly -> fmt(at, "LLL", ru).take(1).uppercase().let { m -> if (series.size <= 13) m else if (fmt(at, "M", ru) == "1" || i == 0) "’" + fmt(at, "yy", ru) else "" }
                        f.span == Span.WEEK -> fmt(at, "EE", ru)
                        i == 0 || i == series.size / 2 || i == series.size - 1 -> fmt(at, "d MMM", ru)
                        else -> ""
                    }
                }
                Bars(series.map { it.second }, labels, highlight = if (!s.monthly) series.size - 1 else -1, wide = !s.monthly && f.span != Span.WEEK,
                    describe = { i -> val (at, m) = series[i]; (if (s.monthly) fmt(at, "LLLL yyyy", ru) else fmt(at, "d MMMM, EE", ru)).replaceFirstChar { it.uppercase() } + " — " + if (m == 0L) (if (ru) "без кино" else "no films") else dur(m, ru) })
            }
        }

        if (s.moments.isEmpty()) Text(t("За это время ничего не отмечено.", "Nothing logged in this span."), style = T.Muted)
        else {
            // The pet's word on the span.
            PetSays(c, s, f)
            // Facts, two to a row, each with a little picture; each opens into its whole story.
            val facts = buildList<Triple<@Composable () -> Unit, Pair<String, String>, String>> {
                s.topFilms.firstOrNull()?.let { r -> add(Triple({ app.reelkin.ui.components.PosterImage(r.name, r.genre ?: app.reelkin.data.Genre.DRAMA, r.cover, 24, 34, Modifier.clip(R8.Sm), scale = 1.6.dp) }, r.name to t("лучший фильм", "best film"), "best")) }
                s.genres.firstOrNull()?.let { (g, m) -> add(Triple({ MiniRing(s.genres) }, Words.genre(g, ru) to (t("жанр № 1 · ", "top genre · ") + "${m * 100 / s.genres.sumOf { it.second }.coerceAtLeast(1)}%"), "genres")) }
                s.favouriteWeekday?.let { d -> add(Triple({ MiniBars(s.weekdays.toList(), d) }, (if (ru) DAYS_RU else DAYS_EN)[d].replaceFirstChar { it.uppercase() } + (s.peakHour?.let { ", $it:00" } ?: "") to t("чаще всего смотришь", "when you watch most"), "when")) }
                if (s.decades.isNotEmpty()) { val top = s.decades.maxBy { it.second }; add(Triple({ MiniBars(s.decades.map { it.second.toLong() }, s.decades.indexOf(top)) }, (if (ru) "${top.first}-е" else "the ${top.first}s") to t("любимое десятилетие", "favourite decade"), "decades")) }
                s.countries.firstOrNull()?.let { (cn, _) -> add(Triple({ Glyph(R.drawable.sec_globe, C.gold, 22, box = 44) }, cn to (if (ru) "страна № 1 · всего ${s.countries.size}" else "top country · ${s.countries.size} in all"), "countries")) }
                s.avgPopcorn?.let { v -> add(Triple({ app.reelkin.ui.components.Popcorn(kotlin.math.round(v).toInt(), size = 16.dp) }, String.format(Locale.ROOT, "%.1f", v) to t("попкорна в среднем", "popcorn on average"), "films")) }
                val (best, _) = Stats.streaks(c)
                add(Triple({ Streak(best) }, (if (ru) "$best " + Words.plural(best, "день", "дня", "дней") else "$best days") to t("подряд с кино — рекорд", "in a row, your best"), "records"))
                s.topSeries.firstOrNull()?.let { r -> add(Triple({ app.reelkin.ui.components.PosterImage(r.name, r.genre ?: app.reelkin.data.Genre.DRAMA, r.cover, 24, 34, Modifier.clip(R8.Sm), scale = 1.6.dp) }, r.name to (if (ru) "сериал № 1 · ${r.count} сер." else "top series · ${r.count} ep."), "best")) }
            }
            for (row in facts.chunked(2)) Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for ((pic, text, key) in row) Fact(pic, text.first, text.second, Modifier.weight(1f).fillMaxHeight()) { nav.go(Overlay.SagaDetail(key)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // The goals, open.
        val active = c.goals.filter { !it.archived }
        Column(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).border(1.dp, C.line, R8.Lg).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Glyph(R.drawable.sec_target, C.gold)
                Text(t("Цели", "Goals"), style = T.Name, modifier = Modifier.weight(1f))
                if (active.isNotEmpty()) Text(if (ru) "${active.count { Goals.progress(c, it).done }} из ${active.size}" else "${active.count { Goals.progress(c, it).done }} of ${active.size}", style = T.Small)
            }
            GoalsBlock(c, nav)
        }

        // Achievements: the count, the latest pictures, the page a tap away.
        val got = c.feats.filter { it.level > 0 }
        Column(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).border(1.dp, C.line, R8.Lg).clickable { nav.go(Overlay.Feats) }.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Glyph(R.drawable.sec_trophy, C.gold)
                Text(t("Достижения", "Achievements"), style = T.Name, modifier = Modifier.weight(1f))
                Text(if (ru) "${got.size} из ${c.feats.size} ›" else "${got.size} of ${c.feats.size} ›", style = T.Small)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val show = (got.sortedByDescending { it.level.toFloat() / it.max } + c.feats.filter { it.level == 0 }.sortedByDescending { it.part }).take(6)
                for (fs in show) Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { FeatIcon(fs.f, fs.level > 0, 44.dp) }
            }
        }

        // The rest, a tap away.
        Column(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).border(1.dp, C.line, R8.Lg)) {
            Link(R.drawable.sec_calendar, t("Календарь", "Calendar"), t("каждый день года", "every day of the year")) { nav.go(Overlay.SagaDetail("calendar")) }
            Link(R.drawable.sec_seats, t("Зал", "The hall"), if (ru) "зал № ${c.seats.size / 100 + 1} · ${c.seats.size % 100} из 100 мест" else "hall ${c.seats.size / 100 + 1} · ${c.seats.size % 100} of 100 seats") { nav.go(Overlay.SagaDetail("hall")) }
            Link(R.drawable.sec_trophy, t("Премии года", "The year's awards"), if (c.s.awards.isEmpty()) t("статуэтки вручаешь ты", "you hand out the statuettes") else (if (ru) "статуэток: ${c.s.awards.size}" else "${c.s.awards.size} statuettes")) { nav.go(Overlay.SagaDetail("awards")) }
            Link(R.drawable.sec_star, t("Открытки", "Postcards"), t("для ленты и сторис", "for a feed or a story"), last = true) { nav.go(Overlay.Cards) }
        }
    }
}

/** The pet, small, and what it makes of the span. */
@Composable
private fun PetSays(c: Cinema, s: Stats, f: Filter) {
    val ru = LocalRu.current
    val p = c.pet?.takeIf { it.pet.hatched } ?: return
    val g = s.genres.firstOrNull()?.first
    val period = when {
        f.year != null -> if (ru) "в ${f.year}-м" else "in ${f.year}"
        f.span == Span.ALL -> if (ru) "за всё время" else "all in all"
        f.span == Span.YEAR -> if (ru) "в этом году" else "this year"
        f.span == Span.LAST_YEAR -> if (ru) "в прошлом году" else "last year"
        f.span == Span.MONTH -> if (ru) "за месяц" else "this month"
        else -> if (ru) "за неделю" else "this week"
    }
    val line = buildString {
        append(if (ru) "${period.replaceFirstChar { it.uppercase() }} мы посмотрели ${Words.films(s.films.size, true)}" else "${period.replaceFirstChar { it.uppercase() }} we watched ${Words.films(s.films.size, false)}")
        if (s.episodes.isNotEmpty()) append(if (ru) " и ${s.episodes.size} " + Words.plural(s.episodes.size, "серию", "серии", "серий") else " and ${s.episodes.size} episodes")
        append(". ")
        if (g != null) append(if (ru) "Чаще всего — ${Words.genre(g, true)}. ${Words.react(g, true)}" else "Mostly ${Words.genre(g, false)}. ${Words.react(g, false)}")
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.height(64.dp), contentAlignment = Alignment.BottomCenter) { app.reelkin.ui.components.BufImage(app.reelkin.ui.art.PetArt.portrait(p.species, p.stage, p.pet.shiny, p.form), scale = 1.dp) }
        Column(Modifier.weight(1f).clip(R8.Lg).background(C.paper).padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(c.name(p, ru).uppercase(), style = T.Eyebrow.copy(color = C.velvet))
            Text(line, style = T.Body.copy(color = C.paperInk))
        }
    }
}

@Composable
private fun MiniRing(g: List<Pair<app.reelkin.data.Genre, Int>>) {
    val total = g.sumOf { it.second }.toFloat().coerceAtLeast(1f)
    androidx.compose.foundation.Canvas(Modifier.size(46.dp)) {
        var a = -90f
        for ((genre, n) in g) {
            val sw = n / total * 360f
            drawArc(C.genre(genre), a, (sw - 2f).coerceAtLeast(.5f), false, style = androidx.compose.ui.graphics.drawscope.Stroke(size.width * .2f))
            a += sw
        }
    }
}

@Composable
private fun MiniBars(values: List<Long>, hi: Int) {
    val top = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    androidx.compose.foundation.Canvas(Modifier.size(width = 84.dp, height = 44.dp)) {
        val w = size.width / values.size
        values.forEachIndexed { i, v ->
            val h = (v.toFloat() / top * size.height).coerceAtLeast(3f)
            drawRoundRect(if (i == hi) C.gold else C.line, androidx.compose.ui.geometry.Offset(i * w + w * .18f, size.height - h),
                androidx.compose.ui.geometry.Size(w * .64f, h), androidx.compose.ui.geometry.CornerRadius(w * .2f))
        }
    }
}

/** The best run as a row of little lights, up to ten. */
@Composable
private fun Streak(n: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(n.coerceIn(1, 10)) { Box(Modifier.size(9.dp).clip(CircleShape).background(C.gold.copy(alpha = .4f + .6f * (it + 1) / n.coerceIn(1, 10)))) }
        if (n > 10) Text("+", style = T.Small.copy(color = C.gold))
    }
}

/** A pill that opens a short list to choose from. */
@Composable
private fun Picker(value: String, options: List<String>, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Text("$value  ▾", style = T.Strong.copy(color = C.ink), maxLines = 1,
            modifier = Modifier.clip(CircleShape).background(C.surface).border(1.dp, C.line, CircleShape).clickable { open = true }.padding(horizontal = 14.dp, vertical = 9.dp))
        DropdownMenu(open, { open = false }, containerColor = C.surface2, modifier = Modifier.heightIn(max = 420.dp)) {
            options.forEachIndexed { i, o -> DropdownMenuItem(text = { Text(o, style = if (o == value) T.Strong.copy(color = C.gold) else T.Body) }, onClick = { onPick(i); open = false }) }
        }
    }
}

@Composable
private fun Fact(picture: @Composable () -> Unit, value: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(R8.Lg).background(C.surface).border(1.dp, C.line, R8.Lg).clickable(onClick = onClick).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.height(54.dp), contentAlignment = Alignment.CenterStart) { picture() }
        Column {
            Text(value, style = T.Strong, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(label, style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Link(icon: Int, title: String, sub: String, last: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Glyph(icon, C.muted)
        Column(Modifier.weight(1f)) { Text(title, style = T.Strong); Text(sub, style = T.Small, maxLines = 1) }
        Text("›", style = T.Title.copy(color = C.faint))
    }
    if (!last) Box(Modifier.fillMaxWidth().padding(start = 62.dp).height(1.dp).background(C.line))
}

/** A fact of the saga, the whole of it, over the screen. */
@Composable
fun SagaDetail(c: Cinema, key: String, nav: Nav) {
    val ru = LocalRu.current
    val f = nav.sagaFilter ?: defaultFilter(c)
    val s = remember(c, f) { Stats(c, f) }
    Sheet(nav::close) {
        when (key) {
            "best" -> {
                var more by remember { mutableStateOf(false) }
                Head(R.drawable.sec_star, t("Лучшее", "The best"))
                if (s.topFilms.isNotEmpty()) Text(t("Фильмы", "Films").uppercase(), style = T.Eyebrow)
                val tf = s.topFilms.take(if (more) 30 else 8)
                val topM = tf.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
                tf.forEachIndexed { i, r -> TopRow(i + 1, r.name, r.sub, if (r.stars > 0) "🍿".repeat(r.stars) else dur(r.minutes.toLong(), ru), r.minutes / topM.toFloat(), cover = r.cover, genre = r.genre, poster = true) }
                if (s.topSeries.isNotEmpty()) Text(t("Сериалы", "Series").uppercase(), style = T.Eyebrow, modifier = Modifier.padding(top = 6.dp))
                val ts = s.topSeries.take(if (more) 30 else 5)
                val topS = ts.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
                ts.forEachIndexed { i, r -> TopRow(i + 1, r.name, dur(r.minutes.toLong(), ru), if (ru) "${r.count} сер." else "${r.count} ep.", r.minutes / topS.toFloat(), cover = r.cover, genre = r.genre, poster = true) }
                if (s.topFilms.size > 8 || s.topSeries.size > 5) Btn(if (more) t("Свернуть", "Less") else t("Показать больше", "Show more"), { more = !more }, Modifier.fillMaxWidth(), kind = Kind.GHOST, small = true)
            }
            "genres" -> {
                Head(R.drawable.sec_palette, t("Жанры", "Genres"))
                val g = s.genres
                val total = g.sumOf { it.second }.coerceAtLeast(1)
                GenreRing(g)
                g.forEachIndexed { i, (genre, m) -> TopRow(i + 1, Words.genre(genre, ru), "${m * 100 / total}%", dur(m.toLong(), ru), m / g.first().second.toFloat(), genre = genre) }
            }
            "countries" -> {
                Head(R.drawable.sec_globe, t("Страны", "Countries"))
                val top = s.countries.firstOrNull()?.second?.coerceAtLeast(1) ?: 1
                s.countries.forEachIndexed { i, (cn, n) -> TopRow(i + 1, cn, "", Words.films(n, ru), n / top.toFloat()) }
            }
            "when" -> {
                Head(R.drawable.sec_clock, t("Когда смотришь", "When you watch"))
                Text(t("По часам", "By hour").uppercase(), style = T.Eyebrow)
                val h = s.hoursOfDay
                Bars(h.toList(), (0 until 24).map { "$it" }, color = C.velvet.copy(alpha = .8f), every = 3, highlight = s.peakHour ?: -1, describe = { i -> "$i:00–${(i + 1) % 24}:00 — " + dur(h[i], ru) })
                Text(t("По дням недели", "By weekday").uppercase(), style = T.Eyebrow)
                val w = s.weekdays
                val names = if (ru) listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс") else listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                Bars(w.toList(), names, highlight = s.favouriteWeekday ?: -1, describe = { i -> (if (ru) DAYS_RU else DAYS_EN)[i] + " — " + dur(w[i], ru) })
            }
            "decades" -> {
                Head(R.drawable.sec_hourglass, t("Десятилетия", "Decades"))
                val d = s.decades
                Bars(d.map { it.second.toLong() }, d.map { "’" + (it.first % 100).toString().padStart(2, '0') }, describe = { i -> (if (ru) "${d[i].first}-е" else "${d[i].first}s") + " — " + Words.films(d[i].second, ru) })
            }
            "films" -> {
                Head(R.drawable.sec_popcorn, t("Попкорн, длина, компания", "Popcorn, length, company"))
                if (s.rated.isNotEmpty()) { val top = s.popcorn.maxOf { it.second }.coerceAtLeast(1).toFloat(); s.popcorn.forEach { (st, n) -> Line("🍿".repeat(st), n, n / top, C.gold) } }
                val l = Stats.LENGTHS
                val names = listOf(t("до ${l[0]} мин", "under ${l[0]} min"), "${l[0]}–${l[1] - 1} " + t("мин", "min"), "${l[1]}–${l[2] - 1} " + t("мин", "min"), t("${l[2]} мин и больше", "${l[2]} min and more"))
                val topL = s.lengths.max().coerceAtLeast(1).toFloat()
                s.lengths.forEachIndexed { i, n -> Line(names[i], n, n / topL, C.velvet) }
                val topC = s.company.maxOf { it.second }.coerceAtLeast(1).toFloat()
                s.company.forEach { (co, n) -> Line(companyName(co, ru), n, n / topC, C.muted) }
            }
            "records" -> {
                Head(R.drawable.sec_medal, t("Рекорды", "Records"))
                val (best, now) = Stats.streaks(c)
                Row2(t("Дней подряд с кино", "Days in a row with films"), t("рекорд $best, сейчас $now", "best $best, now $now"))
                s.longestFilm?.let { Row2(t("Самый длинный фильм", "Longest film"), "«${it.title}», ${dur(it.minutes.toLong(), ru)}") }
                s.busiestDay?.let { (at, m) -> Row2(t("Самый киношный день", "Biggest day"), fmt(at, if (ru) "d MMMM yyyy" else "MMMM d, yyyy", ru) + " — " + dur(m, ru)) }
                s.binge?.let { (at, n) -> Row2(t("Больше всего серий за день", "Most episodes in a day"), "$n · " + fmt(at, if (ru) "d MMMM yyyy" else "MMMM d, yyyy", ru)) }
                s.mostRewatched?.let { (title, n) -> Row2(t("Пересмотрен чаще всего", "Watched most often"), "«$title» · $n") }
                Row2(t("Пересмотры · сцены после титров", "Rewatches · post-credits scenes"), "${s.rewatches} · ${s.scenes}")
                Row2(t("Новых стран · десятилетий", "New countries · decades"), "${s.newCountries} · ${s.newDecades}")
            }
            "calendar" -> { Head(R.drawable.sec_calendar, t("Календарь", "Calendar")); StatsCalendar(c) }
            "hall" -> { Head(R.drawable.sec_seats, t("Зал", "The hall")); HallScreen(c, nav) }
            "awards" -> { Head(R.drawable.sec_trophy, t("Премии года", "The year's awards")); HallScreen(c, nav, awards = true) }
        }
    }
}

@Composable
private fun Head(icon: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Glyph(icon, C.gold, 22, box = 42)
        Text(title, style = T.Title)
    }
}

@Composable
private fun Row2(label: String, value: String) {
    Column(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface2).padding(horizontal = 12.dp, vertical = 9.dp)) {
        Text(label, style = T.Small)
        Text(value, style = T.Body)
    }
}

@Composable
private fun Line(label: String, n: Int, p: Float, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(label, style = T.Body, modifier = Modifier.weight(1f), maxLines = 1)
        Text("$n", style = T.Strong)
    }
    Bar(p, Modifier.fillMaxWidth(), color = color)
}
