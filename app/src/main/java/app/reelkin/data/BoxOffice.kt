package app.reelkin.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Every change to the cinema goes through here. */
class BoxOffice(private val ctx: Context, private val dao: CinemaDao, private val settings: SettingsStore) {
    suspend fun add(t: Title): Long = dao.insert(t)

    suspend fun update(t: Title) = dao.update(t)

    suspend fun remove(t: Title) {
        dao.delete(t)
        if (t.cover != null && dao.ticketList().none { it.cover == t.cover }) Covers.delete(ctx, t.cover)
    }

    /** Tears a stub; a film from the bill leaves it, a running seance ends. Returns the stub with its id. */
    suspend fun tear(t: Ticket, from: Long?): Ticket {
        val title = from?.let { dao.title(it) }
        val tk = if (title != null && t.director.isEmpty()) t.copy(director = title.director, cast = title.cast) else t
        val id = dao.insert(tk)
        if (title != null && title.kind == Kind.FILM) dao.delete(title)
        settings.update { it.copy(seance = null) }
        return tk.copy(id = id)
    }

    suspend fun update(t: Ticket) = dao.update(t)

    suspend fun remove(t: Ticket) {
        dao.delete(t)
        if (t.cover != null && dao.ticketList().none { it.cover == t.cover }) Covers.delete(ctx, t.cover)
    }

    /**
     * One more episode of a series. The last one of a season tears a season stub; returns it, or
     * null for an ordinary episode.
     */
    suspend fun episode(s: Title, ru: Boolean): Ticket? {
        dao.insert(Episode(series = s.id, season = s.season, number = s.episode + 1, minutes = s.minutes))
        val n = s.episode + 1
        if (n < s.perSeason) { dao.update(s.copy(episode = n, cliff = false, status = Status.RUNNING)); return null }
        val stub = Ticket(title = s.title + if (ru) " · сезон ${s.season}" else " · season ${s.season}", season = true, year = s.year, country = s.country,
            genre = s.genre, minutes = s.minutes * s.perSeason, stars = 0, cover = s.cover)
        val id = dao.insert(stub)
        // The next season takes its length from the catalogue, and waits in the cupboard if it isn't out yet.
        val next = s.season + 1
        val out = Seasons.air(s, next).let { it == Air.Out || it == Air.Unknown }
        dao.update(s.copy(season = next, episode = 0, cliff = false, status = if (out) Status.RUNNING else Status.SHELVED, perSeason = Seasons.episodes(s, next) ?: s.perSeason))
        return stub.copy(id = id)
    }

    suspend fun setPlan(id: Long, show: String?, plan: String, at: Long = System.currentTimeMillis()) = dao.setPlan(id, plan, at, show)

    /**
     * Writes an import, leaving out what's already here: stubs imported before (by [Ticket.ref]),
     * stubs with the same name and year, and series already on the bill. Returns what was written,
     * with the ids the posters will need.
     */
    suspend fun import(loot: Loot): Imported {
        val tickets = dao.ticketList()
        val refs = tickets.mapNotNull { it.ref }.toHashSet()
        val names = tickets.map { key(it.title, it.year) }.toHashSet()
        val bill = dao.titleList().map { key(it.title, null) + it.kind }.toHashSet()
        var stubs = 0; var titles = 0
        val covers = ArrayList<Triple<CoverHint, List<Long>, List<Long>>>()
        for (h in loot.hauls) {
            val fresh = h.stubs.filter { (it.ref == null || it.ref !in refs) && key(it.title, it.year) !in names }
            val tIds = dao.insertTickets(fresh)
            fresh.forEach { it.ref?.let(refs::add); names += key(it.title, it.year) }
            var titleId: Long? = null
            if (h.title != null && key(h.title.title, null) + h.title.kind !in bill) {
                titleId = dao.insert(h.title)
                bill += key(h.title.title, null) + h.title.kind
                if (h.episodes.isNotEmpty()) dao.insertEpisodes(h.episodes.map { it.copy(series = titleId) })
                titles++
            }
            stubs += tIds.size
            if (h.cover != null && (tIds.isNotEmpty() || titleId != null)) covers += Triple(h.cover, listOfNotNull(titleId), tIds)
        }
        return Imported(stubs, titles, covers)
    }

    class Imported(val stubs: Int, val titles: Int, val covers: List<Triple<CoverHint, List<Long>, List<Long>>>)

    private fun key(title: String, year: Int?) = title.trim().lowercase().replace('ё', 'е') + "|" + (year ?: "")

    /** Posters for what was imported, a few at a time; slow, so it runs after the import is already on screen. */
    suspend fun fillCovers(jobs: List<Triple<CoverHint, List<Long>, List<Long>>>): Int = coroutineScope {
        val gate = Semaphore(4)
        // Found posters are written a batch at a time: every write redraws the screens, and a thousand of them one by one stall the app.
        val found = ArrayList<Triple<String, List<Long>, List<Long>>>()
        val lock = Any()
        var got = 0
        suspend fun flush() {
            val batch = synchronized(lock) { found.toList().also { found.clear() } }
            if (batch.isEmpty()) return
            dao.coverAll(batch)
        }
        val writer = launch { while (true) { kotlinx.coroutines.delay(3_000); flush() } }
        jobs.map { (hint, titles, tickets) ->
            launch(Dispatchers.IO) {
                gate.withPermit {
                    val name = posterBytes(hint)?.let { Covers.save(ctx, it) } ?: return@withPermit
                    synchronized(lock) { found += Triple(name, titles, tickets); got++ }
                }
            }
        }.joinAll()
        writer.cancel()
        flush()
        got
    }

    /** A poster from the first source that has one: the address given, then Wikipedia's articles, then a guess by name. */
    private suspend fun posterBytes(hint: CoverHint): ByteArray? {
        hint.url?.let { Catalog.picture(it) }?.let { return it }
        Catalog.posterOf(hint.ruwiki, hint.enwiki)?.let { Catalog.picture(it) }?.let { return it }
        return hint.name?.let { Catalog.posterFor(it, hint.year, hint.series) }?.let { Catalog.picture(it) }
    }

    /**
     * Posters for stubs brought from Kinopoisk without one, by the Kinopoisk id. Runs once per
     * install: earlier versions looked only on Wikipedia, which doesn't have every film.
     */
    suspend fun mendCovers() {
        if (settings.done("kp_covers")) return
        val bare = dao.ticketList().filter { it.cover == null && it.ref?.startsWith("kinopoisk:") == true }
        val jobs = bare.mapNotNull { t -> t.ref!!.removePrefix("kinopoisk:").toIntOrNull()?.let { Triple(CoverHint(url = Kinopoisk.poster(it)), emptyList<Long>(), listOf(t.id)) } }
        // Nothing found at all most likely means no connection: try again next launch.
        if (jobs.isEmpty() || fillCovers(jobs) > 0) settings.markDone("kp_covers")
    }

    /** How many stubs came from [source] ("myshows", "kinopoisk"). */
    suspend fun importedFrom(source: String): Int = dao.ticketList().count { it.ref?.startsWith("$source:") == true || it.ref?.startsWith("$source-") == true }

    /** Takes back the stubs an import from [source] tore. */
    suspend fun undoImport(source: String) {
        val gone = dao.ticketList().filter { it.ref?.startsWith("$source:") == true || it.ref?.startsWith("$source-") == true }
        dao.deleteTickets(gone)
        val kept = (dao.ticketList().mapNotNull { it.cover } + dao.titleList().mapNotNull { it.cover }).toHashSet()
        for (c in gone.mapNotNull { it.cover }.toSet()) if (c !in kept) Covers.delete(ctx, c)
    }

    /** Takes back the last episode, if it was this season's. */
    suspend fun undoEpisode(s: Title) {
        if (s.episode <= 0) return
        val e = dao.lastEpisode(s.id) ?: return
        dao.delete(e)
        dao.update(s.copy(episode = s.episode - 1, cliff = false))
    }

    /** Opens a can: the pet goes into the booth and whoever was there goes to rest. */
    suspend fun takePet(sp: Species): Long {
        val now = System.currentTimeMillis()
        val all = dao.petList()
        all.filter { it.spans.endsWith("-0") }.forEach { dao.update(it.copy(spans = it.spans.dropLast(1) + now)) }
        val id = dao.insert(app.reelkin.domain.Pets.hatch(sp, now))
        settings.update { it.copy(pet = id) }
        return id
    }

    /** Brings a pet back into the booth. */
    suspend fun callPet(p: Pet) {
        val now = System.currentTimeMillis()
        val all = dao.petList()
        for (o in app.reelkin.domain.Pets.swap(all, p, now)) dao.update(o)
        settings.update { it.copy(pet = p.id) }
    }

    /** The one pet of 1.x becomes the first in the pets table, fed by everything since the first stub. */
    suspend fun bringOldPet() {
        val s = settings.now()
        val sp = s.species ?: return
        if (dao.petList().isNotEmpty()) return
        val id = dao.insert(Pet(species = sp, name = s.petName, laid = 0, shiny = s.shiny,
            nature = Nature.entries[(sp.ordinal + s.petName.length) % Nature.entries.size], spans = "0-0", hatched = true))
        settings.update { it.copy(pet = id, species = null) }
    }

    /** The catalogue's guess at a film or a series by its name and year: its director and leading actors, or null when offline. */
    private suspend fun peopleOf(title: String, year: Int?, series: Boolean): Pair<String, String>? {
        val ru = Regex("[а-яё]", RegexOption.IGNORE_CASE).containsMatchIn(title)
        val found = Catalog.search(title.substringBefore(" · "), ru) ?: return null
        val kind = if (series) Kind.SERIES else Kind.FILM
        val d = found.firstOrNull { it.kind == kind && (year == null || it.year == null || kotlin.math.abs(it.year - year) <= 1) && (it.director.isNotEmpty() || it.cast.isNotEmpty()) }
            ?: return "?" to ""
        return d.director.ifEmpty { "?" } to d.cast
    }

    /** Fills in who made a title and who's in it, once; returns it as it is now. */
    suspend fun people(t: Title): Title {
        if (t.director.isNotEmpty()) return t
        val (d, c) = peopleOf(t.title, t.year, t.kind == Kind.SERIES) ?: return t
        return t.copy(director = d, cast = c).also { dao.update(it) }
    }

    suspend fun people(t: Ticket): Ticket {
        if (t.director.isNotEmpty()) return t
        val (d, c) = peopleOf(t.title, t.year, t.season) ?: return t
        // Every stub of the same film learns it at once.
        for (o in dao.ticketList().filter { it.title == t.title && it.year == t.year && it.director.isEmpty() }) dao.update(o.copy(director = d, cast = c))
        return t.copy(director = d, cast = c)
    }

    /** Looks up the people of everything on the bill and every stub, a few at a time; [onStep] hears how far it got. */
    suspend fun allPeople(onStep: (Int, Int) -> Unit) {
        val titles = dao.titleList().filter { it.director.isEmpty() }
        val stubs = dao.ticketList().filter { it.director.isEmpty() }.distinctBy { it.title + "|" + it.year }
        val total = titles.size + stubs.size
        var n = 0
        for (t in titles) { people(t); onStep(++n, total) }
        for (t in stubs) { people(t); onStep(++n, total) }
    }

    /** The year's goals of 1.x (films, seasons) become goals like any other, once. */
    suspend fun bringYearGoals() {
        if (settings.done("year_goals")) return
        val s = settings.now()
        val year = java.util.Calendar.getInstance().apply { set(java.util.Calendar.DAY_OF_YEAR, 1); set(java.util.Calendar.HOUR_OF_DAY, 0) }.timeInMillis
        if (dao.goalList().isEmpty()) {
            dao.insert(Goal(kind = GoalKind.FILMS, target = s.goal, period = Period.YEAR, created = year))
            if (s.goalSeasons > 0) dao.insert(Goal(kind = GoalKind.SEASONS, target = s.goalSeasons, period = Period.YEAR, created = year))
        }
        settings.markDone("year_goals")
    }

    suspend fun startSeance(se: Seance) = settings.update { it.copy(seance = se) }
    suspend fun stopSeance() = settings.update { it.copy(seance = null) }
}
