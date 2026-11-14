package app.reelkin.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val DAY = 86_400_000L

/** What a spectator in the hall wears: you see them from behind, so mostly hats. */
enum class Who { DETECTIVE, ASTRO, LADY, CLOWN, PROF, GHOST, COWBOY, MOUSE, TOPHAT, BERET, PITH, SELFIE }

/** The grown form a pet takes for good, after the films it was raised on. */
enum class Form(val color: Int) { SHADOW(0x6A7AB8), STAR(0x4CC8D8), HEART(0xE8608A), LAUGH(0xF0A63A), DUST(0xC8884A) }

/** The genre decides the poster, the spectator in the hall and the form a pet raised on it takes. */
enum class Genre(val color: Int, val who: Who, val form: Form) {
    NOIR(0x3C4A6E, Who.DETECTIVE, Form.SHADOW),
    SCIFI(0x24808E, Who.ASTRO, Form.STAR),
    ROMANCE(0xD4507A, Who.LADY, Form.HEART),
    COMEDY(0xD8902A, Who.CLOWN, Form.LAUGH),
    DOC(0x6E8A44, Who.PROF, Form.DUST),
    HORROR(0x5E2A70, Who.GHOST, Form.SHADOW),
    WESTERN(0xC0642A, Who.COWBOY, Form.DUST),
    ANIMATION(0xD84A3C, Who.MOUSE, Form.LAUGH),
    MUSICAL(0xB83A8A, Who.TOPHAT, Form.HEART),
    DRAMA(0x4E6284, Who.BERET, Form.HEART),
    ADVENTURE(0x4E8A3A, Who.PITH, Form.STAR),
    REALITY(0x8A5AD0, Who.SELFIE, Form.LAUGH),
}

/**
 * The pets, each asleep in a film can of its own. Three cans are there from the start; the rest open by the
 * way you watch. [floats]: it hovers instead of standing. [anime]: it lives on anime, animation from Japan.
 */
enum class Species(val starter: Boolean = false, val floats: Boolean = false, val anime: Boolean = false) {
    CAT(true), FOX(true), OWL(true),
    BAT, COON, HAMSTER, ARMADILLO, LOVEBIRD, MONKEY, FROG,
    MOTH(floats = true), SLOTH, PENGUIN, REDPANDA, PUG, MOUSE, GOLDFISH(floats = true), SQUIRREL,
    GHOST(floats = true), DRAGON, UNICORN, PHOENIX, KITSUNE, YETI, GRIFFIN, GOLEM, ROBOT, FAIRY(floats = true),
    POPKIN, GARGOYLE, SPHINX, WISP(floats = true),
    TANUKI(anime = true), KOI(floats = true, anime = true), KAPPA(anime = true), NEKOMATA(anime = true), TENGU(anime = true),
    SLIME(anime = true), MECHA(anime = true), MANEKI(anime = true), DARUMA(anime = true), CRANE(floats = true, anime = true),
    SHIBA(anime = true), ONI(anime = true), ONIGIRI(anime = true), KIRIN(anime = true), RYU(anime = true), SUZUME(anime = true),
    BAKU(floats = true, anime = true);

    val file get() = name.lowercase()
}

/** Life stages; a pet moves up by the minutes of film it was fed. */
enum class Stage(val from: Int) {
    EGG(0), BABY(180), YOUNG(1_500), ADULT(5_000), ANCIENT(15_000);

    val next: Stage? get() = entries.getOrNull(ordinal + 1)
}

/** How a pet takes things. Decided when its can is opened. */
enum class Nature { CALM, SLEEPY, JUMPY, CURIOUS, SHY, CHATTY, GREEDY }

/** A pet: out of its can since [laid], fed by what was watched while it was in the booth. */
@Entity(tableName = "pets")
data class Pet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val species: Species,
    val name: String = "",
    val laid: Long = System.currentTimeMillis(),
    val shiny: Boolean = false,
    val nature: Nature = Nature.CALM,
    /** Set when it grows up, for good. */
    val form: Form? = null,
    /** When it was in the booth, as «from-to» pairs (to = 0 while it's there now). */
    val spans: String = "$laid-0",
    /** The can opened and the hatching was seen. */
    @ColumnInfo(defaultValue = "0") val hatched: Boolean = false,
)

enum class Kind { FILM, SERIES }

/** A title on the bill: a film waiting, a series waiting, running or put away. */
enum class Status { WATCH, RUNNING, SHELVED }

enum class Company { ALONE, TWO, MANY }

/** What happened after the credits. */
enum class Credits { NOTHING, SCENE, LEFT }

@Entity(tableName = "titles")
data class Title(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: Kind = Kind.FILM,
    val title: String,
    val year: Int? = null,
    val country: String = "",
    val genre: Genre = Genre.DRAMA,
    /** A film's length, or one episode's. */
    val minutes: Int = 0,
    val status: Status = Status.WATCH,
    val cover: String? = null,
    val added: Long = System.currentTimeMillis(),
    val season: Int = 1,
    /** Episodes watched in the current season. */
    val episode: Int = 0,
    val perSeason: Int = 10,
    /** The last episode stopped at the worst moment. */
    val cliff: Boolean = false,
    /** The seasons as the catalogue knows them, with their episodes and first days: see [Seasons]. Empty when unknown. */
    @ColumnInfo(defaultValue = "") val plan: String = "",
    /** When [plan] was last asked for, so it's asked again only now and then. */
    @ColumnInfo(defaultValue = "0") val planAt: Long = 0,
    /** Where the seasons come from: «myshows:73193» or «tvmaze:51394»; empty when it was looked for and isn't anywhere. */
    val show: String? = null,
    /** Who directed it, names joined by commas; «?» when looked for and not found. */
    @ColumnInfo(defaultValue = "") val director: String = "",
    /** The leading actors, by billing, joined by commas. */
    @ColumnInfo(defaultValue = "") val cast: String = "",
)

/** A torn stub: a film watched, or a whole season. It holds a copy of everything so the bill can forget the title. */
@Entity(tableName = "tickets")
data class Ticket(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val season: Boolean = false,
    val year: Int? = null,
    val country: String = "",
    val genre: Genre = Genre.DRAMA,
    val minutes: Int = 0,
    val stars: Int = 0,
    val words: String = "",
    val company: Company = Company.ALONE,
    val credits: Credits = Credits.NOTHING,
    val rewatch: Boolean = false,
    val at: Long = System.currentTimeMillis(),
    val cover: String? = null,
    /** A season watched before it came here: no episodes carry its minutes, so the stub does. */
    @ColumnInfo(defaultValue = "0") val whole: Boolean = false,
    /** Where an imported stub came from, so importing again doesn't tear it twice. */
    val ref: String? = null,
    /** Who directed it, names joined by commas; «?» when looked for and not found. */
    @ColumnInfo(defaultValue = "") val director: String = "",
    /** The leading actors, by billing, joined by commas. */
    @ColumnInfo(defaultValue = "") val cast: String = "",
)

@Entity(tableName = "episodes", indices = [Index("series")])
data class Episode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val series: Long,
    val season: Int,
    val number: Int,
    val minutes: Int,
    val at: Long = System.currentTimeMillis(),
)

enum class GoalKind { FILMS, HOURS, EPISODES, SEASONS, GENRE, NEW_COUNTRIES, NEW_DECADES, DAYS, FIVE_STARS, STREAK, OLD, WORDS, COMPANY, REWATCHES, SCENES, LONG, RATED }

enum class Period { WEEK, MONTH, YEAR, ONCE }

/** Something to reach: films a month, hours a year, new countries, a genre… */
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: GoalKind,
    val target: Int,
    val period: Period = Period.MONTH,
    /** What it's about: a genre's name. */
    val ref: String = "",
    val created: Long = System.currentTimeMillis(),
    /** For ONCE: the last day it counts, 0 for no end. */
    val until: Long = 0,
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
)

/** The people of a film or a series: its director(s) and its leading actors, as names. */
object People {
    fun of(s: String): List<String> = if (s == "?") emptyList() else s.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    fun join(l: List<String>): String = l.joinToString(", ")
}
