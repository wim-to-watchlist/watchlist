package to.wim.watchlist.data

import to.wim.watchlist.data.model.DetailActions
import to.wim.watchlist.data.model.Episode
import to.wim.watchlist.data.model.EpisodeRef
import to.wim.watchlist.data.model.LibraryItem
import to.wim.watchlist.data.model.MediaIds
import to.wim.watchlist.data.model.WatchedSeason
import to.wim.watchlist.data.model.ListStatus
import to.wim.watchlist.data.model.MediaType
import to.wim.watchlist.data.model.Playback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailActionsTest {
    private val moviePlayback = Playback(id = 1, type = MediaType.Movie, simklId = 10, title = "Film", progress = 40.0)
    private val next = EpisodeRef(1, 3)

    private fun showPlayback(episode: EpisodeRef?) = Playback(id = 2, type = MediaType.Tv, simklId = 20, title = "Show", progress = 25.0, episode = episode)

    @Test
    fun movieWatchedButtonShowsWithoutProgress() {
        assertTrue(DetailActions.showMovieWatched(null, null))
        assertTrue(DetailActions.showMovieWatched(ListStatus.PlanToWatch, null))
    }

    @Test
    fun movieWatchedButtonHidesWhenCompletedOrInProgress() {
        assertFalse(DetailActions.showMovieWatched(ListStatus.Completed, null))
        assertFalse(DetailActions.showMovieWatched(null, moviePlayback))
        assertFalse(DetailActions.showMovieWatched(ListStatus.Watching, moviePlayback))
    }

    @Test
    fun nextEpisodeButtonShowsWithoutProgress() {
        assertTrue(DetailActions.showNextEpisodeWatched(next, null))
    }

    @Test
    fun nextEpisodeButtonHidesWhenTheBannerIsOnThatEpisode() {
        assertFalse(DetailActions.showNextEpisodeWatched(next, showPlayback(EpisodeRef(1, 3))))
    }

    @Test
    fun nextEpisodeButtonStaysForAnotherEpisodeOrUnknownEpisode() {
        assertTrue(DetailActions.showNextEpisodeWatched(next, showPlayback(EpisodeRef(1, 2))))
        assertTrue(DetailActions.showNextEpisodeWatched(next, showPlayback(null)))
    }

    @Test
    fun checkInNameIsTheTitleForAMovie() {
        assertEquals("Film", DetailActions.checkInName("Film", null))
    }

    @Test
    fun checkInNameAddsTheEpisodeForAShow() {
        assertEquals("Show S01E03", DetailActions.checkInName("Show", EpisodeRef(1, 3)))
        assertEquals("Anime E12", DetailActions.checkInName("Anime", EpisodeRef(null, 12)))
    }

    private fun ep(
        season: Int?,
        episode: Int,
        aired: Boolean = true,
        special: Boolean = false,
    ) = Episode(season, episode, null, null, null, null, aired, special)

    private val show =
        listOf(ep(0, 1, special = true), ep(1, 1), ep(1, 2), ep(1, 3), ep(2, 1), ep(2, 2), ep(2, 3, aired = false))

    @Test
    fun earlierUnwatchedListsOpenEpisodesBeforeTheTickedOne() {
        val item =
            LibraryItem(type = MediaType.Tv, ids = MediaIds(1), title = "Show", status = ListStatus.Watching, watched = listOf(WatchedSeason(1, listOf(1))))
        assertEquals(
            listOf(EpisodeRef(1, 2), EpisodeRef(1, 3), EpisodeRef(2, 1)),
            DetailActions.earlierUnwatched(show, item, EpisodeRef(2, 2)),
        )
    }

    @Test
    fun earlierUnwatchedIsEmptyWhenEverythingBeforeIsWatched() {
        val item =
            LibraryItem(type = MediaType.Tv, ids = MediaIds(1), title = "Show", status = ListStatus.Watching, watched = listOf(WatchedSeason(1, listOf(1, 2))))
        assertTrue(DetailActions.earlierUnwatched(show, item, EpisodeRef(1, 3)).isEmpty())
        assertTrue(DetailActions.earlierUnwatched(show, null, EpisodeRef(1, 1)).isEmpty())
    }

    @Test
    fun earlierUnwatchedCountsEverythingForATitleNotInTheLibrary() {
        assertEquals(5, DetailActions.earlierUnwatched(show, null, EpisodeRef(2, 3)).size)
    }

    @Test
    fun earlierUnwatchedWorksForAnimeWithoutSeasons() {
        val anime = listOf(ep(null, 1), ep(null, 2), ep(null, 3))
        assertEquals(listOf(EpisodeRef(null, 1), EpisodeRef(null, 2)), DetailActions.earlierUnwatched(anime, null, EpisodeRef(null, 3)))
    }
}
