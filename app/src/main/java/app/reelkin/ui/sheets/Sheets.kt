package app.reelkin.ui.sheets

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
import app.reelkin.ui.pet
import app.reelkin.ui.poster
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T
import kotlinx.coroutines.delay

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
    Cinema.letterOf(title)?.let { if (it !in c.letters) l += if (ru) "новая буква «$it» для вывески" else "a new letter «$it» for the sign" }
    if (c.nearTrait == genre) l += Words.willGet(genre.trait!!, ru).replaceFirstChar { it.uppercase() }
    l += (if (ru) "в зал придёт " else "in comes ") + Words.who(genre.who, ru)
    Column(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface2).padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(t("Если посмотреть", "If you watch it").uppercase(), style = T.Eyebrow)
        for (s in l) Text("+  $s", style = T.Body)
    }
}

/* ---------------- a film on the bill ---------------- */

@Composable
fun FilmSheet(c: Cinema, f: Title, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var sure by remember { mutableStateOf(false) }
    Sheet(nav::close) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BigPoster(f.title, f.genre, f.cover)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(f.title, style = T.Title)
                Facts(f.year, f.country, f.minutes)
                Tag(Words.genre(f.genre, ru), C.genre(f.genre))
            }
        }
        Brings(c, f.title, f.genre, f.year, f.country)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("Сеанс", "Showtime"), {
                if (c.s.seance != null) nav.toast(if (ru) "Сеанс уже идёт." else "A film is already on.")
                else { co.write { office.startSeance(f.seance()) }; nav.tab(Tab.BOOTH); nav.say(if (ru) "Гасим свет. Я сяду поближе к окошку." else "Lights down. I'll sit by the port.") }
            }, Modifier.weight(1f))
            Btn(t("Уже посмотрел", "Watched it"), { nav.go(Overlay.Log(f.seance())) }, Modifier.weight(1f), kind = Kind.GOLD)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("Изменить", "Edit"), { nav.go(Overlay.NewTitle(f.draft(), f.id)) }, Modifier.weight(1f), kind = Kind.GHOST, small = true)
            Btn(if (sure) t("Точно убрать?", "Sure?") else t("Убрать из афиши", "Take off"), {
                if (!sure) sure = true else { co.write { office.remove(f) }; nav.close(); nav.toast(if (ru) "«${f.title}» убран из афиши. Без обид." else "${f.title} is off the bill. No hard feelings.") }
            }, Modifier.weight(1f), kind = Kind.GHOST, small = true)
        }
    }
}

fun Title.draft() = app.reelkin.data.Draft(kind, title, year, country, genre, minutes.takeIf { it > 0 }, perSeason)

/* ---------------- a series ---------------- */

@Composable
fun SeriesSheet(c: Cinema, s: Title, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val co = ctx.container
    var sure by remember { mutableStateOf(false) }
    Sheet(nav::close) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BigPoster(s.title, s.genre, s.cover)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.title, style = T.Title)
                Facts(s.year, s.country, s.minutes)
                Tag(Words.genre(s.genre, ru), C.genre(s.genre))
                Text(when (s.status) {
                    Status.WATCH -> t("ещё не начат", "not started yet")
                    Status.SHELVED -> t("снят с проката", "taken off")
                    Status.RUNNING -> t("сезон ${s.season} · серия ${s.episode} из ${s.perSeason}", "season ${s.season} · episode ${s.episode} of ${s.perSeason}")
                }, style = T.Strong)
            }
        }
        app.reelkin.ui.bill.TicketBook(s)
        Text(t("Каждая серия — корешок абонемента и 3 попкорна. Последняя серия сезона сажает в зал зрителя.", "Each episode tears a ticket and gives 3 popcorn. A season's last episode seats a spectator."), style = T.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("+1 серия", "+1 episode"), { episode(s, nav, ctx, ru); nav.close() }, Modifier.weight(1f))
            Btn(t("−1", "−1"), { co.write { office.undoEpisode(s) } }, kind = Kind.GHOST, enabled = s.episode > 0)
        }
        Flow {
            Chip(if (s.cliff) t("оборвалось ✓", "cliffhanger ✓") else t("оборвалось!", "cliffhanger!"), s.cliff, { co.write { office.update(s.copy(cliff = !s.cliff)) } }, color = C.velvet)
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
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BigPoster(tk.title, tk.genre, tk.cover)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(Words.seat(seat, ru).uppercase(), style = T.Eyebrow.copy(color = C.velvet))
                Text(tk.title, style = T.Title)
                Facts(tk.year, tk.country, tk.minutes)
                Tag(Words.genre(tk.genre, ru), C.genre(tk.genre))
                Popcorn(tk.stars, size = 26.dp) { s -> co.write { office.update(tk.copy(stars = s)) } }
            }
        }
        Text(t("Три слова", "Three words").uppercase(), style = T.Eyebrow)
        Field(words, { words = it.take(60) }, t("дождь, единорог, слёзы", "rain, unicorn, tears"))
        Text(buildString {
            append(if (ru) "В зале сидит ${Words.who(tk.genre.who, true)}." else "In the hall sits ${Words.who(tk.genre.who, false)}.")
            if (tk.rewatch) append(if (ru) " Пересмотрено." else " A rewatch.")
            if (tk.credits == Credits.SCENE) append(if (ru) " Была сцена после титров." else " There was a post-credits scene.")
        }, style = T.Muted)
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
fun LogSheet(c: Cinema, s: Seance, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var stars by remember { mutableIntStateOf(0) }
    var words by remember { mutableStateOf("") }
    var company by remember { mutableStateOf(Company.ALONE) }
    var credits by remember { mutableStateOf(Credits.NOTHING) }
    var rewatch by remember { mutableStateOf(s.rewatch || c.seats.any { it.title == s.title && !it.season }) }
    Sheet(nav::close) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BigPoster(s.title, s.genre, s.cover, 2.6f)
            Column(Modifier.weight(1f)) {
                Text(t("Корешок", "Stub").uppercase(), style = T.Eyebrow)
                Text(s.title, style = T.Title)
                Facts(s.year, s.country, s.minutes)
            }
        }
        Text(t("Как тебе?", "How was it?").uppercase(), style = T.Eyebrow)
        Popcorn(stars, size = 36.dp) { stars = it }
        Text(t("Три слова о фильме", "Three words about it").uppercase(), style = T.Eyebrow)
        Field(words, { words = it.take(60) }, t("дождь, единорог, слёзы", "rain, unicorn, tears"))
        Text(t("С кем", "With whom").uppercase(), style = T.Eyebrow)
        Flow { for ((v, l) in listOf(Company.ALONE to t("один", "alone"), Company.TWO to t("вдвоём", "for two"), Company.MANY to t("компанией", "with friends"))) Chip(l, company == v, { company = v }) }
        Text(t("После титров", "After the credits").uppercase(), style = T.Eyebrow)
        Flow { for ((v, l) in listOf(Credits.SCENE to t("была сцена", "a scene"), Credits.NOTHING to t("ничего", "nothing"), Credits.LEFT to t("не досидел", "left early"))) Chip(l, credits == v, { credits = v }) }
        Flow { Chip(t("впервые", "first time"), !rewatch, { rewatch = false }); Chip(t("пересмотр", "rewatch"), rewatch, { rewatch = true }) }
        Btn(t("Оторвать корешок", "Tear the stub"), {
            val tk = Ticket(title = s.title, year = s.year, country = s.country, genre = s.genre, minutes = s.minutes, stars = stars, words = words.trim(),
                company = company, credits = credits, rewatch = rewatch, cover = s.cover)
            co.write {
                val before = cinema()
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
    val name = c.s.pet(ru)
    fun done() { nav.close(); nav.tab(Tab.BOOTH); nav.say(when {
        g.traits.isNotEmpty() -> if (ru) "Смотри, что у меня теперь есть!" else "Look what I've got now!"
        g.grew != null -> if (ru) "Я подрос! Чувствуешь?" else "I've grown! Can you tell?"
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
        for (l in g.letters) rows += { Gain({ Letter(l, 1) }, if (ru) "Новая буква «$l»" else "A new letter «$l»", t("для вывески", "for the sign")) }
        for (d in g.decades) rows += { Gain({ Text("$d", style = T.Small.copy(color = C.onGold), modifier = Modifier.clip(R8.Sm).background(C.gold).padding(8.dp)) }, if (ru) "Десятилетие: $d-е" else "A decade: the ${d}s", t("в будке прибавилось техники", "the booth gets new gear")) }
        for (cn in g.countries) rows += { Gain({ Box(Modifier.size(56.dp)) { Stamp(cn) } }, if (ru) "Штамп: $cn" else "A stamp: $cn", t("новая страна в кругосветке", "a new country on the map")) }
        for (tr in g.traits) rows += { Gain({ BufImage(PetArt.still(Pose(c.species, c.s.shiny, c.stage, c.worn, mood = PetMood.HAPPY)), scale = 1.dp) }, (if (ru) "$name: " else "$name: ") + Words.trait(tr, ru) + "!", if (ru) "за третий фильм жанра «${Words.genre(tk.genre, true)}»" else "for the third ${Words.genre(tk.genre, false)}") }
        g.grew?.let { st -> rows += { Gain({ BufImage(PetArt.still(Pose(c.species, c.s.shiny, st, c.worn, mood = PetMood.HAPPY)), scale = 1.dp) }, if (ru) "$name подрос!" else "$name has grown!", Words.stage(st, ru)) } }
        if (g.scene) rows += { Gain({ Text("★", style = T.Display.copy(color = C.velvet)) }, t("Пасхалка после титров", "A post-credits scene"), t("+5 попкорна", "+5 popcorn")) }
        var shown by remember { mutableIntStateOf(if (Art.still) rows.size else 0) }
        LaunchedEffect(Unit) { while (shown < rows.size) { delay(320); shown++ } }
        rows.forEachIndexed { i, r -> AnimatedVisibility(i < shown, enter = fadeIn()) { r() } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("В зал", "To the hall"), { nav.close(); nav.tab(Tab.HALL) }, Modifier.weight(1f), kind = Kind.GHOST)
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
        Btn(t("Найти другой", "Find another"), { nav.go(Overlay.Add()) }, Modifier.fillMaxWidth(), kind = Kind.GOLD)
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
        Text(t("Что ставим?", "What's on tonight?"), style = T.Title)
        Text(if (ru) "${c.s.pet(true)} роется в жестянках." else "${c.s.pet(false)} rummages in the cans.", style = T.Muted)
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
    val ru = LocalRu.current
    Sheet(nav::close) {
        if (c.egg) {
            Text(t("Коробка с плёнкой", "A film can"), style = T.Title)
            Text(if (ru) "Внутри кто-то шуршит. Первые три фильма решат, кто это: нуар — кот, хоррор или фантастика — нетопырь, мультфильмы и мелодрамы — лис, комедии и вестерны — енот, драмы и документалки — сыч. Отмечено: ${c.seats.size} из 3."
                else "Something rustles inside. The first three films decide who it is: noir makes a cat, horror or sci-fi a bat, animation and romance a fox, comedies and westerns a raccoon, dramas and documentaries an owlet. Logged: ${c.seats.size} of 3.", style = T.Body)
            return@Sheet
        }
        Box(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface2), contentAlignment = Alignment.Center) {
            PixelCanvas(PetArt.W, PetArt.H, Modifier.width(220.dp)) { b, tick ->
                val tt = tick * Art.FRAME_MS / 1000.0
                PetArt.draw(b, Pose(c.species, c.s.shiny, c.stage, c.worn, tt, if (tick % 60 < 20) PetMood.HAPPY else PetMood.IDLE, blink = tick % 37 == 0, hop = if (tick % 60 < 8) kotlin.math.sin(tick % 60 / 8.0 * Math.PI) * 4 else 0.0))
            }
        }
        Text(c.s.pet(ru), style = T.Display, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(listOf(Words.species(c.species, ru) + if (c.s.shiny) (if (ru) " · техниколор" else " · technicolor") else "", Words.stage(c.stage, ru), Words.minutes(c.minutes, ru)).joinToString(" · "),
            style = T.Muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(t("Черты — третий фильм жанра дарит свою", "Traits: the third film of a genre gives one").uppercase(), style = T.Eyebrow)
        for (g in app.reelkin.data.Genre.entries.filter { it.trait != null }) {
            val tr = g.trait!!; val n = c.byGenre[g] ?: 0; val has = tr in c.traits
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text((if (has) "✓  " else "") + Words.trait(tr, ru), style = if (has) T.Strong.copy(color = C.gold) else T.Body, modifier = Modifier.weight(1f))
                Text("${Words.genre(g, ru)} ${minOf(n, 3)}/3", style = T.Small)
            }
        }
        Text(t("Снять черту, не теряя её, можно в настройках.", "You can take a trait off without losing it in settings."), style = T.Small)
    }
}

/* ---------------- the buffet ---------------- */

@Composable
fun ShopSheet(c: Cinema, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    Sheet(nav::close) {
        Text(t("Буфет", "Buffet"), style = T.Title)
        Text(if (ru) "Попкорн: за фильм 10, за серию 3, за финал сезона 15, за пасхалку и пересмотр ещё по 5. Сейчас: ${c.popcorn}." else "Popcorn: 10 a film, 3 an episode, 15 a season finale, 5 more for a post-credits scene or a rewatch. You have ${c.popcorn}.", style = T.Muted)
        for (d in Decor.entries) {
            val has = c.has(d)
            val (n, about) = when (d) {
                Decor.GARLAND -> t("Гирлянда", "String lights") to t("мигает над проектором, ночью тише", "twinkles over the projector, softer at night")
                Decor.CHAIR -> t("Бархатное кресло", "Velvet armchair") to (if (ru) "${c.s.pet(true)} будет смотреть сидя" else "${c.s.pet(false)} will watch sitting down")
                Decor.NEON -> t("Неон «ТИШЕ»", "«HUSH» neon") to t("розовый, иногда моргает", "pink, flickers now and then")
                Decor.POPCORN -> t("Попкорн-машина", "Popcorn machine") to t("стоит у окошка в зал", "stands by the port")
            }
            Row(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface2).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(n, style = T.Strong); Text(about, style = T.Muted) }
                Btn(if (has) t("убрать", "remove") else "${d.price} 🍿", {
                    co.write { settings.update { s -> if (has) s.copy(bought = s.bought - d.name, spent = s.spent - d.price) else s.copy(bought = s.bought + d.name, spent = s.spent + d.price) } }
                    if (!has) { nav.scene.burst(); nav.say(if (d == Decor.CHAIR) (if (ru) "Моё кресло! Никому не отдам." else "My chair! Nobody else's.") else (if (ru) "Красиво. Совсем как в настоящем кинотеатре." else "Lovely. Just like a real picture house.")) }
                }, kind = if (has) Kind.GHOST else Kind.GOLD, small = true, enabled = has || c.popcorn >= d.price)
            }
        }
        Text(t("Убранное возвращает попкорн целиком.", "Removing something gives all the popcorn back."), style = T.Small)
    }
}

/* ---------------- the sign ---------------- */

@Composable
fun SignSheet(c: Cinema, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var name by remember { mutableStateOf(run { val left = c.letters.toMutableMap(); c.s.cinema.filter { ch -> val n = left[ch] ?: 0; if (n > 0) { left[ch] = n - 1; true } else false }.toList() }) }
    Sheet(nav::close) {
        Text(t("Вывеска", "The sign"), style = T.Title)
        Text(t("Каждую букву можно поставить столько раз, сколько у тебя фильмов на неё.", "Each letter can be used as many times as you have films starting with it."), style = T.Muted)
        app.reelkin.ui.Marquee(name.joinToString(""), Modifier.fillMaxWidth())
        val used = name.groupingBy { it }.eachCount()
        Flow(gap = 5.dp) {
            for (ch in c.letters.keys.sorted()) { val left = (c.letters[ch] ?: 0) - (used[ch] ?: 0); Letter(ch, maxOf(left, 0)) { if (left > 0 && name.size < 12) name = name + ch } }
        }
        if (c.letters.isEmpty()) Text(t("Букв пока нет: каждая приходит с фильмом.", "No letters yet: each one comes with a film."), style = T.Body)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("Стереть", "Erase"), { name = name.dropLast(1) }, Modifier.weight(1f), kind = Kind.GHOST, enabled = name.isNotEmpty())
            Btn(t("Зажечь", "Light it"), {
                co.write { settings.update { it.copy(cinema = name.joinToString("")) } }
                nav.close(); nav.say(if (name.isEmpty()) (if (ru) "Просто «Кино». Тоже хорошо." else "Just «Cinema». Fine by me.") else if (ru) "Кино «${name.joinToString("")}». Звучит!" else "The ${name.joinToString("")}. Sounds grand!")
            }, Modifier.weight(1f))
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
            Text(if (ru) "Премия «${c.s.cinema.ifBlank { "Кино" }}» · ${c.year}" else "The ${c.s.cinema.ifBlank { "Cinema" }} awards · ${c.year}", style = T.Title)
            Text(if (ru) "Номинанты — всё, что ты посмотрел за год. ${c.s.pet(true)} уже в бабочке." else "The nominees are everything you watched this year. ${c.s.pet(false)} is already in a bow tie.", style = T.Muted)
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
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { BufImage(PetArt.still(Pose(c.species, c.s.shiny, c.stage, c.worn + app.reelkin.data.Trait.BOW, mood = PetMood.HAPPY)), scale = 3.dp) }
            var shown by remember { mutableIntStateOf(if (Art.still) 5 else 0) }
            LaunchedEffect(Unit) { while (shown < 5) { delay(600); shown++ } }
            Words.AWARDS.forEachIndexed { k, cat ->
                AnimatedVisibility(k < shown, enter = fadeIn()) {
                    Gain({ BufImage(Icons.statue, scale = 2.dp) }, picks[k].ifBlank { "—" }, if (ru) cat.first else cat.second)
                }
            }
            Btn(t("Овации", "Applause"), { nav.close(); nav.tab(Tab.HALL); nav.toast(if (ru) "Статуэтки стоят на полке в зале. Навсегда." else "The statuettes are on the shelf in the hall. For good.") }, Modifier.fillMaxWidth())
        }
    }
}
