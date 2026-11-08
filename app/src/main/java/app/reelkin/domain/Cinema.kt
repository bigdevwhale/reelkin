package app.reelkin.domain

import app.reelkin.data.Credits
import app.reelkin.data.Episode
import app.reelkin.data.Genre
import app.reelkin.data.Kind
import app.reelkin.data.Settings
import app.reelkin.data.Species
import app.reelkin.data.Status
import app.reelkin.data.Ticket
import app.reelkin.data.Title
import app.reelkin.data.Trait
import app.reelkin.ui.art.hash
import java.util.Calendar

/** The shop: what each thing costs in popcorn. */
enum class Decor(val price: Int) { GARLAND(40), CHAIR(60), NEON(50), POPCORN(80) }

/** Everything the screens read about the cinema, made once per change. */
class Cinema(val titles: List<Title>, val tickets: List<Ticket>, val episodes: List<Episode>, val s: Settings, val now: Long) {
    /** Seat i belongs to the i-th stub, in the order they were torn. */
    val seats = tickets.sortedBy { it.at }

    /** Minutes watched: films, every episode, and seasons watched before they came here (other season stubs aren't counted twice). */
    val minutes: Long = seats.filter { !it.season || it.whole }.sumOf { it.minutes.toLong() } + episodes.sumOf { it.minutes.toLong() }

    val earned: Int = seats.sumOf { if (it.season) 15 else 10 + (if (it.credits == Credits.SCENE) 5 else 0) + (if (it.rewatch) 5 else 0) } + episodes.size * 3
    val popcorn: Int = earned - s.spent

    val byGenre: Map<Genre, Int> = seats.groupingBy { it.genre }.eachCount()
    val traits: Set<Trait> = byGenre.filter { it.value >= TRAIT_AT }.keys.mapNotNull { it.trait }.toSet()
    val worn: Set<Trait> = traits - s.off

    val stage: Int = stageOf(minutes)

    /** How many of each first letter you own, for the sign. */
    val letters: Map<Char, Int> = seats.mapNotNull { letterOf(it.title) }.groupingBy { it }.eachCount()

    val decades: Set<Int> = seats.mapNotNull { it.year }.map { decade(it) }.toSet()
    val countries: List<String> = seats.map { it.country.trim() }.filter { it.isNotEmpty() }.distinct()
    val scenes = seats.filter { it.credits == Credits.SCENE }

    val watch = titles.filter { it.status == Status.WATCH }.sortedByDescending { it.added }
    val running = titles.filter { it.kind == Kind.SERIES && it.status == Status.RUNNING }
    val shelved = titles.filter { it.kind == Kind.SERIES && it.status == Status.SHELVED }

    /** The film can opens after the third stub. */
    val egg = s.species == null
    val hatchReady = egg && seats.size >= HATCH_AT
    val species: Species = s.species ?: speciesFor(seats)

    val favourite: Ticket? = seats.lastOrNull { it.stars == 5 && !it.season } ?: seats.lastOrNull()

    val year: Int = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR)
    fun yearOf(ms: Long): Int = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.YEAR)
    val thisYear = seats.filter { yearOf(it.at) == year }

    /** Watched days of this year: day of year → how much. */
    val days: Map<Int, Int> by lazy {
        val m = HashMap<Int, Int>()
        val cal = Calendar.getInstance()
        fun add(ms: Long, n: Int) { cal.timeInMillis = ms; if (cal.get(Calendar.YEAR) == year) m.merge(cal.get(Calendar.DAY_OF_YEAR), n, Int::plus) }
        for (t in seats) add(t.at, 2)
        for (e in episodes) add(e.at, 1)
        m
    }

    fun title(id: Long) = titles.firstOrNull { it.id == id }
    private val seatIndex: Map<Long, Int> by lazy { HashMap<Long, Int>(seats.size * 2).also { m -> seats.forEachIndexed { i, t -> m[t.id] = i } } }
    fun seatOf(t: Ticket) = seatIndex[t.id] ?: -1
    fun has(d: Decor) = d.name in s.bought

    /** The next trait one film away, if any. */
    val nearTrait: Genre? = Genre.entries.firstOrNull { it.trait != null && it.trait !in traits && byGenre[it] == TRAIT_AT - 1 }

    companion object {
        const val TRAIT_AT = 3
        const val HATCH_AT = 3
        val STEPS = longArrayOf(0, 0, 2_000, 8_000)
        const val SEATS = 100
        val DECADES = (1900..2020 step 10).toList()

        fun stageOf(minutes: Long) = if (minutes >= STEPS[3]) 3 else if (minutes >= STEPS[2]) 2 else 1
        fun decade(y: Int) = y / 10 * 10
        fun letterOf(t: String): Char? = t.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.let { if (it == 'Ё') 'Е' else it }?.takeIf { it.isLetter() }

        /** The film can's guess from the first three films: the most common genre, earliest first on a tie. */
        fun speciesFor(seats: List<Ticket>): Species {
            val first = seats.take(HATCH_AT)
            if (first.isEmpty()) return Species.CAT
            val g = first.groupingBy { it.genre }.eachCount().let { c -> first.map { it.genre }.maxBy { c[it] ?: 0 } }
            return when (g) {
                Genre.NOIR -> Species.CAT
                Genre.HORROR, Genre.SCIFI -> Species.BAT
                Genre.ANIMATION, Genre.ROMANCE, Genre.MUSICAL -> Species.FOX
                Genre.COMEDY, Genre.WESTERN, Genre.ADVENTURE, Genre.REALITY -> Species.COON
                Genre.DOC, Genre.DRAMA -> Species.OWL
            }
        }

        /** One in sixteen comes out in technicolor, decided by the first stub so it never changes. */
        fun shinyFor(seats: List<Ticket>): Boolean = seats.firstOrNull()?.let { hash(it.title + it.at) % 16 == 0L } ?: false
    }
}

/** What a new stub brought, for the reward sheet. */
data class Gains(
    val seat: Int,
    val letters: List<Char>,
    val decades: List<Int>,
    val countries: List<String>,
    val traits: List<Trait>,
    val grew: Int?,
    val scene: Boolean,
    val popcorn: Int,
    val minutes: Long,
)

fun gains(before: Cinema, after: Cinema, t: Ticket): Gains = Gains(
    seat = after.seatOf(t),
    letters = after.letters.keys.filter { it !in before.letters }.sorted(),
    decades = (after.decades - before.decades).sorted(),
    countries = after.countries - before.countries.toSet(),
    traits = (after.traits - before.traits).toList(),
    grew = after.stage.takeIf { it > before.stage },
    scene = t.credits == Credits.SCENE,
    popcorn = after.earned - before.earned,
    minutes = after.minutes - before.minutes,
)
