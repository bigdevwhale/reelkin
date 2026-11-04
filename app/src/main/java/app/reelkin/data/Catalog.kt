package app.reelkin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A search result before it goes on the bill. */
data class Draft(
    val kind: Kind,
    val title: String,
    val year: Int? = null,
    val country: String = "",
    val genre: Genre = Genre.DRAMA,
    val minutes: Int? = null,
    val perSeason: Int? = null,
    val about: String = "",
    val coverUrl: String? = null,
)

/**
 * Looking a film or a series up, so it comes with its poster, year, country and length. Free and
 * keyless: Wikidata for the facts, the Wikipedia article's top picture for the poster. This is the
 * only thing that goes online, and only when someone searches.
 */
object Catalog {
    private const val AGENT = "Reelkin/1.0 (https://github.com/bigdevwhale/reelkin)"
    private const val WD = "https://www.wikidata.org/w/api.php?format=json&"

    private val films = setOf("Q11424", "Q202866", "Q24869", "Q506240", "Q93204", "Q17517379", "Q226730", "Q20650540", "Q24862")
    private val series = setOf("Q5398426", "Q581714", "Q1259759", "Q526877", "Q117467246", "Q63952888", "Q21191270")

    private fun get(url: String): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", AGENT)
        c.connectTimeout = 8_000
        c.readTimeout = 10_000
        try { if (c.responseCode in 200..299) c.inputStream.bufferedReader().readText() else null } finally { c.disconnect() }
    } catch (_: Exception) { null }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** The genre from English genre names and the kind of film; drama when nothing fits. */
    fun guessGenre(s: String): Genre {
        val t = s.lowercase()
        return when {
            "horror" in t || "slasher" in t -> Genre.HORROR
            "noir" in t || "neo-noir" in t || "detective" in t || "crime" in t -> Genre.NOIR
            "science fiction" in t || "sci-fi" in t || "cyberpunk" in t || "space" in t -> Genre.SCIFI
            "anim" in t || "anime" in t || "cartoon" in t -> Genre.ANIMATION
            "musical" in t || "music" in t -> Genre.MUSICAL
            "western" in t -> Genre.WESTERN
            "reality" in t || "game show" in t || "talent show" in t || "dating show" in t -> Genre.REALITY
            "documentary" in t -> Genre.DOC
            "comedy" in t || "sitcom" in t || "parody" in t -> Genre.COMEDY
            "romance" in t || "romantic" in t || "melodrama" in t -> Genre.ROMANCE
            "adventure" in t || "action" in t || "fantasy" in t || "war" in t -> Genre.ADVENTURE
            else -> Genre.DRAMA
        }
    }

    /** Null when the network failed; an empty list when nothing matched. */
    suspend fun search(q: String, ru: Boolean): List<Draft>? = withContext(Dispatchers.IO) {
        val query = q.trim()
        if (query.length < 2) return@withContext emptyList()
        val lang = if (Regex("[а-яё]", RegexOption.IGNORE_CASE).containsMatchIn(query) || ru) "ru" else "en"
        val s = get(WD + "action=wbsearchentities&search=${enc(query)}&language=$lang&uselang=$lang&type=item&limit=20") ?: return@withContext null
        val ids = JSONObject(s).optJSONArray("search")?.let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("id") } }.orEmpty()
        if (ids.isEmpty()) return@withContext emptyList()
        val ents = get(WD + "action=wbgetentities&ids=${ids.joinToString("|")}&props=claims|labels|sitelinks&languages=ru|en|mul&sitefilter=enwiki|ruwiki")
            ?.let { JSONObject(it).optJSONObject("entities") } ?: return@withContext null

        class Raw(val kind: Kind, val label: String, val year: Int?, val countries: List<String>, val genres: List<String>, val types: List<String>,
                  val minutes: Int?, val eps: Int?, val seasons: Int?, val en: String?, val ruw: String?)
        val raws = ArrayList<Raw>()
        for (id in ids) {
            val e = ents.optJSONObject(id) ?: continue
            val c = e.optJSONObject("claims") ?: continue
            val p31 = ids(c, "P31")
            val kind = when { p31.any { it in series } -> Kind.SERIES; p31.any { it in films } -> Kind.FILM; else -> continue }
            val labels = e.optJSONObject("labels")
            val label = listOf(lang, "mul", "en", "ru").firstNotNullOfOrNull { labels?.optJSONObject(it)?.optString("value")?.ifBlank { null } } ?: continue
            val time = (c.optJSONArray("P580") ?: c.optJSONArray("P577"))?.let { first(it)?.optString("time") }
            val dur = c.optJSONArray("P2047")?.let { first(it) }?.let { v ->
                val a = v.optString("amount").toDoubleOrNull() ?: return@let null
                val unit = v.optString("unit")
                when { unit.endsWith("Q11574") -> a / 60; unit.endsWith("Q25235") -> a * 60; else -> a }.toInt().takeIf { it > 0 }
            }
            val links = e.optJSONObject("sitelinks")
            raws += Raw(kind, label, time?.let { Regex("""(\d{4})""").find(it)?.value?.toIntOrNull() }, ids(c, "P495").take(1), ids(c, "P136").take(6), p31,
                dur, amount(c, "P1113"), amount(c, "P2437"), links?.optJSONObject("enwiki")?.optString("title"), links?.optJSONObject("ruwiki")?.optString("title"))
            if (raws.size >= 8) break
        }
        val more = raws.flatMap { it.countries + it.genres }.distinct().take(50)
        val names = HashMap<String, String>()
        val en = HashMap<String, String>()
        if (more.isNotEmpty()) get(WD + "action=wbgetentities&ids=${more.joinToString("|")}&props=labels&languages=$lang|en|mul")
            ?.let { JSONObject(it).optJSONObject("entities") }?.let { es ->
                for (id in more) {
                    val l = es.optJSONObject(id)?.optJSONObject("labels") ?: continue
                    listOf(lang, "mul", "en").firstNotNullOfOrNull { l.optJSONObject(it)?.optString("value") }?.let { names[id] = it }
                    listOf("en", "mul", lang).firstNotNullOfOrNull { l.optJSONObject(it)?.optString("value") }?.let { en[id] = it }
                }
            }
        coroutineScope {
            raws.map { r ->
                async {
                    val g = when {
                        "Q202866" in r.types || "Q20650540" in r.types || "Q581714" in r.types || "Q63952888" in r.types -> Genre.ANIMATION
                        "Q93204" in r.types -> Genre.DOC
                        else -> guessGenre(r.genres.mapNotNull { en[it] }.joinToString(" "))
                    }
                    Draft(
                        kind = r.kind, title = r.label, year = r.year, country = r.countries.mapNotNull { names[it] }.firstOrNull() ?: "", genre = g,
                        minutes = r.minutes, perSeason = if (r.eps != null && r.seasons != null && r.seasons > 0) (r.eps / r.seasons).coerceAtLeast(1) else null,
                        about = r.genres.mapNotNull { names[it] }.take(2).joinToString(", "),
                        coverUrl = poster(r.ruw, "ru") ?: poster(r.en, "en"),
                    )
                }
            }.awaitAll()
        }
    }

    /** A poster address for a known title, trying the usual Wikipedia article names. */
    fun posterFor(title: String, year: Int?, series: Boolean): String? {
        val kind = if (series) "TV series" else "film"
        return listOfNotNull(year?.let { "$title ($it $kind)" }, "$title ($kind)", title).firstNotNullOfOrNull { poster(it, "en") }
    }

    private fun poster(title: String?, lang: String): String? {
        title ?: return null
        val body = get("https://$lang.wikipedia.org/api/rest_v1/page/summary/${enc(title.replace(' ', '_')).replace("+", "%20")}") ?: return null
        return JSONObject(body).optJSONObject("thumbnail")?.optString("source")?.ifBlank { null }
    }

    private fun first(a: JSONArray): JSONObject? = a.optJSONObject(0)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.optJSONObject("value")

    private fun ids(c: JSONObject, p: String): List<String> {
        val a = c.optJSONArray(p) ?: return emptyList()
        return (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.optJSONObject("value")?.optString("id")?.ifBlank { null } }
    }

    private fun amount(c: JSONObject, p: String): Int? = c.optJSONArray(p)?.let { first(it) }?.optString("amount")?.toDoubleOrNull()?.toInt()

    /** Downloads a picture's bytes, or null. */
    suspend fun download(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.setRequestProperty("User-Agent", AGENT)
            c.connectTimeout = 8_000
            c.readTimeout = 15_000
            try { if (c.responseCode in 200..299) c.inputStream.readBytes() else null } finally { c.disconnect() }
        } catch (_: Exception) { null }
    }
}
