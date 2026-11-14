package app.reelkin.domain

import app.reelkin.data.Air
import app.reelkin.data.Genre
import app.reelkin.data.Seasons
import app.reelkin.data.Species
import app.reelkin.data.Title
import app.reelkin.data.Form
import app.reelkin.data.Nature
import app.reelkin.data.Stage
import app.reelkin.data.Who
import kotlin.random.Random

/** Names and sentences in both languages: Russian first, English second. */
object Words {
    fun genre(g: Genre, ru: Boolean) = when (g) {
        Genre.NOIR -> if (ru) "детектив/триллер" else "mystery/thriller"
        Genre.SCIFI -> if (ru) "фантастика" else "sci-fi"
        Genre.ROMANCE -> if (ru) "мелодрама" else "romance"
        Genre.COMEDY -> if (ru) "комедия" else "comedy"
        Genre.DOC -> if (ru) "документальное" else "documentary"
        Genre.HORROR -> if (ru) "хоррор" else "horror"
        Genre.WESTERN -> if (ru) "вестерн" else "western"
        Genre.ANIMATION -> if (ru) "мультфильм" else "animation"
        Genre.MUSICAL -> if (ru) "мюзикл" else "musical"
        Genre.DRAMA -> if (ru) "драма" else "drama"
        Genre.ADVENTURE -> if (ru) "приключения" else "adventure"
        Genre.REALITY -> if (ru) "реалити-шоу" else "reality"
    }

    /** Season numbers as «сезон 3» or «сезоны 1–3, 5». */
    fun seasons(l: List<Int>, ru: Boolean): String {
        val runs = ArrayList<String>()
        var i = 0
        val s = l.distinct().sorted()
        while (i < s.size) {
            var j = i
            while (j + 1 < s.size && s[j + 1] == s[j] + 1) j++
            runs += if (j == i) "${s[i]}" else "${s[i]}–${s[j]}"
            i = j + 1
        }
        val many = s.size > 1
        return (if (ru) (if (many) "сезоны " else "сезон ") else (if (many) "seasons " else "season ")) + runs.joinToString(", ")
    }

    /** "One more X" for the pet's hint. */
    fun oneMore(g: Genre, ru: Boolean) = if (!ru) (if (g == Genre.NOIR) "One more mystery or thriller" else "One more ${genre(g, false)}") else when (g) {
        Genre.NOIR -> "Ещё один детектив или триллер"
        Genre.SCIFI, Genre.ROMANCE, Genre.COMEDY -> "Ещё одна ${genre(g, true)}"
        Genre.DOC -> "Ещё один документальный фильм"
        else -> "Ещё один ${genre(g, true)}"
    }

    fun species(s: Species, ru: Boolean) = when (s) {
        Species.CAT -> if (ru) "кот-сыщик" else "detective cat"
        Species.FOX -> if (ru) "лис-аниматор" else "animator fox"
        Species.OWL -> if (ru) "сова-критик" else "critic owl"
        Species.BAT -> if (ru) "нетопырь" else "bat"
        Species.COON -> if (ru) "енот-комик" else "comic raccoon"
        Species.HAMSTER -> if (ru) "хомяк-космонавт" else "astro hamster"
        Species.ARMADILLO -> if (ru) "броненосец-шериф" else "sheriff armadillo"
        Species.LOVEBIRD -> if (ru) "неразлучник" else "lovebird"
        Species.MONKEY -> if (ru) "мартышка-путешественница" else "explorer monkey"
        Species.FROG -> if (ru) "лягушка-чечёточница" else "tap-dancing frog"
        Species.MOTH -> if (ru) "мотылёк" else "moth"
        Species.SLOTH -> if (ru) "ленивец" else "sloth"
        Species.PENGUIN -> if (ru) "пингвин из немого кино" else "silent-film penguin"
        Species.REDPANDA -> if (ru) "малая панда" else "red panda"
        Species.PUG -> if (ru) "мопс" else "pug"
        Species.MOUSE -> if (ru) "мышка-билетёр" else "usher mouse"
        Species.GOLDFISH -> if (ru) "золотая рыбка" else "goldfish"
        Species.SQUIRREL -> if (ru) "белка" else "squirrel"
        Species.GHOST -> if (ru) "призрак последнего ряда" else "back-row ghost"
        Species.DRAGON -> if (ru) "дракончик" else "little dragon"
        Species.UNICORN -> if (ru) "единорог" else "unicorn"
        Species.PHOENIX -> if (ru) "феникс" else "phoenix"
        Species.KITSUNE -> if (ru) "кицунэ" else "kitsune"
        Species.YETI -> if (ru) "йети" else "yeti"
        Species.GRIFFIN -> if (ru) "грифон" else "griffin"
        Species.GOLEM -> if (ru) "голем из коробок" else "can golem"
        Species.ROBOT -> if (ru) "жестяной робот" else "tin robot"
        Species.FAIRY -> if (ru) "фея свиданий" else "date-night fairy"
        Species.POPKIN -> if (ru) "попкорник" else "popkin"
        Species.GARGOYLE -> if (ru) "горгулья" else "gargoyle"
        Species.SPHINX -> if (ru) "сфинкс" else "sphinx"
        Species.WISP -> if (ru) "огонёк" else "wisp"
        Species.TANUKI -> if (ru) "тануки" else "tanuki"
        Species.KOI -> if (ru) "карп кои" else "koi carp"
        Species.KAPPA -> if (ru) "каппа" else "kappa"
        Species.NEKOMATA -> if (ru) "нэкомата" else "nekomata"
        Species.TENGU -> if (ru) "воронёнок-тэнгу" else "crow tengu"
        Species.SLIME -> if (ru) "слайм" else "slime"
        Species.MECHA -> if (ru) "меха-робот" else "mecha"
        Species.MANEKI -> if (ru) "манэки-нэко" else "maneki-neko"
        Species.DARUMA -> if (ru) "дарума" else "daruma"
        Species.CRANE -> if (ru) "журавлик" else "paper crane"
        Species.SHIBA -> if (ru) "сиба-ину" else "shiba inu"
        Species.ONI -> if (ru) "чертёнок они" else "little oni"
        Species.ONIGIRI -> if (ru) "онигири" else "onigiri"
        Species.KIRIN -> if (ru) "кирин" else "kirin"
        Species.RYU -> if (ru) "дракон рю" else "ryu dragon"
        Species.SUZUME -> if (ru) "воробей судзумэ" else "suzume sparrow"
        Species.BAKU -> if (ru) "баку" else "baku"
    }

    /** What a species loves, in a few words. */
    fun loves(s: Species, ru: Boolean): String = when (s) {
        Species.CAT -> if (ru) "детективы, триллеры и драмы" else "mysteries, thrillers and dramas"
        Species.FOX -> if (ru) "мультфильмы" else "animation"
        Species.OWL -> if (ru) "документальное кино и корешки со словами" else "documentaries and stubs with words on them"
        Species.BAT -> if (ru) "хорроры" else "horror"
        Species.COON -> if (ru) "комедии и реалити-шоу" else "comedies and reality shows"
        Species.HAMSTER -> if (ru) "фантастику" else "sci-fi"
        Species.ARMADILLO -> if (ru) "вестерны" else "westerns"
        Species.LOVEBIRD -> if (ru) "мелодрамы" else "romance"
        Species.MONKEY -> if (ru) "приключения" else "adventure"
        Species.FROG -> if (ru) "мюзиклы" else "musicals"
        Species.MOTH -> if (ru) "кино после 23:00" else "films after 11 pm"
        Species.SLOTH -> if (ru) "долгие вечера у экрана, от трёх часов в день" else "long evenings, three hours a day and more"
        Species.PENGUIN -> if (ru) "фильмы старше 1970 года" else "films from before 1970"
        Species.REDPANDA -> if (ru) "первый фильм из новой страны" else "the first film from a new country"
        Species.PUG -> if (ru) "кино в компании" else "films watched with others"
        Species.MOUSE -> if (ru) "сцены после титров" else "post-credits scenes"
        Species.GOLDFISH -> if (ru) "пересмотры (она всё равно забыла концовку)" else "rewatches (it forgot the ending anyway)"
        Species.SQUIRREL -> if (ru) "серии сериалов" else "episodes of series"
        Species.GHOST -> if (ru) "кино в одиночку после 22:00" else "films alone after 10 pm"
        Species.DRAGON -> if (ru) "длинные фильмы, от двух с половиной часов" else "long films, two and a half hours and more"
        Species.UNICORN -> if (ru) "фильмы на пять попкорнов" else "five-popcorn films"
        Species.PHOENIX -> if (ru) "первый фильм после перерыва в три дня и больше" else "the first film after three days and more without any"
        Species.KITSUNE -> if (ru) "кино из Японии, Кореи и Китая" else "films from Japan, Korea and China"
        Species.YETI -> if (ru) "кино зимой" else "films in winter"
        Species.GRIFFIN -> if (ru) "первый фильм из нового десятилетия" else "the first film from a new decade"
        Species.GOLEM -> if (ru) "любые фильмы, без разбора" else "any film at all"
        Species.ROBOT -> if (ru) "кино каждый день, без пропусков" else "something every day, no gaps"
        Species.FAIRY -> if (ru) "кино вдвоём" else "films for two"
        Species.POPKIN -> if (ru) "фильмы на четыре попкорна и больше" else "films of four popcorn and more"
        Species.GARGOYLE -> if (ru) "кино в выходные" else "weekend films"
        Species.SPHINX -> if (ru) "фильмы старше 1950 года" else "films from before 1950"
        Species.WISP -> if (ru) "короткие фильмы, до полутора часов" else "short films, an hour and a half or less"
        Species.TANUKI -> if (ru) "любое аниме: фильмы и серии" else "any anime, films and episodes"
        Species.KOI -> if (ru) "полнометражное аниме" else "full-length anime films"
        Species.KAPPA -> if (ru) "аниме-сериалы: серии и сезоны" else "anime series, episodes and seasons"
        Species.NEKOMATA -> if (ru) "аниме после 23:00" else "anime after 11 pm"
        Species.TENGU -> if (ru) "аниме старше 2000 года" else "anime from before 2000"
        Species.SLIME -> if (ru) "аниме в год выхода" else "anime in the year it came out"
        Species.MECHA -> if (ru) "аниме в дни, когда у экрана от трёх часов" else "anime on days of three hours and more"
        Species.MANEKI -> if (ru) "аниме на четыре попкорна и больше" else "anime of four popcorn and more"
        Species.DARUMA -> if (ru) "пересмотры аниме" else "anime watched again"
        Species.CRANE -> if (ru) "аниме, о котором на корешке есть слова" else "anime with words on its stub"
        Species.SHIBA -> if (ru) "аниме день за днём, без пропусков" else "anime day after day, no gaps"
        Species.ONI -> if (ru) "аниме в выходные" else "weekend anime"
        Species.ONIGIRI -> if (ru) "аниме в компании" else "anime watched with others"
        Species.KIRIN -> if (ru) "аниме на пять попкорнов" else "five-popcorn anime"
        Species.RYU -> if (ru) "длинное полнометражное аниме, от двух часов" else "long anime films, two hours and more"
        Species.SUZUME -> if (ru) "аниме с утра, до полудня" else "anime in the morning, before noon"
        Species.BAKU -> if (ru) "первое аниме после недели без него" else "the first anime after a week without"
    }

    /** What opens a species' can. */
    fun opens(h: Hint, ru: Boolean): String {
        val n = h.need
        return when (h.species) {
            Species.CAT, Species.FOX, Species.OWL -> if (ru) "открыта с самого начала" else "open from the start"
            Species.BAT -> if (ru) "$n хорроров" else "$n horror films"
            Species.COON -> if (ru) "$n комедий или реалити" else "$n comedies or reality shows"
            Species.HAMSTER -> if (ru) "$n фантастических фильмов" else "$n sci-fi films"
            Species.ARMADILLO -> if (ru) "$n вестерна" else "$n westerns"
            Species.LOVEBIRD -> if (ru) "$n мелодрам" else "$n romances"
            Species.MONKEY -> if (ru) "$n приключенческих фильмов" else "$n adventure films"
            Species.FROG -> if (ru) "$n мюзикла" else "$n musicals"
            Species.MOTH -> if (ru) "кино после 23:00 в $n разных вечеров" else "films after 11 pm on $n nights"
            Species.SLOTH -> if (ru) "$n дня, когда за день 4 серии и больше" else "$n days of 4 episodes or more"
            Species.PENGUIN -> if (ru) "$n фильмов старше 1970 года" else "$n films from before 1970"
            Species.REDPANDA -> if (ru) "фильмы из $n стран" else "films from $n countries"
            Species.PUG -> if (ru) "$n фильмов в компании" else "$n films watched with others"
            Species.MOUSE -> if (ru) "$n сцены после титров" else "$n post-credits scenes"
            Species.GOLDFISH -> if (ru) "$n пересмотра" else "$n rewatches"
            Species.SQUIRREL -> if (ru) "$n досмотренных сезона" else "$n seasons finished"
            Species.GHOST -> if (ru) "$n фильма в одиночку после полуночи" else "$n films alone after midnight"
            Species.DRAGON -> if (ru) "$n фильма от двух с половиной часов" else "$n films of two and a half hours or more"
            Species.UNICORN -> if (ru) "$n фильмов на пять попкорнов" else "$n five-popcorn films"
            Species.PHOENIX -> if (ru) "вернуться к кино после месяца без него" else "come back to films after a month without"
            Species.KITSUNE -> if (ru) "$n фильма из Японии, Кореи или Китая" else "$n films from Japan, Korea or China"
            Species.YETI -> if (ru) "$n фильмов зимой" else "$n films in winter"
            Species.GRIFFIN -> if (ru) "фильмы из $n десятилетий" else "films from $n decades"
            Species.GOLEM -> if (ru) "$n фильмов" else "$n films"
            Species.ROBOT -> if (ru) "кино $n дней подряд" else "something watched $n days in a row"
            Species.FAIRY -> if (ru) "$n фильмов вдвоём" else "$n films for two"
            Species.POPKIN -> if (ru) "$n покупки в буфете" else "$n things from the buffet"
            Species.GARGOYLE -> if (ru) "$n фильмов в выходные" else "$n weekend films"
            Species.SPHINX -> if (ru) "$n фильма старше 1950 года" else "$n films from before 1950"
            Species.WISP -> if (ru) "кино в $n разных дней" else "films on $n different days"
            Species.TANUKI -> if (ru) "аниме $n раз: фильмы, сезоны или серии" else "anime $n times: films, seasons or episodes"
            Species.KOI -> if (ru) "$n полнометражных аниме" else "$n full-length anime films"
            Species.KAPPA -> if (ru) "$n серий аниме или целый сезон" else "$n episodes of anime, or a whole season"
            Species.NEKOMATA -> if (ru) "аниме после 23:00 в $n разных вечера" else "anime after 11 pm on $n nights"
            Species.TENGU -> if (ru) "аниме старше 2000 года $n раза" else "anime from before 2000 $n times"
            Species.SLIME -> if (ru) "аниме в год выхода $n раз" else "anime in the year it came out $n times"
            Species.MECHA -> if (ru) "аниме в $n дня, когда у экрана от трёх часов" else "anime on $n days of three hours and more"
            Species.MANEKI -> if (ru) "$n аниме на четыре попкорна и больше" else "$n anime of four popcorn and more"
            Species.DARUMA -> if (ru) "$n пересмотра аниме" else "$n anime watched again"
            Species.CRANE -> if (ru) "$n корешка аниме со словами" else "$n anime stubs with words"
            Species.SHIBA -> if (ru) "аниме $n дней подряд" else "anime $n days in a row"
            Species.ONI -> if (ru) "аниме в $n разных выходных дней" else "anime on $n weekend days"
            Species.ONIGIRI -> if (ru) "$n аниме в компании" else "$n anime watched with others"
            Species.KIRIN -> if (ru) "$n аниме на пять попкорнов" else "$n five-popcorn anime"
            Species.RYU -> if (ru) "$n полнометражных аниме от двух часов" else "$n anime films of two hours or more"
            Species.SUZUME -> if (ru) "аниме до полудня в $n разных дня" else "anime before noon on $n days"
            Species.BAKU -> if (ru) "вернуться к аниме после двух недель без него" else "come back to anime after two weeks without"
        }
    }

    fun nature(n: Nature, ru: Boolean): String = when (n) {
        Nature.CALM -> if (ru) "спокойный: смотрит что угодно и не жалуется" else "calm: watches anything and never complains"
        Nature.SLEEPY -> if (ru) "соня: засыпает на титрах" else "sleepy: dozes off at the credits"
        Nature.JUMPY -> if (ru) "пугливый: прячется на страшных сценах" else "jumpy: hides at the scary bits"
        Nature.CURIOUS -> if (ru) "любопытный: +5 попкорна за каждую новую страну и десятилетие" else "curious: +5 popcorn for every new country and decade"
        Nature.SHY -> if (ru) "застенчивый: говорит мало, но метко" else "shy: says little, but to the point"
        Nature.CHATTY -> if (ru) "болтун: комментирует весь фильм" else "chatty: comments on the whole film"
        Nature.GREEDY -> if (ru) "обжора: растёт на десятую часть быстрее" else "greedy: grows a tenth faster"
    }

    fun form(f: Form, ru: Boolean): String = when (f) {
        Form.SHADOW -> if (ru) "Тень (детективы и хорроры)" else "Shadow (mysteries and horror)"
        Form.STAR -> if (ru) "Звезда (фантастика и приключения)" else "Star (sci-fi and adventure)"
        Form.HEART -> if (ru) "Сердце (мелодрамы, драмы, мюзиклы)" else "Heart (romance, drama, musicals)"
        Form.LAUGH -> if (ru) "Смех (комедии, мультфильмы, реалити)" else "Laugh (comedy, animation, reality)"
        Form.DUST -> if (ru) "Пыль дорог (вестерны и документальное)" else "Road dust (westerns and documentaries)"
    }

    fun who(w: Who, ru: Boolean) = when (w) {
        Who.DETECTIVE -> if (ru) "детектив" else "a detective"
        Who.ASTRO -> if (ru) "космонавт" else "an astronaut"
        Who.LADY -> if (ru) "дама с бантом" else "a lady with a bow"
        Who.CLOWN -> if (ru) "клоун" else "a clown"
        Who.PROF -> if (ru) "профессор" else "a professor"
        Who.GHOST -> if (ru) "призрак" else "a ghost"
        Who.COWBOY -> if (ru) "ковбой" else "a cowboy"
        Who.MOUSE -> if (ru) "мышонок" else "a mouse"
        Who.TOPHAT -> if (ru) "господин в цилиндре" else "a gent in a top hat"
        Who.BERET -> if (ru) "художник в берете" else "a painter in a beret"
        Who.PITH -> if (ru) "путешественник" else "an explorer"
        Who.SELFIE -> if (ru) "зритель с телефоном" else "a fan filming on a phone"
    }

    fun stage(st: Stage, ru: Boolean) = when (st) {
        Stage.EGG -> if (ru) "в коробке" else "in its can"
        Stage.BABY -> if (ru) "малыш" else "little one"
        Stage.YOUNG -> if (ru) "подросток" else "half-grown"
        Stage.ADULT -> if (ru) "взрослый" else "grown-up"
        Stage.ANCIENT -> if (ru) "старожил" else "old-timer"
    }

    /** A pet's name before it's given one: Lumi for the cat, as it always was, then one per species. */
    fun defaultName(sp: Species?, ru: Boolean) = when (sp) {
        null, Species.CAT -> if (ru) "Люмик" else "Lumi"
        Species.FOX -> if (ru) "Рыжик" else "Rusty"
        Species.OWL -> if (ru) "Профессор" else "Professor"
        Species.BAT -> if (ru) "Носферату" else "Nosferatu"
        Species.COON -> if (ru) "Хрум" else "Crunch"
        Species.HAMSTER -> if (ru) "Гагарин" else "Orbit"
        Species.ARMADILLO -> if (ru) "Шериф" else "Sheriff"
        Species.LOVEBIRD -> if (ru) "Ромео" else "Romeo"
        Species.MONKEY -> if (ru) "Джонс" else "Jones"
        Species.FROG -> if (ru) "Фред" else "Fred"
        Species.MOTH -> if (ru) "Луч" else "Beam"
        Species.SLOTH -> if (ru) "Плед" else "Blanket"
        Species.PENGUIN -> if (ru) "Чарли" else "Charlie"
        Species.REDPANDA -> if (ru) "Паспорт" else "Passport"
        Species.PUG -> if (ru) "Компот" else "Buddy"
        Species.MOUSE -> if (ru) "Титрик" else "Credits"
        Species.GOLDFISH -> if (ru) "Дежавю" else "Déjà vu"
        Species.SQUIRREL -> if (ru) "Запаска" else "Stash"
        Species.GHOST -> if (ru) "Шорох" else "Rustle"
        Species.DRAGON -> if (ru) "Эпос" else "Epic"
        Species.UNICORN -> if (ru) "Пятёрка" else "Five"
        Species.PHOENIX -> if (ru) "Уголёк" else "Ember"
        Species.KITSUNE -> if (ru) "Юки" else "Yuki"
        Species.YETI -> if (ru) "Сугроб" else "Snowdrift"
        Species.GRIFFIN -> if (ru) "Хронос" else "Chronos"
        Species.GOLEM -> if (ru) "Жестянка" else "Tinny"
        Species.ROBOT -> if (ru) "Будильник" else "Tick"
        Species.FAIRY -> if (ru) "Искорка" else "Sparkle"
        Species.POPKIN -> if (ru) "Хрустик" else "Kernel"
        Species.GARGOYLE -> if (ru) "Суббота" else "Saturday"
        Species.SPHINX -> if (ru) "Люмьер" else "Lumière"
        Species.WISP -> if (ru) "Светлячок" else "Glim"
        Species.TANUKI -> if (ru) "Листик" else "Leaf"
        Species.KOI -> if (ru) "Ручеёк" else "Brook"
        Species.KAPPA -> if (ru) "Огурчик" else "Pickle"
        Species.NEKOMATA -> if (ru) "Полночь" else "Midnight"
        Species.TENGU -> if (ru) "Сэнсэй" else "Sensei"
        Species.SLIME -> if (ru) "Буль" else "Blob"
        Species.MECHA -> if (ru) "Болтик" else "Bolt"
        Species.MANEKI -> if (ru) "Удача" else "Lucky"
        Species.DARUMA -> if (ru) "Неваляшка" else "Wobble"
        Species.CRANE -> if (ru) "Оригами" else "Origami"
        Species.SHIBA -> if (ru) "Хатико" else "Hachi"
        Species.ONI -> if (ru) "Рожок" else "Horn"
        Species.ONIGIRI -> if (ru) "Рисик" else "Rice"
        Species.KIRIN -> if (ru) "Цилинь" else "Qilin"
        Species.RYU -> if (ru) "Облако" else "Cloud"
        Species.SUZUME -> if (ru) "Чирик" else "Chirp"
        Species.BAKU -> if (ru) "Сон" else "Dream"
    }

    /** A seat in the hall; -1, a stub without one (seen long ago, only noted down). */
    fun seat(i: Int, ru: Boolean) = if (i < 0) (if (ru) "смотрел раньше" else "seen before") else if (ru) "ряд ${i / 10 + 1}, место ${i % 10 + 1}" else "row ${i / 10 + 1}, seat ${i % 10 + 1}"

    val AWARDS = listOf("Фильм года" to "Film of the year", "Злодей года" to "Villain of the year", "Музыка года" to "Music of the year",
        "Сцена года" to "Scene of the year", "Разочарование года" to "Letdown of the year")

    /** What the pet says during a film of this genre, and how it looks saying it. */
    fun react(g: Genre, ru: Boolean): String = when (g) {
        Genre.HORROR -> if (ru) "Ой. Ой-ой." else "Oh. Oh no."
        Genre.COMEDY -> if (ru) "Ха!" else "Ha!"
        Genre.MUSICAL -> if (ru) "♪ Та-да-да ♪" else "♪ Ta-da-da ♪"
        Genre.ROMANCE -> if (ru) "Я не плачу. Это пыль в луче." else "I'm not crying. It's dust in the beam."
        Genre.DRAMA -> "…"
        Genre.SCIFI -> if (ru) "Ух ты. Космос." else "Wow. Space."
        Genre.NOIR -> if (ru) "Он врёт. Точно врёт." else "He's lying. I know he is."
        Genre.WESTERN -> if (ru) "Ну стреляй уже." else "Just shoot already."
        Genre.ANIMATION -> if (ru) "Ещё!" else "More!"
        Genre.DOC -> if (ru) "Записываю." else "Taking notes."
        Genre.ADVENTURE -> if (ru) "Вперёд!" else "Onwards!"
        Genre.REALITY -> if (ru) "Сейчас будет скандал. Чувствую." else "There's a row coming. I can feel it."
    }

    /** Something to say when poked, from what the cinema holds. */
    fun poke(c: Cinema, ru: Boolean, rnd: Random = Random.Default): String {
        val l = ArrayList<String>()
        c.running.firstOrNull { it.cliff }?.let { l += if (ru) "В «${it.title}» серия закончилась на самом интересном. Я грызу попкорн." else "${it.title} stopped at the worst moment. I'm chewing popcorn." }
        Cinema.DECADES.filter { it >= 1920 }.firstOrNull { it !in c.decades }?.let { d ->
            val w = c.watch.firstOrNull { it.year != null && Cinema.decade(it.year) == d }
            l += if (ru) "В $d-х у нас пусто. " + (w?.let { "В афише есть «${it.title}»." } ?: "Говорят, там было хорошее кино.")
            else "Nothing from the ${d}s yet. " + (w?.let { "${it.title} is on the bill." } ?: "They say films were good then.")
        }
        c.nearCan?.let { h -> l += if (ru) "Слышишь? В коробке «${species(h.species, true)}» кто-то шуршит. Осталось чуть-чуть: ${opens(h, true)}." else "Hear that? Something rustles in the ${species(h.species, false)}'s can. Nearly there: ${opens(h, false)}." }
        c.pet?.takeIf { !it.egg }?.let { p -> l += if (ru) "Я люблю ${loves(p.species, true)}. Такое кино кормит меня в полтора раза сытнее." else "I love ${loves(p.species, false)}. They feed me one and a half times as much." }
        if (c.newCans.isNotEmpty()) l += if (ru) "Открылась новая коробка. Загляни к питомцам." else "A new can has opened. Have a look at the pets."
        c.scenes.lastOrNull()?.let { l += if (ru) "Ты досидел до конца титров «${it.title}». Я тоже." else "You sat through the credits of ${it.title.trimEnd('.')}. So did I." }
        if (c.seats.isNotEmpty()) l += if (ru) "Мы вместе просмотрели кино на ${minutes(c.minutes, true).substringBeforeLast(' ')} мин. Я считал." else "We've watched ${minutes(c.minutes, false)} together. I counted."
        l += if (ru) "Сегодня можно ничего не смотреть. Я протру объектив." else "We don't have to watch anything today. I'll polish the lens."
        if (c.seats.size > 3) l += if (ru) "В зале занято ${c.seats.size.coerceAtMost(100)} мест. Кто-то опять шуршит фантиком." else "${c.seats.size.coerceAtMost(100)} seats taken. Someone's rustling a wrapper again."
        return l[rnd.nextInt(l.size)]
    }

    fun fmt(n: Long): String = String.format(java.util.Locale.ROOT, "%,d", n).replace(',', ' ')

    fun hm(m: Int) = "${m / 60}:${(m % 60).toString().padStart(2, '0')}"

    fun plural(n: Int, one: String, few: String, many: String): String {
        val m10 = n % 10; val m100 = n % 100
        return when { m10 == 1 && m100 != 11 -> one; m10 in 2..4 && m100 !in 12..14 -> few; else -> many }
    }
    fun minutes(n: Long, ru: Boolean) = fmt(n) + " " + if (ru) plural((n % 100).toInt(), "минута", "минуты", "минут") else if (n == 1L) "minute" else "minutes"
    fun seasons(n: Int, ru: Boolean) = if (ru) "$n " + plural(n, "сезон", "сезона", "сезонов") else "$n season" + if (n == 1) "" else "s"

    fun films(n: Int, ru: Boolean) = if (ru) "$n " + plural(n, "фильм", "фильма", "фильмов") else "$n film" + if (n == 1) "" else "s"

    /** A day as «12 марта 2027» or «Mar 12, 2027», in UTC since the catalogue's days are dates, not moments. */
    fun day(ms: Long, ru: Boolean): String = java.text.SimpleDateFormat(if (ru) "d MMMM yyyy" else "MMM d, yyyy", if (ru) java.util.Locale.forLanguageTag("ru") else java.util.Locale.ENGLISH)
        .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date(ms))

    /** What the season [n] of a series is waiting for, or null when it's out or nothing is known. */
    fun air(t: Title, ru: Boolean, now: Long = System.currentTimeMillis(), n: Int = t.season): String? = when (val a = Seasons.air(t, n, now)) {
        is Air.On -> if (ru) "сезон $n выйдет ${day(a.at, true)}" else "season $n comes out ${day(a.at, false)}"
        Air.Later -> if (ru) "сезон $n ещё не вышел, даты пока нет" else "season $n isn't out yet, no date"
        Air.None -> if (ru) "сезона $n пока нет" else "no season $n yet"
        Air.Out, Air.Unknown -> null
    }

    /** «вышло 3 сезона» / «3 seasons out», or null when unknown. */
    fun seasonsOut(t: Title, ru: Boolean, now: Long = System.currentTimeMillis()): String? {
        val n = Seasons.out(Seasons.decode(t.plan), now).takeIf { it > 0 } ?: return null
        return if (ru) plural(n, "вышел", "вышло", "вышло") + " $n " + plural(n, "сезон", "сезона", "сезонов") else "$n season${if (n == 1) "" else "s"} out"
    }
}
