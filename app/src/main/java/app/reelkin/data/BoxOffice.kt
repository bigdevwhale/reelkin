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
        val id = dao.insert(t)
        if (from != null) dao.title(from)?.takeIf { it.kind == Kind.FILM }?.let { dao.delete(it) }
        settings.update { it.copy(seance = null) }
        return t.copy(id = id)
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
    suspend fun fillCovers(jobs: List<Triple<CoverHint, List<Long>, List<Long>>>) = coroutineScope {
        val gate = Semaphore(4)
        jobs.map { (hint, titles, tickets) ->
            launch(Dispatchers.IO) {
                gate.withPermit {
                    val url = hint.url ?: Catalog.posterOf(hint.ruwiki, hint.enwiki) ?: hint.name?.let { Catalog.posterFor(it, hint.year, hint.series) } ?: return@withPermit
                    val name = Catalog.download(url)?.let { Covers.save(ctx, it) } ?: return@withPermit
                    if (titles.isNotEmpty()) dao.coverTitles(titles, name)
                    if (tickets.isNotEmpty()) dao.coverTickets(tickets, name)
                }
            }
        }.joinAll()
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

    suspend fun startSeance(se: Seance) = settings.update { it.copy(seance = se) }
    suspend fun stopSeance() = settings.update { it.copy(seance = null) }
}
