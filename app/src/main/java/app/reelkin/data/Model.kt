package app.reelkin.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val DAY = 86_400_000L

/** What a spectator in the hall wears: you see them from behind, so mostly hats. */
enum class Who { DETECTIVE, ASTRO, LADY, CLOWN, PROF, GHOST, COWBOY, MOUSE, TOPHAT, BERET, PITH, SELFIE }

/** Something the pet picks up from the third film of a genre. */
enum class Trait { HAT, ANTENNA, BLUSH, NOSE, GLASSES, BOLTS, BANDANA, GLOVES, BOW }

/** The genre decides the poster, the spectator in the hall and the pet's trait. */
enum class Genre(val color: Int, val trait: Trait?, val who: Who) {
    NOIR(0x3C4A6E, Trait.HAT, Who.DETECTIVE),
    SCIFI(0x24808E, Trait.ANTENNA, Who.ASTRO),
    ROMANCE(0xD4507A, Trait.BLUSH, Who.LADY),
    COMEDY(0xD8902A, Trait.NOSE, Who.CLOWN),
    DOC(0x6E8A44, Trait.GLASSES, Who.PROF),
    HORROR(0x5E2A70, Trait.BOLTS, Who.GHOST),
    WESTERN(0xC0642A, Trait.BANDANA, Who.COWBOY),
    ANIMATION(0xD84A3C, Trait.GLOVES, Who.MOUSE),
    MUSICAL(0xB83A8A, Trait.BOW, Who.TOPHAT),
    DRAMA(0x4E6284, null, Who.BERET),
    ADVENTURE(0x4E8A3A, null, Who.PITH),
    REALITY(0x8A5AD0, null, Who.SELFIE),
}

/** What hatched from the film can, decided by the first three films. */
enum class Species { CAT, FOX, COON, BAT, OWL }

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
