package app.reelkin.ui.sheets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.reelkin.container
import app.reelkin.data.Catalog
import app.reelkin.data.Covers
import app.reelkin.data.Draft
import app.reelkin.data.Genre
import app.reelkin.data.Kind as TKind
import app.reelkin.data.Seance
import app.reelkin.data.Seasons
import app.reelkin.data.Status
import app.reelkin.data.Title
import app.reelkin.domain.Cinema
import app.reelkin.domain.Words
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Nav
import app.reelkin.ui.Overlay
import app.reelkin.ui.Tab
import app.reelkin.ui.art.CoverArt
import app.reelkin.ui.art.Posters
import app.reelkin.ui.components.Btn
import app.reelkin.ui.components.BufImage
import app.reelkin.ui.components.PosterBuf
import app.reelkin.ui.components.Chip
import app.reelkin.ui.components.Field
import app.reelkin.ui.components.Flow
import app.reelkin.ui.components.Kind
import app.reelkin.ui.components.Sheet
import app.reelkin.ui.components.t
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Downloaded posters for search results, by address, so the draft doesn't fetch them again. */
private val fetched = HashMap<String, ByteArray>()

private suspend fun fetch(url: String): ByteArray? = fetched[url] ?: Catalog.download(url)?.also { fetched[url] = it }

@Composable
fun AddSheet(q0: String, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf(q0) }
    var results by remember { mutableStateOf<List<Draft>?>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    val arts = remember { mutableStateMapOf<String, CoverArt>() }
    fun go() {
        if (q.isBlank()) return
        busy = true
        scope.launch {
            results = Catalog.search(q, ru)
            busy = false
            results?.forEach { d -> d.coverUrl?.let { u -> launch { fetch(u)?.let { b -> withContext(Dispatchers.Default) { Covers.fromBytes(b) } }?.let { arts[u] = it } } } }
        }
    }
    LaunchedEffect(Unit) { if (q0.isNotBlank()) go() }
    Sheet(nav::close) {
        Text(t("Найти", "Find"), style = T.Title)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(q, { q = it }, t("название фильма или сериала", "a film or series title"), Modifier.weight(1f), onDone = ::go)
            Btn(t("Искать", "Search"), ::go, small = true)
        }
        Text(when {
            busy -> t("ищу в Википедии…", "searching Wikipedia…")
            results == null -> t("Поиск не отвечает. Проверь связь или добавь вручную.", "The search isn't answering. Check the connection or add it by hand.")
            results!!.isEmpty() && q.isNotBlank() && !busy -> t("Ищет по Википедии. Можно добавить и вручную.", "Searches Wikipedia. You can also add it by hand.")
            else -> t("Выбери", "Pick one")
        }, style = T.Muted)
        for (d in results.orEmpty()) Row(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface2).clickable { nav.go(Overlay.NewTitle(d)) }.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PosterBuf(d.title, d.genre, d.coverUrl?.let { arts[it] }, 24, 34, Modifier.clip(R8.Sm), scale = 1.4.dp)
            Column(Modifier.weight(1f)) {
                Text(d.title, style = T.Name)
                Text(listOfNotNull(if (d.kind == TKind.SERIES) t("сериал", "series") else t("фильм", "film"), d.year?.toString(), d.country.ifBlank { null }, d.about.ifBlank { null }).joinToString(" · "), style = T.Muted)
            }
        }
        Btn(t("Добавить вручную", "Add by hand"), { nav.go(Overlay.NewTitle(Draft(TKind.FILM, q.trim()))) }, Modifier.fillMaxWidth(), kind = Kind.GHOST)
        Text(t("Перенести всю историю из MyShows или Кинопоиска", "Bring your whole history from MyShows or Kinopoisk"), style = T.Small.copy(color = C.gold),
            modifier = Modifier.clickable { nav.go(Overlay.Import) }.padding(vertical = 6.dp))
    }
}

/** A title before it goes on the bill, or one being edited ([edit] is its id). */
@Composable
fun DraftSheet(c: Cinema, d: Draft, edit: Long?, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val co = ctx.container
    val old = edit?.let { c.title(it) }
    var title by remember { mutableStateOf(d.title) }
    var year by remember { mutableStateOf(d.year?.toString() ?: "") }
    var minutes by remember { mutableStateOf(d.minutes?.toString() ?: "") }
    var country by remember { mutableStateOf(d.country) }
    var director by remember { mutableStateOf(old?.director?.takeIf { it != "?" } ?: d.director) }
    var cast by remember { mutableStateOf(old?.cast ?: d.cast) }
    var genre by remember { mutableStateOf(d.genre) }
    var kind by remember { mutableStateOf(d.kind) }
    var perSeason by remember { mutableStateOf((d.perSeason ?: 10).toString()) }
    var season by remember { mutableStateOf((old?.season ?: 1).toString()) }
    var cover by remember { mutableStateOf(old?.cover) }
    var bytes by remember { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(d.coverUrl) { if (old == null) d.coverUrl?.let { bytes = fetch(it) } }
    // The seasons from TVmaze: how many are out, how long each is, when the next one starts.
    var plan by remember { mutableStateOf(old?.plan ?: "") }
    var show by remember { mutableStateOf(old?.show) }
    var planAt by remember { mutableStateOf(old?.planAt ?: 0L) }
    LaunchedEffect(kind) {
        if (kind != TKind.SERIES || plan.isNotBlank()) return@LaunchedEffect
        val where = show ?: (if (d.imdb != null || d.tvmaze != null) Seasons.findByIds(d.imdb, d.tvmaze) else Seasons.findByName(title, year.toIntOrNull())) ?: return@LaunchedEffect
        val p = if (where.isEmpty()) emptyList() else Seasons.load(where) ?: return@LaunchedEffect
        show = where; planAt = System.currentTimeMillis(); plan = Seasons.encode(p)
        // A new series starts with its first season's length, when the catalogue knows it.
        if (old == null) p.firstOrNull { it.n == (season.toIntOrNull() ?: 1) }?.episodes?.takeIf { it > 0 }?.let { perSeason = it.toString() }
    }
    val art = remember(cover, bytes) { cover?.let { Covers.art(ctx, it) } ?: bytes?.let { Covers.fromBytes(it) } }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) { cover = Covers.saveUri(ctx, uri); bytes = null } }
    suspend fun saveCover(): String? = cover ?: bytes?.let { b -> withContext(Dispatchers.IO) { Covers.save(ctx, b) } }?.also { cover = it; bytes = null }
    fun build(status: Status): Title {
        val sn = season.toIntOrNull()?.coerceIn(1, 99) ?: 1
        // Another season starts from its first episode.
        val same = old != null && old.season == sn
        return Title(
            id = old?.id ?: 0, kind = kind, title = title.trim().ifBlank { if (ru) "Без названия" else "Untitled" }, year = year.toIntOrNull(), country = country.trim(), genre = genre,
            minutes = minutes.toIntOrNull() ?: if (kind == TKind.SERIES) 45 else 100, status = status, added = old?.added ?: System.currentTimeMillis(),
            season = sn, episode = if (same) old!!.episode else 0, perSeason = perSeason.toIntOrNull()?.coerceIn(1, 99) ?: 10, cliff = same && old!!.cliff,
            plan = plan, planAt = planAt, show = show,
            director = director.trim().ifEmpty { old?.director ?: "" }, cast = cast.trim(),
        )
    }
    Sheet(nav::close) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PosterBuf(title.ifBlank { "?" }, genre, art, 30, 44, Modifier.clip(R8.Md), scale = 3.dp)
                Text(t("сменить афишу", "change poster"), style = T.Small.copy(color = C.gold), modifier = Modifier.clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(6.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(title, { title = it }, t("название", "title"))
                Field(year, { year = it.filter(Char::isDigit).take(4) }, t("год", "year"), number = true)
                Field(minutes, { minutes = it.filter(Char::isDigit).take(3) }, if (kind == TKind.SERIES) t("серия, мин", "episode, min") else t("длина, мин", "length, min"), number = true)
            }
        }
        Field(country, { country = it }, t("страна", "country"))
        Field(director, { director = it.take(80) }, t("режиссёр", "director"))
        Field(cast, { cast = it.take(160) }, t("в ролях, через запятую", "starring, comma-separated"))
        Flow { Chip(t("фильм", "film"), kind == TKind.FILM, { kind = TKind.FILM }); Chip(t("сериал", "series"), kind == TKind.SERIES, { kind = TKind.SERIES }) }
        if (kind == TKind.SERIES) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(t("смотрю сезон", "on season"), style = T.Small)
                Field(season, { v ->
                    season = v.filter(Char::isDigit).take(2)
                    // Another season brings its own length, when the catalogue knows it.
                    season.toIntOrNull()?.let { n -> Seasons.decode(plan).firstOrNull { it.n == n }?.episodes?.takeIf { it > 0 }?.let { perSeason = it.toString() } }
                }, "1", number = true)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(t("серий в сезоне", "episodes per season"), style = T.Small)
                Field(perSeason, { perSeason = it.filter(Char::isDigit).take(2) }, "10", number = true)
            }
        }
        if (kind == TKind.SERIES) {
            val probe = Title(title = title, plan = plan, season = season.toIntOrNull() ?: 1)
            val now = System.currentTimeMillis()
            listOfNotNull(Words.seasonsOut(probe, ru, now), Words.air(probe, ru, now)).takeIf { it.isNotEmpty() }?.let { Text(it.joinToString(" · "), style = T.Small.copy(color = C.gold)) }
            val sn = season.toIntOrNull() ?: 1
            val from = old?.season ?: 1
            // The season field is where you are, not how many there are: say so when it skips seasons.
            if (sn > from) Text(if (ru) "Это сезон, который ты смотришь сейчас. ${if (sn - from == 1) "Сезон $from не отметится" else "Сезоны $from–${sn - 1} не отметятся"} просмотренным${if (sn - from == 1) "" else "и"}" +
                (if (old == null) " — для этого есть «Уже посмотрел сезон»." else ".")
                else "This is the season you're on. ${if (sn - from == 1) "Season $from" else "Seasons $from–${sn - 1}"} won't count as watched" + (if (old == null) ": use «Watched season» for that." else "."), style = T.Muted)
        }
        Text(t("Жанр — от него афиша, зритель в зале и черта питомца", "Genre: it sets the poster, the spectator and the pet's trait").uppercase(), style = T.Eyebrow)
        Flow { for (g in Genre.entries) Chip(Words.genre(g, ru), genre == g, { genre = g }, color = C.genre(g)) }
        if (old != null) Btn(t("Сохранить", "Save"), {
            co.write { val cv = saveCover(); office.update(build(old.status).copy(cover = cv)) }; nav.close()
        }, Modifier.fillMaxWidth())
        else if (kind == TKind.SERIES) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("В афишу", "To the bill"), { co.write { val cv = saveCover(); office.add(build(Status.WATCH).copy(cover = cv)) }; nav.close(); nav.tab(Tab.BILL) }, Modifier.weight(1f), kind = Kind.GHOST)
            Btn(t("Смотрю", "Watching"), {
                co.write { val cv = saveCover(); office.add(build(Status.RUNNING).copy(cover = cv)) }; nav.close(); nav.tab(Tab.BILL)
                nav.toast(if (ru) "Абонемент на «${title.trim()}» выдан." else "Season ticket for ${title.trim()} issued.")
            }, Modifier.weight(1f))
        }
            // A season already watched: its stub now, and the series waits in the cupboard at the next season.
            Btn(t("Уже посмотрел сезон ${season.ifBlank { "1" }}", "Watched season ${season.ifBlank { "1" }}"), {
                co.write {
                    val cv = saveCover(); val tt = build(Status.SHELVED).copy(cover = cv)
                    val stub = Seance(tt.title + if (ru) " · сезон ${tt.season}" else " · season ${tt.season}", tt.minutes * tt.perSeason, tt.genre, tt.year, tt.country, cv, -1, false, System.currentTimeMillis())
                    nav.go(Overlay.Log(stub, tt.copy(season = tt.season + 1, episode = 0, perSeason = Seasons.episodes(tt, tt.season + 1) ?: tt.perSeason)))
                }
            }, Modifier.fillMaxWidth(), kind = Kind.GOLD)
        }
        else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("В афишу", "To the bill"), {
                co.write { val cv = saveCover(); office.add(build(Status.WATCH).copy(cover = cv)) }; nav.close()
                nav.toast(if (ru) "«${title.trim()}» в афише." else "${title.trim()} is on the bill.")
            }, Modifier.weight(1f), kind = Kind.GHOST)
            Btn(t("Уже посмотрел", "Watched it"), {
                co.write { val cv = saveCover(); val tt = build(Status.WATCH); nav.go(Overlay.Log(Seance(tt.title, tt.minutes, tt.genre, tt.year, tt.country, cv, -1, false, System.currentTimeMillis()))) }
            }, Modifier.weight(1f))
        }
        Box(Modifier.size(1.dp))
    }
}
