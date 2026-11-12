package app.reelkin.ui.booth

import androidx.compose.foundation.horizontalScroll
import app.reelkin.ui.art.PetArt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.imageResource
import androidx.compose.foundation.layout.size
import app.reelkin.ui.components.Card
import app.reelkin.ui.sheets.seance
import app.reelkin.container
import app.reelkin.data.Stage
import app.reelkin.data.Title
import app.reelkin.domain.Cinema
import app.reelkin.domain.Decor
import app.reelkin.domain.Words
import app.reelkin.domain.gains
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Nav
import app.reelkin.ui.Overlay
import app.reelkin.ui.Tab
import app.reelkin.ui.art.Art
import app.reelkin.ui.art.BoothLook
import app.reelkin.ui.art.BoothScene
import app.reelkin.ui.art.Mood
import app.reelkin.ui.art.PixelCanvas
import app.reelkin.ui.art.Spot
import app.reelkin.ui.art.hex
import app.reelkin.ui.components.Bar
import app.reelkin.ui.components.Btn
import app.reelkin.ui.components.BufImage
import app.reelkin.ui.components.PosterImage
import app.reelkin.ui.components.Kind
import app.reelkin.ui.components.Says
import app.reelkin.ui.components.Section
import app.reelkin.ui.components.t
import app.reelkin.ui.components.Tile
import app.reelkin.ui.poster
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.random.Random

/** The booth as the cinema has it now; [bought] and [wall] stand in for what's bought, for trying things on in the buffet. */
fun lookOf(c: Cinema, ctx: android.content.Context, ru: Boolean, bought: Set<Decor>? = null, wall: app.reelkin.domain.Wall? = null): BoothLook {
    val p = c.pet
    val fav = c.favourite
    val seance = c.s.seance
    fun has(d: Decor) = bought?.contains(d) ?: c.has(d)
    return BoothLook(
        species = p?.species,
        stage = if (p != null && !p.pet.hatched) Stage.EGG else p?.stage ?: Stage.EGG,
        shiny = p?.pet?.shiny == true, form = p?.form, windup = if (bought != null) 0f else c.windup,
        hatch = p?.let { (it.fed / Stage.BABY.from.toFloat()).coerceIn(0f, 1f) } ?: 0f,
        poster = fav?.let { poster(ctx, it.title, it.genre, it.cover, app.reelkin.ui.art.Scenery.poster[2], app.reelkin.ui.art.Scenery.poster[3]) },
        cans = c.seats.takeLast(4).map { hex(it.genre.color.toLong()) },
        garland = has(Decor.GARLAND), chair = has(Decor.CHAIR), neon = has(Decor.NEON), popcorn = has(Decor.POPCORN),
        rug = has(Decor.RUG), plant = has(Decor.PLANT), clock = has(Decor.CLOCK), wall = (wall ?: c.wall).file,
        night = bought == null && isNight(c.now) && seance == null, seance = if (bought != null) null else seance?.genre, ru = ru,
    )
}

fun isNight(now: Long) = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY) < 6

@Composable
fun BoothScreen(c: Cinema, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val scene = nav.scene
    val co = ctx.container
    val seance = c.s.seance
    val p = c.pet
    val look = remember(c, scene.hatching, ru) { lookOf(c, ctx, ru) }
    // the can opens on its own the first time the booth is seen after the pet has eaten enough to hatch
    val ready = p != null && !p.pet.hatched && p.stage >= Stage.BABY
    LaunchedEffect(ready, p?.pet?.id) {
        if (ready && p != null && !scene.hatching) {
            delay(600)
            scene.hatch {
                co.write { db.dao().update(p.pet.copy(hatched = true)) }
                val name = c.name(p, ru)
                nav.say(if (ru) "Привет! Я ${Words.species(p.species, true)}. Меня зовут $name. Я люблю ${Words.loves(p.species, true)}." else "Hi! I'm a ${Words.species(p.species, false)}, $name. I love ${Words.loves(p.species, false)}.")
            }
        }
    }
    // during a film the pet reacts now and then
    LaunchedEffect(seance?.started) {
        if (seance == null || Art.still) return@LaunchedEffect
        while (true) {
            delay(9_000)
            val m = when (seance.genre) {
                app.reelkin.data.Genre.HORROR -> Mood.SCARED
                app.reelkin.data.Genre.COMEDY, app.reelkin.data.Genre.MUSICAL, app.reelkin.data.Genre.ANIMATION, app.reelkin.data.Genre.ADVENTURE -> Mood.HAPPY
                app.reelkin.data.Genre.ROMANCE, app.reelkin.data.Genre.DRAMA -> Mood.SAD
                else -> Mood.WATCH
            }
            scene.mood(m, 2.2); if (m == Mood.HAPPY) scene.hop()
            if (Random.nextDouble() < .6) nav.say(Words.react(seance.genre, ru))
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).padding(6.dp).clip(R8.Md)) {
            Booth(scene, look) { spot -> tap(spot, c, nav, scene, ru) }
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) { app.reelkin.ui.cards.ShareButton({ nav.go(Overlay.ShareIt(app.reelkin.ui.cards.Share.Card(app.reelkin.ui.cards.Postcard.BOOTH))) }, 34.dp) }
        }
        app.reelkin.ui.components.FirstVisit(app.reelkin.ui.components.Tip.BOOTH, c.s)
        if (p == null) Card(Modifier.fillMaxWidth()) {
            Text(t("Здесь будет жить твой питомец", "Your pet will live here"), style = T.Name)
            Text(t("Выбери одну из трёх коробок с плёнкой. В ней кто-то спит: покажи ему кино, и коробка откроется.",
                "Pick one of three film cans. Someone sleeps inside: show it a film and the can opens."), style = T.Muted)
            Btn(t("Выбрать коробку", "Pick a can"), { nav.go(Overlay.Pets) }, Modifier.fillMaxWidth().padding(top = 8.dp), kind = Kind.GOLD)
        } else PetCard(c, p, nav.said.first.ifEmpty { Words.poke(c, ru, Random(c.now / 600_000)) }) { nav.go(Overlay.Pet) }
        if (seance != null) SeanceCard(c, nav) else Tonight(c, nav)
        QuestLine(c) { nav.go(Overlay.Quests) }
        if (c.running.isNotEmpty()) {
            Section(t("Сейчас идёт", "Now running"), t("сериалы", "series"))
            for (s in c.running) SeriesRow(s, nav)
        }
    }
}

/** The pet under the booth, as in Songling's den: who it is, what it says, how far to grow, today and the streak. */
@Composable
private fun PetCard(c: Cinema, p: app.reelkin.domain.PetState, said: String, onClick: () -> Unit) {
    val ru = LocalRu.current
    val (_, streak) = remember(c) { app.reelkin.domain.Stats.streaks(c) }
    val today = remember(c) { app.reelkin.domain.Stats.byDay(c)[(c.now + java.util.TimeZone.getDefault().getOffset(c.now)).floorDiv(app.reelkin.data.DAY)] ?: 0L }
    Column(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).border(1.dp, C.line, R8.Lg).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val hatched = p.pet.hatched
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (hatched) c.name(p, ru) else t("Коробка с плёнкой", "A film can"), style = T.Name)
                Text(listOfNotNull(Words.species(p.species, ru), if (hatched) Words.stage(p.stage, ru) else null, p.form?.let { Words.form(it, ru).substringBefore(" (") }).joinToString(" · "), style = T.Small)
            }
            Text(app.reelkin.ui.bill.dur(p.fed.toLong(), ru), style = T.Small.copy(color = C.gold))
        }
        if (said.isNotEmpty()) Text("«$said»", style = T.Body.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = C.muted), maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Bar(if (hatched) p.progress else p.fed / Stage.BABY.from.toFloat(), Modifier.fillMaxWidth())
        val next = if (hatched) p.stage.next else Stage.BABY
        Text(if (next == null) t("старше не бывает", "as old as they come")
            else if (ru) "до «${Words.stage(next, true)}» ${app.reelkin.ui.bill.dur((next.from - p.fed).coerceAtLeast(0).toLong(), true)} кино" else "${app.reelkin.ui.bill.dur((next.from - p.fed).coerceAtLeast(0).toLong(), false)} of film to «${Words.stage(next, false)}»",
            style = T.Small, maxLines = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (today > 0) Pill(t("сегодня ", "today ") + app.reelkin.ui.bill.dur(today, ru), C.gold)
            if (streak > 1) Pill(if (ru) "$streak ${Words.plural(streak, "день", "дня", "дней")} подряд" else "$streak-day streak", C.velvet)
            if (today == 0L) {
                val last = maxOf(c.seats.lastOrNull()?.at ?: 0L, c.episodes.maxOfOrNull { it.at } ?: 0L)
                if (last > 0) {
                    val days = ((c.now - last) / app.reelkin.data.DAY).toInt()
                    Pill(when {
                        days <= 0 -> t("кино было сегодня", "a film today")
                        days == 1 -> t("последнее кино вчера", "last film yesterday")
                        else -> if (ru) "последнее кино $days ${Words.plural(days, "день", "дня", "дней")} назад" else "last film $days days ago"
                    }, C.muted)
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text, style = T.Small.copy(color = color), maxLines = 1, modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(color.copy(alpha = .12f)).padding(horizontal = 10.dp, vertical = 4.dp))
}

/** The week's tasks in one line: a bar each, and popcorn waiting to be taken. */
@Composable
private fun QuestLine(c: Cinema, onClick: () -> Unit) {
    val ru = LocalRu.current
    val done = c.quests.count { it.p.done }
    val ready = c.quests.filter { it.p.done && "${c.weekId}:${it.q.name}" !in c.s.claimed }.sumOf { it.q.reward }
    Row(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).border(1.dp, C.line, R8.Lg).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(t("Задания недели", "This week's tasks"), style = T.Strong)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (q in c.quests) Box(Modifier.weight(1f)) { Bar(q.p.part, Modifier.fillMaxWidth()) }
            }
        }
        if (ready > 0) Text("+$ready", style = T.Small.copy(color = C.gold), modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(C.goldSoft).padding(horizontal = 10.dp, vertical = 5.dp))
        else Text(if (ru) "$done из ${c.quests.size}" else "$done of ${c.quests.size}", style = T.Small)
        Text("›", style = T.Title.copy(color = C.faint))
    }
}

/**
 * Tonight: the pet's pick from the bill for the mood chosen on top — a big poster on the genre's colour, why it fits as
 * little tags, the pet's word on it, one big button to start the show, and a few more picks under it to swap to.
 * With nothing on the bill, a way to find something.
 */
@Composable
private fun Tonight(c: Cinema, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var skip by remember { mutableStateOf(setOf<Long>()) }
    var mood by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(app.reelkin.domain.Mood.COSY) }
    // The pick and the next few after it, each as if the ones before were skipped.
    val picks = remember(c, skip, mood, ru) {
        val out = ArrayList<app.reelkin.domain.Pick>(); var s = skip
        while (out.size < 4) {
            val p = app.reelkin.domain.Picker.pick(c, 125, mood, s, ru)?.takeIf { it.films.isNotEmpty() } ?: break
            out += p; s = s + p.films.first().id
        }
        out
    }
    var chosen by remember(picks) { mutableStateOf(0) }
    val film = picks.getOrNull(chosen)?.films?.firstOrNull()
    val tint = film?.let { C.genre(it.genre) } ?: C.gold
    Column(Modifier.fillMaxWidth().clip(R8.Lg)
        .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(tint.copy(alpha = .22f), C.surface, C.surface)))
        .border(1.dp, C.line, R8.Lg).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t("Сегодня вечером", "Tonight").uppercase(), style = T.Eyebrow, modifier = Modifier.weight(1f))
            if (film != null) Text(t("Подобрать ›", "Pick ›"), style = T.Small.copy(color = C.gold), modifier = Modifier.clip(R8.Sm).clickable { nav.go(Overlay.Pick) }.padding(4.dp))
        }
        // The mood for the evening.
        Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (m in app.reelkin.domain.Mood.entries) app.reelkin.ui.components.Chip(if (ru) m.ru else m.en, m == mood, { mood = m; skip = emptySet() })
        }
        if (film == null) {
            Text(if (c.watch.isNotEmpty()) t("Под это настроение в афише ничего не нашлось. Попробуй другое или добавь фильм.", "Nothing on the bill for this mood. Try another or add a film.")
                else t("В афише пока пусто. Найди, что посмотреть, — питомец будет предлагать оттуда.", "Nothing on the bill yet. Find something to watch and the pet will pick from it."), style = T.Muted)
            Btn(t("Найти фильм", "Find a film"), { nav.go(Overlay.Add()) }, Modifier.fillMaxWidth(), kind = Kind.GOLD)
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PosterImage(film.title, film.genre, film.cover, 34, 50, Modifier.clip(R8.Md).clickable { nav.go(Overlay.Film(film.id)) }, scale = 2.4.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(film.title, style = T.Name, maxLines = 3, modifier = Modifier.clickable { nav.go(Overlay.Film(film.id)) })
                Text(listOfNotNull(film.year?.toString(), film.minutes.takeIf { it > 0 }?.let { app.reelkin.ui.bill.dur(it.toLong(), ru) }, film.country.ifBlank { null }).joinToString(" · "), style = T.Small)
                // Why this one, as tags: its genre, what's new in it for you, and whether the pet loves it.
                val loves = c.pet?.let { film.genre in app.reelkin.domain.Pets.genres(it.species) } == true
                app.reelkin.ui.components.Flow(gap = 5.dp) {
                    app.reelkin.ui.components.Tag(Words.genre(film.genre, ru), C.genre(film.genre))
                    film.year?.let { y -> Cinema.decade(y).takeIf { it !in c.decades }?.let { d -> Note(if (ru) "новые $d-е" else "new: the ${d}s") } }
                    if (film.country.isNotBlank() && film.country !in c.countries) Note(t("новый штамп", "a new stamp"))
                    if (loves) Note(t("любимое ×1,5", "a favourite ×1.5"), gold = true)
                }
            }
        }
        // The pet's word on it.
        c.pet?.takeIf { it.pet.hatched }?.let { p ->
            val line = when {
                film.genre in mood.genres && loves(c, film) -> if (ru) "Хочется «${mood.ru}», а это как раз моё любимое!" else "You want ${mood.en}, and it's my favourite kind!"
                film.genre in mood.genres -> if (ru) "Хочется «${mood.ru}» — вот оно." else "You want ${mood.en}: here it is."
                else -> if (ru) "Не совсем «${mood.ru}», но тебе понравится." else "Not quite ${mood.en}, but you'll like it."
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                app.reelkin.ui.components.FitBuf(PetArt.portrait(p.species, p.stage, p.pet.shiny, p.form), Modifier.size(40.dp))
                Text(line, style = T.Small.copy(color = C.ink), modifier = Modifier.weight(1f).clip(R8.Md).background(C.surface2).padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
        Btn(t("Ставлю на проектор", "Put it on"), {
            co.write { office.startSeance(film.seance()) }
            nav.say(if (ru) "Гасим свет. Я сяду поближе к окошку." else "Lights down. I'll sit by the port.")
        }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("Уже посмотрел", "Watched it"), { nav.go(Overlay.Log(film.seance())) }, Modifier.weight(1f), kind = Kind.SOFT, small = true)
            Btn(t("Не сегодня", "Not tonight"), { skip = skip + film.id }, Modifier.weight(1f), kind = Kind.SOFT, small = true)
        }
        // A few more to swap to.
        if (picks.size > 1) {
            Text(t("Варианты на вечер", "Options for tonight").uppercase(), style = T.Eyebrow)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((k, pk) in picks.withIndex()) {
                    val f = pk.films.first()
                    val on = k == chosen
                    Column(Modifier.weight(1f).clip(R8.Md).background(if (on) C.goldSoft else C.surface2).clickable { chosen = k }.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PosterImage(f.title, f.genre, f.cover, 24, 34, Modifier.clip(R8.Sm), scale = 2.dp)
                        Text(f.minutes.takeIf { it > 0 }?.let { app.reelkin.ui.bill.dur(it.toLong(), ru) } ?: (f.year?.toString() ?: ""), style = T.Small.copy(color = if (on) C.gold else C.muted), maxLines = 1)
                    }
                }
                repeat(4 - picks.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

private fun loves(c: Cinema, f: app.reelkin.data.Title) = c.pet?.let { f.genre in app.reelkin.domain.Pets.genres(it.species) } == true

/** A quiet tag: why a pick fits. */
@Composable
private fun Note(text: String, gold: Boolean = false) {
    Text(text, style = T.Small.copy(color = if (gold) C.gold else C.muted),
        modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(if (gold) C.goldSoft else C.surface2).padding(horizontal = 10.dp, vertical = 3.dp))
}

/** Five small buttons: things to do from the booth. */
@Composable
private fun Actions(c: Cinema, nav: Nav) {
    Row(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).border(1.dp, C.line, R8.Lg).padding(vertical = 10.dp, horizontal = 4.dp)) {
        Action(app.reelkin.R.drawable.act_seance, t("Сеанс", "Showtime")) { nav.go(Overlay.Choose(false)) }
        Action(app.reelkin.R.drawable.act_log, t("Посмотрел", "Watched")) { nav.go(Overlay.Choose(true)) }
        Action(app.reelkin.R.drawable.act_pick, t("Что ставим", "What's on")) { nav.go(Overlay.Pick) }
        Action(app.reelkin.R.drawable.act_can, t("Питомцы", "Pets"), badge = c.newCans.size.takeIf { it > 0 }?.toString()) { nav.go(Overlay.Pets) }
        Action(app.reelkin.R.drawable.act_buffet, t("Буфет", "Buffet")) { nav.go(Overlay.Shop) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Action(icon: Int, label: String, badge: String? = null, onClick: () -> Unit) {
    Column(Modifier.weight(1f).clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            Box(Modifier.size(46.dp).clip(androidx.compose.foundation.shape.CircleShape).background(C.surface2), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(androidx.compose.ui.graphics.ImageBitmap.imageResource(icon), null, Modifier.size(24.dp),
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.Low, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(C.muted))
            }
            // Something waiting there: a can ready to open.
            if (badge != null) Text(badge, style = T.Small.copy(color = C.onGold, fontSize = T.Small.fontSize * .85f), maxLines = 1,
                modifier = Modifier.align(Alignment.TopEnd).clip(androidx.compose.foundation.shape.CircleShape).background(C.gold).padding(horizontal = 6.dp, vertical = 1.dp))
        }
        Text(label, style = T.Small.copy(color = C.muted), maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun tap(spot: Spot, c: Cinema, nav: Nav, scene: BoothScene, ru: Boolean) {
    when (spot) {
        Spot.PET -> {
            val p = c.pet
            when {
                p == null -> nav.go(Overlay.Pets)
                else -> { if (p.pet.hatched) { scene.hop(); scene.mood(Mood.HAPPY, 1.5) }; nav.go(Overlay.Pet) }
            }
        }
        Spot.PROJECTOR -> if (c.s.seance != null) nav.say(if (ru) "Не трогай, лампа горячая!" else "Careful, the lamp's hot!") else nav.go(Overlay.Choose(false))
        Spot.POSTER -> c.favourite?.let { nav.go(Overlay.Stub(it.id)) }
        Spot.CANS -> nav.tab(Tab.BILL)
        Spot.POPCORN -> nav.go(Overlay.Shop)
        Spot.PORT -> nav.saga(2)
        Spot.NONE -> nav.go(Overlay.Shop)
    }
}

@Composable
private fun Booth(scene: BoothScene, look: BoothLook, onTap: (Spot) -> Unit) {
    val last = remember { mutableLongStateOf(-1L) }
    PixelCanvas(BoothScene.W, BoothScene.H, Modifier.fillMaxWidth().pointerInput(look) {
        detectTapGestures { o -> onTap(scene.hit(o.x * BoothScene.W / size.width.toDouble(), o.y * BoothScene.H / size.height.toDouble(), look)) }
    }, fine = BoothScene.F) { b, tick ->
        val dt = if (last.longValue < 0) 0.0 else ((tick - last.longValue).coerceIn(0, 5) * Art.FRAME_MS / 1000.0)
        last.longValue = tick.toLong()
        scene.draw(b, look, tick * Art.FRAME_MS / 1000.0, dt)
    }
}

@Composable
private fun SeanceCard(c: Cinema, nav: Nav) {
    val s = c.s.seance ?: return
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(s.started) { if (!Art.still) while (true) { now = System.currentTimeMillis(); delay(1_000) } }
    val min = ((now - s.started) / 60_000).toInt().coerceAtLeast(0)
    var asked by remember(s.started) { mutableStateOf(false) }
    LaunchedEffect(min >= s.minutes) {
        if (s.minutes > 0 && min >= s.minutes && !asked) { asked = true; nav.say(if (ru) "Титры! Сидим до конца — вдруг там что-то есть." else "Credits! Stay to the end, there might be something."); delay(1_200); nav.go(app.reelkin.ui.Overlay.Log(s)) }
    }
    Column(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).border(1.dp, C.gold.copy(alpha = .5f), R8.Lg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t("Идёт сеанс · телефон можно положить экраном вниз", "Now showing · you can put the phone face down").uppercase(), style = T.Eyebrow)
        Text(s.title, style = T.Title)
        Bar(if (s.minutes > 0) min / s.minutes.toFloat() else 0f, Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${Words.hm(min.coerceAtMost(maxOf(s.minutes, min)))} ${t("из", "of")} ${Words.hm(s.minutes)}", style = T.Muted, modifier = Modifier.weight(1f))
            Btn(t("Прервать", "Stop"), { co.write { office.stopSeance() }; nav.say(if (ru) "Ладно, досмотрим потом. Я запомнил кадр." else "All right, later then. I've marked the frame.") }, kind = Kind.GHOST, small = true)
            Btn(t("Титры", "Credits"), { nav.go(Overlay.Log(s)) }, kind = Kind.GOLD, small = true)
        }
    }
}

@Composable
fun SeriesRow(s: Title, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    Row(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).border(1.dp, C.line, R8.Lg).clickable { nav.go(Overlay.Series(s.id)) }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PosterImage(s.title, s.genre, s.cover, 24, 34, Modifier.clip(R8.Sm), scale = 1.6.dp)
        Column(Modifier.weight(1f)) {
            Text(s.title, style = T.Name, maxLines = 1)
            Text(t("сезон ${s.season} · серия ${s.episode} из ${s.perSeason}", "season ${s.season} · episode ${s.episode} of ${s.perSeason}"), style = T.Muted)
            if (s.cliff) Text(t("на самом интересном", "at the worst moment"), style = T.Small.copy(color = C.velvet))
            Words.air(s, ru)?.let { Text(it, style = T.Small.copy(color = C.gold), maxLines = 1) }
        }
        Btn(t("+1 серия", "+1 ep"), { episode(s, nav, ctx, ru) }, small = true)
    }
}

/** One more episode, with the pet's joy and, at a season's end, a stub. */
fun episode(s: Title, nav: Nav, ctx: android.content.Context, ru: Boolean) {
    val co = ctx.container
    co.write {
        val before = cinema()
        val stub = office.episode(s, ru)
        if (stub == null) {
            nav.scene.hop(); nav.scene.mood(Mood.HAPPY, 1.5)
            nav.say(if (s.cliff) (if (ru) "Фух. Отпустило. Хотя…" else "Phew. That's settled. Or is it…") else (if (ru) "Серия ${s.episode + 1}. Ещё одну?" else "Episode ${s.episode + 1}. One more?"))
            nav.toast(if (ru) "«${s.title}»: серия ${s.episode + 1} · +3 попкорна" else "${s.title}: episode ${s.episode + 1} · +3 popcorn")
        } else {
            val after = cinema()
            nav.scene.hop(34.0); nav.scene.burst(40); nav.scene.mood(Mood.HAPPY, 3.0)
            nav.go(Overlay.Reward(stub, gains(before, after, stub)))
        }
    }
}
