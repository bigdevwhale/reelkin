package app.reelkin.data

/**
 * Subgenres: finer names under the twelve genres, kept on a title and its stubs as words joined by
 * commas. The genre still decides the poster, the spectator and what the pets make of it; subgenres
 * are only for the eye and the search. A known one is kept by its [Sub.key], so it reads in either
 * language; one typed by hand is kept as typed.
 */
data class Sub(val key: String, val genre: Genre, val ru: String, val en: String, val hints: List<String>)

object Subgenres {
    val all: List<Sub> = listOf(
        Sub("noir", Genre.NOIR, "нуар", "noir", listOf("film noir")),
        Sub("neo-noir", Genre.NOIR, "неонуар", "neo-noir", listOf("neo-noir")),
        Sub("detective", Genre.NOIR, "детектив", "detective", listOf("detective", "mystery")),
        Sub("crime", Genre.NOIR, "криминал", "crime", listOf("crime", "gangster", "mafia")),
        Sub("thriller", Genre.NOIR, "триллер", "thriller", listOf("thriller", "suspense")),
        Sub("psycho", Genre.NOIR, "психологический триллер", "psychological thriller", listOf("psychological thriller")),
        Sub("spy", Genre.NOIR, "шпионский", "spy", listOf("spy", "espionage")),
        Sub("heist", Genre.NOIR, "ограбление", "heist", listOf("heist", "caper")),
        Sub("legal", Genre.NOIR, "судебный", "legal", listOf("legal", "courtroom")),

        Sub("cyberpunk", Genre.SCIFI, "киберпанк", "cyberpunk", listOf("cyberpunk")),
        Sub("space-opera", Genre.SCIFI, "космоопера", "space opera", listOf("space opera", "space western", "space adventure")),
        Sub("dystopia", Genre.SCIFI, "антиутопия", "dystopia", listOf("dystopia")),
        Sub("post-apo", Genre.SCIFI, "постапокалипсис", "post-apocalyptic", listOf("post-apocalyptic", "apocalyptic")),
        Sub("time-travel", Genre.SCIFI, "путешествия во времени", "time travel", listOf("time travel", "time-travel")),
        Sub("aliens", Genre.SCIFI, "пришельцы", "aliens", listOf("alien invasion", "extraterrestrial", "first contact")),
        Sub("hard-sf", Genre.SCIFI, "твёрдая фантастика", "hard sci-fi", listOf("hard science fiction")),
        Sub("steampunk", Genre.SCIFI, "стимпанк", "steampunk", listOf("steampunk")),

        Sub("romcom", Genre.ROMANCE, "романтическая комедия", "romcom", listOf("romantic comedy")),
        Sub("melodrama", Genre.ROMANCE, "мелодрама", "melodrama", listOf("melodrama")),
        Sub("period-romance", Genre.ROMANCE, "костюмная", "period romance", listOf("historical romance", "costume drama", "period drama")),
        Sub("teen-romance", Genre.ROMANCE, "подростковая", "teen romance", listOf("teen romance")),
        Sub("erotic", Genre.ROMANCE, "эротика", "erotic", listOf("erotic")),

        Sub("black-comedy", Genre.COMEDY, "чёрная комедия", "black comedy", listOf("black comedy", "dark comedy")),
        Sub("satire", Genre.COMEDY, "сатира", "satire", listOf("satire", "satirical")),
        Sub("parody", Genre.COMEDY, "пародия", "parody", listOf("parody", "spoof")),
        Sub("sitcom", Genre.COMEDY, "ситком", "sitcom", listOf("sitcom", "situation comedy")),
        Sub("slapstick", Genre.COMEDY, "эксцентрика", "slapstick", listOf("slapstick")),
        Sub("mockumentary", Genre.COMEDY, "мокьюментари", "mockumentary", listOf("mockumentary")),
        Sub("dramedy", Genre.COMEDY, "трагикомедия", "dramedy", listOf("comedy-drama", "comedy drama", "tragicomedy", "dramedy")),
        Sub("buddy", Genre.COMEDY, "бадди-муви", "buddy", listOf("buddy")),

        Sub("bio-doc", Genre.DOC, "биография", "biography", listOf("biographical documentary")),
        Sub("nature", Genre.DOC, "природа", "nature", listOf("nature documentary", "wildlife")),
        Sub("history-doc", Genre.DOC, "история", "history", listOf("historical documentary", "history documentary")),
        Sub("music-doc", Genre.DOC, "о музыке", "music", listOf("music documentary", "concert film", "rockumentary")),
        Sub("sport-doc", Genre.DOC, "о спорте", "sports", listOf("sports documentary")),
        Sub("true-crime", Genre.DOC, "тру-крайм", "true crime", listOf("true crime")),
        Sub("science-doc", Genre.DOC, "о науке", "science", listOf("science documentary")),

        Sub("slasher", Genre.HORROR, "слэшер", "slasher", listOf("slasher")),
        Sub("supernatural", Genre.HORROR, "мистика", "supernatural", listOf("supernatural", "ghost", "haunted")),
        Sub("body-horror", Genre.HORROR, "боди-хоррор", "body horror", listOf("body horror")),
        Sub("found-footage", Genre.HORROR, "найденная плёнка", "found footage", listOf("found footage")),
        Sub("zombie", Genre.HORROR, "зомби", "zombie", listOf("zombie")),
        Sub("psycho-horror", Genre.HORROR, "психологический хоррор", "psychological horror", listOf("psychological horror")),
        Sub("folk-horror", Genre.HORROR, "фолк-хоррор", "folk horror", listOf("folk horror")),
        Sub("vampire", Genre.HORROR, "вампиры", "vampires", listOf("vampire")),
        Sub("monster", Genre.HORROR, "монстры", "monsters", listOf("monster", "kaiju", "creature")),

        Sub("spaghetti", Genre.WESTERN, "спагетти-вестерн", "spaghetti western", listOf("spaghetti western")),
        Sub("neo-western", Genre.WESTERN, "неовестерн", "neo-western", listOf("neo-western", "contemporary western")),
        Sub("revisionist", Genre.WESTERN, "ревизионистский", "revisionist", listOf("revisionist western", "acid western")),

        Sub("anime", Genre.ANIMATION, "аниме", "anime", listOf("anime")),
        Sub("hand-drawn", Genre.ANIMATION, "рисованный", "hand-drawn", listOf("traditional animation", "hand-drawn")),
        Sub("cgi", Genre.ANIMATION, "3D", "CGI", listOf("computer animation", "computer-animated", "cgi")),
        Sub("stop-motion", Genre.ANIMATION, "кукольный", "stop-motion", listOf("stop-motion", "stop motion", "clay animation", "claymation", "puppet")),
        Sub("family", Genre.ANIMATION, "семейный", "family", listOf("family film", "children's film", "family")),
        Sub("adult-anim", Genre.ANIMATION, "для взрослых", "adult animation", listOf("adult animation")),

        Sub("jukebox", Genre.MUSICAL, "на хиты", "jukebox", listOf("jukebox musical")),
        Sub("rock-opera", Genre.MUSICAL, "рок-опера", "rock opera", listOf("rock opera", "opera film")),
        Sub("dance", Genre.MUSICAL, "танцевальный", "dance", listOf("dance film", "dance")),
        Sub("music-bio", Genre.MUSICAL, "о музыкантах", "about musicians", listOf("musical biopic", "music biopic")),

        Sub("historical", Genre.DRAMA, "историческая", "historical", listOf("historical", "period")),
        Sub("war", Genre.DRAMA, "военная", "war", listOf("war film", "war drama", "anti-war")),
        Sub("biopic", Genre.DRAMA, "биография", "biopic", listOf("biographical", "biopic")),
        Sub("sports", Genre.DRAMA, "спортивная", "sports", listOf("sports film", "sports drama")),
        Sub("family-drama", Genre.DRAMA, "семейная", "family drama", listOf("family drama")),
        Sub("coming-of-age", Genre.DRAMA, "взросление", "coming-of-age", listOf("coming-of-age", "teen drama")),
        Sub("social", Genre.DRAMA, "социальная", "social", listOf("social drama", "social realism", "political")),
        Sub("medical", Genre.DRAMA, "медицинская", "medical", listOf("medical drama")),

        Sub("action", Genre.ADVENTURE, "боевик", "action", listOf("action")),
        Sub("fantasy", Genre.ADVENTURE, "фэнтези", "fantasy", listOf("fantasy")),
        Sub("superhero", Genre.ADVENTURE, "супергерои", "superheroes", listOf("superhero")),
        Sub("martial-arts", Genre.ADVENTURE, "боевые искусства", "martial arts", listOf("martial arts", "wuxia", "kung fu")),
        Sub("disaster", Genre.ADVENTURE, "катастрофа", "disaster", listOf("disaster")),
        Sub("pirates", Genre.ADVENTURE, "пираты", "pirates", listOf("pirate", "swashbuckler")),
        Sub("road-movie", Genre.ADVENTURE, "роуд-муви", "road movie", listOf("road movie")),
        Sub("survival", Genre.ADVENTURE, "выживание", "survival", listOf("survival")),

        Sub("talent", Genre.REALITY, "шоу талантов", "talent show", listOf("talent show")),
        Sub("cooking", Genre.REALITY, "кулинарное", "cooking", listOf("cooking")),
        Sub("dating", Genre.REALITY, "о свиданиях", "dating", listOf("dating")),
        Sub("survival-show", Genre.REALITY, "на выживание", "survival show", listOf("survival competition")),
        Sub("game-show", Genre.REALITY, "игровое", "game show", listOf("game show", "quiz")),
        Sub("makeover", Genre.REALITY, "преображение", "makeover", listOf("makeover", "renovation")),
    )

    private val byKey = all.associateBy { it.key }

    /** The subgenres offered under a genre. */
    fun of(g: Genre): List<Sub> = all.filter { it.genre == g }

    fun split(s: String): List<String> = s.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    fun join(l: List<String>): String = l.map { it.replace(',', ' ').trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")

    fun known(key: String): Sub? = byKey[key]

    /** How one reads: a known one in the language asked, one typed by hand as typed. */
    fun name(key: String, ru: Boolean): String = byKey[key]?.let { if (ru) it.ru else it.en } ?: key

    fun names(s: String, ru: Boolean): List<String> = split(s).map { name(it, ru) }

    /** Words to search by: a known one in both languages. */
    fun text(s: String): String = split(s).joinToString(" ") { k -> byKey[k]?.let { "${it.ru} ${it.en}" } ?: k }

    /** Adds or takes away one, by its key or a name typed by hand; a typed name that is a known one is kept by its key. */
    fun toggle(s: String, key: String): String {
        val k = key.trim().let { t -> all.firstOrNull { it.key == t || it.ru.equals(t, true) || it.en.equals(t, true) }?.key ?: t }
        if (k.isEmpty()) return s
        val l = split(s)
        return join(if (k in l) l - k else l + k)
    }

    /**
     * The subgenres in a catalogue's genre names (English, as Wikidata gives them), longest name first so
     * «psychological horror» isn't taken for a mere thriller. Plain genre names («horror film») match
     * nothing. The ones under [genre] come first; at most four.
     */
    fun match(labels: List<String>, genre: Genre): List<String> {
        val found = LinkedHashSet<String>()
        for (raw in labels) {
            val l = raw.lowercase()
            found += all.flatMap { s -> s.hints.map { h -> s to h } }.filter { (_, h) -> h in l }.maxByOrNull { (_, h) -> h.length }?.first?.key ?: continue
        }
        return found.sortedBy { if (byKey[it]?.genre == genre) 0 else 1 }.take(4)
    }
}
