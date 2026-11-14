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
import app.reelkin.data.Pet
import app.reelkin.data.DAY
import app.reelkin.data.Stage
import java.util.Calendar

/** The shop: what each thing costs in popcorn. */
enum class Decor(val price: Int) { GARLAND(40), CHAIR(60), NEON(50), POPCORN(80), RUG(70), PLANT(60), CLOCK(90) }

/** The booth's wallpaper: plum from the start, the rest bought in the buffet. [file] is its picture in resources/booth. */
enum class Wall(val price: Int, val file: String) { PLUM(0, "room"), GREEN(150, "room_green"), NAVY(150, "room_navy"), DECO(200, "room_deco"), BRICK(150, "room_brick") }

/** Everything the screens read about the cinema, made once per change. */
class Cinema(val titles: List<Title>, val tickets: List<Ticket>, val episodes: List<Episode>, val s: Settings, val now: Long, val petList: List<Pet> = emptyList(), val goals: List<app.reelkin.data.Goal> = emptyList()) {
    /** Every stub, in the order they were torn: what the diary shows. */
    val diary = tickets.sortedBy { it.at }

    /** The stubs that count, the seasons only noted down as watched long ago left out. Seat i belongs to the i-th. */
    val seats = diary.filter { !it.past }

    /** Minutes watched: films, every episode, and seasons watched before they came here (other season stubs aren't counted twice). */
    val minutes: Long = seats.filter { !it.season || it.whole }.sumOf { it.minutes.toLong() } + episodes.sumOf { it.minutes.toLong() }

    val byGenre: Map<Genre, Int> = seats.groupingBy { it.genre }.eachCount()

    /** How many of each first letter you own, for the sign. */
    val letters: Map<Char, Int> = seats.mapNotNull { letterOf(it.title) }.groupingBy { it }.eachCount()

    val decades: Set<Int> = seats.mapNotNull { it.year }.map { decade(it) }.toSet()
    val countries: List<String> = seats.map { it.country.trim() }.filter { it.isNotEmpty() }.distinct()
    val scenes = seats.filter { it.credits == Credits.SCENE }

    /** Seasons of the series [title] before [before] with no stub: not marked as watched. */
    fun missingSeasons(title: String, before: Int): List<Int> {
        val torn = diary.filter { it.season }.map { it.title }.toHashSet()
        return (1 until before).filter { n -> "$title · сезон $n" !in torn && "$title · season $n" !in torn }
    }

    val watch = titles.filter { it.status == Status.WATCH }.sortedByDescending { it.added }
    val running = titles.filter { it.kind == Kind.SERIES && it.status == Status.RUNNING }
    val shelved = titles.filter { it.kind == Kind.SERIES && it.status == Status.SHELVED }

    /** Everything watched as the pets eat it. */
    val watches: List<Watch> by lazy { Pets.watches(this) }
    val pets: List<PetState> by lazy { petList.map { Pets.state(it, watches) } }
    /** The pet in the booth, if any. */
    val pet: PetState? by lazy { pets.firstOrNull { it.pet.id == s.pet && it.out } ?: pets.lastOrNull { it.out } }
    val hints: List<Hint> by lazy { Pets.hints(this, watches) }
    fun open(sp: Species) = hints.first { it.species == sp }.open
    /** Cans open and not yet taken. */
    val newCans: List<Species> by lazy { hints.filter { it.open && pets.none { p -> p.species == it.species } }.map { it.species } }
    /** The can nearest to opening, one or two films away. */
    val nearCan: Hint? by lazy { hints.filter { !it.open && it.need - it.have <= 2 && pets.none { p -> p.species == it.species } }.minByOrNull { it.need - it.have } }
    /** The pet's name: the one given, or the default in the phone's language. */
    fun name(p: PetState?, ru: Boolean): String = p?.pet?.name?.ifBlank { null } ?: if (p?.species?.let { it == s.species } == true && s.petName.isNotBlank()) s.petName else Words.defaultName(p?.species, ru)

    /** 0 wide awake .. 1 fast asleep: days without films make the pet drowsy. */
    val windup: Float by lazy {
        val last = maxOf(seats.lastOrNull()?.at ?: 0L, episodes.maxOfOrNull { it.at } ?: 0L)
        if (last == 0L) 0f else (((now - last) / DAY.toFloat() - 3f) / 7f).coerceIn(0f, 1f)
    }

    val earned: Int by lazy { seats.sumOf { if (it.season) 15 else 10 + (if (it.credits == Credits.SCENE) 5 else 0) + (if (it.rewatch) 5 else 0) } + episodes.size * 3 + pets.sumOf { it.bonus } +
        claimedQuests + feats.sumOf { it.level } * Feats.REWARD + goals.count { it.period == app.reelkin.data.Period.ONCE && Goals.progress(this, it).done } * Goals.REWARD }
    /** Achievements and how far each got. */
    val feats: List<FeatState> by lazy { Feats.of(this) }
    val weekId: String by lazy { Quests.weekId(this) }
    val quests: List<QuestState> by lazy { Quests.of(this) }
    /** Popcorn from tasks taken: this week's by their own price, older weeks' at twenty each. */
    private val claimedQuests: Int get() = quests.filter { "$weekId:${it.q.name}" in s.claimed }.sumOf { it.q.reward } + s.claimed.count { !it.startsWith("$weekId:") } * 20
    val popcorn: Int get() = earned - s.spent

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
    fun has(w: Wall) = w == Wall.PLUM || "WALL_" + w.name in s.bought
    val wall: Wall get() = Wall.entries.firstOrNull { it.name == s.wall && has(it) } ?: Wall.PLUM

    companion object {
        const val SEATS = 100
        val DECADES = (1900..2020 step 10).toList()

        fun decade(y: Int) = y / 10 * 10
        fun letterOf(t: String): Char? = t.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.let { if (it == 'Ё') 'Е' else it }?.takeIf { it.isLetter() }
    }
}

/** What a new stub brought, for the reward sheet. */
data class Gains(
    val seat: Int,
    val letters: List<Char>,
    val decades: List<Int>,
    val countries: List<String>,
    /** Cans that opened. */
    val cans: List<Species>,
    val grew: Stage?,
    val scene: Boolean,
    val popcorn: Int,
    val minutes: Long,
    /** The pet loved it. */
    val fav: Boolean = false,
)

fun gains(before: Cinema, after: Cinema, t: Ticket): Gains = Gains(
    seat = after.seatOf(t),
    letters = after.letters.keys.filter { it !in before.letters }.sorted(),
    decades = (after.decades - before.decades).sorted(),
    countries = after.countries - before.countries.toSet(),
    cans = after.newCans - before.newCans.toSet(),
    grew = after.pet?.stage?.takeIf { b -> before.pet?.let { it.pet.id == after.pet?.pet?.id && b > it.stage } == true },
    scene = t.credits == Credits.SCENE,
    popcorn = after.earned - before.earned,
    fav = after.pet?.let { a -> before.pets.firstOrNull { it.pet.id == a.pet.id }?.let { a.favCount > it.favCount } } == true,
    minutes = after.minutes - before.minutes,
)
