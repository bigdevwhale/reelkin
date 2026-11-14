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
    /** The show on TVmaze, for the seasons. */
    val tvmaze: Int? = null,
    val imdb: String? = null,
    val director: String = "",
    val cast: String = "",
    /** Subgenres, as on a title. */
    val sub: String = "",
    /** Genres besides [genre], as on a title. */
    val also: String = "",
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

    internal fun get(url: String): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", AGENT)
        c.connectTimeout = 8_000
        c.readTimeout = 10_000
        try { if (c.responseCode in 200..299) c.inputStream.bufferedReader().readText() else null } finally { c.disconnect() }
    } catch (_: Exception) { null }

    /** A POST with a [type] body, for the Wikidata query service and MyShows. */
    internal fun post(url: String, body: String, type: String, accept: String = "application/json"): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.doOutput = true
        c.setRequestProperty("User-Agent", AGENT)
        c.setRequestProperty("Content-Type", type)
        c.setRequestProperty("Accept", accept)
        c.connectTimeout = 8_000
        c.readTimeout = 30_000
        try {
            c.outputStream.use { it.write(body.toByteArray()) }
            if (c.responseCode in 200..299) c.inputStream.bufferedReader().readText() else c.errorStream?.bufferedReader()?.readText()
        } finally { c.disconnect() }
    } catch (_: Exception) { null }

    internal fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

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
            "thriller" in t || "mystery" in t || "suspense" in t || "spy" in t -> Genre.NOIR
            "adventure" in t || "action" in t || "fantasy" in t || "war" in t -> Genre.ADVENTURE
            else -> Genre.DRAMA
        }
    }

    private val plain = mapOf(
        "drama" to Genre.DRAMA, "comedy" to Genre.COMEDY, "horror" to Genre.HORROR, "science fiction" to Genre.SCIFI, "sci fi" to Genre.SCIFI,
        "romance" to Genre.ROMANCE, "romantic" to Genre.ROMANCE, "melodrama" to Genre.ROMANCE, "western" to Genre.WESTERN, "musical" to Genre.MUSICAL,
        "documentary" to Genre.DOC, "animated" to Genre.ANIMATION, "animation" to Genre.ANIMATION, "adventure" to Genre.ADVENTURE, "action" to Genre.ADVENTURE,
        "fantasy" to Genre.ADVENTURE, "war" to Genre.ADVENTURE, "thriller" to Genre.NOIR, "mystery" to Genre.NOIR, "crime" to Genre.NOIR,
        "detective" to Genre.NOIR, "reality" to Genre.REALITY, "reality show" to Genre.REALITY,
    )

    /**
     * Genres besides [main] from genre names (English, as Wikidata gives them, or MyShows aliases). Only plain genre
     * names count («comedy film», «drama television series»), not finer ones («black comedy», «neo-western»): those are
     * subgenres, and taking them for genres would feed the wrong pets. At most two.
     */
    fun guessAlso(labels: List<String>, main: Genre): String {
        val suffix = Regex("""\s+(film|movie|television series|tv series|television program|television programme|series|anime|show)$""")
        val found = labels.mapNotNull { l -> plain[l.lowercase().replace('-', ' ').trim().replace(suffix, "").trim()] }
        return Genres.join(found.distinct().filter { it != main }.take(2))
    }

    /** Subgenres from English genre names; animation from Japan is anime. */
    fun guessSub(labels: List<String>, genre: Genre, country: String): String {
        val l = Subgenres.match(labels, genre)
        val anime = genre == Genre.ANIMATION && country.lowercase().let { "япон" in it || "japan" in it }
        return Subgenres.join(if (anime) listOf("anime") + l else l)
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
                  val minutes: Int?, val eps: Int?, val seasons: Int?, val en: String?, val ruw: String?, val tvmaze: Int?, val imdb: String?,
                  val directors: List<String> = emptyList(), val cast: List<String> = emptyList())
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
                dur, amount(c, "P1113"), amount(c, "P2437"), links?.optJSONObject("enwiki")?.optString("title"), links?.optJSONObject("ruwiki")?.optString("title"),
                string(c, "P8600")?.toIntOrNull(), string(c, "P345"), ids(c, "P57").take(2), ids(c, "P161").take(4))
            if (raws.size >= 8) break
        }
        val more = raws.flatMap { it.countries + it.genres + it.directors + it.cast }.distinct().take(150)
        val names = HashMap<String, String>()
        val en = HashMap<String, String>()
        // Fifty to a request, as the API takes them.
        for (chunk in more.chunked(50)) get(WD + "action=wbgetentities&ids=${chunk.joinToString("|")}&props=labels&languages=$lang|en|mul")
            ?.let { JSONObject(it).optJSONObject("entities") }?.let { es ->
                for (id in chunk) {
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
                    val country = r.countries.mapNotNull { names[it] }.firstOrNull() ?: ""
                    Draft(
                        kind = r.kind, title = r.label, year = r.year, country = country, genre = g,
                        sub = guessSub(r.genres.mapNotNull { en[it] }, g, r.countries.mapNotNull { en[it] }.firstOrNull() ?: country),
                        also = guessAlso(r.genres.mapNotNull { en[it] }, g),
                        minutes = r.minutes, perSeason = if (r.eps != null && r.seasons != null && r.seasons > 0) (r.eps / r.seasons).coerceAtLeast(1) else null,
                        about = r.genres.mapNotNull { names[it] }.take(2).joinToString(", "),
                        coverUrl = poster(r.ruw, "ru") ?: poster(r.en, "en"), tvmaze = r.tvmaze, imdb = r.imdb,
                        director = People.join(r.directors.mapNotNull { names[it] }), cast = People.join(r.cast.mapNotNull { names[it] }),
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

    private fun string(c: JSONObject, p: String): String? = c.optJSONArray(p)?.optJSONObject(0)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.optString("value")?.ifBlank { null }

    /**
     * The TVmaze show of a series known only by name: through Wikidata (its TVmaze or IMDb id), then
     * TVmaze's own search. 0 when there's none; null when the network failed.
     */
    suspend fun tvmazeFor(title: String, year: Int?): Int? = withContext(Dispatchers.IO) {
        val q = title.substringBefore(" · ").trim()
        if (q.length < 2) return@withContext 0
        val lang = if (Regex("[а-яё]", RegexOption.IGNORE_CASE).containsMatchIn(q)) "ru" else "en"
        val s = get(WD + "action=wbsearchentities&search=${enc(q)}&language=$lang&uselang=$lang&type=item&limit=8") ?: return@withContext null
        val ids = JSONObject(s).optJSONArray("search")?.let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("id") } }.orEmpty()
        if (ids.isNotEmpty()) {
            val ents = get(WD + "action=wbgetentities&ids=${ids.joinToString("|")}&props=claims")?.let { JSONObject(it).optJSONObject("entities") } ?: return@withContext null
            for (id in ids) {
                val c = ents.optJSONObject(id)?.optJSONObject("claims") ?: continue
                if (ids(c, "P31").none { it in series }) continue
                val y = (c.optJSONArray("P580") ?: c.optJSONArray("P577"))?.let { first(it)?.optString("time") }?.let { Regex("""(\d{4})""").find(it)?.value?.toIntOrNull() }
                if (year != null && y != null && kotlin.math.abs(y - year) > 1) continue
                string(c, "P8600")?.toIntOrNull()?.let { return@withContext it }
                string(c, "P345")?.let { Seasons.byImdb(it) }?.let { return@withContext it }
            }
        }
        if (lang == "en") Seasons.byName(q, year) ?: 0 else 0
    }

    /** What Wikidata knows of a title found by its Kinopoisk id, with its Wikipedia articles for the poster. */
    data class Found(val draft: Draft, val enwiki: String?, val ruwiki: String?)

    /** Kinopoisk ids to what Wikidata knows; ids it doesn't know are left out. Null when the query failed. */
    suspend fun byKinopoisk(kp: Collection<Int>, ru: Boolean): Map<Int, Found>? = withContext(Dispatchers.IO) {
        val out = HashMap<Int, Found>()
        for (chunk in kp.distinct().chunked(150)) {
            val q = """
                SELECT ?kp (SAMPLE(?ru) AS ?r) (SAMPLE(?en) AS ?e) (SAMPLE(?mul) AS ?m) (MIN(?date) AS ?d) (SAMPLE(?dur) AS ?min) (SAMPLE(?cL) AS ?c)
                 (GROUP_CONCAT(DISTINCT ?gL; separator="|") AS ?g) (GROUP_CONCAT(DISTINCT STRAFTER(STR(?t), "entity/"); separator="|") AS ?types)
                 (SAMPLE(?enw) AS ?enwiki) (SAMPLE(?ruw) AS ?ruwiki) (SAMPLE(?eps) AS ?ep) (SAMPLE(?ss) AS ?sn) (SAMPLE(?tvm) AS ?tv) WHERE {
                 VALUES ?kp { ${chunk.joinToString(" ") { "\"$it\"" }} }
                 ?item wdt:P2603 ?kp .
                 OPTIONAL { ?item rdfs:label ?ru FILTER(LANG(?ru)="ru") }
                 OPTIONAL { ?item rdfs:label ?en FILTER(LANG(?en)="en") }
                 OPTIONAL { ?item rdfs:label ?mul FILTER(LANG(?mul)="mul") }
                 OPTIONAL { ?item wdt:P577|wdt:P580 ?date }
                 OPTIONAL { ?item wdt:P2047 ?dur }
                 OPTIONAL { ?item wdt:P1113 ?eps }
                 OPTIONAL { ?item wdt:P2437 ?ss }
                 OPTIONAL { ?item wdt:P8600 ?tvm }
                 OPTIONAL { ?item wdt:P495 ?co . ?co rdfs:label ?cL FILTER(LANG(?cL)="${if (ru) "ru" else "en"}") }
                 OPTIONAL { ?item wdt:P136 ?gn . ?gn rdfs:label ?gL FILTER(LANG(?gL)="en") }
                 OPTIONAL { ?item wdt:P31 ?t }
                 OPTIONAL { ?enw schema:about ?item; schema:isPartOf <https://en.wikipedia.org/> }
                 OPTIONAL { ?ruw schema:about ?item; schema:isPartOf <https://ru.wikipedia.org/> }
                } GROUP BY ?kp
            """.trimIndent()
            val body = post("https://query.wikidata.org/sparql", "query=" + enc(q), "application/x-www-form-urlencoded", "application/sparql-results+json") ?: return@withContext null
            val rows = runCatching { JSONObject(body).getJSONObject("results").getJSONArray("bindings") }.getOrNull() ?: return@withContext null
            for (i in 0 until rows.length()) {
                val b = rows.getJSONObject(i)
                fun v(k: String) = b.optJSONObject(k)?.optString("value")?.ifBlank { null }
                val id = v("kp")?.toIntOrNull() ?: continue
                val types = v("types").orEmpty().split('|')
                val kind = if (types.any { it in series }) Kind.SERIES else Kind.FILM
                val label = (if (ru) listOf(v("r"), v("m"), v("e")) else listOf(v("e"), v("m"), v("r"))).firstNotNullOfOrNull { it } ?: continue
                val g = when {
                    types.any { it in setOf("Q202866", "Q20650540", "Q581714", "Q63952888") } -> Genre.ANIMATION
                    "Q93204" in types -> Genre.DOC
                    else -> guessGenre(v("g").orEmpty().replace('|', ' '))
                }
                val eps = v("ep")?.toDoubleOrNull()?.toInt()
                val ss = v("sn")?.toDoubleOrNull()?.toInt()
                fun page(u: String?) = u?.substringAfter("/wiki/", "")?.ifBlank { null }?.let { java.net.URLDecoder.decode(it, "UTF-8").replace('_', ' ') }
                out[id] = Found(
                    Draft(kind, label, v("d")?.take(4)?.toIntOrNull(), v("c").orEmpty(), g, v("min")?.toDoubleOrNull()?.toInt()?.takeIf { it in 1..1000 },
                        if (eps != null && ss != null && ss > 0) (eps / ss).coerceAtLeast(1) else null, tvmaze = v("tv")?.toIntOrNull(),
                        sub = guessSub(v("g").orEmpty().split('|'), g, v("c").orEmpty()), also = guessAlso(v("g").orEmpty().split('|'), g)),
                    page(v("enwiki")), page(v("ruwiki")),
                )
            }
        }
        out
    }

    /** A poster address from a Wikipedia article, Russian first. */
    fun posterOf(ruwiki: String?, enwiki: String?): String? = poster(ruwiki, "ru") ?: poster(enwiki, "en")

    private fun amount(c: JSONObject, p: String): Int? = c.optJSONArray(p)?.let { first(it) }?.optString("amount")?.toDoubleOrNull()?.toInt()

    /** A poster's bytes, or null: placeholders (GIFs, tiny files) don't count, and a failed try is tried once more. */
    suspend fun picture(url: String): ByteArray? {
        repeat(2) { attempt ->
            val b = download(url)
            if (b != null) return b.takeIf { it.size > 4_000 && !(it[0] == 'G'.code.toByte() && it[1] == 'I'.code.toByte() && it[2] == 'F'.code.toByte()) }
            if (attempt == 0) kotlinx.coroutines.delay(1_500)
        }
        return null
    }

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
