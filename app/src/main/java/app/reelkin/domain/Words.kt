package app.reelkin.domain

import app.reelkin.data.Genre
import app.reelkin.data.Species
import app.reelkin.data.Trait
import app.reelkin.data.Who
import kotlin.random.Random

/** Names and sentences in both languages: Russian first, English second. */
object Words {
    fun genre(g: Genre, ru: Boolean) = when (g) {
        Genre.NOIR -> if (ru) "нуар" else "noir"
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
    }

    /** "One more X" for the pet's hint. */
    fun oneMore(g: Genre, ru: Boolean) = if (!ru) "One more ${genre(g, false)}" else when (g) {
        Genre.SCIFI, Genre.ROMANCE, Genre.COMEDY -> "Ещё одна ${genre(g, true)}"
        Genre.DOC -> "Ещё один документальный фильм"
        else -> "Ещё один ${genre(g, true)}"
    }

    fun trait(t: Trait, ru: Boolean) = when (t) {
        Trait.HAT -> if (ru) "шляпа-федора" else "a fedora"
        Trait.ANTENNA -> if (ru) "антенна" else "an antenna"
        Trait.BLUSH -> if (ru) "румянец" else "blushing cheeks"
        Trait.NOSE -> if (ru) "красный нос" else "a red nose"
        Trait.GLASSES -> if (ru) "круглые очки" else "round glasses"
        Trait.BOLTS -> if (ru) "болты на шее" else "neck bolts"
        Trait.BANDANA -> if (ru) "шейный платок" else "a neckerchief"
        Trait.GLOVES -> if (ru) "белые перчатки" else "white gloves"
        Trait.BOW -> if (ru) "бабочка" else "a bow tie"
    }

    /** "…and I'll get X" in the pet's words. */
    fun willGet(t: Trait, ru: Boolean) = if (!ru) "I'll get ${trait(t, false)}" else when (t) {
        Trait.HAT -> "у меня появится шляпа"
        Trait.ANTENNA -> "у меня вырастет антенна"
        Trait.BLUSH -> "у меня появится румянец"
        Trait.NOSE -> "у меня покраснеет нос"
        Trait.GLASSES -> "мне понадобятся очки"
        Trait.BOLTS -> "у меня вырастут болты на шее"
        Trait.BANDANA -> "у меня появится шейный платок"
        Trait.GLOVES -> "у меня появятся белые перчатки"
        Trait.BOW -> "у меня появится бабочка"
    }

    fun species(s: Species, ru: Boolean) = when (s) {
        Species.CAT -> if (ru) "кот" else "cat"
        Species.FOX -> if (ru) "лис" else "fox"
        Species.COON -> if (ru) "енот" else "raccoon"
        Species.BAT -> if (ru) "нетопырь" else "bat"
        Species.OWL -> if (ru) "сыч" else "owlet"
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
    }

    fun stage(n: Int, ru: Boolean) = when (n) { 1 -> if (ru) "малыш" else "little one"; 2 -> if (ru) "подросток" else "half-grown"; else -> if (ru) "взрослый" else "grown-up" }

    fun defaultName(ru: Boolean) = if (ru) "Люмик" else "Lumi"

    fun seat(i: Int, ru: Boolean) = if (ru) "ряд ${i / 10 + 1}, место ${i % 10 + 1}" else "row ${i / 10 + 1}, seat ${i % 10 + 1}"

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
    }

    /** Something to say when poked, from what the cinema holds. */
    fun poke(c: Cinema, ru: Boolean, rnd: Random = Random.Default): String {
        val l = ArrayList<String>()
        c.running.firstOrNull { it.cliff }?.let { l += if (ru) "«${it.title}» оборвался на самом интересном. Я грызу попкорн." else "${it.title} stopped at the worst moment. I'm chewing popcorn." }
        Cinema.DECADES.filter { it >= 1920 }.firstOrNull { it !in c.decades }?.let { d ->
            val w = c.watch.firstOrNull { it.year != null && Cinema.decade(it.year) == d }
            l += if (ru) "В $d-х у нас пусто. " + (w?.let { "В афише есть «${it.title}»." } ?: "Говорят, там было хорошее кино.")
            else "Nothing from the ${d}s yet. " + (w?.let { "${it.title} is on the bill." } ?: "They say films were good then.")
        }
        c.nearTrait?.let { g -> l += if (ru) "${oneMore(g, true)} — и ${willGet(g.trait!!, true)}." else "${oneMore(g, false)} and ${willGet(g.trait!!, false)}." }
        c.scenes.lastOrNull()?.let { l += if (ru) "Ты досидел до конца титров «${it.title}». Я тоже." else "You sat through the credits of ${it.title}. So did I." }
        if (c.seats.isNotEmpty()) l += if (ru) "Я пересчитал кадры: ${fmt(c.frames)}. Сбился два раза." else "I counted the frames: ${fmt(c.frames)}. Lost count twice."
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
    fun films(n: Int, ru: Boolean) = if (ru) "$n " + plural(n, "фильм", "фильма", "фильмов") else "$n film" + if (n == 1) "" else "s"
}
