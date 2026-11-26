package to.wim.watchlist.data.model

/** Which watch buttons the detail screen shows. Kept out of the composable so it can be unit tested. */
object DetailActions {
    /**
     * A movie's own "Watched" button. Hidden for a completed movie, and while the progress banner
     * is showing, because the banner has a Watched button of its own.
     */
    fun showMovieWatched(
        status: ListStatus?,
        playback: Playback?,
    ): Boolean = status != ListStatus.Completed && playback == null

    /**
     * The "Watched <next episode>" button. Hidden while the progress banner is on that very episode;
     * a banner for another episode leaves it alone, because it marks a different episode.
     */
    fun showNextEpisodeWatched(
        next: EpisodeRef,
        playback: Playback?,
    ): Boolean = playback?.episode != next

    /** How the check-in confirmation names what is being checked in: the title, plus the episode for a show. */
    fun checkInName(
        title: String,
        episode: EpisodeRef?,
    ): String = if (episode == null) title else "$title ${episode.label}"

    /**
     * Aired episodes before [ref] that aren't watched yet, in order, so ticking a later episode can
     * offer to mark them too. Specials are left out: they sit outside the story order.
     */
    fun earlierUnwatched(
        episodes: List<Episode>,
        item: LibraryItem?,
        ref: EpisodeRef,
    ): List<EpisodeRef> {
        fun order(season: Int?, episode: Int) = (season ?: 1).toLong() * 100_000 + episode
        val target = order(ref.season, ref.episode)
        return episodes
            .filter { it.aired && !it.special && (it.season ?: 1) > 0 }
            .filter { order(it.season, it.episode) < target }
            .filter { item?.isWatched(it.season, it.episode) != true }
            .sortedBy { order(it.season, it.episode) }
            .map { EpisodeRef(it.season, it.episode) }
            .distinct()
    }
}
