package app.reelkin.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.imageResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reelkin.container
import app.reelkin.data.Covers
import app.reelkin.data.Draft
import app.reelkin.data.Seance
import app.reelkin.data.Settings
import app.reelkin.data.Ticket
import app.reelkin.data.Title
import app.reelkin.domain.Cinema
import app.reelkin.domain.Gains
import app.reelkin.domain.Words
import app.reelkin.ru
import app.reelkin.ui.art.BoothScene
import app.reelkin.ui.art.Buf
import app.reelkin.ui.art.Posters
import app.reelkin.ui.bill.BillScreen
import app.reelkin.ui.booth.BoothScreen
import app.reelkin.ui.components.t
import app.reelkin.ui.onboarding.Onboarding
import app.reelkin.ui.settings.SettingsPage
import app.reelkin.ui.sheets.AddSheet
import app.reelkin.ui.sheets.AwardSheet
import app.reelkin.ui.sheets.ChooseSheet
import app.reelkin.ui.sheets.DraftSheet
import app.reelkin.ui.sheets.FilmSheet
import app.reelkin.ui.sheets.LogSheet
import app.reelkin.ui.sheets.PetSheet
import app.reelkin.ui.sheets.PickSheet
import app.reelkin.ui.sheets.RewardSheet
import app.reelkin.ui.sheets.SeriesSheet
import app.reelkin.ui.sheets.ShopSheet
import app.reelkin.ui.sheets.StubSheet
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.T
import kotlinx.coroutines.delay

val LocalRu = staticCompositionLocalOf { true }

enum class Tab { BOOTH, BILL, SAGA, PETS }

/** What is open over the tabs. */
sealed class Overlay {
    data class Film(val id: Long) : Overlay()
    data class Series(val id: Long) : Overlay()
    data class Stub(val id: Long) : Overlay()
    /** The stub form for a film just watched, or for a season of a new [series] that goes in the cupboard once the stub is torn. */
    /** [earlier]: the season the stub is for, when the seasons before it without a stub are torn too. */
    data class Log(val s: Seance, val series: Title? = null, val earlier: Int = 0) : Overlay()
    data class Reward(val ticket: Ticket, val g: Gains) : Overlay()
    data class Choose(val log: Boolean) : Overlay()
    data object Pick : Overlay()
    data class Add(val q: String = "") : Overlay()
    data class NewTitle(val d: Draft, val edit: Long? = null) : Overlay()
    data object Pet : Overlay()
    data object Pets : Overlay()
    data object Cards : Overlay()
    data object Quests : Overlay()
    data object Menu : Overlay()
    data object Feats : Overlay()
    data class ShareIt(val what: app.reelkin.ui.cards.Share) : Overlay()
    data class PetOf(val id: Long) : Overlay()
    data class Can(val sp: app.reelkin.data.Species) : Overlay()
    data class SagaDetail(val key: String) : Overlay()
    data class GoalEdit(val id: Long? = null) : Overlay()
    data object Shop : Overlay()
    data object Award : Overlay()
    data object Settings : Overlay()
    data object Stats : Overlay()
    data object Import : Overlay()
}

/** Ways screens move around and talk to each other. */
@Stable
class Nav(val go: (Overlay?) -> Unit, val tab: (Tab) -> Unit) {
    fun close() = go(null)
    var said by mutableStateOf("" to 0L)
        private set
    var toast by mutableStateOf("" to 0L)
        private set
    /** The booth stays alive across tabs, so the pet keeps its place and the sheets can make it hop. */
    val scene = BoothScene()
    fun say(t: String) { said = t to System.nanoTime() }
    /** The page the saga opens on: 0 stats, 1 calendar, 2 the hall, 3 awards. */
    var sagaPage by mutableStateOf(0)
    /** A director or an actor the bill should show the films of, set from a film's card. */
    var billPerson by mutableStateOf<String?>(null)
    fun person(name: String) { billPerson = name; go(null); tab(Tab.BILL) }
    /** Genres the bill should show, set from a pet's card. */
    var billGenres by mutableStateOf<Set<app.reelkin.data.Genre>?>(null)
    fun genres(g: Set<app.reelkin.data.Genre>) { billGenres = g; go(null); tab(Tab.BILL) }
    /** What the saga counts, kept so its details open on the same span. */
    var sagaFilter by mutableStateOf<app.reelkin.domain.Filter?>(null)
    fun saga(page: Int) { sagaPage = page; tab(Tab.SAGA) }
    fun toast(t: String) { toast = t to System.nanoTime() }
}

val LocalNav = staticCompositionLocalOf<Nav> { error("no nav") }


/** A picture cut to w×h art pixels, filling it, as many fine pixels to each as the scenes have: no blocks, no palette. */
private fun fine(bmp: android.graphics.Bitmap, w: Int, h: Int): Buf {
    val f = Buf.FINE
    val k = maxOf(w * f / bmp.width.toFloat(), h * f / bmp.height.toFloat())
    val sw = (bmp.width * k).toInt().coerceAtLeast(w * f); val sh = (bmp.height * k).toInt().coerceAtLeast(h * f)
    val scaled = android.graphics.Bitmap.createScaledBitmap(bmp, sw, sh, true)
    val b = Buf(w, h, f)
    scaled.getPixels(b.d, 0, b.fw, (sw - b.fw) / 2, (sh - b.fh) / 2, b.fw, b.fh)
    for (i in b.d.indices) b.d[i] = b.d[i] or Buf.OPAQUE
    return b
}

/** A poster for anything with a title, a genre and maybe a picture. */
fun poster(ctx: Context, title: String, genre: app.reelkin.data.Genre, cover: String?, w: Int, h: Int): Buf =
    Covers.art(ctx, cover)?.full?.let { fine(it, w, h) } ?: Posters.of(title, genre, w, h, Covers.art(ctx, cover))

/** [clock], [tab0] and [overlay0] let renders open a fixed moment on a given screen; the app passes none. */
@Composable
fun ReelkinRoot(s: Settings, clock: Long? = null, tab0: Tab = Tab.BOOTH, overlay0: Overlay? = null) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    CompositionLocalProvider(LocalRu provides ru) {
        if (!s.onboarded) { Onboarding(); return@CompositionLocalProvider }
        val titles by c.db.dao().titles().collectAsStateWithLifecycle(initialValue = null)
        val tickets by c.db.dao().tickets().collectAsStateWithLifecycle(initialValue = null)
        val episodes by c.db.dao().episodes().collectAsStateWithLifecycle(initialValue = null)
        val pets by c.db.dao().pets().collectAsStateWithLifecycle(initialValue = null)
        val goals by c.db.dao().goals().collectAsStateWithLifecycle(initialValue = null)
        // The one pet of 1.x moves into the pets table, with all it ate.
        LaunchedEffect(Unit) { c.office.bringOldPet(); c.office.bringYearGoals() }
        var now by remember { mutableLongStateOf(clock ?: System.currentTimeMillis()) }
        LaunchedEffect(Unit) { if (clock == null) while (true) { delay(30_000); now = System.currentTimeMillis() } }
        val cin = remember(titles, tickets, episodes, pets, goals, s, now) { if (titles == null || tickets == null || episodes == null || pets == null || goals == null) null else Cinema(titles!!, tickets!!, episodes!!, s, now, pets!!, goals!!) } ?: return@CompositionLocalProvider
        // A pet grown up takes its form for good.
        LaunchedEffect(cin.pet?.stage, cin.pet?.pet?.id) {
            val p = cin.pet ?: return@LaunchedEffect
            if (p.pet.form == null && p.stage >= app.reelkin.data.Stage.ADULT) p.leaning?.let { f -> c.db.dao().update(p.pet.copy(form = f)) }
        }
        var tab by rememberSaveable { mutableStateOf(tab0) }
        var overlay by remember { mutableStateOf(overlay0) }
        val nav = remember { Nav({ overlay = it }, { tab = it; overlay = null }) }
        LaunchedEffect(Unit) {
            if (nav.said.first.isEmpty()) nav.say(when {
                cin.pet == null -> if (ru) "В будке пусто. Выбери коробку — в каждой кто-то спит." else "The booth is empty. Pick a can: someone sleeps in each."
                cin.pet?.pet?.hatched == false -> if (ru) "В коробке кто-то шуршит. Покажи ему кино — и она откроется." else "Something rustles in the can. Show it a film and it opens."
                else -> (if (ru) "Добрый вечер. " else "Good evening. ") + Words.poke(cin, ru)
            })
        }

        val backdrop = rememberBackdrop(s.backdrop, Backdrops.nowOn(cin), s.backdropStamp)
        CompositionLocalProvider(LocalNav provides nav, app.reelkin.ui.components.LocalPixelCovers provides s.pixelCovers, LocalBackdrop provides backdrop) {
            Box(Modifier.fillMaxSize()) {
                BackdropLayer()
                Column(Modifier.fillMaxSize()) {
                    Header(cin, onPop = { overlay = Overlay.Shop }, onSettings = { overlay = Overlay.Settings })
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            Tab.BOOTH -> BoothScreen(cin, nav)
                            Tab.BILL -> BillScreen(cin, nav)
                            Tab.SAGA -> app.reelkin.ui.saga.SagaScreen(cin, nav)
                            Tab.PETS -> app.reelkin.ui.pets.PetsScreen(cin, nav)
                        }
                    }
                    Dock(tab, cin.s.seance != null, cin.newCans.size, { tab = it }, { overlay = Overlay.Menu })
                }
                when (val o = overlay) {
                    is Overlay.Film -> cin.title(o.id)?.let { FilmSheet(cin, it, nav) }
                    is Overlay.Series -> cin.title(o.id)?.let { SeriesSheet(cin, it, nav) }
                    is Overlay.Stub -> cin.diary.firstOrNull { it.id == o.id }?.let { StubSheet(cin, it, nav) }
                    is Overlay.Log -> LogSheet(cin, o.s, nav, o.series, o.earlier)
                    is Overlay.Reward -> RewardSheet(cin, o.ticket, o.g, nav)
                    is Overlay.Choose -> ChooseSheet(cin, o.log, nav)
                    Overlay.Pick -> PickSheet(cin, nav)
                    is Overlay.Add -> AddSheet(o.q, nav)
                    is Overlay.NewTitle -> DraftSheet(cin, o.d, o.edit, nav)
                    Overlay.Pet -> PetSheet(cin, nav)
                    Overlay.Pets -> LaunchedEffect(Unit) { tab = Tab.PETS; overlay = null }
                    Overlay.Menu -> MenuSheet(cin, nav)
                    Overlay.Feats -> app.reelkin.ui.saga.FeatsPage(cin, nav)
                    is Overlay.ShareIt -> app.reelkin.ui.cards.ShareSheet(cin, o.what, nav)
                    is Overlay.PetOf -> cin.pets.firstOrNull { it.pet.id == o.id }?.let { app.reelkin.ui.sheets.PetCard(cin, it, nav) }
                    is Overlay.Can -> app.reelkin.ui.pets.CanSheet(cin, o.sp, nav)
                    is Overlay.SagaDetail -> app.reelkin.ui.saga.SagaDetail(cin, o.key, nav)
                    Overlay.Quests -> app.reelkin.ui.goals.QuestsSheet(cin, nav)
                    is Overlay.GoalEdit -> app.reelkin.ui.goals.GoalSheet(cin, o.id, nav)
                    Overlay.Cards -> app.reelkin.ui.cards.CardsPage(cin, nav)
                    Overlay.Shop -> ShopSheet(cin, nav)
                    Overlay.Award -> AwardSheet(cin, nav)
                    Overlay.Settings -> SettingsPage(cin, nav)
                    Overlay.Stats -> LaunchedEffect(Unit) { tab = Tab.SAGA; overlay = null }
                    Overlay.Import -> app.reelkin.ui.settings.ImportPage(cin, nav)
                    null -> Unit
                }
                Toast(nav, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun Header(c: Cinema, onPop: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Reelkin", style = T.Title.copy(color = C.gold), modifier = Modifier.weight(1f))
        Row(Modifier.clip(CircleShape).background(C.surface).clickable(onClick = onPop).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            androidx.compose.foundation.Image(androidx.compose.ui.graphics.ImageBitmap.imageResource(app.reelkin.R.drawable.act_buffet), null, Modifier.size(16.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(C.gold))
            Text("${c.popcorn}", style = T.Strong.copy(color = C.gold))
        }
        val label = t("Настройки", "Settings")
        Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onSettings).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(20.dp)) {
                val r = size.width / 2; val w = 2.dp.toPx()
                for (i in 0 until 8) {
                    val a = i * Math.PI / 4
                    drawLine(C.muted, Offset(r + (r * .55f) * kotlin.math.cos(a).toFloat(), r + (r * .55f) * kotlin.math.sin(a).toFloat()),
                        Offset(r + r * kotlin.math.cos(a).toFloat(), r + r * kotlin.math.sin(a).toFloat()), w * 1.4f)
                }
                drawCircle(C.muted, r * .62f, style = androidx.compose.ui.graphics.drawscope.Stroke(w))
                drawCircle(C.muted, r * .22f, style = androidx.compose.ui.graphics.drawscope.Stroke(w))
            }
        }
    }
}

/** The cinema's sign: brass letters on velvet with a row of bulbs above and below. */
@Composable
fun Marquee(name: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val ru = LocalRu.current
    val text = if (name.isBlank()) (if (ru) "КИНО" else "CINEMA") else if (ru) "КИНО «$name»" else "THE $name"
    Box(modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).background(C.velvetSoft).border(1.dp, C.gold.copy(alpha = .6f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        val frame = app.reelkin.ui.art.rememberTick()
        Canvas(Modifier.matchParentSize()) {
            val step = 9.dp.toPx(); val r = 1.4.dp.toPx()
            var x = step / 2; var i = 0
            while (x < size.width) {
                val on = (i + frame / 4) % 3 != 0
                val col = if (on) C.screen else C.gold.copy(alpha = .35f)
                drawCircle(col, r, Offset(x, 4.dp.toPx())); drawCircle(col, r, Offset(x, size.height - 4.dp.toPx()))
                x += step; i++
            }
        }
        Text(text, style = T.Name.copy(color = C.gold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp))
    }
}


@Composable
private fun Toast(nav: Nav, modifier: Modifier) {
    val (text, stamp) = nav.toast
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(stamp) { if (text.isNotEmpty()) { shown = true; delay(3200); shown = false } }
    if (!shown || text.isEmpty()) return
    Text(text, style = T.Body.copy(color = C.onGold), modifier = modifier.statusBarsPadding().padding(top = 60.dp, start = 16.dp, end = 16.dp).fillMaxWidth()
        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).background(C.gold).padding(horizontal = 16.dp, vertical = 12.dp))
}
