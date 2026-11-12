package app.reelkin.domain

import app.reelkin.data.Genre
import app.reelkin.data.Title
import app.reelkin.ui.art.hash

enum class Mood(val ru: String, val en: String, val genres: Set<Genre>) {
    LAUGH("посмеяться", "a laugh", setOf(Genre.COMEDY, Genre.ANIMATION, Genre.MUSICAL, Genre.REALITY)),
    COSY("уютно", "cosy", setOf(Genre.ANIMATION, Genre.ROMANCE, Genre.MUSICAL, Genre.COMEDY, Genre.REALITY)),
    THINK("подумать", "to think", setOf(Genre.DRAMA, Genre.SCIFI, Genre.DOC)),
    FEAR("испугаться", "a scare", setOf(Genre.HORROR)),
    MYSTERY("загадка", "a mystery", setOf(Genre.NOIR)),
    BEAUTY("красиво", "something beautiful", setOf(Genre.SCIFI, Genre.ADVENTURE, Genre.WESTERN, Genre.ROMANCE)),
}

/** The pet's choice: one film, two for a double bill, or an episode when nothing fits. */
data class Pick(val films: List<Title>, val why: String, val episode: Title? = null)

object Picker {
    val TIMES = listOf(30, 95, 125, 200, 400)

    fun pick(c: Cinema, minutes: Int, mood: Mood, skip: Set<Long>, ru: Boolean): Pick? {
        val pool = c.watch.filter { it.kind == app.reelkin.data.Kind.FILM && it.id !in skip }
        fun score(f: Title): Pair<Double, List<String>> {
            var s = 0.0; val why = ArrayList<String>()
            if (f.genre in mood.genres) s += 3
            f.year?.let { y -> if (Cinema.decade(y) !in c.decades) { s += 2; why += if (ru) "заодно ${Cinema.decade(y)}-е, которых у тебя ещё нет" else "and it's the ${Cinema.decade(y)}s, which you don't have yet" } }
            if (f.country.isNotBlank() && f.country !in c.countries) { s += 2; why += if (ru) "и новый штамп: ${f.country}" else "and a new stamp: ${f.country}" }
            if (c.pet?.let { f.genre in Pets.genres(it.species) } == true) { s += 1; why += if (ru) "а я такое люблю" else "and I love those" }
            s += (hash(f.title + mood.name) % 10) / 20.0
            return s to why
        }
        if (minutes >= 400) {
            var best: Pair<Double, Pick>? = null
            for (a in pool) for (b in pool) {
                if (a.id >= b.id || a.minutes + b.minutes > 330 || a.minutes == 0 || b.minutes == 0) continue
                val link = when {
                    a.year != null && b.year != null && Cinema.decade(a.year) == Cinema.decade(b.year) -> if (ru) "два фильма ${Cinema.decade(a.year)}-х" else "two films from the ${Cinema.decade(a.year)}s"
                    a.genre == b.genre -> if (ru) "дважды ${Words.genre(a.genre, true)}" else "${Words.genre(a.genre, false)} twice"
                    a.country.isNotBlank() && a.country == b.country -> if (ru) "вечер кино: ${a.country}" else "a night of ${a.country}"
                    else -> null
                } ?: continue
                val s = score(a).first + score(b).first
                if (best == null || s > best.first) best = s to Pick(listOf(a, b), if (ru) "Двойной сеанс: $link. Вместе ${Words.hm(a.minutes + b.minutes)}." else "A double bill: $link. ${Words.hm(a.minutes + b.minutes)} together.")
            }
            best?.let { return it.second }
        }
        val fit = pool.filter { it.minutes in 1..(minutes + 5) }.map { Triple(it, score(it).first, score(it).second) }.sortedByDescending { it.second }
        val top = fit.firstOrNull()
        if (top == null) {
            val ep = c.running.firstOrNull { it.minutes <= minutes } ?: return null
            return Pick(emptyList(), if (ru) "За это время влезет только серия." + (if (ep.cliff) " Тем более она закончилась на самом интересном." else " Тебя там ждут.")
                else "Only an episode fits." + (if (ep.cliff) " And it stopped at the worst moment." else " It's waiting for you."), ep)
        }
        val (f, _, why) = top
        val head = if (ru) "${Words.hm(f.minutes)}: как раз влезает. " else "${Words.hm(f.minutes)}: fits just right. "
        val m = if (f.genre in mood.genres) (if (ru) "Хочется «${mood.ru}», а это ${Words.genre(f.genre, true)}." else "You want ${mood.en}; it's ${Words.genre(f.genre, false)}.")
            else (if (ru) "Не совсем «${mood.ru}», но тебе понравится." else "Not quite ${mood.en}, but you'll like it.")
        val tail = if (why.isEmpty()) "" else " " + why.joinToString(", ").replaceFirstChar { it.uppercase() } + "."
        return Pick(listOf(f), head + m + tail)
    }
}
