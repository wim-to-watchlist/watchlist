package to.wim.watchlist.data.model

/**
 * Keeps a list in the order the user first saw it while they are on the screen. Up next sorts by
 * the last watched show, so a show jumped to the top as soon as it was marked and a second tap on
 * the same spot marked another show (AI testers, round 8). Items already shown keep their place;
 * new ones go in at their place in the sorted list. Leaving the screen forgets the order.
 */
object StableOrder {
    /** The items in the order to show, and the keys to remember for the next update. */
    fun <T : Any> apply(
        shownKeys: List<String>,
        sorted: List<T>,
        key: (T) -> String,
    ): Pair<List<T>, List<String>> {
        val byKey = sorted.associateBy(key)
        val result = shownKeys.mapNotNull { byKey[it] }.toMutableList()
        val kept = result.map(key).toSet()
        sorted.forEachIndexed { index, item ->
            if (key(item) !in kept) result.add(minOf(index, result.size), item)
        }
        return result to result.map(key)
    }
}
