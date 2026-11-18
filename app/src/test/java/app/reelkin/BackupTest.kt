package app.reelkin

import app.reelkin.data.Award
import app.reelkin.data.Backup
import app.reelkin.data.Company
import app.reelkin.data.Credits
import app.reelkin.data.Episode
import app.reelkin.data.Form
import app.reelkin.data.Genre
import app.reelkin.data.Goal
import app.reelkin.data.GoalKind
import app.reelkin.data.Kind
import app.reelkin.data.Nature
import app.reelkin.data.Period
import app.reelkin.data.Pet
import app.reelkin.data.Seance
import app.reelkin.data.Settings
import app.reelkin.data.Species
import app.reelkin.data.Status
import app.reelkin.data.Ticket
import app.reelkin.data.Title
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupTest {
    private val all = Backup.Everything(
        titles = listOf(
            Title(id = 3, kind = Kind.SERIES, title = "Твин Пикс", year = 1990, country = "US", genre = Genre.NOIR, minutes = 47, status = Status.RUNNING, cover = "a.jpg",
                added = 111, season = 2, episode = 5, perSeason = 22, cliff = true, plan = "1:8;2:22", planAt = 9, show = "tvmaze:1", director = "David Lynch", cast = "Kyle MacLachlan", sub = "mystery", also = "DRAMA"),
            Title(id = 4, title = "Stalker", show = ""),
        ),
        tickets = listOf(
            Ticket(id = 7, title = "Alien", year = 1979, country = "GB", genre = Genre.SCIFI, minutes = 117, stars = 5, words = "in space \"no one\"\n…", company = Company.TWO,
                credits = Credits.SCENE, rewatch = true, at = 222, cover = "b.jpg", ref = "kinopoisk:345", director = "Ridley Scott", cast = "Sigourney Weaver", sub = "space", also = "HORROR"),
            Ticket(id = 8, title = "Lost · season 1", season = true, whole = true, past = true, at = 5),
        ),
        episodes = listOf(Episode(1, 3, 2, 4, 47, 333), Episode(2, 3, 2, 5, 50, 444)),
        pets = listOf(Pet(id = 1, species = Species.KITSUNE, name = "Рыжик", laid = 10, shiny = true, nature = Nature.SHY, form = Form.STAR, spans = "10-20,30-0", hatched = true), Pet(id = 2, species = Species.CAT, laid = 40)),
        goals = listOf(Goal(id = 1, kind = GoalKind.GENRE, target = 12, period = Period.ONCE, ref = "NOIR", created = 1, until = 99, archived = true)),
        settings = Settings(onboarded = true, petName = "Кот", species = Species.OWL, shiny = true, pet = 2, tips = setOf("BOOTH", "BILL"), wall = "STARS", claimed = setOf("2800:FILM"),
            cinema = "ЛУНА", spent = 40, bought = setOf("lamp"), awards = listOf(Award(2025, 1, "Alien")), goal = 30, goalSeasons = 4, pixelCovers = true,
            seance = Seance("Stalker", 161, Genre.SCIFI, 1979, "SU", "c.jpg", 4, false, 555, "", "DRAMA"), backdrop = "own", backdropStamp = 77, stubSort = "STARS"),
    )

    @Test fun everythingComesBackAsItWent() {
        val back = Backup.decode(Backup.encode(all, "2.6.0"))
        assertEquals(all.titles, back.titles)
        assertEquals(all.tickets, back.tickets)
        assertEquals(all.episodes, back.episodes)
        assertEquals(all.pets, back.pets)
        assertEquals(all.goals, back.goals)
        // the stamp is the phone's own: it is set anew when the file is loaded
        assertEquals(all.settings.copy(backdropStamp = 0), back.settings)
        assertEquals(setOf("a.jpg", "b.jpg", "c.jpg"), back.covers)
    }

    @Test fun thePetInTheBoothMustBeInTheFile() {
        val lost = Backup.Everything(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), Settings(pet = 9))
        assertEquals(0L, Backup.decode(Backup.encode(lost)).settings.pet)
    }

    @Test fun otherFilesAreRefused() {
        fun fails(text: String) = runCatching { Backup.decode(text) }.exceptionOrNull() as? Backup.Unreadable
        assertFalse(fails("not json")!!.newer)
        assertFalse(fails("""{"app":"roamling","version":1}""")!!.newer)
        assertFalse(fails("""{"app":"reelkin","format":1}""")!!.newer)                    // no settings: not a whole file
        assertFalse(fails(Backup.encode(all).replace("KITSUNE", "NOSUCH"))!!.newer)
        assertTrue(fails(Backup.encode(all, format = Backup.FORMAT + 1))!!.newer)
    }
}
