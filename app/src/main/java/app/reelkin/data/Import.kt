package app.reelkin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

/** Where a poster for an imported title can be found: an address, Wikipedia articles, or a name to guess an article from. */
data class CoverHint(val url: String? = null, val ruwiki: String? = null, val enwiki: String? = null, val name: String? = null, val year: Int? = null, val series: Boolean = false)

/**
 * One imported title: a series for the bill (with the episodes of its current season), the stubs
 * already torn, or both. Episodes get their series id when the title is written.
 */
data class Haul(val title: Title?, val stubs: List<Ticket>, val episodes: List<Episode> = emptyList(), val cover: CoverHint? = null)

/** What an import brought: the titles, and the names it couldn't make sense of. */
data class Loot(val hauls: List<Haul>, val missed: List<String> = emptyList()) {
    val stubs get() = hauls.sumOf { it.stubs.size }
    val titles get() = hauls.count { it.title != null }
}

/** The day of a stub: noon of that day, so it lands on the same date in any time zone nearby; never in the future. */
fun noonOf(ms: Long, now: Long): Long = Calendar.getInstance().apply { timeInMillis = ms; set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis.coerceAtMost(now)

/**
 * MyShows: a public profile is read by its login, no password. A series watched to the end tears a
 * stub for every season; one being watched tears its finished seasons and stays on the bill at the
 * episode reached; «will watch» goes on the bill. MyShows doesn't show anyone when an episode was
 * watched, so a season's stub is dated by its last episode's air date.
 */
object MyShows {
    private const val RPC = "https://myshows.me/v3/rpc/"

    sealed class Result {
        data class Ok(val loot: Loot) : Result()
        data object NoSuchUser : Result()
        data object Offline : Result()
    }

    private fun call(method: String, params: JSONObject): JSONObject? {
        val body = JSONObject().put("jsonrpc", "2.0").put("method", method).put("params", params).put("id", 1).toString()
        return Catalog.post(RPC, body, "application/json")?.let { runCatching { JSONObject(it) }.getOrNull() }
    }

    /** The seasons of a show from its episodes: specials left out, each season starting on its first episode's day. */
    fun planOf(episodes: JSONArray?): List<SeasonPlan> {
        episodes ?: return emptyList()
        val by = HashMap<Int, MutableList<Long?>>()
        for (i in 0 until episodes.length()) {
            val e = episodes.optJSONObject(i) ?: continue
            if (e.optBoolean("isSpecial") || e.optInt("seasonNumber") <= 0 || e.optInt("episodeNumber") <= 0) continue
            by.getOrPut(e.optInt("seasonNumber")) { ArrayList() } += Seasons.parseDay(e.optString("airDate").takeIf { !e.isNull("airDate") })
        }
        return by.toSortedMap().map { (n, l) -> SeasonPlan(n, l.size, l.filterNotNull().minOrNull()) }
    }

    /** The seasons of a MyShows show; null when it didn't answer. */
    suspend fun seasons(id: Int): List<SeasonPlan>? = withContext(Dispatchers.IO) {
        call("shows.GetById", JSONObject().put("showId", id).put("withEpisodes", true))?.optJSONObject("result")?.let { planOf(it.optJSONArray("episodes")) }
    }

    /** A MyShows show by its id elsewhere ("imdb", "kinopoisk", "tvmaze"): its id, 0 when there's none, null when it didn't answer. */
    suspend fun byExternal(source: String, id: Int): Int? = withContext(Dispatchers.IO) {
        val r = call("shows.GetByExternalId", JSONObject().put("id", id).put("source", source)) ?: return@withContext null
        r.optJSONObject("result")?.optInt("id")?.takeIf { it > 0 } ?: 0
    }

    /** A MyShows show by its name (Russian or original) and year: its id, 0 when there's none, null when it didn't answer. */
    suspend fun search(title: String, year: Int?): Int? = withContext(Dispatchers.IO) {
        val r = call("shows.Search", JSONObject().put("query", title)) ?: return@withContext null
        val a = r.optJSONArray("result") ?: return@withContext 0
        fun norm(s: String) = s.lowercase().replace('ё', 'е').filter { it.isLetterOrDigit() }
        val want = norm(title)
        (0 until a.length()).map { a.getJSONObject(it) }.firstOrNull { o ->
            (norm(o.optString("title")) == want || norm(o.optString("titleOriginal")) == want) &&
                (year == null || o.optInt("year") == 0 || kotlin.math.abs(o.optInt("year") - year) <= 1)
        }?.optInt("id") ?: 0
    }

    /** A login from what was typed: the login itself or a profile link. */
    fun login(s: String): String = s.trim().trimEnd('/').substringAfterLast("/profile/", s.trim().trimEnd('/')).substringAfterLast('/').removePrefix("@").trim()

    suspend fun load(input: String, ru: Boolean, progress: (Int, Int) -> Unit): Result = withContext(Dispatchers.IO) {
        val login = login(input)
        if (login.isBlank()) return@withContext Result.NoSuchUser
        val r = call("profile.Shows", JSONObject().put("login", login)) ?: return@withContext Result.Offline
        if (r.has("error")) return@withContext if (r.optJSONObject("error")?.optInt("code") == 404) Result.NoSuchUser else Result.Offline
        val list = r.optJSONArray("result") ?: JSONArray()
        val items = (0 until list.length()).map { list.getJSONObject(it) }.filter { it.optString("watchStatus") in WANTED }
        val now = System.currentTimeMillis()
        val gate = Semaphore(4)
        var done = 0
        progress(0, items.size)
        val hauls = coroutineScope {
            items.map { item ->
                async {
                    gate.withPermit {
                        val id = item.optJSONObject("show")?.optInt("id") ?: 0
                        val full = call("shows.GetById", JSONObject().put("showId", id).put("withEpisodes", true))?.optJSONObject("result")
                        synchronized(this@MyShows) { done++; progress(done, items.size) }
                        show(item, full, ru, now)
                    }
                }
            }.awaitAll().filterNotNull()
        }
        val movies = call("profile.WatchedMovies", JSONObject().put("login", login))?.optJSONArray("result")
        Result.Ok(Loot(hauls + movies(movies, ru, now)))
    }

    private val WANTED = setOf("watching", "finished", "later", "cancelled")

    /** One series from a profile entry and, when it came, the show with its episodes. */
    fun show(item: JSONObject, full: JSONObject?, ru: Boolean, now: Long): Haul? {
        val sh = item.optJSONObject("show") ?: return null
        val src = full ?: sh
        val id = sh.optInt("id")
        val name = (if (ru) src.optString("title").ifBlank { src.optString("titleOriginal") } else src.optString("titleOriginal").ifBlank { src.optString("title") }).trim()
        if (name.isEmpty()) return null
        val year = src.optInt("year").takeIf { it > 1880 }
        // MyShows names the country in Russian itself («США»), as the catalogue does; English comes from the code.
        val country = (if (ru) full?.optString("countryTitle")?.trim()?.ifBlank { null } else null)
            ?: full?.optString("country")?.takeIf { it.length == 2 }?.let { Locale("", it).getDisplayCountry(if (ru) Locale("ru") else Locale.ENGLISH) }.orEmpty()
        val aliases = (src.optJSONArray("genres") ?: sh.optJSONArray("genres"))?.let { a -> (0 until a.length()).joinToString(" ") { a.optJSONObject(it)?.optString("alias").orEmpty() } }.orEmpty()
        val genre = if ("anim" in aliases) Genre.ANIMATION else Catalog.guessGenre(aliases)
        val runtime = (full?.optInt("runtime") ?: 0).takeIf { it > 0 } ?: item.optInt("runtime").takeIf { it > 0 } ?: 45
        val stars = item.optInt("rating").coerceIn(0, 5)
        val status = item.optString("watchStatus")
        val ended = src.optString("status").startsWith("Canceled") || src.optString("status").contains("Ended")

        // Episodes by season, specials left out.
        class Ep(val season: Int, val air: Long?)
        val eps = full?.optJSONArray("episodes")?.let { a ->
            (0 until a.length()).mapNotNull { i ->
                val e = a.getJSONObject(i)
                if (e.optBoolean("isSpecial") || e.optInt("seasonNumber") <= 0 || e.optInt("episodeNumber") <= 0) null
                else Ep(e.optInt("seasonNumber"), Seasons.parseDay(e.optString("airDate").takeIf { !e.isNull("airDate") }))
            }
        }.orEmpty()
        val seasons = eps.groupBy { it.season }.toSortedMap()
        val plan = planOf(full?.optJSONArray("episodes"))
        val aired = seasons.filter { (_, l) -> l.any { it.air != null && it.air <= now } }

        val cover = CoverHint(name = src.optString("titleOriginal").ifBlank { name }, year = year, series = true,
            url = sh.optString("image").ifBlank { null }?.takeIf { it.startsWith("https://") })
        fun stub(n: Int, l: List<Ep>) = Ticket(
            title = name + if (ru) " · сезон $n" else " · season $n", season = true, whole = true, year = year, country = country, genre = genre,
            minutes = runtime * l.size, stars = stars, at = noonOf(l.mapNotNull { it.air }.maxOrNull() ?: now, now), ref = "myshows:$id:$n",
        )
        fun title(st: Status, season: Int, episode: Int) = Title(
            kind = Kind.SERIES, title = name, year = year, country = country, genre = genre, minutes = runtime, status = st,
            season = season, episode = episode, perSeason = seasons[season]?.size?.takeIf { it > 0 } ?: plan.lastOrNull()?.episodes?.takeIf { it > 0 } ?: 10,
            plan = Seasons.encode(plan), planAt = now, show = "myshows:$id",
        )

        if (status == "later") return Haul(title(Status.WATCH, 1, 0), emptyList(), cover = cover)
        if (seasons.isEmpty()) {
            // No episode list: only the count is known, so it goes on the bill where the profile says.
            return if (status == "finished") null else Haul(title(if (status == "watching") Status.RUNNING else Status.SHELVED, 1, 0), emptyList(), cover = cover)
        }

        val stubs = ArrayList<Ticket>()
        if (status == "finished") {
            for ((n, l) in aired) stubs += stub(n, l)
            val last = aired.keys.maxOrNull() ?: 0
            // A series that goes on waits in the cupboard for the next season.
            val t = if (ended) null else title(Status.SHELVED, last + 1, 0)
            return Haul(t, stubs, cover = cover)
        }
        var left = item.optInt("watchedEpisodes")
        for ((n, l) in seasons) {
            if (left >= l.size) { stubs += stub(n, l); left -= l.size; continue }
            val st = if (status == "watching") Status.RUNNING else Status.SHELVED
            val t = title(st, n, left)
            val episodes = l.take(left).mapIndexed { i, e -> Episode(series = 0, season = n, number = i + 1, minutes = runtime, at = noonOf(e.air ?: now, now)) }
            return Haul(t, stubs, episodes, cover)
        }
        // Every season so far is watched.
        val last = seasons.keys.max()
        return Haul(if (ended) null else title(Status.SHELVED, last + 1, 0), stubs, cover = cover)
    }

    /** Films watched, if the profile has any: shapes vary, so only the plain fields are read. */
    fun movies(a: JSONArray?, ru: Boolean, now: Long): List<Haul> {
        a ?: return emptyList()
        return (0 until a.length()).mapNotNull { i ->
            val item = a.optJSONObject(i) ?: return@mapNotNull null
            val m = item.optJSONObject("movie") ?: item
            val name = (if (ru) m.optString("title").ifBlank { m.optString("titleOriginal") } else m.optString("titleOriginal").ifBlank { m.optString("title") }).trim()
            if (name.isEmpty()) return@mapNotNull null
            val year = m.optInt("year").takeIf { it > 1880 }
            val aliases = m.optJSONArray("genres")?.let { g -> (0 until g.length()).joinToString(" ") { g.optJSONObject(it)?.optString("alias").orEmpty() } }.orEmpty()
            val at = listOf("watchedAt", "watchDate", "date").firstNotNullOfOrNull { k -> Seasons.parseDay(item.optString(k)) }
            Haul(null, listOf(Ticket(title = name, year = year, genre = if ("anim" in aliases) Genre.ANIMATION else Catalog.guessGenre(aliases),
                minutes = m.optInt("runtime").coerceAtLeast(0), stars = item.optInt("rating").coerceIn(0, 5), at = noonOf(at ?: now, now), ref = "myshows-movie:${m.optInt("id")}")),
                cover = CoverHint(name = m.optString("titleOriginal").ifBlank { name }, year = year))
        }
    }
}

/**
 * Kinopoisk has no export and no open API, and its pages want a real browser: the person opens
 * their ratings in the app's browser and the films are read off each page they open. Only the
 * Kinopoisk ids are trusted; the facts come from Wikidata, which knows most films by that id.
 */
object Kinopoisk {
    /** A film seen on a page: its id, and the rating, date and name if they were next to it. */
    data class Seen(val id: Int, val series: Boolean, val name: String?, val stars: Int?, val at: Long?)

    /** A link to a film's own page (not its reviews or stills): in an href, or written out in a list. */
    private val link = Regex("""(?:href=["'](?:https?://(?:www\.)?kinopoisk\.ru)?|kinopoisk\.ru)/(film|series)/(\d+)/?(?=["'?#\s,;<]|$)""")
    private val tag = Regex("<[^>]+>")
    private val date = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
    private val vote = Regex("""(?:class|data-tid)=["'][^"']*(?:vote|rating|myVote)[^"']*["'][^>]*>\s*(?:<[^>]+>\s*)*(10|[1-9])\s*<""", RegexOption.IGNORE_CASE)

    private val hosts = listOf("kinopoisk.ru", "yandex.ru", "yandex.com", "yandex.net", "ya.ru", "yandex.by", "yandex.kz")

    /** Kinopoisk's own poster for a film by its id; a film without one gets a small GIF placeholder, which [Catalog.picture] turns away. */
    fun poster(id: Int) = "https://st.kp.yandex.net/images/film_iphone/iphone360_$id.jpg"

    /** Pages the in-app browser may open: Kinopoisk and Yandex, where its sign-in and robot check live. */
    fun allowed(host: String?): Boolean = host != null && hosts.any { host == it || host.endsWith(".$it") }

    /** The profile's ratings page for an id or a link to the profile; null when neither is there. */
    fun votesUrl(s: String): String? = Regex("""(\d{3,})""").find(s.substringAfter("/user/", s))?.value?.let { "https://www.kinopoisk.ru/user/$it/votes/" }

    /** The next page of the ratings, or null when [url] isn't one. */
    fun next(url: String): String? {
        if ("/user/" !in url || "/votes" !in url) return null
        Regex("""/page/(\d+)/""").find(url)?.let { m -> return url.replaceRange(m.range, "/page/${m.groupValues[1].toInt() + 1}/") }
        return url.substringBefore("/votes") + "/votes/list/ord/date/page/2/"
    }

    /** Films on a page of HTML, in order, each once. */
    fun parse(html: String): List<Seen> {
        val hits = link.findAll(html).toList()
        if (hits.isEmpty()) return emptyList()
        val out = LinkedHashMap<Int, Seen>()
        // Each film owns the HTML up to the next different film.
        val starts = ArrayList<Pair<Int, MatchResult>>()
        for (m in hits) {
            val id = m.groupValues[2].toInt()
            if (starts.lastOrNull()?.second?.groupValues?.get(2)?.toInt() != id) starts += m.range.first to m
        }
        for ((i, p) in starts.withIndex()) {
            val m = p.second
            val id = m.groupValues[2].toInt()
            val end = starts.getOrNull(i + 1)?.first ?: minOf(html.length, p.first + 4000)
            val chunk = html.substring(p.first, end)
            // The first link is often the poster, with no text: the name is in the first link that has some.
            val texts = sequenceOf(Regex("""^[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL).find(chunk)) +
                Regex("""<a[^>]*/(?:film|series)/$id/?["'?#][^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL).findAll(chunk)
            val name = texts.filterNotNull().map { it.groupValues[1].replace(tag, " ").let(::unescape).replace(Regex("\\s+"), " ").trim() }
                .firstOrNull { it.length in 1..200 && !it.all(Char::isDigit) }
            val at = date.find(chunk)?.let { d ->
                val (dd, mm, yy) = d.destructured
                runCatching { Calendar.getInstance().apply { clear(); set(yy.toInt(), mm.toInt() - 1, dd.toInt(), 12, 0) }.timeInMillis }.getOrNull()
            }
            val stars = vote.find(chunk)?.groupValues?.get(1)?.toIntOrNull()?.let { (it + 1) / 2 }
            val old = out[id]
            out[id] = Seen(id, m.groupValues[1] == "series", old?.name ?: name, old?.stars ?: stars, old?.at ?: at)
        }
        return out.values.toList()
    }

    private fun unescape(s: String) = s.replace("&nbsp;", " ").replace("&laquo;", "«").replace("&raquo;", "»").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&")

    /** Films seen, as stubs: Wikidata fills in what it knows; the rest keep the name from the page. */
    suspend fun loot(seen: Collection<Seen>, ru: Boolean): Loot? {
        val found = Catalog.byKinopoisk(seen.map { it.id }, ru) ?: return null
        val now = System.currentTimeMillis()
        val missed = ArrayList<String>()
        val hauls = seen.mapNotNull { s ->
            val f = found[s.id]
            val d = f?.draft
            val name = d?.title ?: s.name?.replace(Regex("""\s*\(\d{4}\)\s*$"""), "")?.trim()
            if (name.isNullOrBlank()) { missed += "kinopoisk.ru/${if (s.series) "series" else "film"}/${s.id}"; return@mapNotNull null }
            if (d == null) missed += name
            val year = d?.year ?: s.name?.let { Regex("""\((\d{4})""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
            val series = s.series || d?.kind == Kind.SERIES
            Haul(null, listOf(Ticket(
                title = name, season = series, whole = series, year = year, country = d?.country.orEmpty(), genre = d?.genre ?: Genre.DRAMA,
                minutes = if (series) 0 else d?.minutes ?: 0, stars = s.stars ?: 0, at = noonOf(s.at ?: now, now), ref = "kinopoisk:${s.id}",
            )), cover = CoverHint(url = Kinopoisk.poster(s.id), ruwiki = f?.ruwiki, enwiki = f?.enwiki, name = if (f == null) null else name, year = year, series = series))
        }
        return Loot(hauls, missed)
    }
}

/** Gets JSON text out of a WebView's evaluateJavascript result, which comes back quoted. */
fun unquoteJs(s: String?): String = s?.let { runCatching { JSONArray("[$it]").optString(0) }.getOrNull() }.orEmpty()
