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
import androidx.compose.material3.Text
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
import app.reelkin.domain.Cinema
import app.reelkin.domain.Words
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Nav
import app.reelkin.ui.Overlay
import app.reelkin.ui.booth.episode
import app.reelkin.ui.components.Bar
import app.reelkin.ui.components.Btn
import app.reelkin.ui.components.BufImage
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
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(q, { q = it }, t("найти фильм или сериал", "find a film or a series"), Modifier.weight(1f), onDone = { nav.go(Overlay.Add(q)); q = "" })
            Btn("+", { nav.go(Overlay.Add(q)); q = "" })
        }
        if (c.running.isNotEmpty() || c.shelved.isNotEmpty()) {
            Section(t("Сейчас идёт", "Now running"), t("абонементы", "season tickets"))
            for (s in c.running) SeriesCard(s, nav)
            if (c.shelved.isNotEmpty()) {
                var open by remember { mutableStateOf(false) }
                Text(t("Снято с проката · ${c.shelved.size}", "Taken off · ${c.shelved.size}") + if (open) "  ▴" else "  ▾", style = T.Muted, modifier = Modifier.clickable { open = !open }.padding(vertical = 4.dp))
                if (open) for (s in c.shelved) SeriesCard(s, nav)
            }
        }
        Section(t("Ждут показа", "Coming up"), "${c.watch.size}")
        if (c.watch.isEmpty()) Text(t("Афиша пуста. Найди фильм — и он появится здесь.", "The bill is empty. Find a film and it shows up here."), style = T.Muted)
        for (row in c.watch.chunked(3)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (f in row) PosterTile(f, Modifier.weight(1f)) { nav.go(if (f.kind == app.reelkin.data.Kind.SERIES) Overlay.Series(f.id) else Overlay.Film(f.id)) }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Section(t("Корешки", "Stubs"), "${c.seats.size}")
        YearCard(c)
        if (c.seats.isEmpty()) Text(t("Здесь будут корешки от всего, что ты посмотришь.", "Stubs from everything you watch will be kept here."), style = T.Muted)
        for (tk in c.seats.asReversed()) Stub(tk, c.seatOf(tk), ru) { nav.go(Overlay.Stub(tk.id)) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PosterTile(f: Title, modifier: Modifier, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Column(modifier.clip(R8.Md).clickable(onClick = onClick).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(30f / 44f).clip(R8.Sm).background(C.surface2)) {
            BufImage(poster(ctx, f.title, f.genre, f.cover, 30, 44), Modifier.fillMaxSize(), scale = 3.6.dp)
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
            BufImage(poster(ctx, s.title, s.genre, s.cover, 24, 34), Modifier.clip(R8.Sm), scale = 1.8.dp)
            Column(Modifier.weight(1f)) {
                Text(s.title, style = T.Name)
                Text(t("сезон ${s.season} · серия ${s.episode} из ${s.perSeason} · ${s.minutes} мин", "season ${s.season} · episode ${s.episode} of ${s.perSeason} · ${s.minutes} min"), style = T.Muted)
                if (s.cliff) Text(t("оборвалось на самом интересном", "stopped at the worst moment"), style = T.Small.copy(color = C.velvet))
            }
        }
        TicketBook(s)
        if (s.status == Status.SHELVED) Btn(t("Вернуть в прокат", "Back on"), { co.write { office.update(s.copy(status = Status.RUNNING)) }; nav.toast(if (ru) "«${s.title}» снова в афише." else "${s.title} is back on.") }, kind = Kind.SOFT, small = true)
        else Flow {
            Btn(t("+1 серия", "+1 episode"), { episode(s, nav, ctx, ru) }, small = true)
            Btn(if (s.cliff) t("Отпустило", "Settled") else t("Оборвалось!", "Cliffhanger!"), {
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
private fun YearCard(c: Cinema) {
    val ru = LocalRu.current
    val films = c.thisYear.count { !it.season }
    Column(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).border(1.dp, C.line, R8.Lg).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${c.year}", style = T.Name, modifier = Modifier.weight(1f))
            Text(if (ru) "${Words.films(films, true)} из ${c.s.goal}" else "${films} of ${c.s.goal} films", style = T.Small)
        }
        Bar(films / c.s.goal.toFloat().coerceAtLeast(1f), Modifier.fillMaxWidth())
        Heatmap(c)
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

private fun date(ms: Long, ru: Boolean): String = SimpleDateFormat(if (ru) "d MMMM yyyy" else "MMM d, yyyy", if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH).format(Date(ms))

/** A torn ticket: the poster on the stub side, perforation, then the seat, the film and three words. */
@Composable
fun Stub(tk: Ticket, seat: Int, ru: Boolean, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val shape = RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(C.paper).clickable(onClick = onClick)) {
        Box(Modifier.padding(10.dp)) { BufImage(poster(ctx, tk.title, tk.genre, tk.cover, 24, 34), Modifier.clip(R8.Sm), scale = 1.9.dp) }
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
