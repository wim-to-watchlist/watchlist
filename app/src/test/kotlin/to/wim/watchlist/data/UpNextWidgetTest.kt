package to.wim.watchlist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import to.wim.watchlist.data.model.Access
import to.wim.watchlist.data.model.EpisodeRef
import to.wim.watchlist.data.model.LibraryItem
import to.wim.watchlist.data.model.ListStatus
import to.wim.watchlist.data.model.MediaIds
import to.wim.watchlist.data.model.MediaType
import to.wim.watchlist.data.model.NextEpisode
import to.wim.watchlist.data.model.UpNextWidget
import to.wim.watchlist.data.model.WidgetContent
import to.wim.watchlist.data.model.WidgetRow
import java.time.Instant

class UpNextWidgetTest {
    private val now = Instant.parse("2026-11-22T12:00:00Z")
    private val all = MediaType.entries.toSet()

    private fun show(
        id: Long,
        title: String,
        watchedAt: String,
        type: MediaType = MediaType.Tv,
        status: ListStatus = ListStatus.Watching,
        next: NextEpisode? = NextEpisode(EpisodeRef(1, 2), "Second", "2026-11-01"),
    ) = LibraryItem(type, MediaIds(id), title, status = status, lastWatchedAt = watchedAt, nextToWatch = next)

    private fun content(
        vararg items: LibraryItem,
        plus: Boolean = true,
        types: Set<MediaType> = all,
    ) = UpNextWidget.content(plus, LibraryState(items = items.toList()), types, fromLatest = false, now = now)

    @Test
    fun `only a confirmed subscription or trial opens the widget`() {
        assertTrue(UpNextWidget.unlocked(Access.Allowed, previewLocked = false))
        assertFalse(UpNextWidget.unlocked(Access.Checking, previewLocked = false))
        assertFalse(UpNextWidget.unlocked(Access.Locked, previewLocked = false))
        assertFalse(UpNextWidget.unlocked(Access.Allowed, previewLocked = true))
    }

    @Test
    fun `the free version shows the Plus message, not the shows`() {
        assertEquals(WidgetContent.Locked, content(show(1, "Severance", "2026-11-20"), plus = false))
    }

    @Test
    fun `nothing up next shows the empty message`() {
        assertEquals(WidgetContent.Empty, content())
        // Caught up: the next episode hasn't aired yet.
        assertEquals(WidgetContent.Empty, content(show(1, "Severance", "2026-11-20", next = NextEpisode(EpisodeRef(2, 1), date = "2026-12-01"))))
    }

    @Test
    fun `shows are sorted by last watched with their next episode`() {
        val result = content(show(1, "Old", "2026-10-01"), show(2, "New", "2026-11-20", next = NextEpisode(EpisodeRef(3, 4))))
        assertEquals(
            WidgetContent.Shows(
                listOf(
                    WidgetRow(MediaType.Tv, 2, "New", "S03E04"),
                    WidgetRow(MediaType.Tv, 1, "Old", "S01E02 · Second"),
                ),
                more = 0,
            ),
            result,
        )
    }

    @Test
    fun `movies, other lists and turned off types are left out`() {
        val result =
            content(
                show(1, "Movie", "2026-11-20", type = MediaType.Movie),
                show(2, "Planned", "2026-11-20", status = ListStatus.PlanToWatch),
                show(3, "Anime", "2026-11-20", type = MediaType.Anime),
                show(4, "Tv", "2026-11-19"),
                types = setOf(MediaType.Tv, MediaType.Movie),
            )
        assertEquals(listOf("Tv"), (result as WidgetContent.Shows).rows.map { it.title })
    }

    @Test
    fun `at most five shows, the rest is counted`() {
        val items = (1..8L).map { show(it, "Show $it", "2026-11-%02d".format(it)) }.toTypedArray()
        val result = content(*items) as WidgetContent.Shows
        assertEquals(listOf("Show 8", "Show 7", "Show 6", "Show 5", "Show 4"), result.rows.map { it.title })
        assertEquals(3, result.more)
    }
}
