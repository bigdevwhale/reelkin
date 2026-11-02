package app.reelkin

import app.reelkin.data.Credits
import app.reelkin.data.Genre
import app.reelkin.data.Kind
import app.reelkin.data.Settings
import app.reelkin.data.Species
import app.reelkin.data.Status
import app.reelkin.data.Ticket
import app.reelkin.data.Title
import app.reelkin.data.Trait
import app.reelkin.domain.Cinema
import app.reelkin.domain.Mood
import app.reelkin.domain.Picker
import app.reelkin.domain.gains
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelkinTest {
    private fun tk(t: String, g: Genre, y: Int = 2000, cn: String = "", m: Int = 100, at: Long = 0, cr: Credits = Credits.NOTHING) =
        Ticket(id = at + 1, title = t, genre = g, year = y, country = cn, minutes = m, at = at, credits = cr)
    private fun cin(t: List<Ticket>, titles: List<Title> = emptyList()) = Cinema(titles, t, emptyList(), Settings(), 0)

    @Test fun minutesAndPopcorn() {
        val c = cin(listOf(tk("A", Genre.NOIR, m = 100), tk("B", Genre.NOIR, m = 50, at = 1, cr = Credits.SCENE)))
        assertEquals(150L, c.minutes)
        assertEquals(25, c.earned)
    }

    @Test fun thirdFilmOfAGenreGivesItsTrait() {
        val two = cin(listOf(tk("A", Genre.NOIR), tk("B", Genre.NOIR, at = 1)))
        assertTrue(Trait.HAT !in two.traits)
        assertEquals(Genre.NOIR, two.nearTrait)
        val three = cin(two.seats + tk("C", Genre.NOIR, at = 2))
        assertTrue(Trait.HAT in three.traits)
        assertEquals(listOf(Trait.HAT), gains(two, three, three.seats.last()).traits)
    }

    @Test fun firstThreeFilmsDecideTheSpecies() {
        assertEquals(Species.BAT, Cinema.speciesFor(listOf(tk("A", Genre.HORROR), tk("B", Genre.COMEDY, at = 1), tk("C", Genre.HORROR, at = 2))))
        assertEquals(Species.CAT, Cinema.speciesFor(listOf(tk("A", Genre.NOIR), tk("B", Genre.COMEDY, at = 1), tk("C", Genre.DOC, at = 2))))
        assertTrue(cin(listOf(tk("A", Genre.NOIR), tk("B", Genre.NOIR, at = 1), tk("C", Genre.NOIR, at = 2))).hatchReady)
    }

    @Test fun lettersDecadesCountries() {
        val c = cin(listOf(tk("Ёжик в тумане", Genre.ANIMATION, 1975, "СССР"), tk("«Амели»", Genre.ROMANCE, 2001, "Франция", at = 1), tk("12 стульев", Genre.COMEDY, 1971, "СССР", at = 2)))
        assertEquals(mapOf('Е' to 1, 'А' to 1), c.letters)
        assertEquals(setOf(1970, 2000), c.decades)
        assertEquals(listOf("СССР", "Франция"), c.countries)
    }

    @Test fun pickerFitsTheTimeAndPrefersNewDecades() {
        val w = listOf(
            Title(id = 1, title = "Long", genre = Genre.COMEDY, minutes = 180, year = 1990),
            Title(id = 2, title = "Old", genre = Genre.COMEDY, minutes = 87, year = 1931),
            Title(id = 3, title = "New", genre = Genre.COMEDY, minutes = 90, year = 2000),
        )
        val c = cin(listOf(tk("X", Genre.DRAMA, 2000)), w)
        val p = Picker.pick(c, 95, Mood.LAUGH, emptySet(), true)!!
        assertEquals("Old", p.films.single().title)
        assertNull(Picker.pick(c, 30, Mood.LAUGH, emptySet(), true))
        val s = Title(id = 9, kind = Kind.SERIES, title = "S", minutes = 25, status = Status.RUNNING)
        assertEquals("S", Picker.pick(cin(emptyList(), w + s), 30, Mood.LAUGH, emptySet(), true)!!.episode!!.title)
    }
}
