package app.reelkin.ui.hall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.reelkin.ui.components.Tabs
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.reelkin.domain.Cinema
import app.reelkin.domain.Words
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Marquee
import app.reelkin.ui.Nav
import app.reelkin.ui.Overlay
import app.reelkin.ui.art.Art
import app.reelkin.ui.art.HallArt
import app.reelkin.ui.art.Icons
import app.reelkin.ui.art.PixelCanvas
import app.reelkin.ui.art.Seat
import app.reelkin.ui.art.hash
import app.reelkin.ui.components.Btn
import app.reelkin.ui.components.BufImage
import app.reelkin.ui.components.Card
import app.reelkin.ui.components.Flow
import app.reelkin.ui.components.Kind
import app.reelkin.ui.components.t
import app.reelkin.ui.poster
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T

val RU_ABC = "АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЭЮЯ".toList()
val EN_ABC = ('A'..'Z').toList()

@Composable
/** The hall and its collections, or the awards shelf: pages of the saga. */
fun HallScreen(c: Cinema, nav: Nav, awards: Boolean = false) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val seats = remember(c) { c.seats.map { Seat(it.genre.who, hash(it.title)) } }
    val shown = c.s.seance?.let { poster(ctx, it.title, it.genre, it.cover, 120, 36) } ?: c.seats.lastOrNull()?.let { poster(ctx, it.title, it.genre, it.cover, 120, 36) }
    val running = c.s.seance != null
    val dec = Cinema.DECADES.count { it in c.decades }
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!awards) {
                Box(Modifier.fillMaxWidth().clip(R8.Lg).border(1.dp, C.line, R8.Lg)) {
                    PixelCanvas(HallArt.W, HallArt.H, Modifier.fillMaxWidth().pointerInput(c.seats.size) {
                        detectTapGestures { o ->
                            val i = HallArt.seatAt(o.x * HallArt.W / size.width.toDouble(), o.y * HallArt.H / size.height.toDouble(), c.seats.size)
                            if (i >= 0) nav.go(Overlay.Stub(c.seats[i].id))
                        }
                    }, fine = app.reelkin.ui.art.Buf.FINE) { b, tick -> HallArt.draw(b, seats, shown, -1, running, if (running && !Art.still) tick * Art.FRAME_MS / 1000.0 else 0.0) }
                }
                Text(
                    if (ru) "Зал № ${c.seats.size / 100 + 1} · занято ${c.seats.size % 100} из 100 мест. Тронь зрителя — покажет корешок."
                    else "Hall no. ${c.seats.size / 100 + 1} · ${c.seats.size % 100} of 100 seats taken. Tap a spectator to see the stub.",
                    style = T.Muted,
                )
                Card(Modifier.fillMaxWidth()) {
                    Head(t("После титров", "After the credits"), "${c.scenes.size}")
                    Text(t("Досидел до конца, а там что-то было — пасхалка навсегда в коллекции.", "Sat to the very end and there was something: it stays in the collection."), style = T.Muted)
                    for (s in c.scenes) Text("★  ${s.title}", style = T.Body, modifier = Modifier.padding(top = 6.dp).clickable { nav.go(Overlay.Stub(s.id)) })
                }
            }

            if (!awards) Card(Modifier.fillMaxWidth()) {
                Head(t("Машина времени", "Time machine"), t("$dec из ${Cinema.DECADES.size}", "$dec of ${Cinema.DECADES.size}"))
                val proj = when {
                    dec < 4 -> t("кинопередвижка", "a travelling projector")
                    dec < 7 -> t("«Украина» с ручкой", "a hand-cranked projector")
                    dec < 10 -> t("«Ксенон» с двумя бобинами", "a two-reel xenon")
                    else -> t("цифровой проектор и запасной на плёнке", "a digital projector with a film one in reserve")
                }
                Text(t("Каждое новое десятилетие прибавляет техники в будке. Сейчас там $proj.", "Every new decade adds to the booth. Right now it has $proj."), style = T.Muted)
                Spacer(Modifier.height(10.dp))
                Flow(gap = 5.dp) {
                    for (d in Cinema.DECADES) {
                        val on = d in c.decades
                        Text("$d", style = T.Small.copy(color = if (on) C.onGold else C.faint),
                            modifier = Modifier.clip(CircleShape).background(if (on) C.gold else C.surface2).padding(horizontal = 11.dp, vertical = 6.dp))
                    }
                }
            }

            if (!awards) Card(Modifier.fillMaxWidth()) {
                Head(t("Кругосветка", "Around the world"), if (ru) "${c.countries.size} " + Words.plural(c.countries.size, "страна", "страны", "стран") else "${c.countries.size} countries")
                if (c.countries.isEmpty()) Text(t("Страна каждого фильма ставит штамп.", "Each film's country leaves a stamp."), style = T.Muted)
                Spacer(Modifier.height(6.dp))
                Flow(gap = 10.dp) { for (cn in c.countries) Stamp(cn) }
            }

            if (awards) Card(Modifier.fillMaxWidth()) {
                Head(t("Премия года", "Awards of the year"), t("в декабре", "in December"))
                val shelf = c.s.awards.sortedWith(compareByDescending<app.reelkin.data.Award> { it.year }.thenBy { it.cat })
                if (shelf.isEmpty()) Text(t("Полка пуста. В конце года ты сам выбираешь лучшее из своего года, а питомец вручает статуэтки.",
                    "The shelf is empty. At the end of the year you pick the best of your year, and the pet hands out the statuettes."), style = T.Muted)
                Spacer(Modifier.height(8.dp))
                Flow(gap = 12.dp) {
                    for (a in shelf) Column(Modifier.width(92.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        BufImage(Icons.statue, scale = 2.5.dp)
                        Text("${Words.AWARDS[a.cat].let { if (ru) it.first else it.second }} ${a.year}", style = T.Small.copy(color = C.velvet), textAlign = TextAlign.Center)
                        Text(a.title, style = T.Small.copy(color = C.ink), textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Btn(t("Вручить премию ${c.year}", "Hand out ${c.year} awards"), { nav.go(Overlay.Award) }, small = true, enabled = c.thisYear.isNotEmpty())
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Head(title: String, note: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, style = T.Title, modifier = Modifier.weight(1f))
        Text(note, style = T.Small)
    }
}

@Composable
fun Letter(ch: Char, n: Int, used: Boolean = false, onClick: (() -> Unit)? = null) {
    val on = n > 0
    Box(Modifier.size(34.dp, 38.dp).clip(R8.Sm).background(if (on) C.velvetSoft else C.surface2)
        .border(1.dp, if (on && !used) C.gold.copy(alpha = .7f) else Color.Transparent, R8.Sm)
        .then(if (onClick != null && on) Modifier.clickable(onClick = onClick) else Modifier), contentAlignment = Alignment.Center) {
        Text("$ch", style = T.Name.copy(color = if (!on) C.faint.copy(alpha = .6f) else if (used) C.gold.copy(alpha = .4f) else C.gold))
        if (n > 1) Text("$n", style = T.Small.copy(color = C.muted), modifier = Modifier.align(Alignment.BottomEnd).padding(end = 3.dp))
    }
}

@Composable
fun Stamp(country: String) {
    val cols = listOf(C.velvet, Color(0xFF5A9AD8), Color(0xFF7AB060), Color(0xFFB07AD0), C.gold)
    val h = hash(country)
    val col = cols[(h % cols.size).toInt()]
    Box(Modifier.size(78.dp).rotate(((h % 21) - 10).toFloat()).border(2.dp, col.copy(alpha = .85f), CircleShape).padding(5.dp).border(1.dp, col.copy(alpha = .5f), CircleShape).padding(6.dp),
        contentAlignment = Alignment.Center) {
        // Long names get smaller letters instead of breaking in the middle of a word.
        val word = country.split(' ').maxOf { it.length }
        val size = when { word <= 5 -> T.Eyebrow.fontSize; word <= 7 -> 11.sp; word <= 10 -> 9.sp; else -> 7.5.sp }
        Text(country.uppercase(), style = T.Eyebrow.copy(color = col, fontSize = size, letterSpacing = if (word <= 5) T.Eyebrow.letterSpacing else 0.em),
            textAlign = TextAlign.Center, maxLines = 3)
    }
}
