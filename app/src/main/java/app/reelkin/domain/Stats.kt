package app.reelkin.domain

import app.reelkin.data.Company
import app.reelkin.data.Credits
import app.reelkin.data.Genre
import app.reelkin.data.Ticket
import java.util.Calendar

/** What was watched in [year], or over all the years when it's null: films, seasons, episodes, in numbers. */
class Stats(c: Cinema, val year: Int?) {
    private fun yearOf(ms: Long) = Browse.yearOf(ms)

    val stubs: List<Ticket> = c.seats.filter { year == null || yearOf(it.at) == year }
    val films: List<Ticket> = stubs.filter { !it.season }
    val seasons: Int = stubs.count { it.season }
    val episodes = c.episodes.filter { year == null || yearOf(it.at) == year }

    /** Films, every episode and the seasons watched before they came here; other season stubs aren't counted twice. */
    val minutes: Long = stubs.filter { !it.season || it.whole }.sumOf { it.minutes.toLong() } + episodes.sumOf { it.minutes.toLong() }
    val hours: Long = minutes / 60

    val rated = films.filter { it.stars > 0 }
    val avgPopcorn: Double? = rated.takeIf { it.isNotEmpty() }?.map { it.stars }?.average()
    val avgLength: Int? = films.filter { it.minutes > 0 }.takeIf { it.isNotEmpty() }?.map { it.minutes }?.average()?.toInt()
    val countries: List<Pair<String, Int>> = films.map { it.country.trim() }.filter { it.isNotEmpty() }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
    val rewatches = films.count { it.rewatch }
    val scenes = films.count { it.credits == Credits.SCENE }

    /** Films month by month in [year], January first. */
    val byMonth: List<Int> = IntArray(12).also { a -> val cal = Calendar.getInstance(); films.forEach { cal.timeInMillis = it.at; a[cal.get(Calendar.MONTH)]++ } }.toList()

    /** Films year by year, from the first one to [now]'s year, with the empty years in between. */
    val byYear: List<Pair<Int, Int>> = run {
        val n = films.groupingBy { yearOf(it.at) }.eachCount()
        if (n.isEmpty()) emptyList() else (n.keys.min()..maxOf(n.keys.max(), c.year)).map { it to (n[it] ?: 0) }
    }

    val genres: List<Pair<Genre, Int>> = films.groupingBy { it.genre }.eachCount().toList().sortedByDescending { it.second }

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

    /** Everything watched by weekday, Monday first: films and episodes. */
    val weekdays: List<Int> = IntArray(7).also { a ->
        val cal = Calendar.getInstance()
        fun add(ms: Long) { cal.timeInMillis = ms; a[(cal.get(Calendar.DAY_OF_WEEK) + 5) % 7]++ }
        films.forEach { add(it.at) }; episodes.forEach { add(it.at) }
    }.toList()

    companion object {
        val LENGTHS = intArrayOf(90, 120, 150)
    }
}
