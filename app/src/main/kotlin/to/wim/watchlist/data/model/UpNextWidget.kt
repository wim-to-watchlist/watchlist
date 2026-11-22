package to.wim.watchlist.data.model

import to.wim.watchlist.data.LibraryState
import java.time.Instant

/** One show on the Up next home-screen widget. */
data class WidgetRow(
    val type: MediaType,
    val simklId: Long,
    val title: String,
    /** The next episode, such as "S02E05 · Title"; null when SIMKL doesn't say which one. */
    val episode: String?,
)

/** What the Up next widget shows. */
sealed interface WidgetContent {
    /** The free version: the widget is part of Watchlist Plus. */
    data object Locked : WidgetContent

    /** Signed out, not synced yet, or nothing to watch. */
    data object Empty : WidgetContent

    data class Shows(
        val rows: List<WidgetRow>,
        /** Up next shows that don't fit on the widget. */
        val more: Int,
    ) : WidgetContent
}

/**
 * The Up next home-screen widget (Watchlist Plus): the shows of the Up next tabs (TV shows and
 * anime that are turned on), most recently watched first, as the tab sorts them by default. Built
 * from the cached library only, so the widget never calls SIMKL. Plain Kotlin so it is unit tested.
 */
object UpNextWidget {
    const val MAX_ROWS = 5

    /**
     * Only a subscription or trial Google Play confirmed opens the widget. Unlike the app, still
     * asking counts as locked: the widget is drawn in the background, and a free user's home
     * screen should not show Up next while Google Play has not answered yet.
     */
    fun unlocked(
        access: Access,
        previewLocked: Boolean,
    ): Boolean = !previewLocked && access == Access.Allowed

    fun content(
        plus: Boolean,
        state: LibraryState,
        types: Set<MediaType>,
        fromLatest: Boolean,
        now: Instant = Instant.now(),
    ): WidgetContent {
        if (!plus) return WidgetContent.Locked
        val shows =
            state.items
                .filter { it.type != MediaType.Movie && it.type in types }
                .filter { Shelves.shelfOf(it, now, fromLatest, state.latestNext[it.key], state.airedNext(it, fromLatest, now)) == Shelf.UpNext }
                .sortedByDescending { it.lastWatchedAt ?: it.addedAt ?: "" }
        if (shows.isEmpty()) return WidgetContent.Empty
        val rows =
            shows.take(MAX_ROWS).map { item ->
                val next = Shelves.nextEpisode(item, fromLatest, state.latestNext[item.key], state.airedNext(item, fromLatest, now))
                WidgetRow(item.type, item.ids.simkl, item.title, next?.let { episodeLabel(it) })
            }
        return WidgetContent.Shows(rows, shows.size - rows.size)
    }

    fun episodeLabel(next: NextEpisode): String = listOfNotNull(next.ref.label, next.title?.takeIf { it.isNotBlank() }).joinToString(" · ")
}
