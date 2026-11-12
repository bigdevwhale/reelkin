package app.reelkin.domain

import app.reelkin.data.Company
import app.reelkin.data.Credits
import app.reelkin.data.DAY
import app.reelkin.data.Episode
import app.reelkin.data.Genre
import app.reelkin.data.Ticket
import java.util.Calendar

/** The span the numbers look at. */
enum class Span { WEEK, MONTH, YEAR, LAST_YEAR, ALL }

/** What the numbers are narrowed to: a span, or one [year] of the past; films, series or both; a genre. */
data class Filter(val span: Span = Span.YEAR, val year: Int? = null, val genre: Genre? = null, val films: Boolean = true, val series: Boolean = true)

/** One line of a top list. */
data class Top(val name: String, val sub: String, val minutes: Int, val count: Int, val cover: String? = null, val genre: Genre? = null, val stars: Int = 0)

/** What was watched in a [Filter]: films, seasons, episodes, in numbers, as the stats page and the postcards show them. */
class Stats(val c: Cinema, val f: Filter = Filter()) {
    private val cal = Calendar.getInstance()
    fun dayOf(ms: Long): Long = (ms + cal.timeZone.getOffset(ms)).floorDiv(DAY)
    private fun startOfDay(d: Long): Long = d * DAY - cal.timeZone.getOffset(d * DAY)
    val today: Long = dayOf(c.now)
    private fun yearStart(y: Int): Long = Calendar.getInstance().apply { clear(); set(y, 0, 1) }.timeInMillis

    val from: Long
    val to: Long

    init {
        when {
            f.year != null -> { from = yearStart(f.year); to = yearStart(f.year + 1) }
            f.span == Span.WEEK -> { from = startOfDay(today - 6); to = Long.MAX_VALUE }
            f.span == Span.MONTH -> { from = startOfDay(today - 29); to = Long.MAX_VALUE }
            f.span == Span.YEAR -> { from = yearStart(c.year); to = Long.MAX_VALUE }
            f.span == Span.LAST_YEAR -> { from = yearStart(c.year - 1); to = yearStart(c.year) }
            else -> { from = 0; to = Long.MAX_VALUE }
        }
    }

    private fun inside(ms: Long) = ms in from until to
    private fun genreOf(e: Episode) = c.title(e.series)?.genre

    val stubs: List<Ticket> = c.seats.filter { inside(it.at) && (f.genre == null || it.genre == f.genre) }
    val films: List<Ticket> = if (f.films) stubs.filter { !it.season } else emptyList()
    val seasonStubs: List<Ticket> = if (f.series) stubs.filter { it.season } else emptyList()
    val seasons: Int = seasonStubs.size
    val episodes: List<Episode> = if (f.series) c.episodes.filter { inside(it.at) && (f.genre == null || genreOf(it) == f.genre) } else emptyList()

    /** Everything watched, as moments with minutes: films, episodes and whole seasons brought over. */
    val moments: List<Pair<Long, Int>> = (films.map { it.at to it.minutes } + seasonStubs.filter { it.whole }.map { it.at to it.minutes } + episodes.map { it.at to it.minutes }).sortedBy { it.first }

    val minutes: Long = moments.sumOf { it.second.toLong() }
    val hours: Long = minutes / 60
    val days: Int = moments.map { dayOf(it.first) }.toSet().size
    /** Days the span covers up to today, for the average. */
    val spanDays: Int = run {
        val first = moments.firstOrNull()?.first ?: return@run 1
        val begin = if (from == 0L) dayOf(first) else maxOf(dayOf(first), dayOf(from))
        val end = if (to == Long.MAX_VALUE) today else dayOf(to - 1)
        (end - begin + 1).toInt().coerceAtLeast(1)
    }
    val perDay: Int = (minutes / spanDays).toInt()

    val rated = films.filter { it.stars > 0 }
    val avgPopcorn: Double? = rated.takeIf { it.isNotEmpty() }?.map { it.stars }?.average()
    val avgLength: Int? = films.filter { it.minutes > 0 }.takeIf { it.isNotEmpty() }?.map { it.minutes }?.average()?.toInt()
    val rewatches = films.count { it.rewatch }
    val scenes = films.count { it.credits == Credits.SCENE }

    /** Countries and decades seen for the first time in the span. */
    val newCountries: Int = run {
        val before = c.seats.filter { it.at < from }.map { it.country.trim().lowercase() }.toSet()
        films.map { it.country.trim().lowercase() }.filter { it.isNotEmpty() && it !in before }.toSet().size
    }
    val newDecades: Int = run {
        val before = c.seats.filter { it.at < from }.mapNotNull { it.year?.let { y -> Cinema.decade(y) } }.toSet()
        films.mapNotNull { it.year?.let { y -> Cinema.decade(y) } }.filter { it !in before }.toSet().size
    }

    val countries: List<Pair<String, Int>> = films.map { it.country.trim() }.filter { it.isNotEmpty() }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }

    /** Minutes per genre, most first: films and episodes. */
    val genres: List<Pair<Genre, Int>> = run {
        val m = HashMap<Genre, Int>()
        for (t in films) m.merge(t.genre, t.minutes.coerceAtLeast(1), Int::plus)
        for (t in seasonStubs) if (t.whole) m.merge(t.genre, t.minutes, Int::plus)
        for (e in episodes) genreOf(e)?.let { m.merge(it, e.minutes, Int::plus) }
        m.toList().sortedByDescending { it.second }
    }
    val genreCounts: List<Pair<Genre, Int>> = films.groupingBy { it.genre }.eachCount().toList().sortedByDescending { it.second }

    /** How many films got one to five popcorn. */
    val popcorn: List<Pair<Int, Int>> = (5 downTo 1).map { s -> s to films.count { it.stars == s } }

    /** Short, normal, long and very long films: see [LENGTHS]. */
    val lengths: List<Int> = films.filter { it.minutes > 0 }.let { l ->
        listOf(l.count { it.minutes < LENGTHS[0] }, l.count { it.minutes in LENGTHS[0] until LENGTHS[1] },
            l.count { it.minutes in LENGTHS[1] until LENGTHS[2] }, l.count { it.minutes >= LENGTHS[2] })
    }

    /** Films by the decade they came out in, oldest first. */
    val decades: List<Pair<Int, Int>> = films.mapNotNull { it.year }.groupingBy { Cinema.decade(it) }.eachCount().toList().sortedBy { it.first }

    val company: List<Pair<Company, Int>> = Company.entries.map { co -> co to films.count { it.company == co } }

    /** Minutes by weekday, Monday first. */
    val weekdays: LongArray = LongArray(7).also { a -> for ((at, m) in moments) { cal.timeInMillis = at; a[(cal.get(Calendar.DAY_OF_WEEK) + 5) % 7] += m.toLong() } }
    /** Minutes by the hour the film or episode ended in. */
    val hoursOfDay: LongArray = LongArray(24).also { a -> for ((at, m) in moments) { cal.timeInMillis = at; a[cal.get(Calendar.HOUR_OF_DAY)] += m.toLong() } }
    val peakHour: Int? = hoursOfDay.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index
    val favouriteWeekday: Int? = weekdays.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index

    /** By month (a year and more) rather than by day. */
    val monthly: Boolean = f.year != null || f.span == Span.YEAR || f.span == Span.LAST_YEAR || f.span == Span.ALL

    /** Minutes per day (or per month), oldest first, for the bar chart: (start of the day or month, minutes). */
    val series: List<Pair<Long, Long>> = run {
        if (monthly) {
            fun monthKey(ms: Long): Int { cal.timeInMillis = ms; return cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH) }
            val m = HashMap<Int, Long>()
            for ((at, mm) in moments) m.merge(monthKey(at), mm.toLong(), Long::plus)
            val first = if (from == 0L) m.keys.minOrNull() ?: return@run emptyList() else monthKey(from)
            val last = if (to == Long.MAX_VALUE) monthKey(c.now) else monthKey(to - 1)
            (first..last).map { k -> Calendar.getInstance().apply { clear(); set(k / 12, k % 12, 1) }.timeInMillis to (m[k] ?: 0L) }
        } else {
            val n = if (f.span == Span.WEEK) 7 else 30
            val m = HashMap<Long, Long>()
            for ((at, mm) in moments) m.merge(dayOf(at), mm.toLong(), Long::plus)
            (n - 1 downTo 0).map { today - it }.map { startOfDay(it) to (m[it] ?: 0L) }
        }
    }

    /** Films best liked: popcorn first, then the latest. */
    val topFilms: List<Top> = films.sortedWith(compareByDescending<Ticket> { it.stars }.thenByDescending { it.at })
        .distinctBy { it.title.lowercase() }.map { Top(it.title, listOfNotNull(it.year?.toString(), it.country.ifBlank { null }).joinToString(" · "), it.minutes, 1, it.cover, it.genre, it.stars) }

    /** Series by the time given to them in the span. */
    val topSeries: List<Top> = run {
        val m = episodes.groupBy { it.series }
        m.map { (id, l) ->
            val t = c.title(id)
            Top(t?.title ?: "?", t?.let { listOfNotNull(it.year?.toString(), it.country.ifBlank { null }).joinToString(" · ") }.orEmpty(), l.sumOf { it.minutes }, l.size, t?.cover, t?.genre)
        }.sortedByDescending { it.minutes }
    }

    // ---------------------------------------------------------------- records

    val longestFilm: Ticket? = films.maxByOrNull { it.minutes }?.takeIf { it.minutes > 0 }
    val busiestDay: Pair<Long, Long>? = moments.groupBy { dayOf(it.first) }.mapValues { e -> e.value.sumOf { it.second.toLong() } }.maxByOrNull { it.value }?.let { startOfDay(it.key) to it.value }
    /** The most episodes in one day. */
    val binge: Pair<Long, Int>? = episodes.groupingBy { dayOf(it.at) }.eachCount().maxByOrNull { it.value }?.takeIf { it.value >= 2 }?.let { startOfDay(it.key) to it.value }
    val mostRewatched: Pair<String, Int>? = c.seats.filter { !it.season && inside(it.at) }.groupingBy { it.title.lowercase() }.eachCount().maxByOrNull { it.value }
        ?.takeIf { it.value >= 2 }?.let { e -> (c.seats.first { it.title.lowercase() == e.key }.title) to e.value }

    companion object {
        val LENGTHS = intArrayOf(90, 120, 150)

        /** Days with something watched, ever: epoch day → minutes. */
        fun byDay(c: Cinema): Map<Long, Long> {
            val cal = Calendar.getInstance()
            fun d(ms: Long) = (ms + cal.timeZone.getOffset(ms)).floorDiv(DAY)
            val m = HashMap<Long, Long>()
            for (t in c.seats) if (!t.season || t.whole) m.merge(d(t.at), t.minutes.coerceAtLeast(1).toLong(), Long::plus)
            for (e in c.episodes) m.merge(d(e.at), e.minutes.coerceAtLeast(1).toLong(), Long::plus)
            return m
        }

        /** Days in a row with something watched: the best run, and the one going now. */
        fun streaks(c: Cinema): Pair<Int, Int> {
            val days = byDay(c).keys.sorted()
            var best = 0; var run = 0; var prev = Long.MIN_VALUE
            for (d in days) { run = if (d == prev + 1) run + 1 else 1; best = maxOf(best, run); prev = d }
            val cal = Calendar.getInstance()
            val today = (c.now + cal.timeZone.getOffset(c.now)).floorDiv(DAY)
            val set = days.toHashSet()
            var d = if (today in set) today else today - 1
            var now = 0
            while (d in set) { now++; d-- }
            return best to now
        }
    }
}
