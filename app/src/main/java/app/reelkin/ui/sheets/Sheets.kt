package app.reelkin.ui.sheets

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import app.reelkin.container
import app.reelkin.data.Award
import app.reelkin.data.Company
import app.reelkin.data.Credits
import app.reelkin.data.Kind as TKind
import app.reelkin.data.Seance
import app.reelkin.data.Status
import app.reelkin.data.Ticket
import app.reelkin.data.Title
import app.reelkin.domain.Cinema
import app.reelkin.domain.Decor
import app.reelkin.domain.Gains
import app.reelkin.domain.Mood
import app.reelkin.domain.Picker
import app.reelkin.domain.Words
import app.reelkin.domain.gains
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Nav
import app.reelkin.ui.Overlay
import app.reelkin.ui.Tab
import app.reelkin.ui.art.Art
import app.reelkin.ui.art.Icons
import app.reelkin.ui.art.Mood as PetMood
import app.reelkin.ui.art.PetArt
import app.reelkin.ui.art.PixelCanvas
import app.reelkin.ui.art.Pose
import app.reelkin.ui.art.hash
import app.reelkin.ui.booth.episode
import app.reelkin.ui.components.Btn
import app.reelkin.ui.components.BufImage
import app.reelkin.ui.components.PosterImage
import app.reelkin.ui.components.Chip
import app.reelkin.ui.components.Field
import app.reelkin.ui.components.Flow
import app.reelkin.ui.components.Kind
import app.reelkin.ui.components.Popcorn
import app.reelkin.ui.components.Sheet
import app.reelkin.ui.components.Tag
import app.reelkin.ui.components.t
import app.reelkin.ui.hall.Letter
import app.reelkin.ui.hall.Stamp
import app.reelkin.ui.components.Bar
import app.reelkin.ui.components.Card
import app.reelkin.ui.components.Gap
import app.reelkin.ui.components.Section
import app.reelkin.ui.poster
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T
import kotlinx.coroutines.delay
import app.reelkin.data.DAY
import androidx.compose.runtime.mutableLongStateOf
import java.util.Calendar

fun Title.seance(rewatch: Boolean = false) = Seance(title, minutes, genre, year, country, cover, id, rewatch, System.currentTimeMillis())
fun Ticket.seance() = Seance(title, minutes, genre, year, country, cover, -1, true, System.currentTimeMillis())

@Composable
fun BigPoster(title: String, genre: app.reelkin.data.Genre, cover: String?, scale: Float = 3.4f) {
    val ctx = LocalContext.current
    PosterImage(title, genre, cover, 30, 44, Modifier.clip(R8.Md), scale = scale.dp)
}

@Composable
fun Facts(year: Int?, country: String, minutes: Int) =
    Text(listOfNotNull(year?.toString(), country.ifBlank { null }, minutes.takeIf { it > 0 }?.let { Words.hm(it) }).joinToString(" · "), style = T.Muted)

/** What watching this film would bring, so the bill reads like a promise. */
@Composable
fun Brings(c: Cinema, title: String, genre: app.reelkin.data.Genre, year: Int?, country: String) {
    val ru = LocalRu.current
    val l = ArrayList<String>()
    year?.let { if (Cinema.decade(it) !in c.decades) l += if (ru) "новое десятилетие: ${Cinema.decade(it)}-е" else "a new decade: the ${Cinema.decade(it)}s" }
    if (country.isNotBlank() && country !in c.countries) l += if (ru) "штамп: $country" else "a stamp: $country"
    c.pet?.takeIf { it.pet.hatched && genre in app.reelkin.domain.Pets.genres(it.species) }?.let { l += if (ru) "${c.name(it, true)} такое любит: сытнее в полтора раза" else "${c.name(it, false)} loves these: one and a half times the food" }
    l += (if (ru) "в зал придёт " else "in comes ") + Words.who(genre.who, ru)
    Column(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface2).padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(t("Если посмотреть", "If you watch it").uppercase(), style = T.Eyebrow)
        for (s in l) Text("+  $s", style = T.Body)
    }
}

/** The day it was watched: today, yesterday or any earlier day from the calendar. The time of day is kept. */
@Composable
fun WatchedOn(at: Long, onPick: (Long) -> Unit) {
    val ru = LocalRu.current
    val ctx = LocalContext.current
    val now = System.currentTimeMillis()
    fun day(ms: Long) = Calendar.getInstance().apply { timeInMillis = ms }.let { it.get(Calendar.YEAR) * 400 + it.get(Calendar.DAY_OF_YEAR) }
    fun onDay(ms: Long) = Calendar.getInstance().apply { timeInMillis = at }.also { c ->
        val d = Calendar.getInstance().apply { timeInMillis = ms }
        c.set(d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis.coerceAtMost(now)
    val today = day(at) == day(now)
    val yesterday = day(at) == day(now - DAY)
    Text(t("Когда смотрел", "Watched on").uppercase(), style = T.Eyebrow)
    Flow {
        Chip(t("сегодня", "today"), today, { onPick(onDay(now)) })
        Chip(t("вчера", "yesterday"), yesterday, { onPick(onDay(now - DAY)) })
        Chip(if (today || yesterday) t("другой день…", "another day…") else app.reelkin.ui.bill.date(at, ru), !today && !yesterday, {
            val c = Calendar.getInstance().apply { timeInMillis = at }
            DatePickerDialog(ctx, { _, y, m, d -> onPick(onDay(Calendar.getInstance().apply { set(y, m, d) }.timeInMillis)) },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
        })
    }
}

/* ---------------- a film on the bill ---------------- */

@Composable
fun FilmSheet(c: Cinema, f: Title, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var sure by remember { mutableStateOf(false) }
    Sheet(nav::close) {
        Hero(f.title, f.genre, f.cover, f.year, f.country, f.minutes, eyebrow = t("в афише", "on the bill"))
        app.reelkin.ui.components.FirstVisit(app.reelkin.ui.components.Tip.FILM, c.s)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("На проектор", "Put it on"), {
                if (c.s.seance != null) nav.toast(if (ru) "Сеанс уже идёт." else "A film is already on.")
                else { co.write { office.startSeance(f.seance()) }; nav.tab(Tab.BOOTH); nav.say(if (ru) "Гасим свет. Я сяду поближе к окошку." else "Lights down. I'll sit by the port.") }
            }, Modifier.weight(1f))
            Btn(t("Уже посмотрел", "Watched it"), { nav.go(Overlay.Log(f.seance())) }, Modifier.weight(1f), kind = Kind.GOLD)
        }
        PeopleBlock(f.director, f.cast, nav) { co.office.people(f) }
        Brings(c, f.title, f.genre, f.year, f.country)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("Изменить", "Edit"), { nav.go(Overlay.NewTitle(f.draft(), f.id)) }, Modifier.weight(1f), kind = Kind.GHOST, small = true)
            Btn(if (sure) t("Точно убрать?", "Sure?") else t("Убрать из афиши", "Take off"), {
                if (!sure) sure = true else { co.write { office.remove(f) }; nav.close(); nav.toast(if (ru) "«${f.title}» убран из афиши. Без обид." else "${f.title} is off the bill. No hard feelings.") }
            }, Modifier.weight(1f), kind = Kind.GHOST, small = true)
        }
    }
}

fun Title.draft() = app.reelkin.data.Draft(kind, title, year, country, genre, minutes.takeIf { it > 0 }, perSeason, director = director.takeIf { it != "?" } ?: "", cast = cast)

/* ---------------- a series ---------------- */

@Composable
fun SeriesSheet(c: Cinema, s: Title, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val co = ctx.container
    var sure by remember { mutableStateOf(false) }
    // The seasons are asked for again now and then, so a season announced since shows up with its date.
    LaunchedEffect(s.id) {
        if (System.currentTimeMillis() - s.planAt > app.reelkin.data.Seasons.FRESH)
            app.reelkin.data.Seasons.refresh(s)?.let { (tv, plan) -> co.office.setPlan(s.id, tv, plan) }
    }
    Sheet(nav::close) {
        Hero(s.title, s.genre, s.cover, s.year, s.country, s.minutes, eyebrow = t("сериал", "a series")) {
                Text(when (s.status) {
                    Status.WATCH -> t("ещё не начат", "not started yet")
                    Status.SHELVED -> t("снят с проката", "taken off")
                    Status.RUNNING -> t("сезон ${s.season} · серия ${s.episode} из ${s.perSeason}", "season ${s.season} · episode ${s.episode} of ${s.perSeason}")
                }, style = T.Strong)
                Words.air(s, ru, c.now)?.let { Text(it, style = T.Small.copy(color = C.gold)) }
                Words.seasonsOut(s, ru, c.now)?.let { Text(it, style = T.Small) }
        }
        app.reelkin.ui.bill.TicketBook(s)
        Text(t("Каждая серия — корешок абонемента и 3 попкорна. Последняя серия сезона сажает в зал зрителя.", "Each episode tears a ticket and gives 3 popcorn. A season's last episode seats a spectator."), style = T.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("+1 серия", "+1 episode"), { episode(s, nav, ctx, ru); nav.close() }, Modifier.weight(1f))
            Btn(t("−1", "−1"), { co.write { office.undoEpisode(s) } }, kind = Kind.GHOST, enabled = s.episode > 0)
        }
        PeopleBlock(s.director, s.cast, nav) { co.office.people(s) }
        Flow {
            Chip(if (s.cliff) t("на самом интересном ✓", "cliffhanger ✓") else t("на самом интересном!", "cliffhanger!"), s.cliff, { co.write { office.update(s.copy(cliff = !s.cliff)) } }, color = C.velvet)
            if (s.status == Status.RUNNING) Chip(t("снять с проката", "take off"), false, { co.write { office.update(s.copy(status = Status.SHELVED, cliff = false)) }; nav.close(); nav.toast(if (ru) "«${s.title}» в шкафу. Достать можно в любой момент." else "${s.title} is in the cupboard. Bring it back any time.") })
            else Chip(t("смотрю", "watching"), false, { co.write { office.update(s.copy(status = Status.RUNNING)) } })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("Изменить", "Edit"), { nav.go(Overlay.NewTitle(s.draft(), s.id)) }, Modifier.weight(1f), kind = Kind.GHOST, small = true)
            Btn(if (sure) t("Точно удалить?", "Sure?") else t("Удалить", "Delete"), { if (!sure) sure = true else { co.write { office.remove(s) }; nav.close() } }, Modifier.weight(1f), kind = Kind.GHOST, small = true)
        }
    }
}

/* ---------------- a stub ---------------- */

@Composable
fun StubSheet(c: Cinema, tk: Ticket, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var words by remember(tk.id) { mutableStateOf(tk.words) }
    var sure by remember { mutableStateOf(false) }
    val seat = c.seatOf(tk)
    Sheet({ if (words != tk.words) co.write { office.update(tk.copy(words = words.trim())) }; nav.close() }) {
        Hero(tk.title, tk.genre, tk.cover, tk.year, tk.country, tk.minutes, eyebrow = Words.seat(seat, ru) + " · " + app.reelkin.ui.bill.date(tk.at, ru))
        ShareLine(t("Поделиться отзывом", "Share a review"), t("открытка с постером, попкорном и словами", "a card with the poster, popcorn and words")) { nav.go(Overlay.ShareIt(app.reelkin.ui.cards.Share.Review(tk.id))) }
        Column(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface2).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(t("Твой попкорн", "Your popcorn").uppercase(), style = T.Eyebrow)
            Popcorn(tk.stars, size = 30.dp) { s -> co.write { office.update(tk.copy(stars = s)) } }
        }
        app.reelkin.ui.components.FirstVisit(app.reelkin.ui.components.Tip.STUB, c.s)
        PeopleBlock(tk.director, tk.cast, nav) { co.office.people(tk) }
        Text(t("Три слова", "Three words").uppercase(), style = T.Eyebrow)
        Field(words, { words = it.take(60) }, t("дождь, единорог, слёзы", "rain, unicorn, tears"))
        Text(buildString {
            append(if (ru) "В зале сидит ${Words.who(tk.genre.who, true)}." else "In the hall sits ${Words.who(tk.genre.who, false)}.")
            if (tk.rewatch) append(if (ru) " Пересмотрено." else " A rewatch.")
            if (tk.credits == Credits.SCENE) append(if (ru) " Была сцена после титров." else " There was a post-credits scene.")
        }, style = T.Muted)
        WatchedOn(tk.at) { at -> co.write { office.update(tk.copy(at = at, words = words.trim())) } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!tk.season) Btn(t("Пересмотреть", "Watch again"), {
                if (c.s.seance == null) { co.write { office.startSeance(tk.seance()) }; nav.tab(Tab.BOOTH) } else nav.toast(if (ru) "Сеанс уже идёт." else "A film is already on.")
            }, Modifier.weight(1f), kind = Kind.SOFT)
            Btn(if (sure) t("Точно?", "Sure?") else t("Удалить корешок", "Delete stub"), { if (!sure) sure = true else { co.write { office.remove(tk) }; nav.close() } }, Modifier.weight(1f), kind = Kind.GHOST)
        }
    }
}

/* ---------------- tearing a stub ---------------- */

@Composable
fun LogSheet(c: Cinema, s: Seance, nav: Nav, series: Title? = null) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var stars by remember { mutableIntStateOf(0) }
    var words by remember { mutableStateOf("") }
    var company by remember { mutableStateOf(Company.ALONE) }
    var credits by remember { mutableStateOf(Credits.NOTHING) }
    var rewatch by remember { mutableStateOf(s.rewatch || c.seats.any { it.title == s.title && !it.season }) }
    var at by remember { mutableLongStateOf(System.currentTimeMillis()) }
    Sheet(nav::close) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BigPoster(s.title, s.genre, s.cover, 2.6f)
            Column(Modifier.weight(1f)) {
                Text(t("Корешок", "Stub").uppercase(), style = T.Eyebrow)
                Text(s.title, style = T.Title)
                Facts(s.year, s.country, s.minutes)
            }
        }
        app.reelkin.ui.components.FirstVisit(app.reelkin.ui.components.Tip.LOG, c.s)
        Text(t("Как тебе?", "How was it?").uppercase(), style = T.Eyebrow)
        Popcorn(stars, size = 36.dp) { stars = it }
        Text((if (series != null) t("Три слова о сезоне", "Three words about it") else t("Три слова о фильме", "Three words about it")).uppercase(), style = T.Eyebrow)
        Field(words, { words = it.take(60) }, t("дождь, единорог, слёзы", "rain, unicorn, tears"))
        Text(t("С кем", "With whom").uppercase(), style = T.Eyebrow)
        Flow { for ((v, l) in listOf(Company.ALONE to t("один", "alone"), Company.TWO to t("вдвоём", "for two"), Company.MANY to t("компанией", "with friends"))) Chip(l, company == v, { company = v }) }
        Text(t("После титров", "After the credits").uppercase(), style = T.Eyebrow)
        Flow { for ((v, l) in listOf(Credits.SCENE to t("была сцена", "a scene"), Credits.NOTHING to t("ничего", "nothing"), Credits.LEFT to t("не досидел", "left early"))) Chip(l, credits == v, { credits = v }) }
        Flow { Chip(t("впервые", "first time"), !rewatch, { rewatch = false }); Chip(t("пересмотр", "rewatch"), rewatch, { rewatch = true }) }
        WatchedOn(at) { at = it }
        Btn(t("Оторвать корешок", "Tear the stub"), {
            val tk = Ticket(title = s.title, year = s.year, country = s.country, genre = s.genre, minutes = s.minutes, stars = stars, words = words.trim(),
                company = company, credits = credits, rewatch = rewatch, cover = s.cover, at = at, season = series != null, whole = series != null)
            co.write {
                val before = cinema()
                series?.let { office.add(it) }
                val saved = office.tear(tk, s.titleId.takeIf { it > 0 })
                val g = gains(before, cinema(), saved)
                nav.scene.hop(30.0); nav.scene.burst(); nav.scene.mood(PetMood.HAPPY, 3.0)
                nav.go(Overlay.Reward(saved, g))
            }
        }, Modifier.fillMaxWidth())
    }
}

@Composable
fun RewardSheet(c: Cinema, tk: Ticket, g: Gains, nav: Nav) {
    val ru = LocalRu.current
    val p = c.pet
    val name = c.name(p, ru)
    fun done() { nav.close(); nav.tab(Tab.BOOTH); nav.say(when {
        g.cans.isNotEmpty() -> if (ru) "Слышишь? Открылась новая коробка. Загляни к питомцам." else "Hear that? A new can has opened. Have a look at the pets."
        g.grew != null -> if (ru) "Я подрос! Чувствуешь?" else "I've grown! Can you tell?"
        g.fav -> if (ru) "Моё любимое! Я наелся на полтора фильма." else "My favourite! That was a film and a half."
        tk.stars == 5 -> if (ru) "Пять попкорнов! Повешу афишу в будке." else "Five popcorns! The poster goes on the wall."
        else -> if (ru) "Хорошее было кино. Я всё запомнил." else "That was a good one. I'll remember it."
    }) }
    Sheet(::done) {
        Text(t("Корешок оторван", "Stub torn"), style = T.Display)
        Column(Modifier.fillMaxWidth().rotate(-1.5f).clip(R8.Md).background(C.gold).padding(vertical = 14.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(tk.title, style = T.Small.copy(color = C.onGold), textAlign = TextAlign.Center)
            Text(Words.seat(g.seat, ru).uppercase(), style = T.Stamp.copy(color = C.onGold))
            Text(listOfNotNull(if (g.minutes > 0) "+" + Words.minutes(g.minutes, ru) else null,
                if (ru) "+${g.popcorn} попкорна" else "+${g.popcorn} popcorn").joinToString(" · "), style = T.Small.copy(color = C.onGold))
        }
        val rows = ArrayList<@Composable () -> Unit>()
        rows += { Gain({ BufImage(Icons.seated(tk.genre.who, hash(tk.title)), scale = 3.dp) }, (if (ru) "В зал пришёл " else "In comes ") + Words.who(tk.genre.who, ru), Words.seat(g.seat, ru)) }
        for (d in g.decades) rows += { Gain({ Text("$d", style = T.Small.copy(color = C.onGold), modifier = Modifier.clip(R8.Sm).background(C.gold).padding(8.dp)) }, if (ru) "Десятилетие: $d-е" else "A decade: the ${d}s", t("в будке прибавилось техники", "the booth gets new gear")) }
        for (cn in g.countries) rows += { Gain({ Box(Modifier.size(56.dp)) { Stamp(cn) } }, if (ru) "Штамп: $cn" else "A stamp: $cn", t("новая страна в кругосветке", "a new country on the map")) }
        if (g.fav && p != null) rows += { Gain({ BufImage(PetArt.still(Pose(p.species, p.stage, p.pet.shiny, p.form, mood = PetMood.HAPPY)), scale = 1.dp) }, if (ru) "$name в восторге" else "$name loves it", if (ru) "любимое: в зачёт в полтора раза" else "a favourite: it counts one and a half times") }
        g.grew?.let { st -> if (p != null) rows += { Gain({ BufImage(PetArt.still(Pose(p.species, st, p.pet.shiny, p.form, mood = PetMood.HAPPY)), scale = 1.dp) }, if (ru) "$name подрос!" else "$name has grown!", Words.stage(st, ru)) } }
        for (sp in g.cans) rows += { Gain({ BufImage(PetArt.still(Pose(sp, app.reelkin.data.Stage.EGG)), scale = 1.dp) }, if (ru) "Открылась коробка: ${Words.species(sp, true)}" else "A can opened: ${Words.species(sp, false)}", t("загляни к питомцам", "have a look at the pets")) }
        if (g.scene) rows += { Gain({ Text("★", style = T.Display.copy(color = C.velvet)) }, t("Пасхалка после титров", "A post-credits scene"), t("+5 попкорна", "+5 popcorn")) }
        var shown by remember { mutableIntStateOf(if (Art.still) rows.size else 0) }
        LaunchedEffect(Unit) { while (shown < rows.size) { delay(320); shown++ } }
        rows.forEachIndexed { i, r -> AnimatedVisibility(i < shown, enter = fadeIn()) { r() } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("В зал", "To the hall"), { nav.close(); nav.saga(2) }, Modifier.weight(1f), kind = Kind.GHOST)
            Btn(t("Хорошо", "Lovely"), ::done, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Gain(icon: @Composable () -> Unit, title: String, sub: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) { icon() }
        Column { Text(title, style = T.Strong); Text(sub, style = T.Muted) }
    }
}

/* ---------------- choosing a film for the projector or the stub ---------------- */

@Composable
fun ChooseSheet(c: Cinema, log: Boolean, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val co = ctx.container
    Sheet(nav::close) {
        Text(if (log) t("Что посмотрел?", "What did you watch?") else t("Что ставим на проектор?", "What goes on the projector?"), style = T.Title)
        Text(if (log) t("Из афиши или найди новый.", "From the bill, or find another.") else t("Сеанс идёт столько, сколько длится фильм. Потом — корешок.", "The show runs as long as the film. Then a stub."), style = T.Muted)
        val films = c.watch.filter { it.kind == TKind.FILM }
        for (f in films) Row(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface2).clickable {
            if (log) nav.go(Overlay.Log(f.seance())) else { co.write { office.startSeance(f.seance()) }; nav.close(); nav.tab(Tab.BOOTH); nav.say(if (ru) "Гасим свет. Я сяду поближе к окошку." else "Lights down. I'll sit by the port.") }
        }.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PosterImage(f.title, f.genre, f.cover, 24, 34, Modifier.clip(R8.Sm), scale = 1.4.dp)
            Column(Modifier.weight(1f)) { Text(f.title, style = T.Name); Facts(f.year, f.country, f.minutes) }
        }
        if (log) for (s in c.running) Row(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface2).clickable { episode(s, nav, ctx, ru); nav.close() }.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PosterImage(s.title, s.genre, s.cover, 24, 34, Modifier.clip(R8.Sm), scale = 1.4.dp)
            Column(Modifier.weight(1f)) { Text(s.title, style = T.Name); Text(t("серия ${s.episode + 1} · сезон ${s.season}", "episode ${s.episode + 1} · season ${s.season}"), style = T.Muted) }
        }
        val empty = films.isEmpty() && (!log || c.running.isEmpty())
        if (empty) Text(if (log) t("В афише пусто — найди фильм или сериал по названию.", "The bill is empty: find a film or a series by its name.")
            else t("В афише пока ничего не ждёт. Найди фильм — он встанет в афишу и сразу на проектор.", "Nothing waits on the bill yet. Find a film: it goes on the bill and straight onto the projector."), style = T.Body)
        Btn(if (empty) t("Найти фильм", "Find a film") else t("Найти другой", "Find another"), { nav.go(Overlay.Add()) }, Modifier.fillMaxWidth(), kind = Kind.GOLD)
    }
}

/* ---------------- what's on tonight ---------------- */

@Composable
fun PickSheet(c: Cinema, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val co = ctx.container
    var time by remember { mutableIntStateOf(95) }
    var mood by remember { mutableStateOf(Mood.COSY) }
    var skip by remember { mutableStateOf(setOf<Long>()) }
    val p = remember(time, mood, skip, c) { Picker.pick(c, time, mood, skip, ru) }
    Sheet(nav::close) {
        app.reelkin.ui.components.FirstVisit(app.reelkin.ui.components.Tip.PICK, c.s)
        Text(t("Что ставим?", "What's on tonight?"), style = T.Title)
        Text(if (ru) "${c.name(c.pet, true)} роется в жестянках." else "${c.name(c.pet, false)} rummages in the cans.", style = T.Muted)
        Text(t("Сколько есть времени", "How much time").uppercase(), style = T.Eyebrow)
        Flow {
            for ((m, l) in Picker.TIMES.zip(listOf(t("полчаса", "half an hour"), t("полтора часа", "an hour and a half"), t("два часа", "two hours"), t("весь вечер", "all evening"), t("двойной сеанс", "a double bill"))))
                Chip(l, time == m, { time = m; skip = emptySet() })
        }
        Text(t("Настроение", "In the mood for").uppercase(), style = T.Eyebrow)
        Flow { for (m in Mood.entries) Chip(if (ru) m.ru else m.en, mood == m, { mood = m; skip = emptySet() }) }
        Column(Modifier.fillMaxWidth().clip(R8.Lg).background(C.velvetSoft).border(1.dp, C.velvet.copy(alpha = .6f), R8.Lg).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                p == null -> Text(if (c.watch.isEmpty()) t("Афиша пуста. Добавь пару фильмов — будет из чего выбирать.", "The bill is empty. Add a couple of films to choose from.") else t("Ничего не влезает. Может, просто посидим?", "Nothing fits. Shall we just sit?"), style = T.Body)
                p.episode != null -> {
                    Text(if (ru) "«${p.episode.title}», серия ${p.episode.episode + 1}" else "${p.episode.title}, episode ${p.episode.episode + 1}", style = T.Name)
                    Text(p.why, style = T.Body)
                }
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { for (f in p.films) PosterImage(f.title, f.genre, f.cover, 30, 44, Modifier.clip(R8.Sm), scale = 2.dp) }
                    Text(p.films.joinToString(" + ") { if (ru) "«${it.title}»" else it.title }, style = T.Name)
                    Text(p.why, style = T.Body)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("Другое", "Something else"), { p?.films?.let { fs -> skip = skip + fs.map { it.id } } }, Modifier.weight(1f), kind = Kind.GHOST, enabled = p?.films?.isNotEmpty() == true)
            Btn(t("Ставлю", "Play it"), {
                when {
                    p == null -> Unit
                    p.episode != null -> { episode(p.episode, nav, ctx, ru); nav.close() }
                    c.s.seance != null -> nav.toast(if (ru) "Сеанс уже идёт." else "A film is already on.")
                    else -> { co.write { office.startSeance(p.films.first().seance()) }; nav.close(); nav.tab(Tab.BOOTH); nav.say(if (ru) "Гасим свет. Я сяду поближе к окошку." else "Lights down. I'll sit by the port.") }
                }
            }, Modifier.weight(1f), enabled = p != null)
        }
    }
}

/* ---------------- the pet ---------------- */

@Composable
fun PetSheet(c: Cinema, nav: Nav) {
    val p = c.pet ?: run { nav.go(Overlay.Pets); return }
    PetCard(c, p, nav)
}

/** Everything about one pet, block by block: alive in its frame, its name, how it grows, what it loves and what that gives, its nature, its form and what it ate. */
@Composable
fun PetCard(c: Cinema, p: app.reelkin.domain.PetState, nav: Nav, onClose: () -> Unit = nav::close) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var name by remember(p.pet.id) { mutableStateOf(p.pet.name) }
    val inBooth = p.pet.id == c.pet?.pet?.id
    val hatched = p.pet.hatched
    Sheet({ if (name.trim() != p.pet.name) co.write { db.dao().update(p.pet.copy(name = name.trim())) }; onClose() }) {
        Box(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface2), contentAlignment = Alignment.Center) {
            // The frame cut to the pet itself, so it fills the card instead of sitting small in an empty square.
            val st = if (hatched) p.stage else app.reelkin.data.Stage.EGG
            val box = remember(p.species, st) { PetArt.bounds(p.species, st) }
            PixelCanvas(box[2], box[3], Modifier.fillMaxWidth(.62f).padding(vertical = 10.dp), fine = PetArt.FINE) { b, tick ->
                val tt = tick * Art.FRAME_MS / 1000.0
                if (!hatched) PetArt.drawIn(b, Pose(p.species, app.reelkin.data.Stage.EGG, t = tt, hatch = p.fed / app.reelkin.data.Stage.BABY.from.toFloat()), box)
                else PetArt.drawIn(b, Pose(p.species, p.stage, p.pet.shiny, p.form, tt, if (tick % 60 < 20) PetMood.HAPPY else PetMood.IDLE, blink = tick % 37 == 0,
                    hop = if (tick % 60 < 8) kotlin.math.sin(tick % 60 / 8.0 * Math.PI) * 4 else 0.0), box)
            }
            if (inBooth) Text(t("в будке", "in the booth"), style = T.Small.copy(color = C.gold), modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                .clip(androidx.compose.foundation.shape.CircleShape).background(C.goldSoft).padding(horizontal = 10.dp, vertical = 3.dp))
            if (p.pet.shiny) Text(t("✦ техниколор", "✦ technicolor"), style = T.Small.copy(color = C.gold), modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
        }
        Text(if (hatched) c.name(p, ru) else t("Коробка с плёнкой", "A film can"), style = T.Display, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(Words.species(p.species, ru), style = T.Muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (hatched) ShareLine(t("Поделиться питомцем", "Share the pet"), t("открытка с ним и его стадией", "a card with it and its stage")) { nav.go(Overlay.ShareIt(app.reelkin.ui.cards.Share.Card(app.reelkin.ui.cards.Postcard.PET))) }
        app.reelkin.ui.components.FirstVisit(app.reelkin.ui.components.Tip.PET, c.s)
        if (hatched) Field(name, { name = it.take(16) }, t("Имя: ", "Name: ") + Words.defaultName(p.species, ru))

        // How it grows: the four stages, the one it's at lit, the bar to the next.
        PetBlock(app.reelkin.R.drawable.sec_chart, t("Рост", "Growing")) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (st in listOf(app.reelkin.data.Stage.BABY, app.reelkin.data.Stage.YOUNG, app.reelkin.data.Stage.ADULT, app.reelkin.data.Stage.ANCIENT)) {
                    val now = hatched && st == p.stage; val past = hatched && st < p.stage
                    Column(Modifier.weight(1f).clip(R8.Md).background(if (now) C.goldSoft else C.surface).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) { BufImage(PetArt.portrait(p.species, st, p.pet.shiny, p.form, dim = !now && !past), scale = .75.dp) }
                        Text(Words.stage(st, ru), style = T.Small.copy(color = if (now) C.gold else C.muted), maxLines = 1)
                    }
                }
            }
            Bar(if (hatched) p.progress else p.fed / app.reelkin.data.Stage.BABY.from.toFloat(), Modifier.fillMaxWidth())
            val next = if (hatched) p.stage.next else app.reelkin.data.Stage.BABY
            Text(if (next == null) (if (ru) "Старше не бывает. Съедено ${app.reelkin.ui.bill.dur(p.fed.toLong(), true)} кино." else "As old as they come. ${app.reelkin.ui.bill.dur(p.fed.toLong(), false)} of film eaten.")
                else if (ru) "До стадии «${Words.stage(next, true)}» — ${app.reelkin.ui.bill.dur((next.from - p.fed).coerceAtLeast(0).toLong(), true)} кино" else "${app.reelkin.ui.bill.dur((next.from - p.fed).coerceAtLeast(0).toLong(), false)} of film to «${Words.stage(next, false)}»", style = T.Small)
        }

        // What it loves, in parts: what, what it gives, how much it has had, and where to find more.
        PetBlock(app.reelkin.R.drawable.sec_star, t("Любимое кино", "Favourite films")) {
            Text(Words.loves(p.species, ru).replaceFirstChar { it.uppercase() }, style = T.Name)
            val genres = app.reelkin.domain.Pets.genres(p.species)
            if (genres.isNotEmpty()) Flow { for (g in genres) Tag(Words.genre(g, ru), C.genre(g)) }
            Text(lovesWhy(p.species, ru), style = T.Small)
        }
        PetBlock(app.reelkin.R.drawable.sec_popcorn, t("Что это даёт", "What it gives")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Perk(app.reelkin.R.drawable.sec_chart, "×1,5", t("растёт быстрее", "grows faster"), Modifier.weight(1f))
                Perk(app.reelkin.R.drawable.act_seance, "+5 🍿", t("за фильм", "a film"), Modifier.weight(1f))
                Perk(app.reelkin.R.drawable.act_can, "+1 🍿", t("за серию", "an episode"), Modifier.weight(1f))
            }
        }
        if (hatched) PetBlock(app.reelkin.R.drawable.sec_medal, t("Уже съедено любимого", "Favourites so far")) {
            val share = if (p.minutes > 0) p.favMin.toFloat() / p.minutes else 0f
            Row(verticalAlignment = Alignment.Bottom) {
                Text(app.reelkin.ui.bill.dur(p.favMin.toLong(), ru), style = T.Number.copy(color = C.gold), modifier = Modifier.weight(1f))
                Text(if (ru) "${(share * 100).toInt()}% всего кино" else "${(share * 100).toInt()}% of all its films", style = T.Small)
            }
            Bar(share, Modifier.fillMaxWidth())
            Text(if (p.favMin == 0) t("Пока ничего любимого — покажи ему такое кино, и он подрастёт быстрее.", "Nothing it loves yet: show it one and it grows faster.")
                else t("Чем больше любимого, тем быстрее он растёт и тем больше попкорна в буфете.", "The more it loves, the faster it grows and the more popcorn for the buffet."), style = T.Small)
            if (genres(p.species).isNotEmpty()) Btn(t("Найти такое в афише", "Find some on the bill"), { nav.genres(genres(p.species)) }, Modifier.fillMaxWidth(), kind = Kind.SOFT, small = true)
        }

        // Its nature: a word for it, and what it means.
        PetBlock(app.reelkin.R.drawable.sec_medal, t("Характер", "Nature")) {
            val (word, about) = Words.nature(p.pet.nature, ru).split(": ", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            Text(word.replaceFirstChar { it.uppercase() }, style = T.Name)
            Text(about.replaceFirstChar { it.uppercase() }, style = T.Body)
        }

        // The form, when grown, or the one it leans to.
        val f = p.form ?: p.leaning
        if (f != null && hatched) PetBlock(app.reelkin.R.drawable.sec_palette, if (p.form != null) t("Форма", "Form") else t("Склоняется к форме", "Leaning to the form")) {
            val col = androidx.compose.ui.graphics.Color(0xFF000000 or f.color.toLong())
            Text(Words.form(f, ru).substringBefore(" ("), style = T.Name.copy(color = col))
            Text(Words.form(f, ru).substringAfter("(").removeSuffix(")"), style = T.Small)
            if (p.form == null) Text(t("Выберется навсегда, когда питомец вырастет, — по тому, чем его кормили.", "Set for good when the pet grows up, by what it was fed."), style = T.Small)
        }

        if (p.byGenre.isNotEmpty()) PetBlock(app.reelkin.R.drawable.sec_popcorn, t("Чем кормили", "What it ate")) {
            val total = p.byGenre.values.sum().coerceAtLeast(1)
            for ((g, m) in p.byGenre.entries.sortedByDescending { it.value }.take(5)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Words.genre(g, ru), style = T.Small, modifier = Modifier.width(110.dp), maxLines = 1)
                    Bar(m / total.toFloat(), Modifier.weight(1f), C.genre(g))
                    Text(app.reelkin.ui.bill.dur(m.toLong(), ru), style = T.Small)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!inBooth) Btn(t("Позвать в будку", "Call to the booth"), { co.write { office.callPet(p.pet) }; onClose(); nav.close(); nav.tab(Tab.BOOTH) }, Modifier.weight(1f))
            Btn(t("Все питомцы", "All pets"), { nav.go(Overlay.Pets) }, Modifier.weight(1f), kind = Kind.SOFT)
        }
    }
}

/** A block of the pet's card: a glyph, a name, and what's in it. */
@Composable
internal fun PetBlock(icon: Int, title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface2).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            app.reelkin.ui.goals.Glyph(icon, C.gold, 16, back = C.surface, box = 30)
            Text(title.uppercase(), style = T.Eyebrow)
        }
        content()
    }
}

@Composable
internal fun Perk(icon: Int, value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(R8.Md).background(C.surface).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        app.reelkin.ui.goals.Glyph(icon, C.gold, 16, back = C.surface2, box = 30)
        Text(value, style = T.Strong.copy(color = C.gold), maxLines = 1)
        Text(label, style = T.Small, maxLines = 1)
    }
}

private fun genres(sp: app.reelkin.data.Species) = app.reelkin.domain.Pets.genres(sp)

/** Why a species loves what it loves, in a line. */
internal fun lovesWhy(sp: app.reelkin.data.Species, ru: Boolean): String {
    val g = genres(sp)
    return if (g.isNotEmpty()) (if (ru) "Каждый такой фильм или серия идёт в зачёт в полтора раза." else "Every such film or episode counts one and a half times.")
    else if (ru) "Тут дело не в жанре, а в том, как ты смотришь: это засчитывается любому фильму." else "It's not about the genre but the way you watch: any film can count."
}

/* ---------------- the buffet ---------------- */

@Composable
fun ShopSheet(c: Cinema, nav: Nav) {
    val ru = LocalRu.current
    val ctx = LocalContext.current
    val co = ctx.container
    // What's on show in the booth above: bought things, plus or minus what's being tried.
    var trying by remember { mutableStateOf(Decor.entries.filter { c.has(it) }.toSet()) }
    var wallTry by remember { mutableStateOf(c.wall) }
    LaunchedEffect(c.s.bought) { trying = trying + Decor.entries.filter { c.has(it) } }
    val preview = remember { app.reelkin.ui.art.BoothScene() }
    val look = remember(trying, wallTry, c) { app.reelkin.ui.booth.lookOf(c, ctx, ru, trying, wallTry) }
    Sheet(nav::close) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t("Буфет", "Buffet"), style = T.Title, modifier = Modifier.weight(1f))
            Text("🍿 ${c.popcorn}", style = T.Strong.copy(color = C.gold), modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(C.goldSoft).padding(horizontal = 12.dp, vertical = 5.dp))
        }
        app.reelkin.ui.components.FirstVisit(app.reelkin.ui.components.Tip.SHOP, c.s)
        Box(Modifier.fillMaxWidth().clip(R8.Lg).border(1.dp, C.line, R8.Lg)) {
            app.reelkin.ui.art.PixelCanvas(app.reelkin.ui.art.BoothScene.W, app.reelkin.ui.art.BoothScene.H, Modifier.fillMaxWidth(), fine = app.reelkin.ui.art.BoothScene.F) { b, tick ->
                preview.draw(b, look, tick * Art.FRAME_MS / 1000.0, Art.FRAME_MS / 1000.0)
            }
            val tried = trying.count { !c.has(it) } + if (wallTry != c.wall) 1 else 0
            if (tried > 0) Text(if (ru) "примерка · $tried" else "trying on · $tried", style = T.Small.copy(color = C.onGold),
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(C.gold).padding(horizontal = 10.dp, vertical = 3.dp))
        }
        Text(t("Нажми на вещь — она появится в будке на примерку. Понравится — купи.", "Tap a thing to try it in the booth. Like it? Buy it."), style = T.Small)
        Text(t("Попкорн: 10 за фильм, 3 за серию, 15 за финал сезона, ещё 5 за пасхалку и пересмотр. Убранное возвращает попкорн целиком.",
            "Popcorn: 10 a film, 3 an episode, 15 a season finale, 5 more for a post-credits scene or a rewatch. Anything taken away pays it all back."), style = T.Small)
        Text(t("Для будки", "For the booth").uppercase(), style = T.Eyebrow)
        for (row in Decor.entries.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (d in row) {
                val has = c.has(d)
                val (n, about) = when (d) {
                    Decor.GARLAND -> t("Гирлянда", "String lights") to t("мигает над проектором", "twinkles over the projector")
                    Decor.CHAIR -> t("Бархатное кресло", "Velvet armchair") to t("смотреть сидя", "to watch sitting down")
                    Decor.NEON -> t("Неон «ТИШЕ»", "«HUSH» neon") to t("розовый, моргает", "pink, flickers")
                    Decor.POPCORN -> t("Попкорн-машина", "Popcorn machine") to t("у окошка в зал", "by the port")
                    Decor.RUG -> t("Ковёр с бобиной", "A reel rug") to t("красный, с каймой", "red, gold-edged")
                    Decor.PLANT -> t("Монстера", "A monstera") to t("в латунном горшке", "in a brass pot")
                    Decor.CLOCK -> t("Часы на стене", "A wall clock") to t("к началу сеанса", "for showtime")
                }
                ShopItem(n, about, has, d.price, c.popcorn >= d.price, Modifier.weight(1f), trying = d in trying && !has, onTry = { if (!has) trying = if (d in trying) trying - d else trying + d }, picture = { DecorPicture(d) }) {
                    if (has) trying = trying - d
                    co.write { settings.update { s -> if (has) s.copy(bought = s.bought - d.name, spent = s.spent - d.price) else s.copy(bought = s.bought + d.name, spent = s.spent + d.price) } }
                    if (!has) { nav.scene.burst(); nav.say(if (d == Decor.CHAIR) (if (ru) "Моё кресло! Никому не отдам." else "My chair! Nobody else's.") else (if (ru) "Красиво. Совсем как в настоящем кинотеатре." else "Lovely. Just like a real picture house.")) }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Text(t("Обои", "Wallpaper").uppercase(), style = T.Eyebrow, modifier = Modifier.padding(top = 6.dp))
        for (row in app.reelkin.domain.Wall.entries.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (w in row) {
                val has = c.has(w); val on = c.wall == w
                val n = when (w) {
                    app.reelkin.domain.Wall.PLUM -> t("Сливовые", "Plum damask")
                    app.reelkin.domain.Wall.GREEN -> t("Зелёный бархат", "Green velvet")
                    app.reelkin.domain.Wall.NAVY -> t("Звёздная ночь", "Starry night")
                    app.reelkin.domain.Wall.DECO -> t("Ар-деко", "Art deco")
                    app.reelkin.domain.Wall.BRICK -> t("Кирпич", "Brick")
                }
                val room = remember(w) { app.reelkin.ui.art.Scenery.room(w.file) }
                Column(Modifier.weight(1f).clip(R8.Lg).background(C.surface2).then(if (on) Modifier.border(2.dp, C.gold, R8.Lg) else if (wallTry == w) Modifier.border(2.dp, C.velvet, R8.Lg) else Modifier)
                    .clickable { wallTry = w; if (has && !on) co.write { settings.update { it.copy(wall = w.name) } } }.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (room != null) BufImage(room, Modifier.clip(R8.Md), scale = .85.dp)
                    Text(n, style = T.Strong, maxLines = 1)
                    when {
                        on -> Text(t("на стенах", "on the walls"), style = T.Small.copy(color = C.gold))
                        has -> Text(t("поклеить", "put up"), style = T.Small.copy(color = C.ink))
                        else -> Btn("${w.price} 🍿", {
                            co.write { settings.update { s -> s.copy(bought = s.bought + ("WALL_" + w.name), spent = s.spent + w.price, wall = w.name) } }
                            nav.scene.burst(); nav.say(if (ru) "Новые обои! Пахнет клеем и попкорном." else "New wallpaper! Smells of glue and popcorn.")
                        }, Modifier.fillMaxWidth(), kind = Kind.GOLD, small = true, enabled = c.popcorn >= w.price)
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/** A thing for sale: its picture on a soft stage, name, a word on it, and the price or a way to take it back. */
@Composable
private fun ShopItem(name: String, about: String, has: Boolean, price: Int, can: Boolean, modifier: Modifier, trying: Boolean = false, onTry: () -> Unit = {}, picture: @Composable () -> Unit, onClick: () -> Unit) {
    Column(modifier.clip(R8.Lg).background(C.surface2).then(if (has) Modifier.border(2.dp, C.gold.copy(alpha = .7f), R8.Lg) else if (trying) Modifier.border(2.dp, C.velvet, R8.Lg) else Modifier)
        .clickable(onClick = onTry).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().height(96.dp).clip(R8.Md).background(C.surface), contentAlignment = Alignment.Center) { picture() }
        Text(name, style = T.Strong, maxLines = 1)
        Text(if (trying) t("на примерке", "trying on") else about, style = T.Small.copy(color = if (trying) C.velvet else C.muted), maxLines = 1)
        Btn(if (has) t("убрать", "take away") else "$price 🍿", onClick, Modifier.fillMaxWidth().padding(top = 4.dp), kind = if (has) Kind.GHOST else Kind.GOLD, small = true, enabled = has || can)
    }
}

/** What a thing looks like: its picture from the booth, or, for the lights and the neon, drawn here. */
@Composable
private fun DecorPicture(d: Decor) {
    val file = when (d) { Decor.CHAIR -> "chair"; Decor.POPCORN -> "popcorn"; Decor.RUG -> "rug"; Decor.PLANT -> "plant"; Decor.CLOCK -> "clock"; else -> null }
    val pic = remember(file) { file?.let { app.reelkin.ui.art.Scenery.pic(it) } }
    if (pic != null) {
        val k = minOf(80f / pic.buf.h, 140f / pic.buf.w).coerceAtMost(2.4f)
        BufImage(pic.buf, scale = k.dp)
        return
    }
    if (d == Decor.NEON) Text(t("ТИШЕ", "HUSH"), style = T.Title.copy(color = androidx.compose.ui.graphics.Color(0xFFFF7AB0),
        shadow = androidx.compose.ui.graphics.Shadow(androidx.compose.ui.graphics.Color(0xFFFF3A8A), blurRadius = 18f)))
    else Canvas(Modifier.fillMaxWidth().height(60.dp)) {
        val cols = listOf(0xFFFF5A6A, 0xFFFFD84A, 0xFF6AD0FF, 0xFF8AFF7A).map { androidx.compose.ui.graphics.Color(it) }
        val n = 9
        var prev: Offset? = null
        for (i in 0 until n) {
            val x = size.width * (i + .5f) / n; val y = size.height * (.3f + .25f * kotlin.math.sin(i * 0.9f))
            prev?.let { drawLine(C.line, it, Offset(x, y), 3f) }
            prev = Offset(x, y)
            drawCircle(cols[i % 4].copy(alpha = .3f), 14f, Offset(x, y + 10f)); drawCircle(cols[i % 4], 7f, Offset(x, y + 10f))
        }
    }
}

/* ---------------- the year's awards ---------------- */

@Composable
fun AwardSheet(c: Cinema, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val pool = c.thisYear
    var picks by remember { mutableStateOf(Words.AWARDS.indices.map { k -> c.s.awards.firstOrNull { it.year == c.year && it.cat == k }?.title ?: pool.getOrNull((hash("$k") % pool.size.coerceAtLeast(1)).toInt())?.title ?: "" }) }
    var open by remember { mutableIntStateOf(-1) }
    var handed by remember { mutableStateOf(false) }
    Sheet(nav::close) {
        if (!handed) {
            Text(if (ru) "Премия Reelkin · ${c.year}" else "The Reelkin awards · ${c.year}", style = T.Title)
            Text(if (ru) "Номинанты — всё, что ты посмотрел за год. ${c.name(c.pet, true)} уже волнуется." else "The nominees are everything you watched this year. ${c.name(c.pet, false)} is already nervous.", style = T.Muted)
            Words.AWARDS.forEachIndexed { k, cat ->
                Column(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface2).clickable { open = if (open == k) -1 else k }.padding(12.dp)) {
                    Text((if (ru) cat.first else cat.second).uppercase(), style = T.Eyebrow)
                    Text(picks[k].ifBlank { "—" }, style = T.Name)
                    if (open == k) for (tk in pool) Text(tk.title, style = T.Body.copy(color = if (tk.title == picks[k]) C.gold else C.ink),
                        modifier = Modifier.fillMaxWidth().clickable { picks = picks.toMutableList().also { it[k] = tk.title }; open = -1 }.padding(vertical = 6.dp))
                }
            }
            Btn(t("Вскрыть конверты", "Open the envelopes"), {
                co.write { settings.update { s -> s.copy(awards = s.awards.filter { it.year != c.year } + picks.mapIndexedNotNull { k, p -> p.ifBlank { null }?.let { Award(c.year, k, it) } }) } }
                handed = true; nav.scene.burst(40)
            }, Modifier.fillMaxWidth())
        } else {
            Text(t("И статуэтка отправляется…", "And the statuette goes to…"), style = T.Title)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { c.pet?.let { p -> BufImage(PetArt.still(Pose(p.species, p.stage, p.pet.shiny, p.form, mood = PetMood.HAPPY)), scale = 2.6.dp) } }
            var shown by remember { mutableIntStateOf(if (Art.still) 5 else 0) }
            LaunchedEffect(Unit) { while (shown < 5) { delay(600); shown++ } }
            Words.AWARDS.forEachIndexed { k, cat ->
                AnimatedVisibility(k < shown, enter = fadeIn()) {
                    Gain({ BufImage(Icons.statue, scale = 2.dp) }, picks[k].ifBlank { "—" }, if (ru) cat.first else cat.second)
                }
            }
            Btn(t("Овации", "Applause"), { nav.close(); nav.saga(2); nav.toast(if (ru) "Статуэтки стоят на полке в зале. Навсегда." else "The statuettes are on the shelf in the hall. For good.") }, Modifier.fillMaxWidth())
        }
    }
}


/** A quiet row that opens a postcard of what's on screen. */
@Composable
fun ShareLine(title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface2).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        app.reelkin.ui.cards.ShareButton(onClick, 36.dp)
        Column(Modifier.weight(1f)) { Text(title, style = T.Strong); Text(sub, style = T.Small, maxLines = 1) }
        Text("›", style = T.Title.copy(color = C.faint))
    }
}
