package app.reelkin.domain

import app.reelkin.data.Company
import app.reelkin.data.Credits
import app.reelkin.data.DAY
import app.reelkin.data.Genre
import app.reelkin.data.People
import app.reelkin.data.Period
import app.reelkin.data.Stage
import java.util.Calendar

/** The shelves the achievements stand on. */
enum class FeatGroup { TIME, FINDS, HABITS, GENRES, SERIES, ANIME, BOOTH }

/**
 * Achievements: each counts something and has steps ([tiers]) to climb; every step reached pays [Feats.REWARD] popcorn.
 * Worked out from what was watched each time, so nothing is stored. The picture is resources/feats/<name in lower case>.png.
 */
enum class Feat(val group: FeatGroup, val tiers: IntArray) {
    HOURS(FeatGroup.TIME, intArrayOf(10, 100, 500, 1000, 2500)),
    FILMS(FeatGroup.TIME, intArrayOf(10, 50, 100, 500, 1000)),
    DAYS(FeatGroup.TIME, intArrayOf(30, 100, 365, 1000)),
    STREAK(FeatGroup.TIME, intArrayOf(3, 7, 30)),
    BIG_DAY(FeatGroup.TIME, intArrayOf(2, 3, 4)),
    MARATHON(FeatGroup.TIME, intArrayOf(1, 5, 20)),
    VETERAN(FeatGroup.TIME, intArrayOf(30, 182, 365, 730)),
    WEEKEND(FeatGroup.TIME, intArrayOf(10, 50, 200)),
    LONG_DAY(FeatGroup.TIME, intArrayOf(4, 6, 8, 12)),
    MONTH(FeatGroup.TIME, intArrayOf(10, 20, 31)),

    COUNTRIES(FeatGroup.FINDS, intArrayOf(3, 10, 25, 50)),
    DECADES(FeatGroup.FINDS, intArrayOf(3, 6, 9, 11)),
    OLD(FeatGroup.FINDS, intArrayOf(1, 10, 30)),
    SILENT(FeatGroup.FINDS, intArrayOf(1, 5)),
    NEW(FeatGroup.FINDS, intArrayOf(1, 10, 50)),
    DIRECTORS(FeatGroup.FINDS, intArrayOf(10, 50, 200)),
    DIRECTOR_FAN(FeatGroup.FINDS, intArrayOf(3, 5, 10)),
    ACTOR_FAN(FeatGroup.FINDS, intArrayOf(3, 5, 10)),
    GENRES(FeatGroup.FINDS, intArrayOf(5, 8, 12)),
    EAST(FeatGroup.FINDS, intArrayOf(1, 10, 30)),
    CAST(FeatGroup.FINDS, intArrayOf(50, 200, 1000)),
    LETTERS(FeatGroup.FINDS, intArrayOf(10, 18, 26)),
    EIGHTIES(FeatGroup.FINDS, intArrayOf(5, 20, 50)),
    NINETIES(FeatGroup.FINDS, intArrayOf(5, 20, 50)),

    NIGHT(FeatGroup.HABITS, intArrayOf(5, 25, 100)),
    MORNING(FeatGroup.HABITS, intArrayOf(5, 25)),
    REWATCH(FeatGroup.HABITS, intArrayOf(1, 10, 30)),
    SCENES(FeatGroup.HABITS, intArrayOf(1, 5, 20)),
    WORDS(FeatGroup.HABITS, intArrayOf(10, 50, 200)),
    RATED(FeatGroup.HABITS, intArrayOf(10, 100, 500)),
    FIVE(FeatGroup.HABITS, intArrayOf(5, 25, 100)),
    COMPANY(FeatGroup.HABITS, intArrayOf(5, 25, 100)),
    DATE(FeatGroup.HABITS, intArrayOf(3, 10, 30)),
    PARTY(FeatGroup.HABITS, intArrayOf(3, 10, 30)),
    HONEST(FeatGroup.HABITS, intArrayOf(1, 5)),
    SHORT(FeatGroup.HABITS, intArrayOf(5, 25, 100)),
    ONE_STAR(FeatGroup.HABITS, intArrayOf(1, 5, 20)),
    FULL(FeatGroup.HABITS, intArrayOf(10, 50, 200)),
    WEEKDAY(FeatGroup.HABITS, intArrayOf(10, 50, 200)),
    SUMMER(FeatGroup.HABITS, intArrayOf(10, 30, 100)),
    WINTER(FeatGroup.HABITS, intArrayOf(10, 30, 100)),
    NEW_YEAR(FeatGroup.HABITS, intArrayOf(1, 3, 5)),
    HALLOWEEN(FeatGroup.HABITS, intArrayOf(1, 3, 5)),
    COMEBACK(FeatGroup.HABITS, intArrayOf(1, 5, 20)),

    G_NOIR(FeatGroup.GENRES, intArrayOf(3, 10, 30, 100)),
    G_SCIFI(FeatGroup.GENRES, intArrayOf(3, 10, 30, 100)),
    G_ROMANCE(FeatGroup.GENRES, intArrayOf(3, 10, 30, 100)),
    G_COMEDY(FeatGroup.GENRES, intArrayOf(3, 10, 30, 100)),
    G_DOC(FeatGroup.GENRES, intArrayOf(3, 10, 30, 100)),
    G_HORROR(FeatGroup.GENRES, intArrayOf(3, 10, 30, 100)),
    G_WESTERN(FeatGroup.GENRES, intArrayOf(3, 10, 30)),
    G_ANIMATION(FeatGroup.GENRES, intArrayOf(3, 10, 30, 100)),
    G_MUSICAL(FeatGroup.GENRES, intArrayOf(3, 10, 30)),
    G_DRAMA(FeatGroup.GENRES, intArrayOf(3, 10, 30, 100)),
    G_ADVENTURE(FeatGroup.GENRES, intArrayOf(3, 10, 30, 100)),
    G_REALITY(FeatGroup.GENRES, intArrayOf(1, 5, 15)),

    EPISODES(FeatGroup.SERIES, intArrayOf(10, 100, 500, 1000)),
    SEASONS(FeatGroup.SERIES, intArrayOf(1, 5, 20, 50)),
    BINGE(FeatGroup.SERIES, intArrayOf(3, 6, 10)),
    SHOWS(FeatGroup.SERIES, intArrayOf(1, 5, 15)),
    LOYAL(FeatGroup.SERIES, intArrayOf(25, 100, 300)),
    S_NIGHT(FeatGroup.SERIES, intArrayOf(10, 50, 200)),
    S_HOURS(FeatGroup.SERIES, intArrayOf(10, 100, 500)),

    ANIME(FeatGroup.ANIME, intArrayOf(3, 10, 30, 100)),
    A_EPISODES(FeatGroup.ANIME, intArrayOf(10, 100, 500, 1000)),
    A_FILMS(FeatGroup.ANIME, intArrayOf(1, 5, 20)),
    A_SHOWS(FeatGroup.ANIME, intArrayOf(1, 5, 15)),
    A_SEASONS(FeatGroup.ANIME, intArrayOf(1, 5, 20)),
    A_NIGHT(FeatGroup.ANIME, intArrayOf(5, 25, 100)),
    A_BINGE(FeatGroup.ANIME, intArrayOf(6, 12, 24)),
    A_RETRO(FeatGroup.ANIME, intArrayOf(1, 5, 20)),
    A_FRESH(FeatGroup.ANIME, intArrayOf(3, 12, 50)),
    A_FIVE(FeatGroup.ANIME, intArrayOf(1, 5, 20)),
    A_PETS(FeatGroup.ANIME, intArrayOf(1, 5, 10, 17)),
    A_GROWN(FeatGroup.ANIME, intArrayOf(1, 3, 6)),

    PETS(FeatGroup.BOOTH, intArrayOf(3, 8, 16, 32, 49)),
    GROWN(FeatGroup.BOOTH, intArrayOf(1, 3, 6)),
    ANCIENT(FeatGroup.BOOTH, intArrayOf(1, 3)),
    SHINY(FeatGroup.BOOTH, intArrayOf(1)),
    NATURES(FeatGroup.BOOTH, intArrayOf(3, 5, 7)),
    FORMS(FeatGroup.BOOTH, intArrayOf(1, 3, 5)),
    NAMED(FeatGroup.BOOTH, intArrayOf(1, 5, 15)),
    COSY(FeatGroup.BOOTH, intArrayOf(3, 6, 12)),
    QUESTS(FeatGroup.BOOTH, intArrayOf(5, 25, 100)),
    GOALS(FeatGroup.BOOTH, intArrayOf(1, 5, 20)),
    AWARDS(FeatGroup.BOOTH, intArrayOf(1, 3, 5)),
    HALL(FeatGroup.BOOTH, intArrayOf(1, 3, 5, 10)),
    BUFFET(FeatGroup.BOOTH, intArrayOf(100, 500, 2000));

    val file get() = name.lowercase()
}

/** How far one achievement got: [level] steps of its tiers reached. */
data class FeatState(val f: Feat, val have: Int) {
    val level: Int = f.tiers.count { have >= it }
    val max get() = f.tiers.size
    val done get() = level == max
    val need: Int get() = f.tiers.getOrElse(level) { f.tiers.last() }
    val from: Int get() = if (level == 0) 0 else f.tiers[level - 1]
    val part: Float get() = if (done) 1f else ((have - from).toFloat() / (need - from)).coerceIn(0f, 1f)
}

object Feats {
    /** Popcorn for every step reached. */
    const val REWARD = 10

    fun of(c: Cinema): List<FeatState> {
        val films = c.seats.filter { !it.season }
        val ws = c.watches
        val cal = Calendar.getInstance()
        fun day(ms: Long) = (ms + cal.timeZone.getOffset(ms)).floorDiv(DAY)
        val days = ws.map { day(it.at) }.toSet()
        val perDayFilms = films.groupingBy { day(it.at) }.eachCount()
        val perDayEps = c.episodes.groupingBy { day(it.at) }.eachCount()
        val directors = films.flatMap { People.of(it.director) }
        val actors = films.flatMap { People.of(it.cast) }
        val (best, _) = Stats.streaks(c)
        fun genre(g: Genre) = films.count { it.genre == g }
        val perDayMin = HashMap<Long, Int>().also { m -> for (w in ws) m.merge(day(w.at), w.minutes, Int::plus) }
        /** The month and the day of the month, January being 0. */
        fun date(ms: Long): Pair<Int, Int> { cal.timeInMillis = ms; return cal.get(Calendar.MONTH) to cal.get(Calendar.DAY_OF_MONTH) }
        val perMonth = films.groupingBy { cal.timeInMillis = it.at; cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH) }.eachCount()
        val hatched = c.pets.filter { it.pet.hatched }
        // Anime: the stubs (films and seasons), the watches (with episodes) and the series themselves.
        val aSeats = c.seats.filter { Pets.anime(it.genre, it.country) }
        val aw = ws.filter { it.anime }
        val aSeries = c.episodes.map { it.series }.toSet().filter { id -> c.title(id)?.let { Pets.anime(it.genre, it.country) } == true }.toSet()
        val aEps = c.episodes.filter { it.series in aSeries }
        fun value(f: Feat): Int = when (f) {
            Feat.HOURS -> (c.minutes / 60).toInt()
            Feat.FILMS -> films.size
            Feat.DAYS -> days.size
            Feat.STREAK -> best
            Feat.BIG_DAY -> perDayFilms.values.maxOrNull() ?: 0
            Feat.MARATHON -> films.count { it.minutes >= 180 }
            Feat.VETERAN -> (ws.firstOrNull()?.at)?.let { ((c.now - it) / DAY).toInt() } ?: 0
            Feat.WEEKEND -> ws.count { !it.episode && it.weekend }
            Feat.LONG_DAY -> (perDayMin.values.maxOrNull() ?: 0) / 60
            Feat.MONTH -> perMonth.values.maxOrNull() ?: 0
            Feat.CAST -> actors.toSet().size
            Feat.LETTERS -> c.letters.size
            Feat.EIGHTIES -> films.count { it.year in 1980..1989 }
            Feat.NINETIES -> films.count { it.year in 1990..1999 }
            Feat.ONE_STAR -> films.count { it.stars == 1 }
            Feat.FULL -> films.count { it.stars > 0 && it.words.isNotBlank() }
            Feat.WEEKDAY -> ws.count { !it.episode && !it.weekend }
            Feat.SUMMER -> ws.count { !it.episode && it.month in 5..7 }
            Feat.WINTER -> ws.count { !it.episode && (it.month == 11 || it.month <= 1) }
            Feat.NEW_YEAR -> films.count { date(it.at).let { d -> d == (11 to 31) || d == (0 to 1) } }
            Feat.HALLOWEEN -> films.count { it.genre == Genre.HORROR && date(it.at) == (9 to 31) }
            Feat.COMEBACK -> ws.count { it.gap >= 7 }
            Feat.LOYAL -> c.episodes.groupingBy { it.series }.eachCount().values.maxOrNull() ?: 0
            Feat.S_NIGHT -> ws.count { it.episode && it.late }
            Feat.S_HOURS -> (c.episodes.sumOf { it.minutes } + c.seats.filter { it.season && it.whole }.sumOf { it.minutes }) / 60
            Feat.NATURES -> hatched.map { it.pet.nature }.toSet().size
            Feat.FORMS -> hatched.mapNotNull { it.form }.toSet().size
            Feat.NAMED -> hatched.count { it.pet.name.isNotBlank() }
            Feat.COUNTRIES -> c.countries.size
            Feat.DECADES -> c.decades.size
            Feat.OLD -> films.count { (it.year ?: 9999) < 1960 }
            Feat.SILENT -> films.count { (it.year ?: 9999) < 1930 }
            Feat.NEW -> films.count { it.year != null && it.year >= Browse.yearOf(it.at) }
            Feat.DIRECTORS -> directors.toSet().size
            Feat.DIRECTOR_FAN -> directors.groupingBy { it }.eachCount().values.maxOrNull() ?: 0
            Feat.ACTOR_FAN -> actors.groupingBy { it }.eachCount().values.maxOrNull() ?: 0
            Feat.GENRES -> films.map { it.genre }.toSet().size
            Feat.EAST -> films.count { Pets.eastAsia(it.country) }
            Feat.NIGHT -> ws.count { !it.episode && (it.hour >= 23 || it.hour < 4) }
            Feat.MORNING -> ws.count { !it.episode && it.hour in 6..11 }
            Feat.REWATCH -> films.count { it.rewatch }
            Feat.SCENES -> films.count { it.credits == Credits.SCENE }
            Feat.WORDS -> films.count { it.words.isNotBlank() }
            Feat.RATED -> films.count { it.stars > 0 }
            Feat.FIVE -> films.count { it.stars == 5 }
            Feat.COMPANY -> films.count { it.company != Company.ALONE }
            Feat.DATE -> films.count { it.company == Company.TWO }
            Feat.PARTY -> films.count { it.company == Company.MANY }
            Feat.HONEST -> films.count { it.credits == Credits.LEFT }
            Feat.SHORT -> films.count { it.minutes in 1..90 }
            Feat.G_NOIR -> genre(Genre.NOIR)
            Feat.G_SCIFI -> genre(Genre.SCIFI)
            Feat.G_ROMANCE -> genre(Genre.ROMANCE)
            Feat.G_COMEDY -> genre(Genre.COMEDY)
            Feat.G_DOC -> genre(Genre.DOC)
            Feat.G_HORROR -> genre(Genre.HORROR)
            Feat.G_WESTERN -> genre(Genre.WESTERN)
            Feat.G_ANIMATION -> genre(Genre.ANIMATION)
            Feat.G_MUSICAL -> genre(Genre.MUSICAL)
            Feat.G_DRAMA -> genre(Genre.DRAMA)
            Feat.G_ADVENTURE -> genre(Genre.ADVENTURE)
            Feat.G_REALITY -> genre(Genre.REALITY)
            Feat.EPISODES -> c.episodes.size
            Feat.SEASONS -> c.seats.count { it.season }
            Feat.BINGE -> perDayEps.values.maxOrNull() ?: 0
            Feat.SHOWS -> c.episodes.map { it.series }.toSet().size
            Feat.ANIME -> aSeats.size
            Feat.A_EPISODES -> aEps.size
            Feat.A_FILMS -> aSeats.count { !it.season }
            Feat.A_SHOWS -> aSeries.size
            Feat.A_SEASONS -> aSeats.count { it.season }
            Feat.A_NIGHT -> aw.count { it.late }
            Feat.A_BINGE -> aEps.groupingBy { day(it.at) }.eachCount().values.maxOrNull() ?: 0
            Feat.A_RETRO -> aSeats.count { (it.year ?: 9999) < 2000 }
            Feat.A_FRESH -> aw.count { it.fresh }
            Feat.A_FIVE -> aSeats.count { it.stars == 5 }
            Feat.A_PETS -> c.pets.count { it.pet.hatched && it.species.anime }
            Feat.A_GROWN -> c.pets.count { it.pet.hatched && it.species.anime && it.stage >= Stage.ADULT }
            Feat.PETS -> c.pets.count { it.pet.hatched }
            Feat.GROWN -> c.pets.count { it.pet.hatched && it.stage >= Stage.ADULT }
            Feat.ANCIENT -> c.pets.count { it.stage == Stage.ANCIENT }
            Feat.SHINY -> c.pets.count { it.pet.shiny && it.pet.hatched }
            Feat.COSY -> c.s.bought.size
            Feat.QUESTS -> c.s.claimed.size
            Feat.GOALS -> c.goals.count { it.period == Period.ONCE && Goals.progress(c, it).done } + c.goals.count { it.period != Period.ONCE && Goals.progress(c, it).done }
            Feat.AWARDS -> c.s.awards.map { it.year }.toSet().size
            Feat.HALL -> c.seats.size / 100
            Feat.BUFFET -> c.s.spent
        }
        return Feat.entries.map { FeatState(it, value(it)) }
    }

    fun group(g: FeatGroup, ru: Boolean) = when (g) {
        FeatGroup.TIME -> if (ru) "Время у экрана" else "Time"
        FeatGroup.FINDS -> if (ru) "Открытия" else "Discoveries"
        FeatGroup.HABITS -> if (ru) "Привычки" else "Habits"
        FeatGroup.GENRES -> if (ru) "Жанры" else "Genres"
        FeatGroup.SERIES -> if (ru) "Сериалы" else "Series"
        FeatGroup.ANIME -> if (ru) "Аниме" else "Anime"
        FeatGroup.BOOTH -> if (ru) "Будка и питомцы" else "The booth and the pets"
    }

    fun name(f: Feat, ru: Boolean): String = if (ru) when (f) {
        Feat.HOURS -> "Песочные часы"; Feat.FILMS -> "Фильмотека"; Feat.DAYS -> "Календарь"; Feat.STREAK -> "Без пропусков"
        Feat.BIG_DAY -> "Большой день"; Feat.MARATHON -> "Марафонец"; Feat.VETERAN -> "Старожил кинозала"; Feat.WEEKEND -> "Выходные дома"
        Feat.COUNTRIES -> "Кругосветка"; Feat.DECADES -> "Машина времени"; Feat.OLD -> "Классика"; Feat.SILENT -> "Великий немой"
        Feat.NEW -> "С пылу с жару"; Feat.DIRECTORS -> "Режиссёрский цех"; Feat.DIRECTOR_FAN -> "Свой режиссёр"; Feat.ACTOR_FAN -> "Любимый актёр"
        Feat.GENRES -> "Всеядный"; Feat.EAST -> "Восточный экспресс"
        Feat.NIGHT -> "Полуночник"; Feat.MORNING -> "Утренний сеанс"; Feat.REWATCH -> "Ещё раз"; Feat.SCENES -> "До последнего титра"
        Feat.WORDS -> "Три слова"; Feat.RATED -> "Критик"; Feat.FIVE -> "Пять попкорнов"; Feat.COMPANY -> "Не в одиночку"
        Feat.DATE -> "Свидание в кино"; Feat.PARTY -> "Киновечеринка"; Feat.HONEST -> "Честный зритель"; Feat.SHORT -> "Коротко и ясно"
        Feat.G_NOIR -> "Сыщик"; Feat.G_SCIFI -> "Звёздный путник"; Feat.G_ROMANCE -> "Романтик"; Feat.G_COMEDY -> "Хохотун"
        Feat.G_DOC -> "Документалист"; Feat.G_HORROR -> "Не страшно"; Feat.G_WESTERN -> "Ковбой"; Feat.G_ANIMATION -> "Мультяшка"
        Feat.G_MUSICAL -> "Чечётка"; Feat.G_DRAMA -> "Драматург"; Feat.G_ADVENTURE -> "Искатель приключений"; Feat.G_REALITY -> "Реалити"
        Feat.EPISODES -> "Серия за серией"; Feat.SEASONS -> "Сезон закрыт"; Feat.BINGE -> "Запоем"; Feat.SHOWS -> "Сериаломан"
        Feat.ANIME -> "Отаку"; Feat.A_EPISODES -> "Ещё одну серию"; Feat.A_FILMS -> "Полный метр"; Feat.A_SHOWS -> "Тайтл за тайтлом"
        Feat.A_SEASONS -> "Сезон аниме"; Feat.A_NIGHT -> "Ночной эфир"; Feat.A_BINGE -> "Залпом"; Feat.A_RETRO -> "Ретро-аниме"
        Feat.A_FRESH -> "Онгоинг"; Feat.A_FIVE -> "Шедевр"; Feat.A_PETS -> "Ёкаи в будке"; Feat.A_GROWN -> "Сэнсэй"
        Feat.PETS -> "Зверинец"; Feat.GROWN -> "Вырастил"; Feat.ANCIENT -> "Старожилы"; Feat.SHINY -> "Техниколор"
        Feat.COSY -> "Уютная будка"; Feat.QUESTS -> "Задания недели"; Feat.GOALS -> "Цель достигнута"; Feat.AWARDS -> "Церемония"
        Feat.HALL -> "Аншлаг"; Feat.BUFFET -> "Завсегдатай буфета"
        Feat.LONG_DAY -> "Весь день в кино"; Feat.MONTH -> "Месяц кино"; Feat.CAST -> "Массовка"; Feat.LETTERS -> "От А до Я"
        Feat.EIGHTIES -> "Восьмидесятые"; Feat.NINETIES -> "Девяностые"; Feat.ONE_STAR -> "Разгромная рецензия"; Feat.FULL -> "Всё по полочкам"
        Feat.WEEKDAY -> "Будний вечер"; Feat.SUMMER -> "Летний кинотеатр"; Feat.WINTER -> "Зимние вечера"; Feat.NEW_YEAR -> "Новогодний сеанс"
        Feat.HALLOWEEN -> "Хэллоуин"; Feat.COMEBACK -> "Возвращение"; Feat.LOYAL -> "Верный зритель"; Feat.S_NIGHT -> "Ещё одну и спать"
        Feat.S_HOURS -> "Часы сериалов"; Feat.NATURES -> "Все характеры"; Feat.FORMS -> "Все формы"; Feat.NAMED -> "Клички"
    } else when (f) {
        Feat.HOURS -> "Hourglass"; Feat.FILMS -> "Film library"; Feat.DAYS -> "Calendar"; Feat.STREAK -> "No gaps"
        Feat.BIG_DAY -> "Big day"; Feat.MARATHON -> "Marathon"; Feat.VETERAN -> "Old hand"; Feat.WEEKEND -> "Weekend in"
        Feat.COUNTRIES -> "Around the world"; Feat.DECADES -> "Time machine"; Feat.OLD -> "Classics"; Feat.SILENT -> "The great silent"
        Feat.NEW -> "Fresh out"; Feat.DIRECTORS -> "Directors' guild"; Feat.DIRECTOR_FAN -> "My director"; Feat.ACTOR_FAN -> "My actor"
        Feat.GENRES -> "Omnivore"; Feat.EAST -> "Eastern express"
        Feat.NIGHT -> "Night owl"; Feat.MORNING -> "Morning show"; Feat.REWATCH -> "Once more"; Feat.SCENES -> "To the last credit"
        Feat.WORDS -> "Three words"; Feat.RATED -> "Critic"; Feat.FIVE -> "Five popcorn"; Feat.COMPANY -> "Not alone"
        Feat.DATE -> "Date night"; Feat.PARTY -> "Film party"; Feat.HONEST -> "Honest viewer"; Feat.SHORT -> "Short and sweet"
        Feat.G_NOIR -> "Sleuth"; Feat.G_SCIFI -> "Star traveller"; Feat.G_ROMANCE -> "Romantic"; Feat.G_COMEDY -> "Giggler"
        Feat.G_DOC -> "Documentarian"; Feat.G_HORROR -> "Not scared"; Feat.G_WESTERN -> "Cowpoke"; Feat.G_ANIMATION -> "Cartoon fan"
        Feat.G_MUSICAL -> "Tap dance"; Feat.G_DRAMA -> "Dramatist"; Feat.G_ADVENTURE -> "Adventurer"; Feat.G_REALITY -> "Reality"
        Feat.EPISODES -> "Episode after episode"; Feat.SEASONS -> "Season closed"; Feat.BINGE -> "Binge"; Feat.SHOWS -> "Series fan"
        Feat.ANIME -> "Otaku"; Feat.A_EPISODES -> "One more episode"; Feat.A_FILMS -> "Feature length"; Feat.A_SHOWS -> "Title after title"
        Feat.A_SEASONS -> "Anime season"; Feat.A_NIGHT -> "Late-night slot"; Feat.A_BINGE -> "In one go"; Feat.A_RETRO -> "Retro anime"
        Feat.A_FRESH -> "Ongoing"; Feat.A_FIVE -> "Masterpiece"; Feat.A_PETS -> "Yokai in the booth"; Feat.A_GROWN -> "Sensei"
        Feat.PETS -> "Menagerie"; Feat.GROWN -> "Raised"; Feat.ANCIENT -> "Old-timers"; Feat.SHINY -> "Technicolor"
        Feat.COSY -> "Cosy booth"; Feat.QUESTS -> "Weekly tasks"; Feat.GOALS -> "Goal reached"; Feat.AWARDS -> "Ceremony"
        Feat.HALL -> "Full house"; Feat.BUFFET -> "Buffet regular"
        Feat.LONG_DAY -> "All day at the pictures"; Feat.MONTH -> "A month of films"; Feat.CAST -> "Cast of thousands"; Feat.LETTERS -> "A to Z"
        Feat.EIGHTIES -> "The eighties"; Feat.NINETIES -> "The nineties"; Feat.ONE_STAR -> "Scathing review"; Feat.FULL -> "Chapter and verse"
        Feat.WEEKDAY -> "School night"; Feat.SUMMER -> "Summer cinema"; Feat.WINTER -> "Winter evenings"; Feat.NEW_YEAR -> "New Year's show"
        Feat.HALLOWEEN -> "Halloween"; Feat.COMEBACK -> "The return"; Feat.LOYAL -> "Faithful viewer"; Feat.S_NIGHT -> "One more and to bed"
        Feat.S_HOURS -> "Series hours"; Feat.NATURES -> "Every temper"; Feat.FORMS -> "Every form"; Feat.NAMED -> "Pet names"
    }

    /** What the next step asks, in words. */
    fun goal(f: Feat, n: Int, ru: Boolean): String {
        fun films(x: Int) = Words.films(x, ru)
        fun pl(x: Int, one: String, few: String, many: String) = "$x " + Words.plural(x, one, few, many)
        return if (ru) when (f) {
            Feat.HOURS -> pl(n, "час", "часа", "часов") + " у экрана"; Feat.FILMS -> films(n); Feat.DAYS -> pl(n, "день", "дня", "дней") + " с кино"
            Feat.STREAK -> "кино ${pl(n, "день", "дня", "дней")} подряд"; Feat.BIG_DAY -> "${films(n)} за один день"; Feat.MARATHON -> "${films(n)} от трёх часов"
            Feat.VETERAN -> pl(n, "день", "дня", "дней") + " с первого корешка"; Feat.WEEKEND -> "${films(n)} в выходные"
            Feat.COUNTRIES -> "фильмы из ${pl(n, "страны", "стран", "стран")}"; Feat.DECADES -> "фильмы ${pl(n, "десятилетия", "десятилетий", "десятилетий")}"
            Feat.OLD -> "${films(n)} старше 1960 года"; Feat.SILENT -> "${films(n)} старше 1930 года"; Feat.NEW -> "${films(n)} в год выхода"
            Feat.DIRECTORS -> pl(n, "режиссёр", "режиссёра", "режиссёров"); Feat.DIRECTOR_FAN -> "${films(n)} одного режиссёра"; Feat.ACTOR_FAN -> "${films(n)} с одним актёром"
            Feat.GENRES -> pl(n, "жанр", "жанра", "жанров"); Feat.EAST -> "${films(n)} из Японии, Кореи или Китая"
            Feat.NIGHT -> "${films(n)} после 23:00"; Feat.MORNING -> "${films(n)} до полудня"; Feat.REWATCH -> pl(n, "пересмотр", "пересмотра", "пересмотров")
            Feat.SCENES -> pl(n, "сцена", "сцены", "сцен") + " после титров"; Feat.WORDS -> pl(n, "корешок", "корешка", "корешков") + " со словами"
            Feat.RATED -> "оценить ${films(n)}"; Feat.FIVE -> "${films(n)} на пять попкорнов"; Feat.COMPANY -> "${films(n)} не в одиночку"
            Feat.DATE -> "${films(n)} вдвоём"; Feat.PARTY -> "${films(n)} компанией"; Feat.HONEST -> "уйти до титров ${pl(n, "раз", "раза", "раз")}"; Feat.SHORT -> "${films(n)} до полутора часов"
            Feat.G_NOIR, Feat.G_SCIFI, Feat.G_ROMANCE, Feat.G_COMEDY, Feat.G_DOC, Feat.G_HORROR, Feat.G_WESTERN, Feat.G_ANIMATION, Feat.G_MUSICAL, Feat.G_DRAMA,
            Feat.G_ADVENTURE, Feat.G_REALITY -> "${films(n)}: " + Words.genre(Genre.valueOf(f.name.removePrefix("G_")), true)
            Feat.EPISODES -> pl(n, "серия", "серии", "серий"); Feat.SEASONS -> pl(n, "сезон", "сезона", "сезонов") + " досмотреть"
            Feat.BINGE -> pl(n, "серия", "серии", "серий") + " за день"; Feat.SHOWS -> pl(n, "сериал", "сериала", "сериалов")
            Feat.ANIME -> pl(n, "корешок", "корешка", "корешков") + " аниме: фильмы и сезоны"; Feat.A_EPISODES -> pl(n, "серия", "серии", "серий") + " аниме"
            Feat.A_FILMS -> pl(n, "полнометражное", "полнометражных", "полнометражных") + " аниме"; Feat.A_SHOWS -> pl(n, "аниме-сериал", "аниме-сериала", "аниме-сериалов")
            Feat.A_SEASONS -> pl(n, "сезон", "сезона", "сезонов") + " аниме досмотреть"; Feat.A_NIGHT -> "аниме после 23:00 ${pl(n, "раз", "раза", "раз")}"
            Feat.A_BINGE -> pl(n, "серия", "серии", "серий") + " аниме за день"; Feat.A_RETRO -> pl(n, "корешок", "корешка", "корешков") + " аниме старше 2000 года"
            Feat.A_FRESH -> "аниме в год выхода ${pl(n, "раз", "раза", "раз")}"; Feat.A_FIVE -> "$n аниме на пять попкорнов"
            Feat.A_PETS -> pl(n, "аниме-питомец", "аниме-питомца", "аниме-питомцев"); Feat.A_GROWN -> "вырастить ${pl(n, "аниме-питомца", "аниме-питомцев", "аниме-питомцев")}"
            Feat.PETS -> pl(n, "питомец", "питомца", "питомцев"); Feat.GROWN -> "вырастить ${pl(n, "питомца", "питомцев", "питомцев")}"
            Feat.ANCIENT -> pl(n, "старожил", "старожила", "старожилов"); Feat.SHINY -> "питомец в техниколоре"; Feat.COSY -> pl(n, "вещь", "вещи", "вещей") + " в будке"
            Feat.QUESTS -> pl(n, "задание", "задания", "заданий"); Feat.GOALS -> pl(n, "цель", "цели", "целей"); Feat.AWARDS -> "вручить премию ${pl(n, "год", "года", "лет")}"
            Feat.HALL -> "заполнить ${pl(n, "зал", "зала", "залов")}"; Feat.BUFFET -> "потратить $n попкорна"
            Feat.LONG_DAY -> pl(n, "час", "часа", "часов") + " у экрана за один день"; Feat.MONTH -> "${films(n)} за один месяц"
            Feat.CAST -> pl(n, "актёр", "актёра", "актёров"); Feat.LETTERS -> "фильмы на ${pl(n, "букву", "разные буквы", "разных букв")}"
            Feat.EIGHTIES -> "${films(n)} 1980-х"; Feat.NINETIES -> "${films(n)} 1990-х"; Feat.ONE_STAR -> "${films(n)} на один попкорн"
            Feat.FULL -> pl(n, "корешок", "корешка", "корешков") + " с оценкой и словами"; Feat.WEEKDAY -> "${films(n)} в будни"
            Feat.SUMMER -> "${films(n)} летом"; Feat.WINTER -> "${films(n)} зимой"; Feat.NEW_YEAR -> "${films(n)} 31 декабря или 1 января"
            Feat.HALLOWEEN -> pl(n, "хоррор", "хоррора", "хорроров") + " 31 октября"; Feat.COMEBACK -> "вернуться к кино после недели без него ${pl(n, "раз", "раза", "раз")}"
            Feat.LOYAL -> pl(n, "серия", "серии", "серий") + " одного сериала"; Feat.S_NIGHT -> pl(n, "серия", "серии", "серий") + " после 23:00"
            Feat.S_HOURS -> pl(n, "час", "часа", "часов") + " сериалов"; Feat.NATURES -> "питомцы ${pl(n, "характера", "разных характеров", "разных характеров")}"
            Feat.FORMS -> "питомцы ${pl(n, "формы", "разных форм", "разных форм")}"; Feat.NAMED -> "дать имя ${pl(n, "питомцу", "питомцам", "питомцам")}"
        } else when (f) {
            Feat.HOURS -> "$n hours watched"; Feat.FILMS -> films(n); Feat.DAYS -> "$n days with films"; Feat.STREAK -> "films $n days in a row"
            Feat.BIG_DAY -> "${films(n)} in a day"; Feat.MARATHON -> "${films(n)} of three hours or more"; Feat.VETERAN -> "$n days since the first stub"
            Feat.WEEKEND -> "${films(n)} at weekends"; Feat.COUNTRIES -> "films from $n countries"; Feat.DECADES -> "films from $n decades"
            Feat.OLD -> "${films(n)} from before 1960"; Feat.SILENT -> "${films(n)} from before 1930"; Feat.NEW -> "${films(n)} in their year"
            Feat.DIRECTORS -> "$n directors"; Feat.DIRECTOR_FAN -> "${films(n)} by one director"; Feat.ACTOR_FAN -> "${films(n)} with one actor"
            Feat.GENRES -> "$n genres"; Feat.EAST -> "${films(n)} from Japan, Korea or China"; Feat.NIGHT -> "${films(n)} after 11 pm"; Feat.MORNING -> "${films(n)} before noon"
            Feat.REWATCH -> "$n rewatches"; Feat.SCENES -> "$n post-credits scenes"; Feat.WORDS -> "$n stubs with words"; Feat.RATED -> "rate ${films(n)}"
            Feat.FIVE -> "${films(n)} of five popcorn"; Feat.COMPANY -> "${films(n)} with someone"; Feat.DATE -> "${films(n)} for two"; Feat.PARTY -> "${films(n)} with friends"
            Feat.HONEST -> "leave before the credits $n times"; Feat.SHORT -> "${films(n)} of 90 minutes or less"
            Feat.G_NOIR, Feat.G_SCIFI, Feat.G_ROMANCE, Feat.G_COMEDY, Feat.G_DOC, Feat.G_HORROR, Feat.G_WESTERN, Feat.G_ANIMATION, Feat.G_MUSICAL, Feat.G_DRAMA,
            Feat.G_ADVENTURE, Feat.G_REALITY -> "${films(n)}: " + Words.genre(Genre.valueOf(f.name.removePrefix("G_")), false)
            Feat.EPISODES -> "$n episodes"; Feat.SEASONS -> "$n seasons finished"; Feat.BINGE -> "$n episodes in a day"; Feat.SHOWS -> "$n series"
            Feat.ANIME -> "$n anime stubs: films and seasons"; Feat.A_EPISODES -> "$n episodes of anime"; Feat.A_FILMS -> "$n full-length anime films"
            Feat.A_SHOWS -> "$n anime series"; Feat.A_SEASONS -> "$n anime seasons finished"; Feat.A_NIGHT -> "anime after 11 pm $n times"
            Feat.A_BINGE -> "$n episodes of anime in a day"; Feat.A_RETRO -> "$n anime stubs from before 2000"; Feat.A_FRESH -> "anime in the year it came out $n times"
            Feat.A_FIVE -> "$n anime of five popcorn"; Feat.A_PETS -> "$n anime pets"; Feat.A_GROWN -> "raise $n anime pets"
            Feat.PETS -> "$n pets"; Feat.GROWN -> "raise $n pets"; Feat.ANCIENT -> "$n old-timers"; Feat.SHINY -> "a technicolor pet"; Feat.COSY -> "$n things in the booth"
            Feat.QUESTS -> "$n tasks"; Feat.GOALS -> "$n goals"; Feat.AWARDS -> "hand out awards $n years"; Feat.HALL -> "fill $n halls"; Feat.BUFFET -> "spend $n popcorn"
            Feat.LONG_DAY -> "$n hours watched in one day"; Feat.MONTH -> "${films(n)} in one month"; Feat.CAST -> "$n actors"
            Feat.LETTERS -> "films starting with $n different letters"; Feat.EIGHTIES -> "${films(n)} of the 1980s"; Feat.NINETIES -> "${films(n)} of the 1990s"
            Feat.ONE_STAR -> "${films(n)} of one popcorn"; Feat.FULL -> "$n stubs with a rating and words"; Feat.WEEKDAY -> "${films(n)} on weekdays"
            Feat.SUMMER -> "${films(n)} in summer"; Feat.WINTER -> "${films(n)} in winter"; Feat.NEW_YEAR -> "${films(n)} on 31 December or 1 January"
            Feat.HALLOWEEN -> "$n horror films on 31 October"; Feat.COMEBACK -> "come back to films after a week without, $n times"
            Feat.LOYAL -> "$n episodes of one series"; Feat.S_NIGHT -> "$n episodes after 11 pm"; Feat.S_HOURS -> "$n hours of series"
            Feat.NATURES -> "pets of $n different natures"; Feat.FORMS -> "pets of $n different forms"; Feat.NAMED -> "name $n pets"
        }
    }
}
