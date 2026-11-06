package app.reelkin.ui.settings

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.reelkin.container
import app.reelkin.data.Kinopoisk
import app.reelkin.data.Loot
import app.reelkin.data.MyShows
import app.reelkin.data.unquoteJs
import app.reelkin.domain.Cinema
import app.reelkin.domain.Words
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Nav
import app.reelkin.ui.Tab
import app.reelkin.ui.components.Bar
import app.reelkin.ui.components.Btn
import app.reelkin.ui.components.Field
import app.reelkin.ui.components.Kind
import app.reelkin.ui.components.Page
import app.reelkin.ui.components.Section
import app.reelkin.ui.components.Tabs
import app.reelkin.ui.components.t
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bringing a history over from elsewhere: MyShows by its public profile, Kinopoisk through its
 * own pages in a browser here (it has no export). Nothing is written until «Перенести».
 */
@Composable
fun ImportPage(c: Cinema, nav: Nav) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var browsing by remember { mutableStateOf<String?>(null) }
    val seen = remember { mutableStateMapOf<Int, Kinopoisk.Seen>() }
    browsing?.let { url -> KinopoiskBrowser(url, seen, onDone = { browsing = null }); return }
    Page(t("Перенести историю", "Bring your history"), nav::close) {
        Tabs(listOf("MyShows", t("Кинопоиск", "Kinopoisk")), page, { page = it })
        if (page == 0) MyShowsTab(c, nav) else KinopoiskTab(c, nav, seen) { browsing = it }
    }
}

@Composable
private fun MyShowsTab(c: Cinema, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var login by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0 to 0) }
    var note by remember { mutableStateOf("") }
    var loot by remember { mutableStateOf<Loot?>(null) }
    var had by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { had = co.office.importedFrom("myshows") }
    fun go() {
        if (login.isBlank() || busy) return
        busy = true; loot = null; note = ""
        scope.launch {
            when (val r = MyShows.load(login, ru) { d, n -> progress = d to n }) {
                is MyShows.Result.Ok -> { loot = r.loot; if (r.loot.hauls.isEmpty()) note = t(ru, "В профиле нет сериалов. Он точно открытый?", "No series in the profile. Is it public?") }
                MyShows.Result.NoSuchUser -> note = t(ru, "Такого профиля нет. Проверь логин.", "No such profile. Check the login.")
                MyShows.Result.Offline -> note = t(ru, "MyShows не отвечает. Проверь связь и попробуй ещё раз.", "MyShows isn't answering. Check the connection and try again.")
            }
            busy = false
        }
    }
    Text(t("Логин на MyShows или ссылка на профиль. Пароль не нужен, но профиль должен быть открытым.",
        "Your MyShows login or a link to the profile. No password, but the profile has to be public."), style = T.Muted)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Field(login, { login = it }, t("логин", "login"), Modifier.weight(1f), onDone = ::go)
        Btn(t("Найти", "Find"), ::go, small = true, enabled = !busy)
    }
    if (busy) {
        Text(if (progress.second == 0) t("смотрю профиль…", "reading the profile…") else t("сериалы: ${progress.first} из ${progress.second}", "series: ${progress.first} of ${progress.second}"), style = T.Small)
        Bar(if (progress.second == 0) 0f else progress.first.toFloat() / progress.second, Modifier.fillMaxWidth())
    }
    if (note.isNotEmpty()) Text(note, style = T.Body)
    loot?.takeIf { it.hauls.isNotEmpty() }?.let { l ->
        Preview(l, ru)
        Btn(t("Перенести", "Bring it over"), { bring(co, l, nav, ru); loot = null }, Modifier.fillMaxWidth())
    }
    Section(t("Как это переносится", "How it comes over"))
    Text(t("Досмотренный сериал — корешок за каждый сезон. Тот, что смотришь, — корешки за пройденные сезоны, а сам он в «Идут» на нужной серии. «Буду смотреть» — в «Ждут». " +
        "Когда смотрел, MyShows не показывает, поэтому дата корешка — день выхода последней серии сезона. Попкорн за сезон — твоя оценка на MyShows.",
        "A finished series tears a stub for each season. One you're watching tears its finished seasons and goes to Running at the episode you reached. «Will watch» goes to Waiting. " +
        "MyShows doesn't show when you watched, so a stub is dated by the season's last episode. The popcorn is your MyShows rating."), style = T.Muted)
    Undo(co, "myshows", had, ru) { had = 0 }
}

@Composable
private fun KinopoiskTab(c: Cinema, nav: Nav, seen: MutableMap<Int, Kinopoisk.Seen>, onBrowse: (String) -> Unit) {
    val ru = LocalRu.current
    val ctx = LocalContext.current
    val co = ctx.container
    val scope = rememberCoroutineScope()
    var profile by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var loot by remember { mutableStateOf<Loot?>(null) }
    var had by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { had = co.office.importedFrom("kinopoisk") }
    val file = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = withContext(Dispatchers.IO) { runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull() }.orEmpty()
            val found = withContext(Dispatchers.Default) { Kinopoisk.parse(text) }
            found.forEach { s -> seen.merge(s.id, s) { a, b -> a.copy(name = a.name ?: b.name, stars = a.stars ?: b.stars, at = a.at ?: b.at) } }
            note = if (found.isEmpty()) t(ru, "В файле нет ссылок на фильмы Кинопоиска.", "No Kinopoisk film links in the file.") else ""
        }
    }
    Text(t("У Кинопоиска нет выгрузки, поэтому оценки читаются прямо с его страниц. Открой свой профиль здесь и листай оценки: фильмы собираются с каждой открытой страницы. " +
        "Если профиль закрыт или Кинопоиск спросит, не робот ли ты, — войди или ответь там же.",
        "Kinopoisk has no export, so the ratings are read off its pages. Open your profile here and go through your ratings: films are picked up from every page you open. " +
        "If the profile is private or Kinopoisk asks whether you're a robot, sign in or answer right there."), style = T.Muted)
    Field(profile, { profile = it }, t("ссылка на профиль или его номер", "profile link or its number"))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Btn(t("Открыть оценки", "Open ratings"), { onBrowse(Kinopoisk.votesUrl(profile) ?: "https://www.kinopoisk.ru/") }, Modifier.weight(1f))
        Btn(t("Из файла", "From a file"), { file.launch(arrayOf("text/*", "application/octet-stream")) }, kind = Kind.GHOST)
    }
    Text(t("Файл — сохранённая страница оценок или любой список ссылок на фильмы Кинопоиска.", "A file: a saved ratings page, or any list of Kinopoisk film links."), style = T.Small)
    if (seen.isNotEmpty()) {
        Text(t("Собрано: ${Words.films(seen.size, true)}", "Picked up: ${Words.films(seen.size, false)}"), style = T.Strong)
        Text(seen.values.mapNotNull { it.name }.take(6).joinToString(", ") + if (seen.size > 6) "…" else "", style = T.Small)
        if (loot == null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(if (busy) t("сверяю с Википедией…", "checking Wikipedia…") else t("Дальше", "Next"), {
                if (busy) return@Btn
                busy = true; note = ""
                scope.launch {
                    val l = Kinopoisk.loot(seen.values.toList(), ru)
                    if (l == null) note = t(ru, "Википедия не отвечает. Проверь связь и попробуй ещё раз.", "Wikipedia isn't answering. Check the connection and try again.") else loot = l
                    busy = false
                }
            }, Modifier.weight(1f), enabled = !busy)
            Btn(t("Сбросить", "Clear"), { seen.clear(); loot = null }, kind = Kind.GHOST, enabled = !busy)
        }
    }
    if (note.isNotEmpty()) Text(note, style = T.Body)
    loot?.let { l ->
        Preview(l, ru)
        if (l.missed.isNotEmpty()) Text(t("Википедия не знает ${l.missed.size}: они перенесутся только с названием — жанр и страну можно поправить в корешке.",
            "Wikipedia doesn't know ${l.missed.size}: they come over with just the name; the genre and country can be fixed on the stub."), style = T.Small)
        Btn(t("Перенести", "Bring it over"), { bring(co, l, nav, ru); loot = null; seen.clear() }, Modifier.fillMaxWidth())
    }
    Undo(co, "kinopoisk", had, ru) { had = 0 }
}

@Composable
private fun Preview(l: Loot, ru: Boolean) {
    val films = l.hauls.sumOf { h -> h.stubs.count { !it.season } }
    val seasons = l.hauls.sumOf { h -> h.stubs.count { it.season } }
    Column(Modifier.fillMaxWidth().background(C.surface2).padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(t("Перенесётся", "Coming over").uppercase(), style = T.Eyebrow)
        if (films > 0) Text("+  " + Words.films(films, ru), style = T.Body)
        if (seasons > 0) Text("+  " + if (ru) "$seasons ${Words.plural(seasons, "корешок", "корешка", "корешков")} сезонов" else "$seasons season stub${if (seasons == 1) "" else "s"}", style = T.Body)
        if (l.titles > 0) Text("+  " + if (ru) "${l.titles} ${Words.plural(l.titles, "сериал", "сериала", "сериалов")} в афишу" else "${l.titles} series on the bill", style = T.Body)
        Text(t("Что уже есть, второй раз не добавится.", "What's already here won't be added twice."), style = T.Small)
    }
}

/** Writes the import, then fetches the posters in the background. */
private fun bring(co: app.reelkin.AppContainer, l: Loot, nav: Nav, ru: Boolean) {
    co.write {
        val done = office.import(l)
        nav.close(); nav.tab(Tab.BILL)
        nav.toast(if (ru) "Перенесено: ${done.stubs} ${Words.plural(done.stubs, "корешок", "корешка", "корешков")}" + (if (done.titles > 0) ", ${done.titles} в афишу" else "") + ". Обложки подтянутся следом."
            else "Brought over: ${done.stubs} stubs" + (if (done.titles > 0) ", ${done.titles} on the bill" else "") + ". The posters follow.")
        office.fillCovers(done.covers)
    }
}

@Composable
private fun Undo(co: app.reelkin.AppContainer, source: String, had: Int, ru: Boolean, onDone: () -> Unit) {
    if (had <= 0) return
    var sure by remember { mutableStateOf(false) }
    Section(t("Передумал", "Changed your mind"))
    Btn(if (sure) t("Точно убрать $had?", "Remove $had for sure?") else if (ru) "Убрать перенесённые корешки ($had)" else "Remove the stubs brought over ($had)", {
        if (!sure) sure = true else { co.write { office.undoImport(source) }; onDone() }
    }, Modifier.fillMaxWidth(), kind = Kind.GHOST, small = true)
}

private fun t(ru: Boolean, a: String, b: String) = if (ru) a else b

/** Kinopoisk in a browser: every page that loads is read for films, and «Следующая» goes through the ratings page by page. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun KinopoiskBrowser(start: String, seen: MutableMap<Int, Kinopoisk.Seen>, onDone: () -> Unit) {
    BackHandler(onBack = onDone)
    var web by remember { mutableStateOf<WebView?>(null) }
    var url by remember { mutableStateOf(start) }
    var last by remember { mutableIntStateOf(0) }
    fun grab(w: WebView) = w.evaluateJavascript("document.documentElement.outerHTML") { html ->
        val found = Kinopoisk.parse(unquoteJs(html))
        last = found.size
        found.forEach { s -> seen.merge(s.id, s) { a, b -> a.copy(name = a.name ?: b.name, stars = a.stars ?: b.stars, at = a.at ?: b.at) } }
    }
    // Pages that fill themselves in after loading are read again every couple of seconds.
    LaunchedEffect(web) { val w = web ?: return@LaunchedEffect; while (true) { delay(2_000); grab(w) } }
    Column(Modifier.fillMaxSize().background(C.surface)) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(t("Собрано: ${Words.films(seen.size, true)}", "Picked up: ${Words.films(seen.size, false)}") + if (last > 0) t(" · на странице $last", " · $last on this page") else "", style = T.Strong)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val next = Kinopoisk.next(url)
                Btn(t("Следующая страница", "Next page"), { next?.let { n -> web?.loadUrl(n) } }, Modifier.weight(1f), kind = Kind.SOFT, small = true, enabled = next != null)
                Btn(t("Готово", "Done"), onDone, small = true)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()) {
            AndroidView(factory = { ctx ->
                CookieManager.getInstance().setAcceptCookie(true)
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    // The desktop pages keep the long ratings list; the mobile site hides it in the app.
                    settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, u: String) { url = u; grab(view) }
                        // Only Kinopoisk and the Yandex sign-in it sends you to: this is not a browser for the whole web.
                        override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean =
                            !Kinopoisk.allowed(request.url.host)
                    }
                    loadUrl(start)
                    web = this
                }
            }, modifier = Modifier.fillMaxSize(), onRelease = { it.stopLoading(); it.destroy() })
        }
    }
}
