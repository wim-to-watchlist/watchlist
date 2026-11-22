package to.wim.watchlist.data.model

import to.wim.watchlist.data.AppSettings
import to.wim.watchlist.data.NavConfig
import to.wim.watchlist.data.NavTab

/**
 * The free version and Watchlist Plus. Free: signing in, the lists, marking watched, ratings,
 * details, search, Discover and the watch totals on the profile. Plus (the subscription or its free
 * trial): Up next, the Calendar, new-episode notifications, the stats per type, Surprise me, the
 * list of rated titles (rating stays free), trailers and similar titles, colours from the wallpaper,
 * a bottom bar of one's own, the Up next home-screen widget, every setting except light or dark mode
 * (see [settings]), and
 * everything from TMDB (cast, people, people search). The free version never mentions TMDB.
 * Locked parts stay on screen, blurred, under a card
 * that leads to the subscription screen. Plain Kotlin so it is unit tested.
 */
object Plus {
    /**
     * Whether Plus features are open. Still asking Google Play counts as open, so a subscriber
     * doesn't see a locked screen flash at every start; someone Google Play said no to before is
     * locked straight away by the stored answer ([SubscriptionRules.access]).
     */
    fun unlocked(
        access: Access,
        previewLocked: Boolean = false,
    ): Boolean = !previewLocked && access != Access.Locked

    /** The user's TMDB token as the app may use it: none in the free version, so nothing asks TMDB. */
    fun tmdbKey(
        plus: Boolean,
        stored: String?,
    ): String? = stored.takeIf { plus }

    /** The bottom bar: the user's own order and tabs with Plus, the default ones without. */
    fun nav(
        plus: Boolean,
        stored: NavConfig,
    ): NavConfig = if (plus) stored else NavConfig(NavTab.defaultOrder, NavTab.defaultEnabled)

    /**
     * The settings as the app uses them: the user's own with Plus; without it the defaults for
     * everything but light or dark mode (all types shown, specials hidden, Up next from the latest
     * episode, the default bottom bar, no wallpaper colours, no notifications, no TMDB token). The
     * stored choices are kept, so they come back when Plus does.
     */
    fun settings(
        plus: Boolean,
        stored: AppSettings,
    ): AppSettings {
        if (plus) return stored
        val defaults = AppSettings()
        return stored.copy(
            dynamicColor = false,
            nav = defaults.nav,
            notifications = false,
            hideSpecials = defaults.hideSpecials,
            continueFromLatest = defaults.continueFromLatest,
            types = defaults.types,
            tmdbKey = null,
        )
    }

    /** How many Up next rows stay readable while locked, so the feature shows itself on the user's own shows. */
    const val UP_NEXT_PREVIEW = 1

    /** Up next while locked: the rows shown as they are, and how many more are blurred. */
    fun <T> preview(items: List<T>): Pair<List<T>, Int> =
        items.take(UP_NEXT_PREVIEW) to (items.size - UP_NEXT_PREVIEW).coerceAtLeast(0)

    /**
     * A grouped list while locked (the Calendar by day, My ratings by type): the first item of
     * the first group stays readable, every other item is blurred. Group headers and their counts
     * always stay readable, so only the paywalled content is hidden.
     */
    fun <K, T> previewGroups(groups: List<Pair<K, List<T>>>): List<PreviewGroup<K, T>> {
        var left = UP_NEXT_PREVIEW
        return groups.map { (key, items) ->
            val shown = items.take(left)
            left -= shown.size
            PreviewGroup(key, items.size, shown, items.drop(shown.size))
        }
    }

    /** How many items [previewGroups] blurs in all. */
    fun <K, T> lockedCount(groups: List<PreviewGroup<K, T>>): Int = groups.sumOf { it.locked.size }
}

/** One group of a locked list: its header ([key], [total]), the readable items and the blurred ones. */
data class PreviewGroup<K, T>(
    val key: K,
    val total: Int,
    val shown: List<T>,
    val locked: List<T>,
)
