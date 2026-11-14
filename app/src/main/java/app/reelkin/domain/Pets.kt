package app.reelkin.domain

import app.reelkin.data.Company
import app.reelkin.data.Credits
import app.reelkin.data.Form
import app.reelkin.data.Genre
import app.reelkin.data.Nature
import app.reelkin.data.Pet
import app.reelkin.data.Species
import app.reelkin.data.Stage
import app.reelkin.ui.art.hash
import java.util.Calendar
import kotlin.random.Random

/** Something watched, film or episode, as the pets eat it. */
class Watch(
    val at: Long,
    val minutes: Int,
    val genre: Genre,
    val year: Int?,
    val country: String,
    val episode: Boolean = false,
    val season: Boolean = false,
    val company: Company = Company.ALONE,
    val credits: Credits = Credits.NOTHING,
    val rewatch: Boolean = false,
    val words: Boolean = false,
    /** The first film from its country. */
    val newCountry: Boolean = false,
    val newDecade: Boolean = false,
    /** On a day with three hours and more in front of the screen. */
    val longDay: Boolean = false,
    val hour: Int = 20,
    val stars: Int = 0,
    /** January is 0. */
    val month: Int = 0,
    val weekend: Boolean = false,
    /** Whole days since anything was watched before it. */
    val gap: Int = 0,
    /** Something was watched the day before too. */
    val running: Boolean = false,
    /** Animation from Japan. */
    val anime: Boolean = false,
    /** It came out the year it was watched. */
    val fresh: Boolean = false,
    /** For anime: whole days since the anime before it. */
    val animeGap: Int = 0,
) {
    val late get() = hour >= 23 || hour < 4
    /** A full-length film, not an episode or a season. */
    val film get() = !episode && !season
}

/** A pet as it stands now. [favMin]: minutes of what it loves; [found]: new countries and decades seen while it was out. */
class PetState(val pet: Pet, val fed: Int, val stage: Stage, val byGenre: Map<Genre, Int>, val favMin: Int, val favCount: Int, val found: Int, val minutes: Int) {
    val species get() = pet.species
    val egg get() = stage == Stage.EGG
    /** Popcorn its tastes brought: five for each favourite film, one for a favourite episode, five for a find if it's curious. */
    val bonus: Int get() = favCount + if (pet.nature == Nature.CURIOUS) found * 5 else 0
    /** The form it leans to by what it ate. */
    val leaning: Form? get() = byGenre.entries.groupBy({ it.key.form }, { it.value }).mapValues { it.value.sum() }.maxByOrNull { it.value }?.key
    val form: Form? get() = pet.form ?: if (stage >= Stage.ADULT) leaning else null
    /** 0..1 through the current stage. */
    val progress: Float get() {
        val next = stage.next ?: return 1f
        return ((fed - stage.from).toFloat() / (next.from - stage.from)).coerceIn(0f, 1f)
    }
    val left: Int get() = stage.next?.let { (it.from - fed).coerceAtLeast(0) } ?: 0
    val out get() = pet.spans.endsWith("-0")
}

/** How near a can is to opening. */
data class Hint(val species: Species, val have: Int, val need: Int) {
    val open get() = have >= need
}

object Pets {
    /** The genres a species is especially fond of, if any. */
    fun genres(sp: Species): Set<Genre> = when (sp) {
        Species.CAT -> setOf(Genre.NOIR, Genre.DRAMA)
        Species.FOX -> setOf(Genre.ANIMATION)
        Species.OWL -> setOf(Genre.DOC)
        Species.BAT -> setOf(Genre.HORROR)
        Species.COON -> setOf(Genre.COMEDY, Genre.REALITY)
        Species.HAMSTER -> setOf(Genre.SCIFI)
        Species.ARMADILLO -> setOf(Genre.WESTERN)
        Species.LOVEBIRD -> setOf(Genre.ROMANCE)
        Species.MONKEY -> setOf(Genre.ADVENTURE)
        Species.FROG -> setOf(Genre.MUSICAL)
        else -> emptySet()
    }

    /** Whether a watch feeds [sp] especially well. */
    fun favourite(sp: Species, w: Watch): Boolean = when (sp) {
        Species.OWL -> w.genre == Genre.DOC || w.words
        Species.MOTH -> w.hour >= 23 || w.hour < 4
        Species.SLOTH -> w.longDay
        Species.PENGUIN -> (w.year ?: 9999) < 1970 && !w.episode
        Species.REDPANDA -> w.newCountry
        Species.PUG -> w.company != Company.ALONE && !w.episode
        Species.MOUSE -> w.credits == Credits.SCENE
        Species.GOLDFISH -> w.rewatch
        Species.SQUIRREL -> w.episode
        Species.GHOST -> w.company == Company.ALONE && (w.hour >= 22 || w.hour < 4)
        Species.DRAGON -> !w.episode && w.minutes >= 150
        Species.UNICORN -> w.stars == 5
        Species.PHOENIX -> w.gap >= 3
        Species.KITSUNE -> eastAsia(w.country)
        Species.YETI -> w.month == 11 || w.month <= 1
        Species.GRIFFIN -> w.newDecade
        Species.GOLEM -> !w.episode
        Species.ROBOT -> w.running
        Species.FAIRY -> w.company == Company.TWO
        Species.POPKIN -> w.stars >= 4
        Species.GARGOYLE -> w.weekend
        Species.SPHINX -> (w.year ?: 9999) < 1950
        Species.WISP -> w.minutes in 1..95
        Species.TANUKI -> w.anime
        Species.KOI -> w.anime && w.film
        Species.KAPPA -> w.anime && !w.film
        Species.NEKOMATA -> w.anime && w.late
        Species.TENGU -> w.anime && (w.year ?: 9999) < 2000
        Species.SLIME -> w.anime && w.fresh
        Species.MECHA -> w.anime && w.longDay
        Species.MANEKI -> w.anime && w.stars >= 4
        Species.DARUMA -> w.anime && w.rewatch
        Species.CRANE -> w.anime && w.words
        Species.SHIBA -> w.anime && w.running
        Species.ONI -> w.anime && w.weekend
        Species.ONIGIRI -> w.anime && w.company != Company.ALONE
        Species.KIRIN -> w.anime && w.stars == 5
        Species.RYU -> w.anime && w.film && w.minutes >= 120
        Species.SUZUME -> w.anime && w.hour in 5..11
        Species.BAKU -> w.anime && w.animeGap >= 7
        else -> w.genre in genres(sp)
    }

    private val EAST = listOf("япон", "japan", "коре", "korea", "китай", "china", "гонконг", "hong kong", "тайван", "taiwan")
    fun eastAsia(country: String): Boolean = country.lowercase().let { c -> c.isNotEmpty() && EAST.any { it in c } }

    /** Anime: animation from Japan. */
    fun anime(genre: Genre, country: String): Boolean = genre == Genre.ANIMATION && country.lowercase().let { "япон" in it || "japan" in it }

    /** Every film, whole season brought from elsewhere and episode, oldest first, with what the pets care about. */
    fun watches(c: Cinema): List<Watch> {
        val cal = Calendar.getInstance()
        fun dayOf(ms: Long): Int { cal.timeInMillis = ms; return (ms + cal.timeZone.getOffset(ms)).floorDiv(86_400_000L).toInt() }
        class Raw(val at: Long, val minutes: Int, val ticket: app.reelkin.data.Ticket?, val title: app.reelkin.data.Title?)
        val raw = ArrayList<Raw>()
        for (t in c.seats) if (!t.season || t.whole) raw += Raw(t.at, t.minutes, t, null)
        for (e in c.episodes) raw += Raw(e.at, e.minutes, null, c.title(e.series))
        raw.sortBy { it.at }
        val perDay = HashMap<Int, Int>()
        for (r in raw) perDay.merge(dayOf(r.at), r.minutes, Int::plus)
        val countries = HashSet<String>(); val decades = HashSet<Int>()
        var lastDay = Int.MIN_VALUE; var lastAnime = Int.MIN_VALUE
        return raw.map { r ->
            val t = r.ticket
            val country = (t?.country ?: r.title?.country).orEmpty().trim()
            val year = t?.year ?: r.title?.year
            val nc = t != null && country.isNotEmpty() && countries.add(country.lowercase())
            val nd = t != null && year != null && decades.add(Cinema.decade(year))
            val day = dayOf(r.at)
            val gap = if (lastDay == Int.MIN_VALUE) 0 else (day - lastDay - 1).coerceAtLeast(0)
            val running = lastDay == day - 1 || (lastDay == day && perDay.containsKey(day - 1))
            lastDay = day
            val genre = t?.genre ?: r.title?.genre ?: Genre.DRAMA
            val anime = anime(genre, country)
            val animeGap = if (!anime || lastAnime == Int.MIN_VALUE) 0 else (day - lastAnime - 1).coerceAtLeast(0)
            if (anime) lastAnime = day
            cal.timeInMillis = r.at
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            Watch(r.at, r.minutes, genre, year, country, episode = t == null, season = t?.season == true,
                company = t?.company ?: Company.ALONE, credits = t?.credits ?: Credits.NOTHING, rewatch = t?.rewatch == true,
                words = t?.words?.isNotBlank() == true, newCountry = nc, newDecade = nd, longDay = (perDay[day] ?: 0) >= 180,
                hour = cal.get(Calendar.HOUR_OF_DAY), stars = t?.stars ?: 0, month = cal.get(Calendar.MONTH),
                weekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY, gap = gap, running = running,
                anime = anime, fresh = year != null && year >= cal.get(Calendar.YEAR), animeGap = animeGap)
        }
    }

    private fun spans(p: Pet): List<LongRange> = p.spans.split(',').mapNotNull { r ->
        val a = r.split('-'); if (a.size != 2) return@mapNotNull null
        val from = a[0].toLongOrNull() ?: return@mapNotNull null
        val to = a[1].toLongOrNull()?.takeIf { it > 0 } ?: Long.MAX_VALUE
        from..to
    }

    fun state(p: Pet, ws: List<Watch>): PetState {
        val ranges = spans(p)
        var min = 0; var fav = 0; var favN = 0; var found = 0
        val g = HashMap<Genre, Int>()
        for (w in ws) if (ranges.any { w.at in it }) {
            min += w.minutes
            g.merge(w.genre, w.minutes, Int::plus)
            if (w.newCountry) found++
            if (w.newDecade) found++
            if (favourite(p.species, w)) { fav += w.minutes; favN += if (w.episode) 1 else 5 }
        }
        // A favourite counts half again; a greedy one eats a tenth more of everything.
        val greedy = if (p.nature == Nature.GREEDY) 1.1 else 1.0
        val fed = ((min + fav / 2) * greedy).toInt()
        return PetState(p, fed, Stage.entries.last { fed >= it.from }, g, fav, favN, found, min)
    }

    /** How near each can is to opening, from everything ever watched; the three starters are always open. */
    fun hints(c: Cinema, ws: List<Watch>): List<Hint> {
        val films = ws.filter { !it.episode && !it.season }
        fun genre(vararg g: Genre) = films.count { it.genre in g }
        val cal = Calendar.getInstance()
        fun day(ms: Long): Int = (ms + cal.timeZone.getOffset(ms)).floorDiv(86_400_000L).toInt()
        val lateDays = ws.filter { it.hour >= 23 || it.hour < 4 }.map { day(it.at) }.distinct().size
        val bingeDays = ws.filter { it.episode }.groupingBy { day(it.at) }.eachCount().count { it.value >= 4 }
        // The longest run of days in a row with something watched.
        var streak = 0; var run = 0; var prev = Int.MIN_VALUE
        for (d in ws.map { day(it.at) }.distinct().sorted()) { run = if (d == prev + 1) run + 1 else 1; streak = maxOf(streak, run); prev = d }
        val aw = ws.filter { it.anime }
        var animeStreak = 0; run = 0; prev = Int.MIN_VALUE
        for (d in aw.map { day(it.at) }.distinct().sorted()) { run = if (d == prev + 1) run + 1 else 1; animeStreak = maxOf(animeStreak, run); prev = d }
        fun days(l: List<Watch>) = l.map { day(it.at) }.distinct().size
        return Species.entries.map { sp ->
            when (sp) {
                Species.CAT, Species.FOX, Species.OWL -> Hint(sp, 1, 1)
                Species.BAT -> Hint(sp, genre(Genre.HORROR), 5)
                Species.COON -> Hint(sp, genre(Genre.COMEDY, Genre.REALITY), 5)
                Species.HAMSTER -> Hint(sp, genre(Genre.SCIFI), 5)
                Species.ARMADILLO -> Hint(sp, genre(Genre.WESTERN), 3)
                Species.LOVEBIRD -> Hint(sp, genre(Genre.ROMANCE), 5)
                Species.MONKEY -> Hint(sp, genre(Genre.ADVENTURE), 5)
                Species.FROG -> Hint(sp, genre(Genre.MUSICAL), 3)
                Species.MOTH -> Hint(sp, lateDays, 5)
                Species.SLOTH -> Hint(sp, bingeDays, 3)
                Species.PENGUIN -> Hint(sp, films.count { (it.year ?: 9999) < 1970 }, 5)
                Species.REDPANDA -> Hint(sp, c.countries.size, 8)
                Species.PUG -> Hint(sp, films.count { it.company != Company.ALONE }, 10)
                Species.MOUSE -> Hint(sp, c.scenes.size, 3)
                Species.GOLDFISH -> Hint(sp, films.count { it.rewatch }, 3)
                Species.SQUIRREL -> Hint(sp, c.seats.count { it.season }, 3)
                Species.GHOST -> Hint(sp, films.count { it.company == Company.ALONE && it.hour < 4 }, 3)
                Species.DRAGON -> Hint(sp, films.count { it.minutes >= 150 }, 3)
                Species.UNICORN -> Hint(sp, films.count { it.stars == 5 }, 10)
                Species.PHOENIX -> Hint(sp, ws.count { it.gap >= 30 }, 1)
                Species.KITSUNE -> Hint(sp, films.count { eastAsia(it.country) }, 3)
                Species.YETI -> Hint(sp, films.count { it.month == 11 || it.month <= 1 }, 10)
                Species.GRIFFIN -> Hint(sp, c.decades.size, 6)
                Species.GOLEM -> Hint(sp, films.size, 100)
                Species.ROBOT -> Hint(sp, streak, 7)
                Species.FAIRY -> Hint(sp, films.count { it.company == Company.TWO }, 5)
                Species.POPKIN -> Hint(sp, c.s.bought.size, 2)
                Species.GARGOYLE -> Hint(sp, films.count { it.weekend }, 10)
                Species.SPHINX -> Hint(sp, films.count { (it.year ?: 9999) < 1950 }, 3)
                Species.WISP -> Hint(sp, ws.map { day(it.at) }.distinct().size, 30)
                Species.TANUKI -> Hint(sp, aw.size, 5)
                Species.KOI -> Hint(sp, aw.count { it.film }, 3)
                // A season brought from elsewhere stands for a dozen episodes.
                Species.KAPPA -> Hint(sp, aw.sumOf { if (it.season) 12 else if (it.episode) 1 else 0 }, 12)
                Species.NEKOMATA -> Hint(sp, days(aw.filter { it.late }), 3)
                Species.TENGU -> Hint(sp, aw.count { (it.year ?: 9999) < 2000 }, 3)
                Species.SLIME -> Hint(sp, aw.count { it.fresh }, 5)
                Species.MECHA -> Hint(sp, days(aw.filter { it.longDay }), 2)
                Species.MANEKI -> Hint(sp, aw.count { it.stars >= 4 }, 5)
                Species.DARUMA -> Hint(sp, aw.count { it.rewatch }, 2)
                Species.CRANE -> Hint(sp, aw.count { it.words }, 3)
                Species.SHIBA -> Hint(sp, animeStreak, 5)
                Species.ONI -> Hint(sp, days(aw.filter { it.weekend }), 5)
                Species.ONIGIRI -> Hint(sp, aw.count { it.company != Company.ALONE }, 3)
                Species.KIRIN -> Hint(sp, aw.count { it.stars == 5 }, 3)
                Species.RYU -> Hint(sp, aw.count { it.film && it.minutes >= 120 }, 2)
                Species.SUZUME -> Hint(sp, days(aw.filter { it.hour in 5..11 }), 3)
                Species.BAKU -> Hint(sp, aw.count { it.animeGap >= 14 }, 1)
            }
        }
    }

    /** A new pet out of its can: one in sixteen in technicolor, its nature by chance. */
    fun hatch(sp: Species, now: Long): Pet = Pet(species = sp, laid = now, shiny = hash(sp.name + now) % 16 == 0L,
        nature = Nature.entries[Random(now).nextInt(Nature.entries.size)])

    /** Puts [p] in the booth and every other pet out of it; returns the pets that changed. */
    fun swap(all: List<Pet>, p: Pet, now: Long): List<Pet> = all.mapNotNull { o ->
        val out = o.spans.endsWith("-0")
        when {
            o.id == p.id && !out -> o.copy(spans = "${o.spans},$now-0")
            o.id != p.id && out -> o.copy(spans = o.spans.dropLast(1) + now)
            else -> null
        }
    }
}
