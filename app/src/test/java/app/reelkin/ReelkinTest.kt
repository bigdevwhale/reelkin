package app.reelkin

import app.reelkin.data.Credits
import app.reelkin.data.Genre
import app.reelkin.data.Kind
import app.reelkin.data.Settings
import app.reelkin.data.Species
import app.reelkin.data.Status
import app.reelkin.data.Ticket
import app.reelkin.data.Title
import app.reelkin.data.Pet
import app.reelkin.data.Stage
import app.reelkin.domain.Browse
import app.reelkin.domain.Cinema
import app.reelkin.domain.Stats
import app.reelkin.domain.Filter
import app.reelkin.domain.Span
import app.reelkin.domain.Mood
import app.reelkin.domain.Picker
import app.reelkin.domain.Pets
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
        // 10 a film, 5 more for the post-credits scene, and the achievements their steps.
        assertEquals(25 + c.feats.sumOf { it.level } * app.reelkin.domain.Feats.REWARD, c.earned)
    }

    @Test fun aPetEatsWhatIsWatchedWhileItIsOutAndLovesItsGenres() {
        val cat = Pet(id = 1, species = Species.CAT, laid = 0, hatched = true)
        val films = listOf(tk("A", Genre.NOIR, m = 100), tk("B", Genre.COMEDY, m = 100, at = 1))
        val c = Cinema(emptyList(), films, emptyList(), Settings(pet = 1), 0, listOf(cat))
        val p = c.pet!!
        // Noir is the cat's favourite: half again.
        assertEquals(250, p.fed)
        assertEquals(Stage.BABY, p.stage)
        // A pet laid after the films ate none of them.
        val late = Pets.state(Pet(id = 2, species = Species.FOX, laid = 10), Pets.watches(c))
        assertEquals(0, late.fed)
        assertEquals(Stage.EGG, late.stage)
    }

    @Test fun cansOpenByTheWayYouWatch() {
        val horror = (0 until 5).map { tk("H$it", Genre.HORROR, at = it.toLong()) }
        val before = cin(horror.dropLast(1))
        val after = cin(horror)
        assertTrue(after.open(Species.CAT))
        assertTrue(!before.open(Species.BAT))
        assertTrue(after.open(Species.BAT))
        assertEquals(listOf(Species.BAT), gains(before, after, horror.last()).cans)
        assertEquals(32, after.hints.size)
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

    @Test fun filtersNarrowStubsAndKeepTheirOrder() {
        val day = 86_400_000L
        val a = tk("Alien", Genre.HORROR, m = 117, at = 1).copy(stars = 5)
        val b = tk("Brazil", Genre.SCIFI, m = 142, at = 2 * day).copy(stars = 3)
        val s = tk("Lost · season 1", Genre.ADVENTURE, at = 3 * day).copy(season = true)
        val all = listOf(a, b, s)
        assertEquals(listOf(s, b, a), Browse().stubs(all))
        assertEquals(listOf(a), Browse(stars = 4).stubs(all))
        assertEquals(listOf(s), Browse(kind = Kind.SERIES).stubs(all))
        assertEquals(listOf(b, a), Browse(genres = setOf(Genre.HORROR, Genre.SCIFI), sort = Browse.Sort.LENGTH).stubs(all))
        assertEquals(listOf(b), Browse(query = "braz").stubs(all))
        val back = Browse.decode(Browse(kind = Kind.FILM, genres = setOf(Genre.NOIR), stars = 4, year = 2026, sort = Browse.Sort.TITLE).encode())
        assertEquals(Browse(kind = Kind.FILM, genres = setOf(Genre.NOIR), stars = 4, year = 2026, sort = Browse.Sort.TITLE), back)
        assertEquals(Browse(), Browse.decode("garbage"))
    }

    @Test fun statsCountFilmsNotSeasonsTwice() {
        val f1 = tk("A", Genre.NOIR, m = 80, cn = "France").copy(stars = 4)
        val f2 = tk("B", Genre.NOIR, m = 130, at = 1, cn = "France").copy(stars = 2)
        val sn = tk("S · season 1", Genre.DRAMA, m = 400, at = 2).copy(season = true)
        val st = Stats(cin(listOf(f1, f2, sn)), Filter(span = Span.ALL))
        assertEquals(2, st.films.size)
        assertEquals(1, st.seasons)
        assertEquals(210L, st.minutes)
        assertEquals(3.0, st.avgPopcorn!!, 0.001)
        assertEquals(listOf(Genre.NOIR to 2), st.genreCounts)
        assertEquals(listOf(1, 0, 1, 0), st.lengths)
        assertEquals(listOf("France" to 2), st.countries)
    }
}
