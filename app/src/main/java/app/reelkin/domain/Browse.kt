package app.reelkin.domain

import app.reelkin.data.Company
import app.reelkin.data.Genre
import app.reelkin.data.Kind
import app.reelkin.data.Ticket
import app.reelkin.data.Title
import java.util.Calendar

/**
 * What the bill shows: the words typed in the search field, the filters that are on and how the
 * stubs are ordered. The same filters narrow the titles waiting and the stubs torn.
 */
data class Browse(
    val query: String = "",
    /** Films, series or both. */
    val kind: Kind? = null,
    val genres: Set<Genre> = emptySet(),
    /** At least this much popcorn; 0 for any. Stubs only. */
    val stars: Int = 0,
    /** The year a stub was torn; null for any. Stubs only. */
    val year: Int? = null,
    val company: Company? = null,
    val sort: Sort = Sort.NEWEST,
) {
    enum class Sort { NEWEST, OLDEST, POPCORN, TITLE, LENGTH, RELEASE }

    /** How many filters are on (the words typed and the order aren't counted). */
    val filters: Int get() = listOf(kind != null, genres.isNotEmpty(), stars > 0, year != null, company != null).count { it }
    val narrowed: Boolean get() = filters > 0 || query.isNotBlank()

    private fun matches(text: String) = query.isBlank() || query.trim().lowercase().let { q -> q in text.lowercase() }

    fun titles(all: List<Title>): List<Title> = all.filter { t ->
        matches(t.title + " " + t.country) && (kind == null || t.kind == kind) && (genres.isEmpty() || t.genre in genres) &&
            // A title waiting has no popcorn, no day and no company yet: those filters are about stubs.
            stars == 0 && year == null && company == null
    }

    /** Series running or put away: only the words, the kind and the genre apply to them. */
    fun series(all: List<Title>): List<Title> = all.filter { t ->
        matches(t.title + " " + t.country) && kind != Kind.FILM && (genres.isEmpty() || t.genre in genres)
    }

    fun stubs(all: List<Ticket>): List<Ticket> = all.filter { t ->
        matches(t.title + " " + t.country + " " + t.words) && (kind == null || t.season == (kind == Kind.SERIES)) &&
            (genres.isEmpty() || t.genre in genres) && (stars == 0 || t.stars >= stars) && (year == null || yearOf(t.at) == year) &&
            (company == null || t.company == company)
    }.let { l ->
        when (sort) {
            Sort.NEWEST -> l.sortedByDescending { it.at }
            Sort.OLDEST -> l.sortedBy { it.at }
            Sort.POPCORN -> l.sortedWith(compareByDescending<Ticket> { it.stars }.thenByDescending { it.at })
            Sort.TITLE -> l.sortedBy { it.title.lowercase() }
            Sort.LENGTH -> l.sortedByDescending { it.minutes }
            Sort.RELEASE -> l.sortedWith(compareBy<Ticket> { it.year ?: Int.MAX_VALUE }.thenBy { it.at })
        }
    }

    /** Kept across tab switches and rotations as one line. */
    fun encode(): String = listOf(query.replace('\t', ' '), kind?.name ?: "", genres.joinToString(",") { it.name }, stars.toString(),
        year?.toString() ?: "", company?.name ?: "", sort.name).joinToString("\t")

    companion object {
        fun yearOf(ms: Long): Int = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.YEAR)

        fun sortOf(name: String): Sort = Sort.entries.firstOrNull { it.name == name } ?: Sort.NEWEST

        fun decode(s: String): Browse = runCatching {
            val p = s.split('\t')
            Browse(
                query = p[0],
                kind = p[1].takeIf { it.isNotEmpty() }?.let(Kind::valueOf),
                genres = p[2].split(',').filter { it.isNotEmpty() }.mapNotNull { n -> Genre.entries.firstOrNull { it.name == n } }.toSet(),
                stars = p[3].toIntOrNull() ?: 0,
                year = p[4].toIntOrNull(),
                company = p[5].takeIf { it.isNotEmpty() }?.let(Company::valueOf),
                sort = sortOf(p[6]),
            )
        }.getOrDefault(Browse())
    }
}
