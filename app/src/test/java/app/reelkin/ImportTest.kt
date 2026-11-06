package app.reelkin

import app.reelkin.data.Air
import app.reelkin.data.DAY
import app.reelkin.data.Kinopoisk
import app.reelkin.data.MyShows
import app.reelkin.data.SeasonPlan
import app.reelkin.data.Seasons
import app.reelkin.data.Settings
import app.reelkin.data.Status
import app.reelkin.data.Ticket
import app.reelkin.data.Title
import app.reelkin.domain.Cinema
import app.reelkin.domain.Stats
import app.reelkin.domain.Words
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImportTest {
    private val day = { s: String -> Seasons.parseDay(s)!! }

    @Test fun planRoundTripsAndTellsWhatIsOut() {
        val plan = listOf(SeasonPlan(1, 6, day("2021-07-11")), SeasonPlan(2, 7, day("2022-10-30")), SeasonPlan(3, 8, day("2025-02-16")), SeasonPlan(4, 0, null))
        assertEquals(plan, Seasons.decode(Seasons.encode(plan)))
        val now = day("2026-11-06")
        assertEquals(Air.Out, Seasons.air(plan, 3, now))
        assertEquals(Air.Later, Seasons.air(plan, 4, now))
        assertEquals(Air.None, Seasons.air(plan, 5, now))
        assertEquals(Air.On(day("2025-02-16")), Seasons.air(plan, 3, day("2025-01-01")))
        assertEquals(Air.Unknown, Seasons.air(emptyList(), 2, now))
        assertEquals(3, Seasons.out(plan, now))
        val t = Title(title = "The White Lotus", season = 4, plan = Seasons.encode(plan))
        assertEquals("сезон 4 ещё не вышел, даты пока нет", Words.air(t, true, now))
        assertEquals("вышло 3 сезона", Words.seasonsOut(t, true, now))
        assertNull(Words.air(t.copy(season = 2), true, now))
        assertEquals("сезон 3 выйдет 16 февраля 2025", Words.air(t.copy(season = 3), true, day("2025-01-01")))
    }

    private fun show(status: String, watched: Int, showStatus: String = "Returning Series") = JSONObject()
        .put("show", JSONObject().put("id", 7).put("title", "Белый лотос").put("titleOriginal", "The White Lotus").put("year", 2021)
            .put("genres", JSONArray().put(JSONObject().put("alias", "comedy"))))
        .put("watchStatus", status).put("rating", 4).put("watchedEpisodes", watched).put("runtime", 60)

    private fun full(showStatus: String): JSONObject {
        val eps = JSONArray()
        fun ep(s: Int, n: Int, air: String?) = eps.put(JSONObject().put("seasonNumber", s).put("episodeNumber", n).put("airDate", air ?: JSONObject.NULL).put("isSpecial", false))
        for (n in 1..6) ep(1, n, "2021-07-${10 + n}T21:00:00+0000")
        ep(1, 0, "2021-08-30T21:00:00+0000") // a special: left out
        for (n in 1..7) ep(2, n, "2022-11-${10 + n}T21:00:00+0000")
        for (n in 1..3) ep(3, n, "2030-02-${10 + n}T21:00:00+0000")
        return JSONObject().put("title", "Белый лотос").put("titleOriginal", "The White Lotus").put("year", 2021).put("country", "US").put("countryTitle", "США").put("runtime", 60)
            .put("status", showStatus).put("episodes", eps)
    }

    private val now = Seasons.parseDay("2026-11-06")!!

    @Test fun watchingTearsFinishedSeasonsAndStaysOnTheEpisode() {
        val h = MyShows.show(show("watching", 9), full("Returning Series"), true, now)!!
        assertEquals(listOf("Белый лотос · сезон 1"), h.stubs.map { it.title })
        val s = h.stubs.single()
        assertTrue(s.season && s.whole)
        assertEquals(360, s.minutes)
        assertEquals(4, s.stars)
        assertEquals("myshows:7:1", s.ref)
        assertEquals("США", s.country)
        val t = h.title!!
        assertEquals(Status.RUNNING, t.status)
        assertEquals(2 to 3, t.season to t.episode)
        assertEquals(7, t.perSeason)
        assertEquals(3, h.episodes.size)
        // The third season is listed but not out yet.
        assertEquals(Air.On(Seasons.parseDay("2030-02-11")!!), Seasons.air(t, 3, now))
    }

    @Test fun finishedTearsEveryAiredSeasonAndWaitsForTheNext() {
        val h = MyShows.show(show("finished", 13), full("Returning Series"), false, now)!!
        assertEquals(listOf("The White Lotus · season 1", "The White Lotus · season 2"), h.stubs.map { it.title })
        assertEquals(Status.SHELVED, h.title!!.status)
        assertEquals(3, h.title!!.season)
        val ended = MyShows.show(show("finished", 13), full("Canceled/Ended"), false, now)!!
        assertNull(ended.title)
    }

    @Test fun laterGoesOnTheBill() {
        val h = MyShows.show(show("later", 0), full("Returning Series"), true, now)!!
        assertTrue(h.stubs.isEmpty())
        assertEquals(Status.WATCH, h.title!!.status)
    }

    @Test fun loginFromALink() {
        assertEquals("dmitriy", MyShows.login("https://myshows.me/dmitriy/"))
        assertEquals("dmitriy", MyShows.login("https://myshows.me/m/profile/dmitriy"))
        assertEquals("dmitriy", MyShows.login(" dmitriy "))
    }

    @Test fun kinopoiskPageGivesFilmsWithRatingsAndDates() {
        val html = """
            <div class="item"><a href="/film/326/"><img src="x.jpg"></a>
              <div class="nameRus"><a href="/film/326/">Побег из Шоушенка (1994)</a></div>
              <a href="/film/326/reviews/">рецензии</a>
              <div class="date">05.03.2021, 22:14</div><div class="vote">10</div></div>
            <div class="item"><div class="nameRus"><a href="https://www.kinopoisk.ru/series/2000461/">Белый лотос</a></div>
              <div class="date">01.02.2023, 10:00</div><div class="vote"> 7 </div></div>
            <a href="/film/435/">Зелёная миля</a>
        """.trimIndent()
        val seen = Kinopoisk.parse(html)
        assertEquals(listOf(326, 2000461, 435), seen.map { it.id })
        assertEquals("Побег из Шоушенка (1994)", seen[0].name)
        assertEquals(5, seen[0].stars)
        assertEquals(4, seen[1].stars)
        assertTrue(seen[1].series)
        assertEquals(Seasons.parseDay("2021-03-05")!! / DAY, seen[0].at!! / DAY)
        assertNull(seen[2].stars)
        // Plain links in a list count too; the reviews page doesn't.
        assertEquals(listOf(326, 435), Kinopoisk.parse("https://www.kinopoisk.ru/film/326/\nkinopoisk.ru/film/435, kinopoisk.ru/film/999/reviews/").map { it.id })
    }

    @Test fun kinopoiskPages() {
        assertEquals("https://www.kinopoisk.ru/user/123456/votes/", Kinopoisk.votesUrl("https://www.kinopoisk.ru/user/123456/"))
        assertEquals("https://www.kinopoisk.ru/user/123456/votes/", Kinopoisk.votesUrl("123456"))
        assertEquals("https://www.kinopoisk.ru/user/1/votes/list/ord/date/page/2/", Kinopoisk.next("https://www.kinopoisk.ru/user/1/votes/"))
        assertEquals("https://www.kinopoisk.ru/user/1/votes/list/ord/date/page/3/#list", Kinopoisk.next("https://www.kinopoisk.ru/user/1/votes/list/ord/date/page/2/#list"))
        assertNull(Kinopoisk.next("https://www.kinopoisk.ru/film/326/"))
        assertTrue(Kinopoisk.allowed("www.kinopoisk.ru") && Kinopoisk.allowed("passport.yandex.ru") && Kinopoisk.allowed("sso.kinopoisk.ru"))
        assertTrue(!Kinopoisk.allowed("evil.com") && !Kinopoisk.allowed("notkinopoisk.ru") && !Kinopoisk.allowed(null))
    }

    @Test fun aSeasonWatchedBeforeCountsItsMinutes() {
        val film = Ticket(id = 1, title = "A", minutes = 100, at = 0)
        val tracked = Ticket(id = 2, title = "S · season 1", season = true, minutes = 400, at = 1)
        val brought = Ticket(id = 3, title = "S · season 2", season = true, whole = true, minutes = 300, at = 2)
        val c = Cinema(emptyList(), listOf(film, tracked, brought), emptyList(), Settings(), 0)
        assertEquals(400L, c.minutes)
        assertEquals(400L, Stats(c, null).minutes)
    }
}
