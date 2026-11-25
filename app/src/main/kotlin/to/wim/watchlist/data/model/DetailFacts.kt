package to.wim.watchlist.data.model

/** Which facts of a title the info card shows. Kept out of the composable so it can be unit tested. */
object DetailFacts {
    /** Facts shown while the card is collapsed: the first few, in the card's order (rank, genres, status). */
    const val COLLAPSED_COUNT = 3

    /** All the facts when [expanded], else only the first [COLLAPSED_COUNT]. */
    fun <T> visible(
        facts: List<T>,
        expanded: Boolean,
    ): List<T> = if (expanded) facts else facts.take(COLLAPSED_COUNT)

    /** Whether there is anything more to show; a card with few facts has no expand button. */
    fun canExpand(count: Int): Boolean = count > COLLAPSED_COUNT
}

/** The air time ("Mondays at 21:00") only means something while new episodes still come. */
fun showsAirTime(status: String?): Boolean =
    when (ShowStatus.fromApi(status)) {
        ShowStatus.Ended, ShowStatus.Cancelled -> false
        else -> true
    }

/**
 * SIMKL gives a country as a two-letter code ("AT"). Shown as its name in the phone's language
 * ("Austria", "Oostenrijk"); anything that isn't a known code is shown as SIMKL wrote it.
 */
fun countryName(
    raw: String,
    locale: java.util.Locale,
): String {
    val code = raw.trim()
    if (code.length != 2 || !code.all { it.isLetter() }) return raw
    val upper = code.uppercase(java.util.Locale.ROOT)
    if (upper !in java.util.Locale.getISOCountries()) return raw
    val name = java.util.Locale.Builder().setRegion(upper).build().getDisplayCountry(locale)
    return name.ifBlank { raw }
}

/**
 * The genres SIMKL uses for shows and movies, so the app can show them in the phone's language.
 * A genre not in this list is shown as SIMKL wrote it.
 */
enum class Genre {
    Action,
    Adventure,
    Animation,
    Anime,
    Biography,
    Children,
    Comedy,
    Crime,
    Documentary,
    Drama,
    Family,
    Fantasy,
    GameShow,
    History,
    Horror,
    MiniSeries,
    Music,
    Musical,
    Mystery,
    News,
    Reality,
    Romance,
    ScienceFiction,
    Soap,
    Sport,
    Suspense,
    TalkShow,
    Thriller,
    War,
    Western,
    ;

    companion object {
        private val byKey: Map<String, Genre> =
            buildMap {
                Genre.entries.forEach { put(it.name.lowercase(), it) }
                put("sci-fi", ScienceFiction)
                put("science fiction", ScienceFiction)
                put("science-fiction", ScienceFiction)
                put("game show", GameShow)
                put("game-show", GameShow)
                put("mini-series", MiniSeries)
                put("mini series", MiniSeries)
                put("talk show", TalkShow)
                put("talk-show", TalkShow)
                put("kids", Children)
                put("sports", Sport)
            }

        fun fromApi(raw: String): Genre? = byKey[raw.trim().lowercase()]
    }
}
