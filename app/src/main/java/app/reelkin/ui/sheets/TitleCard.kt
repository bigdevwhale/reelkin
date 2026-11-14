package app.reelkin.ui.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelkin.data.Covers
import app.reelkin.data.Genre
import app.reelkin.data.Genres
import app.reelkin.data.People
import app.reelkin.data.Subgenres
import app.reelkin.domain.Words
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Nav
import app.reelkin.ui.components.Chip
import app.reelkin.ui.components.Field
import app.reelkin.ui.components.Flow
import app.reelkin.ui.components.PosterImage
import app.reelkin.ui.components.t
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The top of a film's card: its poster, wide and dim, behind the poster itself and the name, with what's known as soft
 * pills. [eyebrow] goes above the name, [under] below the pills.
 */
@Composable
fun Hero(title: String, genre: Genre, cover: String?, year: Int?, country: String, minutes: Int, eyebrow: String? = null, sub: String = "", also: String = "",
         under: @Composable ColumnScope.() -> Unit = {}) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val full by produceState(Covers.peek(cover)?.takeIf { it.ready }?.full, cover) {
        if (value == null && cover != null) value = withContext(Dispatchers.IO) { Covers.art(ctx, cover)?.full }
    }
    val tint = Color(0xFF000000 or genre.color.toLong())
    Box(Modifier.fillMaxWidth().clip(R8.Lg).background(tint.copy(alpha = .35f))) {
        full?.let { b ->
            val img = remember(b) { b.asImageBitmap() }
            Image(img, null, Modifier.matchParentSize().alpha(.38f), contentScale = ContentScale.Crop, filterQuality = FilterQuality.Low)
        }
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, C.surface.copy(alpha = .85f), C.surface))))
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
            PosterImage(title, genre, cover, 30, 44, Modifier.clip(R8.Md).border(1.dp, Color.White.copy(alpha = .12f), R8.Md), scale = 3.2.dp)
            Column(Modifier.weight(1f).padding(top = 30.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                eyebrow?.let { Text(it.uppercase(), style = T.Eyebrow.copy(color = C.velvet)) }
                Text(title, style = T.Title, maxLines = 4, overflow = TextOverflow.Ellipsis)
                Flow(gap = 5.dp) {
                    Pill(Words.genre(genre, ru), tint)
                    for (g in Genres.parse(also)) if (g != genre) Pill(Words.genre(g, ru), Color(0xFF000000 or g.color.toLong()).copy(alpha = .8f))
                    for (n in Subgenres.names(sub, ru)) Pill(n, tint.copy(alpha = .55f))
                    year?.let { Pill("$it") }
                    if (country.isNotBlank()) Pill(country)
                    if (minutes > 0) Pill(app.reelkin.ui.bill.dur(minutes.toLong(), ru))
                }
                under()
            }
        }
    }
}

/** Genres besides the main one: a pet that loves one of them gets a quarter more instead of half. */
@Composable
fun AlsoPicker(genre: Genre, also: String, onChange: (String) -> Unit) {
    val ru = LocalRu.current
    val picked = Genres.parse(also)
    Text(t("Ещё жанры", "More genres").uppercase(), style = T.Eyebrow)
    Text(t("Питомец, который любит один из них, наестся на четверть больше — за главный жанр в полтора раза.",
        "A pet that loves one of these eats a quarter more; the main genre gives half again."), style = T.Small)
    Flow { for (g in Genre.entries) if (g != genre) Chip(Words.genre(g, ru), g in picked, { onChange(Genres.toggle(also, genre, g)) }, color = C.genre(g)) }
}

/**
 * Subgenres to tick under [genre], and one's own to type in. Those ticked that belong elsewhere (another genre's, or typed)
 * stay in front, so nothing ticked is hidden.
 */
@Composable
fun SubPicker(genre: Genre, sub: String, onChange: (String) -> Unit) {
    val ru = LocalRu.current
    var own by remember { mutableStateOf("") }
    val tint = Color(0xFF000000 or genre.color.toLong())
    val offered = Subgenres.of(genre).map { it.key }
    val picked = Subgenres.split(sub)
    Text(t("Поджанры", "Subgenres").uppercase(), style = T.Eyebrow)
    Flow {
        for (k in picked.filter { it !in offered }) Chip(Subgenres.name(k, ru) + " ×", true, { onChange(Subgenres.toggle(sub, k)) }, color = tint)
        for (k in offered) Chip(Subgenres.name(k, ru), k in picked, { onChange(Subgenres.toggle(sub, k)) }, color = tint)
    }
    fun add() { if (own.isNotBlank()) { onChange(Subgenres.toggle(sub, own.trim().lowercase())); own = "" } }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Field(own, { own = it.replace(",", "").take(30) }, t("свой поджанр", "your own subgenre"), Modifier.weight(1f), onDone = ::add)
        app.reelkin.ui.components.Btn(t("Добавить", "Add"), ::add, small = true, kind = app.reelkin.ui.components.Kind.GHOST, enabled = own.isNotBlank())
    }
}

@Composable
private fun Pill(text: String, color: Color? = null) {
    Text(text, style = T.Small.copy(color = if (color != null) Color.White else C.ink), maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(color ?: Color.White.copy(alpha = .08f)).padding(horizontal = 10.dp, vertical = 4.dp))
}

/**
 * Who made it and who's in it, as names to tap: a tap shows their films on the bill. Unknown, they're looked up in the
 * catalogue once ([lookup] saves what it finds).
 */
@Composable
fun PeopleBlock(director: String, cast: String, nav: Nav, lookup: (suspend () -> Unit)?) {
    val ru = LocalRu.current
    var looking by remember { mutableStateOf(false) }
    LaunchedEffect(director) { if (director.isEmpty() && lookup != null) { looking = true; runCatching { lookup() }; looking = false } }
    val ds = People.of(director); val cs = People.of(cast)
    if (ds.isEmpty() && cs.isEmpty()) {
        if (looking) Text(t("Ищу режиссёра и актёров в каталоге…", "Looking up the director and the cast…"), style = T.Small)
        return
    }
    Column(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface2).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (ds.isNotEmpty()) {
            Text((if (ds.size > 1) t("Режиссёры", "Directors") else t("Режиссёр", "Director")).uppercase(), style = T.Eyebrow)
            Flow { for (p in ds) Person(p, true) { nav.person(p) } }
        }
        if (cs.isNotEmpty()) {
            Text(t("В ролях", "Starring").uppercase(), style = T.Eyebrow)
            Flow { for (p in cs) Person(p, false) { nav.person(p) } }
        }
        Text(t("Нажми на имя — покажу всё с ним в афише и корешках.", "Tap a name to see everything with them on the bill and the stubs."), style = T.Small.copy(color = C.faint))
    }
}

@Composable
private fun Person(name: String, director: Boolean, onClick: () -> Unit) {
    Row(Modifier.clip(CircleShape).background(C.surface).border(1.dp, if (director) C.gold.copy(alpha = .5f) else C.line, CircleShape).clickable(onClick = onClick)
        .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Initials in a little circle, as a face.
        val initials = name.split(' ').filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
        Box(Modifier.heightIn(min = 28.dp).clip(CircleShape).background(if (director) C.goldSoft else C.surface2).padding(horizontal = 7.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
            Text(initials, style = T.Small.copy(color = if (director) C.gold else C.muted))
        }
        Text(name, style = T.Strong, maxLines = 1)
    }
}
