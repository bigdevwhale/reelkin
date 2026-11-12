package app.reelkin.domain

import app.reelkin.data.Company
import app.reelkin.data.DAY
import app.reelkin.data.Genre
import app.reelkin.data.Goal
import app.reelkin.data.GoalKind
import app.reelkin.data.Period
import app.reelkin.ui.art.hash
import java.util.Calendar

data class Progress(val have: Int, val need: Int) {
    val done get() = have >= need
    val part get() = if (need <= 0) 1f else (have.toFloat() / need).coerceIn(0f, 1f)
}

/** What was watched between [from] and [to]: the films, seasons and episodes the goals and tasks count. */
class Window(val c: Cinema, val from: Long, val to: Long) {
    val films = c.seats.filter { !it.season && it.at in from until to }
    val seasons = c.seats.filter { it.season && it.at in from until to }
    val episodes = c.episodes.filter { it.at in from until to }
    val minutes: Int get() = films.sumOf { it.minutes } + seasons.filter { it.whole }.sumOf { it.minutes } + episodes.sumOf { it.minutes }
    val days: Int get() {
        val cal = Calendar.getInstance()
        fun d(ms: Long) = (ms + cal.timeZone.getOffset(ms)).floorDiv(DAY)
        return (films.map { d(it.at) } + episodes.map { d(it.at) }).toSet().size
    }
    private val before = c.seats.filter { !it.season && it.at < from }
    val newCountries: Int get() { val old = before.map { it.country.trim().lowercase() }.toSet(); return films.map { it.country.trim().lowercase() }.filter { it.isNotEmpty() && it !in old }.toSet().size }
    val newDecades: Int get() { val old = before.mapNotNull { it.year?.let(Cinema::decade) }.toSet(); return films.mapNotNull { it.year?.let(Cinema::decade) }.filter { it !in old }.toSet().size }
    val genres: Int get() = films.map { it.genre }.toSet().size
    /** The longest run of days in a row with something watched. */
    val streak: Int get() {
        val cal = Calendar.getInstance()
        fun d(ms: Long) = (ms + cal.timeZone.getOffset(ms)).floorDiv(DAY)
        var best = 0; var run = 0; var prev = Long.MIN_VALUE
        for (x in (films.map { d(it.at) } + episodes.map { d(it.at) }).toSortedSet()) { run = if (x == prev + 1) run + 1 else 1; best = maxOf(best, run); prev = x }
        return best
    }
}

object Goals {
    private fun weekStart(now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = now; firstDayOfWeek = Calendar.MONDAY
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        if (timeInMillis > now) add(Calendar.DAY_OF_YEAR, -7)
    }.timeInMillis

    fun window(c: Cinema, g: Goal): Window {
        val cal = Calendar.getInstance().apply { timeInMillis = c.now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val from = when (g.period) {
            Period.WEEK -> weekStart(c.now)
            Period.MONTH -> cal.apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
            Period.YEAR -> cal.apply { set(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
            Period.ONCE -> g.created
        }
        return Window(c, from, if (g.period == Period.ONCE && g.until > 0) g.until else Long.MAX_VALUE)
    }

    fun progress(c: Cinema, g: Goal): Progress {
        val w = window(c, g)
        val have = when (g.kind) {
            GoalKind.FILMS -> w.films.size
            GoalKind.HOURS -> w.minutes / 60
            GoalKind.EPISODES -> w.episodes.size
            GoalKind.SEASONS -> w.seasons.size
            GoalKind.GENRE -> w.films.count { it.genre.name == g.ref }
            GoalKind.NEW_COUNTRIES -> w.newCountries
            GoalKind.NEW_DECADES -> w.newDecades
            GoalKind.DAYS -> w.days
            GoalKind.FIVE_STARS -> w.films.count { it.stars == 5 }
            GoalKind.STREAK -> w.streak
            GoalKind.OLD -> w.films.count { (it.year ?: 9999) < 1980 }
            GoalKind.WORDS -> w.films.count { it.words.isNotBlank() }
            GoalKind.COMPANY -> w.films.count { it.company != Company.ALONE }
            GoalKind.REWATCHES -> w.films.count { it.rewatch }
            GoalKind.SCENES -> w.films.count { it.credits == app.reelkin.data.Credits.SCENE }
            GoalKind.LONG -> w.films.count { it.minutes >= 150 }
            GoalKind.RATED -> w.films.count { it.stars > 0 }
        }
        return Progress(have, g.target)
    }

    /** One-off goals reached pay this much popcorn. */
    const val REWARD = 15

    /** The catalogue of goals to pick from, a group at a time: (group, goals). */
    fun catalogue(): List<Pair<Int, List<Goal>>> = listOf(
        0 to listOf(
            Goal(kind = GoalKind.FILMS, target = 4, period = Period.MONTH), Goal(kind = GoalKind.FILMS, target = 52, period = Period.YEAR),
            Goal(kind = GoalKind.HOURS, target = 10, period = Period.MONTH), Goal(kind = GoalKind.HOURS, target = 150, period = Period.YEAR),
            Goal(kind = GoalKind.EPISODES, target = 20, period = Period.MONTH), Goal(kind = GoalKind.SEASONS, target = 6, period = Period.YEAR),
        ),
        1 to listOf(
            Goal(kind = GoalKind.NEW_COUNTRIES, target = 3, period = Period.YEAR), Goal(kind = GoalKind.NEW_DECADES, target = 2, period = Period.YEAR),
            Goal(kind = GoalKind.OLD, target = 5, period = Period.YEAR), Goal(kind = GoalKind.LONG, target = 5, period = Period.YEAR),
            Goal(kind = GoalKind.GENRE, target = 5, period = Period.YEAR, ref = Genre.HORROR.name), Goal(kind = GoalKind.GENRE, target = 5, period = Period.YEAR, ref = Genre.DOC.name),
            Goal(kind = GoalKind.GENRE, target = 5, period = Period.YEAR, ref = Genre.ANIMATION.name), Goal(kind = GoalKind.GENRE, target = 3, period = Period.YEAR, ref = Genre.WESTERN.name),
        ),
        2 to listOf(
            Goal(kind = GoalKind.DAYS, target = 3, period = Period.WEEK), Goal(kind = GoalKind.STREAK, target = 7, period = Period.YEAR),
            Goal(kind = GoalKind.COMPANY, target = 4, period = Period.MONTH), Goal(kind = GoalKind.REWATCHES, target = 3, period = Period.YEAR),
            Goal(kind = GoalKind.SCENES, target = 3, period = Period.YEAR),
        ),
        3 to listOf(
            Goal(kind = GoalKind.RATED, target = 10, period = Period.MONTH), Goal(kind = GoalKind.WORDS, target = 5, period = Period.MONTH),
            Goal(kind = GoalKind.FIVE_STARS, target = 10, period = Period.YEAR),
        ),
    )

    fun group(n: Int, ru: Boolean) = when (n) {
        0 -> if (ru) "Сколько смотреть" else "How much"
        1 -> if (ru) "Открытия" else "Discoveries"
        2 -> if (ru) "Привычки" else "Habits"
        else -> if (ru) "Впечатления" else "Impressions"
    }

    /** The same goal, as far as the catalogue is concerned. */
    fun same(a: Goal, b: Goal) = a.kind == b.kind && a.period == b.period && a.ref == b.ref

    fun title(g: Goal, ru: Boolean): String {
        val n = g.target
        val what = when (g.kind) {
            GoalKind.FILMS -> Words.films(n, ru)
            GoalKind.HOURS -> if (ru) "$n " + Words.plural(n, "час", "часа", "часов") + " кино" else "$n hours of film"
            GoalKind.EPISODES -> if (ru) "$n " + Words.plural(n, "серия", "серии", "серий") else "$n episodes"
            GoalKind.SEASONS -> Words.seasons(n, ru) + if (ru) " досмотреть" else " finished"
            GoalKind.GENRE -> (Genre.entries.firstOrNull { it.name == g.ref }?.let { Words.genre(it, ru) } ?: "?") + ": " + Words.films(n, ru)
            GoalKind.NEW_COUNTRIES -> if (ru) "$n " + Words.plural(n, "новая страна", "новые страны", "новых стран") else "$n new countries"
            GoalKind.NEW_DECADES -> if (ru) "$n " + Words.plural(n, "новое десятилетие", "новых десятилетия", "новых десятилетий") else "$n new decades"
            GoalKind.DAYS -> if (ru) "кино $n " + Words.plural(n, "день", "дня", "дней") else "films on $n days"
            GoalKind.FIVE_STARS -> if (ru) "$n " + Words.plural(n, "фильм", "фильма", "фильмов") + " на пять попкорнов" else "$n five-popcorn films"
            GoalKind.STREAK -> if (ru) "кино $n " + Words.plural(n, "день", "дня", "дней") + " подряд" else "films $n days in a row"
            GoalKind.OLD -> if (ru) "$n " + Words.plural(n, "фильм", "фильма", "фильмов") + " старше 1980 года" else "$n films from before 1980"
            GoalKind.WORDS -> if (ru) "$n " + Words.plural(n, "корешок", "корешка", "корешков") + " со словами" else "$n stubs with words"
            GoalKind.COMPANY -> if (ru) "$n " + Words.plural(n, "фильм", "фильма", "фильмов") + " не в одиночку" else "$n films with someone"
            GoalKind.REWATCHES -> if (ru) "$n " + Words.plural(n, "пересмотр", "пересмотра", "пересмотров") else "$n rewatches"
            GoalKind.SCENES -> if (ru) "$n " + Words.plural(n, "сцена", "сцены", "сцен") + " после титров" else "$n post-credits scenes"
            GoalKind.LONG -> if (ru) "$n " + Words.plural(n, "фильм", "фильма", "фильмов") + " от 2,5 часа" else "$n films of 2.5 hours and more"
            GoalKind.RATED -> if (ru) "оценить $n " + Words.plural(n, "фильм", "фильма", "фильмов") else "rate $n films"
        }
        return what + if (g.kind == GoalKind.STREAK) "" else " " + period(g.period, ru)
    }

    fun period(p: Period, ru: Boolean) = when (p) {
        Period.WEEK -> if (ru) "за неделю" else "a week"
        Period.MONTH -> if (ru) "за месяц" else "a month"
        Period.YEAR -> if (ru) "за год" else "a year"
        Period.ONCE -> if (ru) "один раз" else "once"
    }
}

/** A small task for the week. */
enum class Quest(val need: Int, val reward: Int) {
    FILMS(2, 20), EPISODES(5, 20), OLD(1, 25), RATE(3, 15), COMPANY(1, 25), NEW_COUNTRY(1, 30), LONG(1, 20), GENRES(3, 25), DAYS(3, 20);
}

data class QuestState(val q: Quest, val p: Progress)

object Quests {
    /** This week's id, so a task taken is taken for the week. */
    fun weekId(c: Cinema): String { val cal = Calendar.getInstance().apply { timeInMillis = c.now; firstDayOfWeek = Calendar.MONDAY; minimalDaysInFirstWeek = 4 }; return "${cal.getWeekYear()}-${cal.get(Calendar.WEEK_OF_YEAR)}" }

    /** Three tasks a week, the same three all week. */
    fun of(c: Cinema): List<QuestState> {
        val id = weekId(c)
        val pick = Quest.entries.sortedBy { hash(id + it.name) }.take(3)
        val cal = Calendar.getInstance().apply { timeInMillis = c.now; firstDayOfWeek = Calendar.MONDAY; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) }
        if (cal.timeInMillis > c.now) cal.add(Calendar.DAY_OF_YEAR, -7)
        val w = Window(c, cal.timeInMillis, Long.MAX_VALUE)
        return pick.map { q ->
            val have = when (q) {
                Quest.FILMS -> w.films.size
                Quest.EPISODES -> w.episodes.size
                Quest.OLD -> w.films.count { (it.year ?: 9999) < 1980 }
                Quest.RATE -> w.films.count { it.stars > 0 }
                Quest.COMPANY -> w.films.count { it.company != Company.ALONE }
                Quest.NEW_COUNTRY -> w.newCountries
                Quest.LONG -> w.films.count { it.minutes >= 140 }
                Quest.GENRES -> w.genres
                Quest.DAYS -> w.days
            }
            QuestState(q, Progress(have, q.need))
        }
    }

    fun name(q: Quest, ru: Boolean) = when (q) {
        Quest.FILMS -> if (ru) "Посмотреть 2 фильма" else "Watch 2 films"
        Quest.EPISODES -> if (ru) "Посмотреть 5 серий" else "Watch 5 episodes"
        Quest.OLD -> if (ru) "Фильм старше 1980 года" else "A film from before 1980"
        Quest.RATE -> if (ru) "Оценить 3 фильма попкорном" else "Rate 3 films in popcorn"
        Quest.COMPANY -> if (ru) "Фильм не в одиночку" else "A film with someone"
        Quest.NEW_COUNTRY -> if (ru) "Фильм из новой страны" else "A film from a new country"
        Quest.LONG -> if (ru) "Фильм дольше 2 ч 20 мин" else "A film over 2 h 20 min"
        Quest.GENRES -> if (ru) "Три разных жанра" else "Three different genres"
        Quest.DAYS -> if (ru) "Кино в три разных дня" else "Films on three different days"
    }
}
