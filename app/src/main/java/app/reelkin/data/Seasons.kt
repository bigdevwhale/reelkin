package app.reelkin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** One season as the catalogue knows it: how many episodes and the day it starts, if that's known. */
data class SeasonPlan(val n: Int, val episodes: Int, val premiere: Long?)

/** Where a season stands on a given day. */
sealed class Air {
    /** Out, or at least started. */
    data object Out : Air()
    /** Comes out on [at]. */
    data class On(val at: Long) : Air()
    /** Announced, no day yet. */
    data object Later : Air()
    /** Past the last season anyone has announced. */
    data object None : Air()
    /** Nothing is known about the seasons. */
    data object Unknown : Air()
}

/**
 * The seasons of a series: how many are out, how long each is and when the next one starts. They
 * come from TVmaze (free, keyless), found through the show's Wikidata entry, and are kept on the
 * title as a short line "season:episodes:day;…" with the day counted from 1970.
 */
object Seasons {
    private const val TVMAZE = "https://api.tvmaze.com"

    /** How long a plan is trusted before it's asked for again. */
    const val FRESH = 2 * DAY

    fun encode(l: List<SeasonPlan>): String = l.sortedBy { it.n }.joinToString(";") { "${it.n}:${it.episodes}:${it.premiere?.let { p -> Math.floorDiv(p, DAY) } ?: ""}" }

    fun decode(s: String): List<SeasonPlan> = if (s.isBlank()) emptyList() else s.split(';').mapNotNull { p ->
        val a = p.split(':')
        val n = a.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
        SeasonPlan(n, a.getOrNull(1)?.toIntOrNull() ?: 0, a.getOrNull(2)?.toLongOrNull()?.times(DAY))
    }

    fun air(plan: List<SeasonPlan>, n: Int, now: Long): Air {
        if (plan.isEmpty()) return Air.Unknown
        val p = plan.firstOrNull { it.n == n } ?: return if (n > plan.maxOf { it.n }) Air.None else Air.Out
        val at = p.premiere ?: return Air.Later
        // A season counts as out from its first day; the plan keeps days, so the whole day counts.
        return if (at <= now) Air.Out else Air.On(at)
    }

    fun air(t: Title, n: Int = t.season, now: Long = System.currentTimeMillis()) = air(decode(t.plan), n, now)

    /** Seasons out by [now]: 0 when nothing is known. */
    fun out(plan: List<SeasonPlan>, now: Long): Int = plan.count { it.premiere != null && it.premiere <= now }

    /** Episodes in season [n] by the plan, or null. */
    fun episodes(t: Title, n: Int): Int? = decode(t.plan).firstOrNull { it.n == n }?.episodes?.takeIf { it > 0 }

    private val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

    /** "2025-02-16" or "2025-02-16T…" as the start of that day in UTC. */
    fun parseDay(s: String?): Long? = s?.takeIf { it.length >= 10 }?.let { runCatching { synchronized(day) { day.parse(it.take(10)) }?.time }.getOrNull() }

    /** The seasons from TVmaze; null when it didn't answer. */
    suspend fun fetch(tvmaze: Int): List<SeasonPlan>? = withContext(Dispatchers.IO) {
        val body = Catalog.get("$TVMAZE/shows/$tvmaze/seasons") ?: return@withContext null
        runCatching {
            val a = JSONArray(body)
            (0 until a.length()).mapNotNull { i ->
                val o = a.getJSONObject(i)
                val n = o.optInt("number", 0).takeIf { it > 0 } ?: return@mapNotNull null
                SeasonPlan(n, o.optInt("episodeOrder", 0), parseDay(o.optString("premiereDate").takeIf { o.has("premiereDate") && !o.isNull("premiereDate") }))
            }
        }.getOrNull()
    }

    /** The TVmaze show for an IMDb id, or null. */
    suspend fun byImdb(imdb: String): Int? = withContext(Dispatchers.IO) {
        Catalog.get("$TVMAZE/lookup/shows?imdb=${imdb.trim()}")?.let { runCatching { JSONObject(it).optInt("id").takeIf { id -> id > 0 } }.getOrNull() }
    }

    /** The TVmaze show by its name, when the year matches; English names only find anything. */
    suspend fun byName(title: String, year: Int?): Int? = withContext(Dispatchers.IO) {
        val body = Catalog.get("$TVMAZE/singlesearch/shows?q=${java.net.URLEncoder.encode(title, "UTF-8")}") ?: return@withContext null
        runCatching {
            val o = JSONObject(body)
            val y = o.optString("premiered").take(4).toIntOrNull()
            o.optInt("id").takeIf { it > 0 && (year == null || y == null || kotlin.math.abs(y - year) <= 1) }
        }.getOrNull()
    }

    /** The seasons from wherever [show] says: MyShows or TVmaze. Null when it didn't answer. */
    suspend fun load(show: String): List<SeasonPlan>? {
        val id = show.substringAfter(':').toIntOrNull() ?: return emptyList()
        return when (show.substringBefore(':')) {
            "myshows" -> MyShows.seasons(id)
            "tvmaze" -> fetch(id)
            else -> emptyList()
        }
    }

    /**
     * Where a series' seasons can be found, from the ids the catalogue gave. MyShows comes first: it
     * answers from anywhere, while TVmaze can't be reached from everywhere. Empty when there's
     * nowhere; null when nothing answered.
     */
    suspend fun findByIds(imdb: String?, tvmaze: Int?): String? {
        var failed = false
        imdb?.removePrefix("tt")?.toIntOrNull()?.let { id ->
            when (val m = MyShows.byExternal("imdb", id)) { null -> failed = true; 0 -> Unit; else -> return "myshows:$m" }
        }
        tvmaze?.takeIf { it > 0 }?.let { id ->
            when (val m = MyShows.byExternal("tvmaze", id)) { null -> failed = true; 0 -> Unit; else -> return "myshows:$m" }
            return "tvmaze:$id"
        }
        return if (failed) null else ""
    }

    /** The same, for a series known only by its name and year. */
    suspend fun findByName(title: String, year: Int?): String? {
        val q = title.substringBefore(" · ").trim()
        val m = MyShows.search(q, year)
        if (m != null && m > 0) return "myshows:$m"
        val tv = Catalog.tvmazeFor(q, year)
        if (tv != null && tv > 0) return "tvmaze:$tv"
        return if (m == null && tv == null) null else ""
    }

    /**
     * Asks for the seasons of [t] again, finding where they are first if that isn't known. Returns
     * (source, plan), with an empty source when there's none, or null when the network failed.
     */
    suspend fun refresh(t: Title): Pair<String, String>? {
        val show = t.show ?: findByName(t.title, t.year) ?: return null
        if (show.isEmpty()) return "" to t.plan
        val plan = load(show) ?: return null
        return show to encode(plan).ifBlank { t.plan }
    }
}
