package to.wim.watchlist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import to.wim.watchlist.data.model.EpisodeRef
import to.wim.watchlist.data.model.LibraryItem
import to.wim.watchlist.data.model.ListStatus
import to.wim.watchlist.data.model.MediaIds
import to.wim.watchlist.data.model.MediaType
import to.wim.watchlist.data.model.Playback
import to.wim.watchlist.data.model.Shelves

class PausedMoviesTest {
    private fun movie(
        id: Long,
        status: ListStatus = ListStatus.PlanToWatch,
    ) = LibraryItem(MediaType.Movie, MediaIds(id), "Movie $id", poster = "p$id", status = status)

    private fun paused(
        id: Long,
        at: String,
        type: MediaType = MediaType.Movie,
        episode: EpisodeRef? = null,
    ) = Playback(id * 10, type, id, "Paused $id", 40.0, at, episode)

    @Test
    fun noPausedMoviesMeansNoTab() {
        assertTrue(Shelves.pausedMovies(listOf(movie(1)), emptyList()).isEmpty())
    }

    @Test
    fun mostRecentlyPausedFirstAndOnlyMovies() {
        val playback =
            listOf(
                paused(1, "2026-11-20T10:00:00Z"),
                paused(2, "2026-11-24T10:00:00Z"),
                paused(3, "2026-11-25T10:00:00Z", MediaType.Tv, EpisodeRef(1, 2)),
            )
        val result = Shelves.pausedMovies(listOf(movie(1), movie(2, ListStatus.Completed)), playback)
        assertEquals(listOf(2L, 1L), result.map { it.ids.simkl })
        // The library's own item is used, with its poster.
        assertEquals("p2", result.first().poster)
    }

    @Test
    fun movieNotOnAnyListStillShows() {
        val result = Shelves.pausedMovies(emptyList(), listOf(paused(7, "2026-11-25T10:00:00Z")))
        assertEquals(1, result.size)
        assertEquals("Paused 7", result.single().title)
        assertNull(result.single().poster)
    }

    @Test
    fun twoSessionsOfOneMovieShowOnce() {
        val result = Shelves.pausedMovies(listOf(movie(1)), listOf(paused(1, "2026-11-20T10:00:00Z"), paused(1, "2026-11-21T10:00:00Z")))
        assertEquals(1, result.size)
    }
}
