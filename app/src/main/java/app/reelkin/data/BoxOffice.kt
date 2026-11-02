package app.reelkin.data

import android.content.Context

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
        dao.update(s.copy(season = s.season + 1, episode = 0, cliff = false, status = Status.RUNNING))
        return stub.copy(id = id)
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
