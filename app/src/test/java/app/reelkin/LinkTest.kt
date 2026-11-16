package app.reelkin

import app.reelkin.data.Catalog
import app.reelkin.data.Catalog.Link
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkTest {
    @Test fun knownSites() {
        assertEquals(Link.Prop("P345", "tt0133093"), Catalog.link("https://m.imdb.com/title/tt0133093/?ref_=x"))
        assertEquals(Link.Prop("P2603", "301"), Catalog.link("Посмотри «Матрица» https://www.kinopoisk.ru/film/301/"))
        assertEquals(Link.Prop("P2603", "464963"), Catalog.link("https://www.kinopoisk.ru/series/464963/"))
        assertEquals(Link.Prop("P6127", "the-matrix"), Catalog.link("https://letterboxd.com/someone/film/the-matrix/"))
        assertEquals(Link.Prop("P4947", "603"), Catalog.link("https://www.themoviedb.org/movie/603-the-matrix"))
        assertEquals(Link.Prop("P4983", "1396"), Catalog.link("https://www.themoviedb.org/tv/1396-breaking-bad"))
        assertEquals(Link.Prop("P8600", "169"), Catalog.link("https://www.tvmaze.com/shows/169/breaking-bad"))
        assertEquals(Link.Item("Q83495"), Catalog.link("https://www.wikidata.org/wiki/Q83495"))
        assertEquals(Link.Wiki("ru", "Матрица (фильм)"), Catalog.link("https://ru.m.wikipedia.org/wiki/%D0%9C%D0%B0%D1%82%D1%80%D0%B8%D1%86%D0%B0_(%D1%84%D0%B8%D0%BB%D1%8C%D0%BC)"))
    }

    @Test fun plainWordsAndUnknownSites() {
        assertNull(Catalog.link("Матрица"))
        assertNull(Catalog.link("https://example.com/film/1"))
    }
}
