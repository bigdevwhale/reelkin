package app.reelkin.ui.bill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import app.reelkin.ui.components.Tabs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelkin.container
import app.reelkin.data.Company
import app.reelkin.data.Credits
import app.reelkin.data.Status
import app.reelkin.data.Ticket
import app.reelkin.data.Title
import app.reelkin.data.Genre
import app.reelkin.domain.Browse
import app.reelkin.domain.Cinema
import app.reelkin.domain.Words
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Nav
import app.reelkin.ui.Overlay
import app.reelkin.ui.booth.episode
import app.reelkin.ui.components.Bar
import app.reelkin.ui.components.Btn
import app.reelkin.ui.components.BufImage
import app.reelkin.ui.components.Chip
import app.reelkin.ui.components.Sheet
import app.reelkin.ui.components.PosterImage
import app.reelkin.ui.components.Field
import app.reelkin.ui.components.Flow
import app.reelkin.ui.components.Kind
import app.reelkin.ui.components.Popcorn
import app.reelkin.ui.components.Section
import app.reelkin.ui.components.t
import app.reelkin.ui.poster
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun BillScreen(c: Cinema, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var q by rememberSaveable { mutableStateOf("") }
    // The filters live as one line, so they survive switching tabs; the order is kept in the settings.
    var kept by rememberSaveable { mutableStateOf("") }
    var filtering by remember { mutableStateOf(false) }
    val b = Browse.decode(kept).copy(query = q, sort = Browse.sortOf(c.s.stubSort))
    fun set(n: Browse) {
        kept = n.copy(query = "").encode()
        if (n.sort != b.sort) co.write { settings.update { it.copy(stubSort = n.sort.name) } }
    }
    val watch = remember(c.watch, kept, q) { b.titles(c.watch) }
    val running = remember(c.running, kept, q) { b.series(c.running) }
    val shelved = remember(c.shelved, kept, q) { b.series(c.shelved) }
    val stubs = remember(c.seats, kept, q, c.s.stubSort) { b.stubs(c.seats) }
    fun search() { nav.go(Overlay.Add(q)); q = "" }

    // Waiting, running, stubs: a page each; one the bill opens on is the one with something in it.
    var page by rememberSaveable { mutableIntStateOf(if (c.watch.isEmpty() && c.running.isNotEmpty()) 1 else 0) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(q, { q = it.take(60) }, t("найти фильм или сериал", "find a film or a series"), Modifier.weight(1f), onDone = ::search)
                Btn("+", ::search)
            }
            Tabs(listOf(t("Ждут", "Waiting"), t("Идут", "Running"), t("Корешки", "Stubs")), page, { page = it },
                counts = listOf(watch.size, running.size + shelved.size, stubs.size))
            if (c.titles.isNotEmpty() || c.seats.isNotEmpty()) Tools(b, page == 2, ::set, onFilter = { filtering = true }, onReset = { q = ""; set(Browse(sort = b.sort)) })
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (q.isNotBlank()) Text(t("Искать «${q.trim()}» в каталоге ›", "Look «${q.trim()}» up in the catalogue ›"), style = T.Strong.copy(color = C.gold),
                modifier = Modifier.clip(R8.Sm).clickable(onClick = ::search).padding(vertical = 4.dp))
            when (page) {
                0 -> {
                    if (c.watch.isEmpty()) Empty(t("Афиша пуста. Найди фильм или сериал — и он появится здесь.", "The bill is empty. Find a film or a series and it shows up here."), t("Найти", "Find"), ::search)
                    else if (watch.isEmpty()) Text(t("Под эти фильтры ничего не нашлось.", "Nothing fits these filters."), style = T.Muted)
                    for (row in watch.chunked(3)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (f in row) PosterTile(f, Modifier.weight(1f)) { nav.go(if (f.kind == app.reelkin.data.Kind.SERIES) Overlay.Series(f.id) else Overlay.Film(f.id)) }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                1 -> {
                    if (c.running.isEmpty() && c.shelved.isEmpty()) Empty(t("Сериалов пока нет. Добавь сериал и нажми «Смотрю» — абонемент появится здесь.",
                        "No series yet. Add one and tap «Watching»: its season ticket shows up here."), t("Найти сериал", "Find a series"), ::search)
                    else if (running.isEmpty() && shelved.isEmpty()) Text(t("Под эти фильтры ничего не нашлось.", "Nothing fits these filters."), style = T.Muted)
                    for (s in running) SeriesCard(s, nav)
                    if (shelved.isNotEmpty()) {
                        var open by remember { mutableStateOf(false) }
                        Text(t("Снято с проката · ${shelved.size}", "Taken off · ${shelved.size}") + if (open || b.narrowed) "  ▴" else "  ▾", style = T.Muted, modifier = Modifier.clickable { open = !open }.padding(vertical = 4.dp))
                        if (open || b.narrowed) for (s in shelved) SeriesCard(s, nav)
                    }
                }
                else -> {
                    if (!b.narrowed) YearCard(c) { nav.go(Overlay.Stats) }
                    if (c.seats.isEmpty()) Text(t("Здесь будут корешки от всего, что ты посмотришь.", "Stubs from everything you watch will be kept here."), style = T.Muted)
                    else if (stubs.isEmpty()) Text(t("Под эти фильтры ничего не нашлось.", "Nothing fits these filters."), style = T.Muted)
                    for (tk in stubs) Stub(tk, c.seatOf(tk), ru) { nav.go(Overlay.Stub(tk.id)) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (filtering) FilterSheet(c, b, ::set) { filtering = false }
}

/** How the bill is shown: the filter and the order as two buttons, then the filters that are on, as chips to take off. */
@Composable
private fun Tools(b: Browse, sorting: Boolean, set: (Browse) -> Unit, onFilter: () -> Unit, onReset: () -> Unit) {
    val ru = LocalRu.current
    Flow {
        Tool((if (ru) "Фильтр" else "Filter") + if (b.filters > 0) " · ${b.filters}" else "", b.filters > 0, onFilter)
        // The order is about stubs only, so it shows on their page.
        if (sorting) Box {
            var open by remember { mutableStateOf(false) }
            Tool(sortName(b.sort, ru) + " ▾", false) { open = true }
            DropdownMenu(open, { open = false }, containerColor = C.surface2) {
                for (s in Browse.Sort.entries) DropdownMenuItem(
                    text = { Text(sortName(s, ru), style = if (s == b.sort) T.Strong.copy(color = C.gold) else T.Body) },
                    onClick = { set(b.copy(sort = s)); open = false },
                )
            }
        }
        b.kind?.let { k -> Chip("✕ " + kindName(k, ru), true, { set(b.copy(kind = null)) }) }
        for (g in b.genres) Chip("✕ " + Words.genre(g, ru), true, { set(b.copy(genres = b.genres - g)) }, color = C.genre(g))
        if (b.stars > 0) Chip("✕ " + starsName(b.stars), true, { set(b.copy(stars = 0)) })
        b.year?.let { y -> Chip("✕ $y", true, { set(b.copy(year = null)) }) }
        b.company?.let { co -> Chip("✕ " + companyName(co, ru), true, { set(b.copy(company = null)) }) }
        if (b.filters > 1 || b.filters > 0 && b.query.isNotBlank()) Text(if (ru) "Сбросить" else "Reset", style = T.Strong.copy(color = C.velvet),
            modifier = Modifier.clip(CircleShape).clickable(onClick = onReset).padding(horizontal = 10.dp, vertical = 8.dp))
    }
}

/** A page with nothing on it yet: what goes here, and the way to put it there. */
@Composable
private fun Empty(text: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).border(1.dp, C.line, R8.Lg).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text, style = T.Muted)
        Btn(action, onAction, kind = Kind.GOLD, small = true)
    }
}

/** A plain button for how the bill is shown, gold-rimmed while something is on. */
@Composable
private fun Tool(text: String, on: Boolean, onClick: () -> Unit) {
    Text(text, style = T.Small.copy(color = if (on) C.gold else C.ink, fontSize = T.Small.fontSize * 1.1f), maxLines = 1,
        modifier = Modifier.clip(CircleShape).border(1.dp, if (on) C.gold else C.line, CircleShape).background(C.surface).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 8.dp))
}

/** Every filter in one sheet; the bill behind changes as they're tapped. */
@Composable
private fun FilterSheet(c: Cinema, b: Browse, set: (Browse) -> Unit, onDismiss: () -> Unit) {
    val ru = LocalRu.current
    val genres = remember(c) { (c.titles.map { it.genre } + c.seats.map { it.genre }).toSet() }
    val years = remember(c) { c.seats.map { Browse.yearOf(it.at) }.distinct().sortedDescending() }
    val shown = b.stubs(c.seats).size + b.titles(c.watch).size + b.series(c.running + c.shelved).size
    Sheet(onDismiss) {
        Text(t("Фильтр", "Filter"), style = T.Title)
        Section(t("Что", "What"))
        Flow {
            Chip(t("всё", "all"), b.kind == null, { set(b.copy(kind = null)) })
            for (k in app.reelkin.data.Kind.entries) Chip(kindName(k, ru), b.kind == k, { set(b.copy(kind = if (b.kind == k) null else k)) })
        }
        Section(t("Жанр", "Genre"), t("можно несколько", "any number"))
        Flow {
            for (g in Genre.entries.filter { it in genres || it in b.genres }) {
                val on = g in b.genres
                Chip(Words.genre(g, ru), on, { set(b.copy(genres = if (on) b.genres - g else b.genres + g)) }, color = C.genre(g))
            }
        }
        Section(t("Попкорн", "Popcorn"), t("у корешков", "on the stubs"))
        Flow {
            Chip(t("любой", "any"), b.stars == 0, { set(b.copy(stars = 0)) })
            for (n in 3..5) Chip(starsName(n), b.stars == n, { set(b.copy(stars = if (b.stars == n) 0 else n)) })
        }
        if (years.size > 1 || b.year != null) {
            Section(t("Год просмотра", "Year watched"))
            Flow {
                Chip(t("все", "all"), b.year == null, { set(b.copy(year = null)) })
                for (y in years) Chip("$y", b.year == y, { set(b.copy(year = if (b.year == y) null else y)) })
            }
        }
        Section(t("С кем", "Who with"))
        Flow {
            Chip(t("неважно", "anyone"), b.company == null, { set(b.copy(company = null)) })
            for (co in Company.entries) Chip(companyName(co, ru), b.company == co, { set(b.copy(company = if (b.company == co) null else co)) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("Сбросить", "Reset"), { set(Browse(sort = b.sort)) }, Modifier.weight(1f), kind = Kind.GHOST, enabled = b.filters > 0)
            Btn(if (ru) "Показать · $shown" else "Show · $shown", onDismiss, Modifier.weight(1f))
        }
    }
}

private fun sortName(s: Browse.Sort, ru: Boolean) = when (s) {
    Browse.Sort.NEWEST -> if (ru) "Сначала новые" else "Newest first"
    Browse.Sort.OLDEST -> if (ru) "Сначала старые" else "Oldest first"
    Browse.Sort.POPCORN -> if (ru) "По попкорну" else "By popcorn"
    Browse.Sort.TITLE -> if (ru) "По названию" else "By title"
    Browse.Sort.LENGTH -> if (ru) "Самые длинные" else "Longest first"
    Browse.Sort.RELEASE -> if (ru) "По году выхода" else "By release year"
}

private fun kindName(k: app.reelkin.data.Kind, ru: Boolean) = when (k) {
    app.reelkin.data.Kind.FILM -> if (ru) "фильмы" else "films"
    app.reelkin.data.Kind.SERIES -> if (ru) "сериалы" else "series"
}

private fun starsName(n: Int) = "🍿 " + if (n >= 5) "5" else "$n+"

fun companyName(co: Company, ru: Boolean) = when (co) {
    Company.ALONE -> if (ru) "один" else "alone"
    Company.TWO -> if (ru) "вдвоём" else "for two"
    Company.MANY -> if (ru) "компанией" else "with friends"
}

@Composable
private fun PosterTile(f: Title, modifier: Modifier, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Column(modifier.clip(R8.Md).clickable(onClick = onClick).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(30f / 44f).clip(R8.Sm).background(C.surface2)) {
            PosterImage(f.title, f.genre, f.cover, 30, 44, Modifier.fillMaxSize(), scale = 3.6.dp)
        }
        Text(f.title, style = T.Small.copy(color = C.ink), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(listOfNotNull(f.year?.toString(), f.minutes.takeIf { it > 0 }?.let { Words.hm(it) }).joinToString(" · "), style = T.Small)
    }
}

@Composable
fun SeriesCard(s: Title, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val co = ctx.container
    Column(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).border(1.dp, C.line, R8.Lg).clickable { nav.go(Overlay.Series(s.id)) }.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PosterImage(s.title, s.genre, s.cover, 24, 34, Modifier.clip(R8.Sm), scale = 1.8.dp)
            Column(Modifier.weight(1f)) {
                Text(s.title, style = T.Name)
                Text(t("сезон ${s.season} · серия ${s.episode} из ${s.perSeason} · ${s.minutes} мин", "season ${s.season} · episode ${s.episode} of ${s.perSeason} · ${s.minutes} min"), style = T.Muted)
                if (s.cliff) Text(t("серия закончилась на самом интересном", "stopped at the worst moment"), style = T.Small.copy(color = C.velvet))
            }
        }
        TicketBook(s)
        if (s.status == Status.SHELVED) Btn(t("Вернуть в прокат", "Back on"), { co.write { office.update(s.copy(status = Status.RUNNING)) }; nav.toast(if (ru) "«${s.title}» снова в афише." else "${s.title} is back on.") }, kind = Kind.SOFT, small = true)
        else Flow {
            Btn(t("+1 серия", "+1 episode"), { episode(s, nav, ctx, ru) }, small = true)
            Btn(if (s.cliff) t("Отпустило", "Settled") else t("На самом интересном!", "Cliffhanger!"), {
                co.write { office.update(s.copy(cliff = !s.cliff)) }
                if (!s.cliff) nav.say(if (ru) "На самом интересном? Я теперь не усну." else "At the worst moment? I won't sleep now.")
            }, kind = Kind.GHOST, small = true)
        }
    }
}

/** A booklet of tickets for the season: torn ones pale, the cliffhanger red. */
@Composable
fun TicketBook(s: Title) {
    Canvas(Modifier.fillMaxWidth().height(12.dp)) {
        val n = s.perSeason.coerceAtLeast(1); val gap = 2.dp.toPx(); val w = (size.width - gap * (n - 1)) / n
        for (i in 0 until n) {
            val col = when { i == s.episode - 1 && s.cliff -> C.velvet; i < s.episode -> C.line; else -> C.gold }
            drawRoundRect(col, Offset(i * (w + gap), 0f), Size(w, size.height), CornerRadius(2.dp.toPx()))
        }
    }
}

@Composable
private fun YearCard(c: Cinema, onStats: () -> Unit) {
    val ru = LocalRu.current
    val films = c.thisYear.count { !it.season }
    Column(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).border(1.dp, C.line, R8.Lg).clickable(onClick = onStats).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${c.year}", style = T.Name, modifier = Modifier.weight(1f))
            Text(if (ru) "${Words.films(films, true)} из ${c.s.goal}" else "${films} of ${c.s.goal} films", style = T.Small)
        }
        Bar(films / c.s.goal.toFloat().coerceAtLeast(1f), Modifier.fillMaxWidth())
        Heatmap(c)
        Text(t("Статистика ›", "Stats ›"), style = T.Strong.copy(color = C.gold))
    }
}

@Composable
private fun Heatmap(c: Cinema) {
    val cal = Calendar.getInstance().apply { set(c.year, 0, 1) }
    val off = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val today = Calendar.getInstance().apply { timeInMillis = c.now }.get(Calendar.DAY_OF_YEAR)
    val days = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
    Canvas(Modifier.fillMaxWidth().aspectRatio(53f / 7f)) {
        val cell = size.width / 53; val g = cell * .18f
        for (d in 1..days) {
            val i = d - 1 + off; val n = c.days[d] ?: 0
            val col = when { d > today -> C.surface2.copy(alpha = .5f); n == 0 -> C.surface2; n == 1 -> C.gold.copy(alpha = .55f); n == 2 -> C.gold; else -> C.velvet }
            drawRoundRect(col, Offset((i / 7) * cell, (i % 7) * cell), Size(cell - g, cell - g), CornerRadius(cell * .2f))
        }
    }
}

fun date(ms: Long, ru: Boolean): String = SimpleDateFormat(if (ru) "d MMMM yyyy" else "MMM d, yyyy", if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH).format(Date(ms))

/** A torn ticket: the poster on the stub side, perforation, then the seat, the film and three words. */
@Composable
fun Stub(tk: Ticket, seat: Int, ru: Boolean, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val shape = RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(C.paper).clickable(onClick = onClick)) {
        Box(Modifier.padding(10.dp)) { PosterImage(tk.title, tk.genre, tk.cover, 24, 34, Modifier.clip(R8.Sm), scale = 1.9.dp) }
        Canvas(Modifier.width(2.dp).height(84.dp).padding(vertical = 8.dp)) {
            drawLine(C.paperMuted.copy(alpha = .5f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text((Words.seat(seat, ru) + " · " + date(tk.at, ru)).uppercase(), style = T.Eyebrow.copy(color = C.velvet))
            Text(tk.title, style = T.Name.copy(color = C.paperInk), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tk.stars > 0) Popcorn(tk.stars, size = 14.dp)
                Text(listOfNotNull(tk.year?.toString(), tk.country.ifBlank { null }, Words.hm(tk.minutes)).joinToString(" · "), style = T.Small.copy(color = C.paperMuted))
            }
            if (tk.words.isNotBlank()) Text("«${tk.words}»", style = T.Muted.copy(color = C.paperInk))
            val badges = listOfNotNull(
                (if (ru) "сезон" else "season").takeIf { tk.season },
                (if (ru) "пересмотр" else "rewatch").takeIf { tk.rewatch },
                (if (ru) "после титров" else "post-credits").takeIf { tk.credits == Credits.SCENE },
                when (tk.company) { Company.TWO -> if (ru) "вдвоём" else "for two"; Company.MANY -> if (ru) "компанией" else "with friends"; else -> null },
            )
            if (badges.isNotEmpty()) Text(badges.joinToString(" · "), style = T.Small.copy(color = C.paperMuted))
        }
    }
}
