package to.wim.watchlist.data.model

/**
 * The free version and Watchlist Plus. Free: signing in, the lists, marking watched, ratings,
 * details, search, Discover and the watch totals on the profile. Plus (the subscription or its free
 * trial): Up next, the Calendar, new-episode notifications, the stats per type, Surprise me, and
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

    /** How many Up next rows stay readable while locked, so the feature shows itself on the user's own shows. */
    const val UP_NEXT_PREVIEW = 1

    /** Up next while locked: the rows shown as they are, and how many more are blurred. */
    fun <T> preview(items: List<T>): Pair<List<T>, Int> =
        items.take(UP_NEXT_PREVIEW) to (items.size - UP_NEXT_PREVIEW).coerceAtLeast(0)
}
