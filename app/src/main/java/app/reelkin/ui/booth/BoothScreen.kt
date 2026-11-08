package app.reelkin.ui.booth

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
import app.reelkin.container
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
import app.reelkin.ui.pet
import app.reelkin.ui.poster
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.random.Random

fun isNight(now: Long) = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY) < 6

@Composable
fun BoothScreen(c: Cinema, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val scene = nav.scene
    val co = ctx.container
    val seance = c.s.seance
    val look = remember(c, scene.hatching, ru) {
        val fav = c.favourite
        BoothLook(
            species = if (c.egg && !scene.hatching) null else c.species,
            shiny = if (c.egg) Cinema.shinyFor(c.seats) else c.s.shiny,
            stage = c.stage, traits = c.worn,
            poster = fav?.let { poster(ctx, it.title, it.genre, it.cover, 24, 34) },
            cans = c.seats.takeLast(4).map { hex(it.genre.color.toLong()) },
            garland = c.has(Decor.GARLAND), chair = c.has(Decor.CHAIR), neon = c.has(Decor.NEON), popcorn = c.has(Decor.POPCORN),
            night = isNight(c.now) && seance == null, seance = seance?.genre, ru = ru,
        )
    }
    // the can opens on its own the first time the booth is seen after the third stub
    LaunchedEffect(c.hatchReady) {
        if (c.hatchReady && !scene.hatching) {
            delay(600)
            scene.hatch {
                val sp = Cinema.speciesFor(c.seats); val sh = Cinema.shinyFor(c.seats)
                co.write { settings.update { it.copy(species = sp, shiny = sh) } }
                nav.say(if (ru) "Привет! Я ${Words.species(sp, true)}. Меня зовут ${c.s.pet(true)}. Покажешь мне кино?" else "Hi! I'm a ${Words.species(sp, false)}, ${c.s.pet(false)}. Will you show me a film?")
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().clip(R8.Lg).border(1.dp, C.line, R8.Lg)) {
            Booth(scene, look) { spot -> tap(spot, c, nav, scene, ru) }
            Bubble(c, nav, Modifier.align(Alignment.TopCenter).padding(8.dp))
        }
        if (seance != null) SeanceCard(c, nav) else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("Сеанс", "Showtime"), { nav.go(Overlay.Choose(false)) }, Modifier.weight(1f), small = true)
            Btn(t("Посмотрел", "Watched"), { nav.go(Overlay.Choose(true)) }, Modifier.weight(1f), kind = Kind.GOLD, small = true)
            Btn(t("Что ставим?", "What's on?"), { nav.go(Overlay.Pick) }, Modifier.weight(1f), kind = Kind.SOFT, small = true)
        }
        PetLine(c) { nav.go(Overlay.Pet) }
        // The rest of the cinema, a tap away instead of behind the numbers in the header.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(t("Буфет", "Buffet"), if (ru) "${c.popcorn} попкорна" else "${c.popcorn} popcorn", Modifier.weight(1f)) { nav.go(Overlay.Shop) }
            Tile(t("Вывеска", "The sign"), if (ru) "букв: ${c.letters.values.sum()}" else "${c.letters.values.sum()} letters", Modifier.weight(1f)) { nav.go(Overlay.Sign) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val films = c.thisYear.count { !it.season }
            Tile(t("Статистика", "Stats"), if (ru) "${Words.films(films, true)} в ${c.year}" else "${Words.films(films, false)} in ${c.year}", Modifier.weight(1f)) { nav.go(Overlay.Stats) }
            Tile(t("Премия года", "The awards"), if (c.s.awards.any { it.year == c.year }) t("вручена", "handed out") else t("вручить ${c.year}", "hand out ${c.year}"), Modifier.weight(1f), accent = C.velvet) {
                if (c.thisYear.isEmpty()) nav.say(if (ru) "Сначала посмотрим что-нибудь в этом году — потом вручим." else "Let's watch something this year first, then we'll hand them out.") else nav.go(Overlay.Award)
            }
        }
        if (c.running.isNotEmpty()) {
            Section(t("Сейчас идёт", "Now running"), t("сериалы", "series"))
            for (s in c.running) SeriesRow(s, nav)
        }
    }
}

private fun tap(spot: Spot, c: Cinema, nav: Nav, scene: BoothScene, ru: Boolean) {
    when (spot) {
        Spot.PET -> if (c.egg) {
            val n = Cinema.HATCH_AT - c.seats.size
            nav.say(if (ru) "Тук-тук. Отметь ещё ${Words.films(n.coerceAtLeast(0), true)} — и коробка откроется." else "Knock knock. Log ${Words.films(n.coerceAtLeast(0), false)} more and the can opens.")
        } else { scene.hop(); scene.mood(Mood.HAPPY, 1.5); nav.say(Words.poke(c, ru)) }
        Spot.PROJECTOR -> if (c.s.seance != null) nav.say(if (ru) "Не трогай, лампа горячая!" else "Careful, the lamp's hot!") else nav.go(Overlay.Choose(false))
        Spot.POSTER -> c.favourite?.let { nav.go(Overlay.Stub(it.id)) }
        Spot.CANS -> nav.tab(Tab.BILL)
        Spot.POPCORN -> nav.go(Overlay.Shop)
        Spot.PORT -> nav.tab(Tab.HALL)
        Spot.NONE -> if (!c.egg) nav.say(Words.poke(c, ru))
    }
}

@Composable
private fun Booth(scene: BoothScene, look: BoothLook, onTap: (Spot) -> Unit) {
    val last = remember { mutableLongStateOf(-1L) }
    PixelCanvas(BoothScene.W, BoothScene.H, Modifier.fillMaxWidth().pointerInput(look) {
        detectTapGestures { o -> onTap(scene.hit(o.x * BoothScene.W / size.width.toDouble(), o.y * BoothScene.H / size.height.toDouble(), look)) }
    }) { b, tick ->
        val dt = if (last.longValue < 0) 0.0 else ((tick - last.longValue).coerceIn(0, 5) * Art.FRAME_MS / 1000.0)
        last.longValue = tick.toLong()
        scene.draw(b, look, tick * Art.FRAME_MS / 1000.0, dt)
    }
}

@Composable
private fun Bubble(c: Cinema, nav: Nav, modifier: Modifier) {
    val (text, stamp) = nav.said
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(stamp) { if (text.isNotEmpty()) { shown = true; if (!Art.still) { delay(3_000L + text.length * 45L); shown = false } } }
    if (!shown || text.isEmpty()) return
    val ru = LocalRu.current
    Says(text, if (c.egg && !nav.scene.hatching) (if (ru) "коробка" else "the can") else c.s.pet(ru), modifier.clickable { shown = false })
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
private fun PetLine(c: Cinema, onClick: () -> Unit) {
    val ru = LocalRu.current
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (c.egg) {
            Text(t("Коробка с плёнкой", "A film can"), style = T.Strong)
            Bar(c.seats.size / Cinema.HATCH_AT.toFloat(), Modifier.weight(1f))
            Text(minOf(c.seats.size, Cinema.HATCH_AT).let { n -> t("$n из ${Cinema.HATCH_AT}", "$n of ${Cinema.HATCH_AT}") }, style = T.Small)
        } else {
            Text("${c.s.pet(ru)} · ${Words.stage(c.stage, ru)}", style = T.Strong)
            val nx = Cinema.STEPS.getOrNull(c.stage + 1)
            Bar(if (nx == null) 1f else ((c.minutes - Cinema.STEPS[c.stage]).toFloat() / (nx - Cinema.STEPS[c.stage])), Modifier.weight(1f))
            if (nx != null) Text(if (ru) "ещё ${Words.minutes(nx - c.minutes, true)}" else "${Words.minutes(nx - c.minutes, false)} to go", style = T.Small)
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
