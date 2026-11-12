package app.reelkin.data

import android.content.Context
import app.reelkin.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The demo build's cinema (`-Pdemo`): a year of real classics, three series and a bill of films to come. */
object DemoSeed {
    suspend fun seedIfEmpty(ctx: Context, c: AppContainer) {
        val dao = c.db.dao()
        if (dao.ticketCount() > 0 || dao.titleCount() > 0) return
        val ru = ctx.resources.configuration.locales[0].language == "ru"
        val now = System.currentTimeMillis()
        fun ago(d: Int) = now - d * DAY - 3 * 3_600_000L
        fun r(a: String, b: String) = if (ru) a else b
        val usa = r("США", "USA"); val ussr = r("СССР", "USSR"); val japan = r("Япония", "Japan")
        fun tk(t: String, y: Int, cn: String, g: Genre, m: Int, st: Int, w: String, co: Company, d: Int, cr: Credits = Credits.NOTHING, re: Boolean = false, season: Boolean = false) =
            Ticket(title = t, year = y, country = cn, genre = g, minutes = m, stars = st, words = w, company = co, credits = cr, rewatch = re, at = ago(d), season = season)
        val A = Company.ALONE; val T = Company.TWO; val M = Company.MANY
        dao.insertTickets(listOf(
            tk(r("Метрополис", "Metropolis"), 1927, r("Германия", "Germany"), Genre.SCIFI, 153, 5, r("город, сердце, машина", "city, heart, machine"), A, 353),
            tk(r("Человек с киноаппаратом", "Man with a Movie Camera"), 1929, ussr, Genre.DOC, 68, 4, r("трамваи, монтаж, утро", "trams, montage, morning"), A, 335),
            tk(r("Мальтийский сокол", "The Maltese Falcon"), 1941, usa, Genre.NOIR, 100, 4, r("птица, ложь, дым", "bird, lies, smoke"), T, 297),
            tk(r("Двойная страховка", "Double Indemnity"), 1944, usa, Genre.NOIR, 107, 5, r("полис, жалюзи, поезд", "policy, blinds, train"), T, 283),
            tk(r("Третий человек", "The Third Man"), 1949, r("Великобритания", "UK"), Genre.NOIR, 104, 5, r("цитра, Вена, колесо", "zither, Vienna, wheel"), A, 269),
            tk(r("Семь самураев", "Seven Samurai"), 1954, japan, Genre.ADVENTURE, 207, 5, r("дождь, рис, флаг", "rain, rice, banner"), A, 254),
            tk(r("В джазе только девушки", "Some Like It Hot"), 1959, usa, Genre.COMEDY, 121, 5, r("контрабас, поезд, никто", "double bass, train, nobody"), M, 240),
            tk(r("Хороший, плохой, злой", "The Good, the Bad and the Ugly"), 1966, r("Италия", "Italy"), Genre.WESTERN, 178, 4, r("кладбище, дуэль, свист", "graveyard, duel, whistle"), T, 227),
            tk(r("Иван Васильевич меняет профессию", "Ivan Vasilievich Changes Profession"), 1973, ussr, Genre.COMEDY, 88, 5, r("машина, царь, ключи", "machine, tsar, keys"), M, 215, re = true),
            tk(r("Сталкер", "Stalker"), 1979, ussr, Genre.SCIFI, 163, 4, r("комната, вода, тишина", "room, water, silence"), A, 198),
            tk(r("Нечто", "The Thing"), 1982, usa, Genre.HORROR, 109, 4, r("снег, кровь, собаки", "snow, blood, dogs"), T, 184),
            tk(r("Бегущий по лезвию", "Blade Runner"), 1982, usa, Genre.SCIFI, 117, 5, r("дождь, единорог, слёзы", "rain, unicorn, tears"), A, 170),
            tk(r("Вкус вишни", "Taste of Cherry"), 1997, r("Иран", "Iran"), Genre.DRAMA, 95, 4, r("холм, машина, вишня", "hill, car, cherry"), A, 156),
            tk(r("Амели", "Amélie"), 2001, r("Франция", "France"), Genre.ROMANCE, 122, 5, r("ложка, гном, фото", "spoon, gnome, photos"), T, 143),
            tk(r("Унесённые призраками", "Spirited Away"), 2001, japan, Genre.ANIMATION, 125, 5, r("баня, поезд, имя", "bathhouse, train, name"), T, 128, re = true),
            tk(r("Корпорация монстров", "Monsters, Inc."), 2001, usa, Genre.ANIMATION, 92, 4, r("двери, шкаф, Бу", "doors, closet, Boo"), M, 114, Credits.SCENE),
            tk(r("Ла-Ла Ленд", "La La Land"), 2016, usa, Genre.MUSICAL, 128, 4, r("пианино, звёзды, джаз", "piano, stars, jazz"), T, 100),
            tk(r("Прочь", "Get Out"), 2017, usa, Genre.HORROR, 104, 4, r("чашка, гипноз, сад", "teacup, hypnosis, garden"), T, 86),
            tk(r("Паразиты", "Parasite"), 2019, r("Южная Корея", "South Korea"), Genre.DRAMA, 132, 5, r("камень, лестница, ливень", "stone, stairs, flood"), T, 72),
            tk(r("Достать ножи", "Knives Out"), 2019, usa, Genre.COMEDY, 130, 4, r("свитер, пончик, кружка", "sweater, donut, mug"), M, 58),
            tk(r("Чернобыль · сезон 1", "Chernobyl · season 1"), 2019, usa, Genre.DRAMA, 330, 5, r("графит, ложь, цена", "graphite, lies, cost"), A, 44, season = true),
            tk(r("Твин Пикс · сезон 1", "Twin Peaks · season 1"), 1990, usa, Genre.NOIR, 380, 5, r("кофе, пирог, совы", "coffee, pie, owls"), T, 30, season = true),
            tk(r("Дюна", "Dune"), 2021, usa, Genre.SCIFI, 155, 4, r("песок, червь, пряность", "sand, worm, spice"), T, 16),
            tk(r("Ёжик в тумане", "Hedgehog in the Fog"), 1975, ussr, Genre.ANIMATION, 10, 5, r("туман, лошадь, варенье", "fog, horse, jam"), A, 3, re = true),
        ))
        val tp = dao.insert(Title(kind = Kind.SERIES, title = r("Твин Пикс", "Twin Peaks"), year = 1990, country = usa, genre = Genre.NOIR, minutes = 47, status = Status.RUNNING, season = 2, episode = 9, perSeason = 22, cliff = true, added = ago(40)))
        val tl = dao.insert(Title(kind = Kind.SERIES, title = r("Тед Лассо", "Ted Lasso"), year = 2020, country = usa, genre = Genre.COMEDY, minutes = 45, status = Status.RUNNING, season = 3, episode = 4, perSeason = 12, added = ago(120)))
        val lo = dao.insert(Title(kind = Kind.SERIES, title = r("Остаться в живых", "Lost"), year = 2004, country = usa, genre = Genre.ADVENTURE, minutes = 43, status = Status.SHELVED, season = 3, episode = 7, perSeason = 23, added = ago(300)))
        val eps = ArrayList<Episode>()
        for (i in 1..9) eps += Episode(series = tp, season = 2, number = i, minutes = 47, at = ago(28 - i * 3))
        for (i in 1..26) eps += Episode(series = tl, season = if (i <= 10) 1 else if (i <= 22) 2 else 3, number = i, minutes = 45, at = ago(120 - i * 4))
        for (i in 1..56) eps += Episode(series = lo, season = if (i <= 25) 1 else if (i <= 49) 2 else 3, number = i, minutes = 43, at = ago(330 - i * 3))
        dao.insertEpisodes(eps)
        fun w(t: String, y: Int, cn: String, g: Genre, m: Int, d: Int) = Title(title = t, year = y, country = cn, genre = g, minutes = m, added = ago(d))
        dao.insertTitles(listOf(
            w(r("Огни большого города", "City Lights"), 1931, usa, Genre.COMEDY, 87, 60),
            w(r("Касабланка", "Casablanca"), 1942, usa, Genre.ROMANCE, 102, 55),
            w(r("Поющие под дождём", "Singin' in the Rain"), 1952, usa, Genre.MUSICAL, 103, 50),
            w(r("Окно во двор", "Rear Window"), 1954, usa, Genre.NOIR, 112, 45),
            w(r("Бриллиантовая рука", "The Diamond Arm"), 1968, ussr, Genre.COMEDY, 94, 40),
            w(r("Солярис", "Solaris"), 1972, ussr, Genre.SCIFI, 167, 35),
            w(r("Крёстный отец", "The Godfather"), 1972, usa, Genre.DRAMA, 175, 30),
            w(r("Сияние", "The Shining"), 1980, usa, Genre.HORROR, 146, 25),
            w(r("Кин-дза-дза!", "Kin-dza-dza!"), 1986, ussr, Genre.SCIFI, 135, 20),
            w(r("Мой сосед Тоторо", "My Neighbor Totoro"), 1988, japan, Genre.ANIMATION, 86, 15),
            w(r("Город Бога", "City of God"), 2002, r("Бразилия", "Brazil"), Genre.DRAMA, 130, 10),
        ))
        if (app.reelkin.BuildConfig.DEMO) c.scope.launch(Dispatchers.IO) { posters(ctx, c) }
        // Three pets: a fox raised on the first half of the films, a dragon on a few after it, and the cat in the booth now.
        val ts = c.db.dao().ticketList()
        val a1 = ts[ts.size / 2].at; val a2 = ts[ts.size * 2 / 3].at
        c.db.dao().insert(Pet(species = Species.FOX, laid = 0, spans = "0-$a1", hatched = true, nature = Nature.CURIOUS))
        c.db.dao().insert(Pet(species = Species.DRAGON, laid = a1, spans = "$a1-$a2", hatched = true, nature = Nature.GREEDY))
        val cat = c.db.dao().insert(Pet(species = Species.CAT, laid = a2, spans = "$a2-0", hatched = true, nature = Nature.CALM))
        c.settings.update { it.copy(onboarded = false, pet = cat, cinema = r("МЕЧТА", "MAGIC"), bought = setOf("GARLAND")) }
    }

    /** Real posters from English Wikipedia for the demo, fetched once in the background. */
    private suspend fun posters(ctx: Context, c: AppContainer) {
        val dao = c.db.dao()
        val names = HashMap<String, String?>()
        suspend fun cover(en: String, year: Int?, series: Boolean): String? = names.getOrPut(en) {
            Catalog.posterFor(en, year, series)?.let { Catalog.download(it) }?.let { Covers.save(ctx, it) }
        }
        for (t in dao.ticketList()) {
            val en = EN[t.title.substringBefore(" · ")] ?: t.title.substringBefore(" · ")
            cover(en, if (t.season) null else t.year, t.season)?.let { dao.update(t.copy(cover = it)) }
        }
        for (t in dao.titles().first()) {
            val en = EN[t.title] ?: t.title
            cover(en, if (t.kind == Kind.SERIES) null else t.year, t.kind == Kind.SERIES)?.let { dao.update(t.copy(cover = it)) }
        }
    }

    private val EN = mapOf(
        "Метрополис" to "Metropolis", "Человек с киноаппаратом" to "Man with a Movie Camera", "Мальтийский сокол" to "The Maltese Falcon",
        "Двойная страховка" to "Double Indemnity", "Третий человек" to "The Third Man", "Семь самураев" to "Seven Samurai",
        "В джазе только девушки" to "Some Like It Hot", "Хороший, плохой, злой" to "The Good, the Bad and the Ugly",
        "Иван Васильевич меняет профессию" to "Ivan Vasilievich Changes Profession", "Сталкер" to "Stalker", "Нечто" to "The Thing",
        "Бегущий по лезвию" to "Blade Runner", "Вкус вишни" to "Taste of Cherry", "Амели" to "Amélie", "Унесённые призраками" to "Spirited Away",
        "Корпорация монстров" to "Monsters, Inc.", "Ла-Ла Ленд" to "La La Land", "Прочь" to "Get Out", "Паразиты" to "Parasite",
        "Достать ножи" to "Knives Out", "Чернобыль" to "Chernobyl", "Твин Пикс" to "Twin Peaks", "Дюна" to "Dune", "Ёжик в тумане" to "Hedgehog in the Fog",
        "Тед Лассо" to "Ted Lasso", "Остаться в живых" to "Lost", "Огни большого города" to "City Lights", "Касабланка" to "Casablanca",
        "Поющие под дождём" to "Singin' in the Rain", "Окно во двор" to "Rear Window", "Бриллиантовая рука" to "The Diamond Arm", "Солярис" to "Solaris",
        "Крёстный отец" to "The Godfather", "Сияние" to "The Shining", "Кин-дза-дза!" to "Kin-dza-dza!", "Мой сосед Тоторо" to "My Neighbor Totoro", "Город Бога" to "City of God",
    )
}
