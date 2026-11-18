package app.reelkin.data

import app.reelkin.AppContainer
import app.reelkin.ui.Backdrops
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The whole cinema as one zip, to move to a new phone: reelkin.json (the bill, the stubs, the episodes, the pets,
 * the goals and the settings), the posters under covers/ and one's own backdrop.jpg. Loading a file replaces what is
 * on the phone; the file is read and checked in full before anything is touched.
 */
object Backup {
    const val APP = "reelkin"
    const val FORMAT = 1
    private const val DATA = "reelkin.json"
    private const val COVERS = "covers/"
    private const val BACKDROP = "backdrop.jpg"
    private const val STAGE = "restore.tmp"
    private const val MAX_JSON = 64 * 1024 * 1024

    fun fileName() = "reelkin-${LocalDate.now()}.zip"

    /** Not a Reelkin backup, or ([newer]) one written by a later version than this one reads. */
    class Unreadable(val newer: Boolean = false) : Exception()

    class Everything(val titles: List<Title>, val tickets: List<Ticket>, val episodes: List<Episode>, val pets: List<Pet>, val goals: List<Goal>, val settings: Settings) {
        /** The posters all of it shows. */
        val covers: Set<String> get() = (titles.mapNotNull { it.cover } + tickets.mapNotNull { it.cover } + listOfNotNull(settings.seance?.cover)).toSet()
    }

    // ---- the JSON, free of Android so it can be tested ----

    fun encode(e: Everything, version: String = "", format: Int = FORMAT): String = JSONObject()
        .put("app", APP).put("format", format).put("version", version).put("at", System.currentTimeMillis())
        .put("settings", settings(e.settings))
        .put("titles", JSONArray().apply { e.titles.forEach { t ->
            put(JSONObject().put("id", t.id).put("kind", t.kind.name).put("title", t.title).putOpt("year", t.year).put("country", t.country).put("genre", t.genre.name)
                .put("minutes", t.minutes).put("status", t.status.name).putOpt("cover", t.cover).put("added", t.added).put("season", t.season).put("episode", t.episode)
                .put("perSeason", t.perSeason).put("cliff", t.cliff).put("plan", t.plan).put("planAt", t.planAt).putOpt("show", t.show)
                .put("director", t.director).put("cast", t.cast).put("sub", t.sub).put("also", t.also))
        } })
        .put("tickets", JSONArray().apply { e.tickets.forEach { t ->
            put(JSONObject().put("id", t.id).put("title", t.title).put("season", t.season).putOpt("year", t.year).put("country", t.country).put("genre", t.genre.name)
                .put("minutes", t.minutes).put("stars", t.stars).put("words", t.words).put("company", t.company.name).put("credits", t.credits.name).put("rewatch", t.rewatch)
                .put("at", t.at).putOpt("cover", t.cover).put("whole", t.whole).putOpt("ref", t.ref).put("director", t.director).put("cast", t.cast).put("sub", t.sub)
                .put("past", t.past).put("also", t.also))
        } })
        // the episodes are the bulk: series, season, number, minutes, at
        .put("episodes", JSONArray().apply { e.episodes.forEach { put(JSONArray().put(it.id).put(it.series).put(it.season).put(it.number).put(it.minutes).put(it.at)) } })
        .put("pets", JSONArray().apply { e.pets.forEach { p ->
            put(JSONObject().put("id", p.id).put("species", p.species.name).put("name", p.name).put("laid", p.laid).put("shiny", p.shiny).put("nature", p.nature.name)
                .putOpt("form", p.form?.name).put("spans", p.spans).put("hatched", p.hatched))
        } })
        .put("goals", JSONArray().apply { e.goals.forEach { g ->
            put(JSONObject().put("id", g.id).put("kind", g.kind.name).put("target", g.target).put("period", g.period.name).put("ref", g.ref).put("created", g.created)
                .put("until", g.until).put("archived", g.archived))
        } })
        .toString()

    private fun settings(s: Settings) = JSONObject()
        .put("petName", s.petName).putOpt("species", s.species?.name).put("shiny", s.shiny).put("pet", s.pet).put("tips", JSONArray(s.tips.sorted()))
        .put("wall", s.wall).put("claimed", JSONArray(s.claimed.sorted())).put("cinema", s.cinema).put("spent", s.spent).put("bought", JSONArray(s.bought.sorted()))
        .put("awards", JSONArray().apply { s.awards.forEach { put(JSONObject().put("year", it.year).put("cat", it.cat).put("title", it.title)) } })
        .put("goal", s.goal).put("goalSeasons", s.goalSeasons).put("pixelCovers", s.pixelCovers).putOpt("seance", s.seance?.json())
        .put("backdrop", s.backdrop).put("stubSort", s.stubSort)

    /** Throws [Unreadable] if [text] is not a Reelkin backup this version can read. */
    fun decode(text: String): Everything {
        val o = runCatching { JSONObject(text) }.getOrNull() ?: throw Unreadable()
        if (o.optString("app") != APP) throw Unreadable()
        if (o.optInt("format", 0) > FORMAT) throw Unreadable(newer = true)
        return try {
            fun objs(k: String) = o.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } }.orEmpty()
            fun JSONObject.str(k: String): String? = if (has(k) && !isNull(k)) getString(k) else null
            fun JSONObject.int(k: String): Int? = if (has(k) && !isNull(k)) getInt(k) else null
            fun JSONObject.set(k: String) = optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty().toSet()
            val titles = objs("titles").map { t ->
                Title(t.getLong("id"), Kind.valueOf(t.getString("kind")), t.getString("title"), t.int("year"), t.optString("country"), Genre.valueOf(t.getString("genre")),
                    t.optInt("minutes"), Status.valueOf(t.getString("status")), t.str("cover"), t.getLong("added"), t.optInt("season", 1), t.optInt("episode"),
                    t.optInt("perSeason", 10), t.optBoolean("cliff"), t.optString("plan"), t.optLong("planAt"), t.str("show"),
                    t.optString("director"), t.optString("cast"), t.optString("sub"), t.optString("also"))
            }
            val tickets = objs("tickets").map { t ->
                Ticket(t.getLong("id"), t.getString("title"), t.optBoolean("season"), t.int("year"), t.optString("country"), Genre.valueOf(t.getString("genre")),
                    t.optInt("minutes"), t.optInt("stars"), t.optString("words"), Company.valueOf(t.getString("company")), Credits.valueOf(t.getString("credits")),
                    t.optBoolean("rewatch"), t.getLong("at"), t.str("cover"), t.optBoolean("whole"), t.str("ref"), t.optString("director"), t.optString("cast"),
                    t.optString("sub"), t.optBoolean("past"), t.optString("also"))
            }
            val episodes = o.optJSONArray("episodes")?.let { a -> (0 until a.length()).map { i -> a.getJSONArray(i).let { e ->
                Episode(e.getLong(0), e.getLong(1), e.getInt(2), e.getInt(3), e.getInt(4), e.getLong(5)) } } }.orEmpty()
            val pets = objs("pets").map { p ->
                Pet(p.getLong("id"), Species.valueOf(p.getString("species")), p.optString("name"), p.getLong("laid"), p.optBoolean("shiny"), Nature.valueOf(p.getString("nature")),
                    p.str("form")?.let { Form.valueOf(it) }, p.getString("spans"), p.optBoolean("hatched"))
            }
            val goals = objs("goals").map { g ->
                Goal(g.getLong("id"), GoalKind.valueOf(g.getString("kind")), g.getInt("target"), Period.valueOf(g.getString("period")), g.optString("ref"), g.getLong("created"),
                    g.optLong("until"), g.optBoolean("archived"))
            }
            val s = o.getJSONObject("settings").let { s ->
                Settings(
                    onboarded = true, petName = s.optString("petName"), species = s.str("species")?.let { Species.valueOf(it) }, shiny = s.optBoolean("shiny"),
                    // the pet in the booth must be one of the file's own
                    pet = s.optLong("pet").takeIf { id -> pets.any { it.id == id } } ?: 0, tips = s.set("tips"), wall = s.optString("wall"), claimed = s.set("claimed"),
                    cinema = s.optString("cinema"), spent = s.optInt("spent"), bought = s.set("bought"),
                    awards = s.optJSONArray("awards")?.let { a -> (0 until a.length()).map { a.getJSONObject(it) }.map { Award(it.getInt("year"), it.getInt("cat"), it.getString("title")) } }.orEmpty(),
                    goal = s.optInt("goal", 52), goalSeasons = s.optInt("goalSeasons"), pixelCovers = s.optBoolean("pixelCovers"), seance = Seance.of(s.str("seance")),
                    backdrop = s.optString("backdrop"), stubSort = s.optString("stubSort"),
                )
            }
            Everything(titles, tickets, episodes, pets, goals, s)
        } catch (e: Exception) {
            throw Unreadable()
        }
    }

    // ---- the file ----

    suspend fun everything(c: AppContainer): Everything {
        val dao = c.db.dao()
        return Everything(dao.titleList(), dao.ticketList(), dao.episodes().first(), dao.petList(), dao.goalList(), c.settings.now())
    }

    /** Writes the whole cinema to [out] as a zip and closes it. */
    suspend fun write(c: AppContainer, out: OutputStream, version: String = app.reelkin.BuildConfig.VERSION_NAME) = withContext(Dispatchers.IO) {
        val e = everything(c)
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DATA)); zip.write(encode(e, version).toByteArray()); zip.closeEntry()
            for (name in e.covers) {
                val f = Covers.file(c.app, name) ?: continue
                zip.putNextEntry(ZipEntry(COVERS + name)); f.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
            }
            Backdrops.ownFile(c.app).takeIf { it.exists() }?.let { f ->
                zip.putNextEntry(ZipEntry(BACKDROP)); f.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
            }
        }
    }

    /** A file read and checked, its pictures waiting aside until [load]. */
    class Found(val all: Everything, internal val stage: File) {
        val films get() = all.tickets.count { !it.season }
        val seasons get() = all.tickets.count { it.season }
        val pets get() = all.pets.count { it.hatched }
    }

    /** Reads [input] in full without touching the cinema; throws [Unreadable] when it isn't ours. */
    suspend fun open(c: AppContainer, input: InputStream): Found = withContext(Dispatchers.IO) {
        val stage = File(c.app.filesDir, STAGE)
        stage.deleteRecursively()
        try {
            var json: String? = null
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    when {
                        entry.isDirectory -> Unit
                        name == DATA -> {
                            val buf = java.io.ByteArrayOutputStream()
                            val chunk = ByteArray(64 * 1024)
                            while (true) { val n = zip.read(chunk); if (n < 0) break; buf.write(chunk, 0, n); if (buf.size() > MAX_JSON) throw Unreadable() }
                            json = buf.toString("UTF-8")
                        }
                        name == BACKDROP -> { stage.mkdirs(); File(stage, BACKDROP).outputStream().use { zip.copyTo(it) } }
                        name.startsWith(COVERS) -> {
                            // only a bare file name: nothing in the file may write outside the folder
                            val bare = name.removePrefix(COVERS)
                            if (bare.isNotEmpty() && bare == File(bare).name && bare != "." && bare != "..") {
                                val dir = File(stage, "covers").apply { mkdirs() }
                                File(dir, bare).outputStream().use { zip.copyTo(it) }
                            }
                        }
                    }
                }
            }
            Found(decode(json ?: throw Unreadable()), stage)
        } catch (e: Throwable) {
            stage.deleteRecursively()
            throw if (e is Unreadable) e else Unreadable()
        }
    }

    fun discard(f: Found) { f.stage.deleteRecursively() }

    /** Replaces everything on the phone with [f]. */
    suspend fun load(c: AppContainer, f: Found) = withContext(Dispatchers.IO) {
        try {
            val e = f.all
            // the new posters come in beside the old ones, so a failure below leaves the cinema as it was
            val covers = Covers.dir(c.app)
            File(f.stage, "covers").listFiles()?.forEach { it.copyTo(File(covers, it.name), overwrite = true) }
            c.db.dao().replaceAll(e)
            val own = Backdrops.ownFile(c.app)
            val staged = File(f.stage, BACKDROP)
            if (staged.exists()) staged.copyTo(own, overwrite = true) else own.delete()
            val s = e.settings
            c.settings.update { s.copy(onboarded = true, backdrop = if (s.backdrop == Backdrops.OWN && !own.exists()) Backdrops.NONE else s.backdrop, backdropStamp = System.currentTimeMillis()) }
            val keep = e.covers
            covers.listFiles()?.forEach { if (it.name !in keep) it.delete() }
            Covers.forget()
        } finally {
            f.stage.deleteRecursively()
        }
    }
}
