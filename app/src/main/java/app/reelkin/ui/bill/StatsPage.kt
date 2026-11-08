package app.reelkin.ui.bill

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.reelkin.domain.Browse
import app.reelkin.domain.Cinema
import app.reelkin.domain.Stats
import app.reelkin.domain.Words
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Nav
import app.reelkin.ui.components.Bar
import app.reelkin.ui.components.Card
import app.reelkin.ui.components.Chip
import app.reelkin.ui.components.Flow
import app.reelkin.ui.components.Page
import app.reelkin.ui.components.Section
import app.reelkin.ui.components.t
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.T
import java.util.Locale

/** What was watched, in numbers: this year or all of them. */
@Composable
fun StatsPage(c: Cinema, nav: Nav) {
    val ru = LocalRu.current
    // All time first: stubs brought over from elsewhere carry the days they were watched, mostly in years past.
    var year by rememberSaveable { mutableStateOf<Int?>(null) }
    val all = year == null
    val years = remember(c) { (c.seats.map { Browse.yearOf(it.at) } + c.episodes.map { Browse.yearOf(it.at) } + c.year).distinct().sortedDescending() }
    val s = remember(c, year) { Stats(c, year) }
    Page(t("Статистика", "Stats"), nav::close) {
        Flow {
            Chip(t("за всё время", "all time"), all, { year = null })
            for (y in years) Chip("$y", year == y, { year = y })
        }
        Card(Modifier.fillMaxWidth()) {
            Row {
                Num(s.films.size.toString(), t("фильмов", "films"), Modifier.weight(1f))
                Num(Words.fmt(s.hours), t("часов у экрана", "hours watched"), Modifier.weight(1f))
                Num(s.avgPopcorn?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "–", t("попкорна в среднем", "popcorn on average"), Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Row {
                Num(s.episodes.size.toString(), t("серий", "episodes"), Modifier.weight(1f))
                Num(s.seasons.toString(), t("сезонов досмотрено", "seasons finished"), Modifier.weight(1f))
                Num(s.countries.size.toString(), t("стран", "countries"), Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Row {
                Num(s.avgLength?.let { Words.hm(it) } ?: "–", t("средний фильм", "average film"), Modifier.weight(1f))
                Num(s.rewatches.toString(), t("пересмотров", "rewatches"), Modifier.weight(1f))
                Num(s.scenes.toString(), t("сцен после титров", "post-credits scenes"), Modifier.weight(1f))
            }
        }
        if (s.films.isEmpty() && s.episodes.isEmpty()) {
            Text(if (all) t("Пока пусто. Посмотри фильм — и здесь появятся цифры.", "Nothing yet. Watch a film and the numbers show up here.")
                else if (year == c.year) t("В этом году пока ничего. Загляни в «за всё время».", "Nothing this year yet. Try «all time».")
                else t("В $year году ничего не отмечено.", "Nothing logged in $year."), style = T.Muted)
            return@Page
        }

        // Month by month this year, or year by year over all of them.
        val months = if (ru) listOf("Я", "Ф", "М", "А", "М", "И", "И", "А", "С", "О", "Н", "Д") else listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        val columns = if (all) s.byYear.map { (y, n) -> "’" + y.toString().takeLast(2) to n } else s.byMonth.mapIndexed { i, n -> months[i] to n }
        val current = if (all) columns.lastIndex else if (year == c.year) java.util.Calendar.getInstance().apply { timeInMillis = c.now }.get(java.util.Calendar.MONTH) else -1
        if (columns.size > 1) {
            Section(if (all) t("Фильмы по годам", "Films by year") else t("Фильмы по месяцам", "Films by month"))
            Card(Modifier.fillMaxWidth()) { Columns(columns, current) }
        }

        if (s.genres.isNotEmpty()) {
            Section(t("Жанры", "Genres"))
            Card(Modifier.fillMaxWidth()) {
                val top = s.genres.first().second.toFloat()
                s.genres.forEachIndexed { i, (g, n) -> if (i > 0) Spacer(Modifier.height(10.dp)); Line(Words.genre(g, ru), n, n / top, C.genre(g)) }
            }
        }

        if (s.rated.isNotEmpty()) {
            Section(t("Попкорн", "Popcorn"), t("сколько дал фильмам", "what the films got"))
            Card(Modifier.fillMaxWidth()) {
                val top = s.popcorn.maxOf { it.second }.coerceAtLeast(1).toFloat()
                s.popcorn.forEachIndexed { i, (st, n) -> if (i > 0) Spacer(Modifier.height(10.dp)); Line("🍿".repeat(st), n, n / top, C.gold) }
            }
        }

        if (s.lengths.any { it > 0 }) {
            Section(t("Длина", "Length"))
            Card(Modifier.fillMaxWidth()) {
                val l = Stats.LENGTHS
                val names = listOf(t("до ${l[0]} мин", "under ${l[0]} min"), "${l[0]}–${l[1] - 1} " + t("мин", "min"), "${l[1]}–${l[2] - 1} " + t("мин", "min"), t("${l[2]} мин и больше", "${l[2]} min and more"))
                val top = s.lengths.max().coerceAtLeast(1).toFloat()
                s.lengths.forEachIndexed { i, n -> if (i > 0) Spacer(Modifier.height(10.dp)); Line(names[i], n, n / top, C.velvet) }
            }
        }

        if (s.decades.isNotEmpty()) {
            Section(t("Десятилетия", "Decades"), t("когда фильм вышел", "when the film came out"))
            Card(Modifier.fillMaxWidth()) {
                val top = s.decades.maxOf { it.second }.toFloat()
                s.decades.forEachIndexed { i, (d, n) -> if (i > 0) Spacer(Modifier.height(10.dp)); Line(if (ru) "${d}-е" else "${d}s", n, n / top, C.gold) }
            }
        }

        if (s.countries.isNotEmpty()) {
            Section(t("Страны", "Countries"))
            Card(Modifier.fillMaxWidth()) {
                val top = s.countries.first().second.toFloat()
                s.countries.take(8).forEachIndexed { i, (cn, n) -> if (i > 0) Spacer(Modifier.height(10.dp)); Line(cn, n, n / top, C.muted) }
                if (s.countries.size > 8) { Spacer(Modifier.height(10.dp)); Text(t("и ещё ${s.countries.size - 8}", "and ${s.countries.size - 8} more"), style = T.Small) }
            }
        }

        if (s.films.isNotEmpty()) {
            Section(t("С кем", "Who with"))
            Card(Modifier.fillMaxWidth()) {
                val top = s.company.maxOf { it.second }.coerceAtLeast(1).toFloat()
                s.company.forEachIndexed { i, (co, n) -> if (i > 0) Spacer(Modifier.height(10.dp)); Line(companyName(co, ru), n, n / top, C.velvet) }
            }
        }

        Section(t("Дни недели", "Weekdays"), t("фильмы и серии", "films and episodes"))
        Card(Modifier.fillMaxWidth()) {
            val days = if (ru) listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс") else listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
            val best = s.weekdays.indices.maxBy { s.weekdays[it] }
            Columns(days.zip(s.weekdays), best)
        }
    }
}

/** A big number with what it counts under it. */
@Composable
private fun Num(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = T.Number, maxLines = 1)
        Text(label, style = T.Small.copy(textAlign = TextAlign.Center), maxLines = 2)
    }
}

/** A name, a count and a bar as long as its share of the biggest one. */
@Composable
private fun Line(label: String, n: Int, p: Float, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(label, style = T.Body, modifier = Modifier.weight(1f), maxLines = 1)
        Text("$n", style = T.Strong)
    }
    Spacer(Modifier.height(4.dp))
    Bar(p, Modifier.fillMaxWidth(), color = color)
}

/** Columns with their counts above and labels below; [current] stands out in gold. */
@Composable
private fun Columns(columns: List<Pair<String, Int>>, current: Int) {
    val top = columns.maxOf { it.second }.coerceAtLeast(1).toFloat()
    Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        columns.forEachIndexed { i, (_, n) ->
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                // Many narrow columns of three-digit counts: a smaller figure that's never cut short.
                if (n > 0) Text("$n", style = if (columns.size > 10 && top >= 100) T.Small.copy(fontSize = T.Small.fontSize * .8f) else T.Small, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Visible)
                Spacer(Modifier.height(3.dp))
                Box(Modifier.fillMaxWidth().height((92 * (n / top)).coerceAtLeast(3f).dp).clip(RoundedCornerShape(4.dp))
                    .background(if (n == 0) C.line else if (i == current) C.gold else C.velvet.copy(alpha = .7f)))
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        columns.forEach { (label, _) -> Text(label, style = T.Small, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
    }
}
