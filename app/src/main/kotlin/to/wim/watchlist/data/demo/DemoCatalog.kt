package to.wim.watchlist.data.demo

import to.wim.watchlist.data.model.ListStatus
import to.wim.watchlist.data.model.MediaType
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/** The colours of a title's drawn poster: a sky from [skyTop] to [skyBottom], a [sun] and dark [hills]. */
data class DemoPalette(
    val skyTop: Long,
    val skyBottom: Long,
    val sun: Long,
    val hills: Long,
)

/**
 * A made-up title of the demo. [seasons] holds the episode count of each season (anime: one entry,
 * numbered without seasons); the last [upcoming] episodes haven't aired yet and air weekly from
 * tomorrow. [list], [watched] (episodes, in order) and [rating] are where the demo's lists start.
 */
data class DemoTitle(
    val id: Long,
    val type: MediaType,
    val title: String,
    val year: Int,
    val genres: List<String>,
    val overview: String,
    val network: String? = null,
    val runtime: Int,
    val communityRating: Double,
    val votes: Int,
    val ended: Boolean = true,
    val seasons: List<Int> = emptyList(),
    val upcoming: Int = 0,
    /** Movies: days until release; negative or null when released. */
    val releaseInDays: Int? = null,
    val list: ListStatus? = null,
    val watched: Int = 0,
    val rating: Int? = null,
    val palette: Int,
) {
    val total: Int get() = seasons.sum()
    val aired: Int get() = total - upcoming
    val poster: String get() = "${DemoCatalog.SCHEME}poster/$id"
    val fanart: String get() = "${DemoCatalog.SCHEME}fanart/$id"
}

/** One episode of a demo show, with its air date worked out from "now". */
data class DemoEpisode(
    val season: Int?,
    val number: Int,
    val title: String,
    val date: Instant,
) {
    val seasonOrOne: Int get() = season ?: 1
}

/**
 * The fictional shows, anime and movies the demo is filled with. None of them exist; their
 * pictures are drawn on the phone (see `ui/demo/DemoArt.kt`) from the `demo:` image paths, so the
 * demo makes no network requests at all. Plain Kotlin so it is unit tested.
 */
object DemoCatalog {
    const val SCHEME = "demo:"

    /** Far above SIMKL's own IDs, so nothing the demo leaves in memory can match a real title. */
    private const val BASE = 9_900_000_000L

    val palettes =
        listOf(
            DemoPalette(0xFF2A2F4A, 0xFFB0706A, 0xFFE8C48A, 0xFF141A2C),
            DemoPalette(0xFF123A5C, 0xFF2C7DA0, 0xFFE0C23A, 0xFF0B2234),
            DemoPalette(0xFF2B1B2E, 0xFF5E2B5C, 0xFFE8875E, 0xFF161018),
            DemoPalette(0xFF3A0CA3, 0xFF7209B7, 0xFFE7307A, 0xFF1C0752),
            DemoPalette(0xFF2B2A30, 0xFFB07A2A, 0xFFDCDCDC, 0xFF111522),
            DemoPalette(0xFF22063F, 0xFF6A2C9E, 0xFFF29111, 0xFF120326),
            DemoPalette(0xFF0B3B45, 0xFF0F9C83, 0xFFEBC55A, 0xFF062C33),
            DemoPalette(0xFF0B3D66, 0xFF0C7AA6, 0xFFF0BC0F, 0xFF041B33),
            DemoPalette(0xFF27263A, 0xFF6E6478, 0xFFE6DCDC, 0xFF11111A),
            DemoPalette(0xFF3E6B48, 0xFF83B051, 0xFFE8E2C4, 0xFF1D3522),
            DemoPalette(0xFF6B0F2A, 0xFFB3173F, 0xFFF2A5B0, 0xFF2A0612),
            DemoPalette(0xFF5A3518, 0xFF9E6A43, 0xFFE8D6C6, 0xFF2A1708),
        )

    private const val SHOW = "Drama"

    val titles: List<DemoTitle> =
        listOf(
            // TV shows in the lists
            DemoTitle(
                BASE + 1, MediaType.Tv, "The Lantern Keepers", 2025, listOf(SHOW, "Mystery"),
                "On a fogbound island, the last lighthouse keepers guard a secret older than the lamp itself. When a stranger washes ashore with a map nobody can read, the family must decide what the light is really for.",
                network = "Aurora Network", runtime = 46, communityRating = 8.6, votes = 4210, ended = false,
                seasons = listOf(10, 10), upcoming = 3, list = ListStatus.Watching, watched = 13, rating = 9, palette = 0,
            ),
            DemoTitle(
                BASE + 2, MediaType.Tv, "Harbor Lights", 2021, listOf(SHOW, "Romance"),
                "Two rival ferry captains share one crumbling harbor town and, slowly, a lot more than the timetable.",
                network = "Northshore TV", runtime = 42, communityRating = 7.9, votes = 2380,
                seasons = listOf(8, 8), list = ListStatus.Watching, watched = 12, palette = 1,
            ),
            DemoTitle(
                BASE + 3, MediaType.Tv, "Midnight Orchard", 2026, listOf("Thriller", "Mystery"),
                "Every night at twelve, the apple trees on the Varga farm bloom for one hour. This year someone is picking the fruit.",
                network = "Aurora Network", runtime = 52, communityRating = 8.1, votes = 1190, ended = false,
                seasons = listOf(10), upcoming = 3, list = ListStatus.Watching, watched = 5, palette = 2,
            ),
            DemoTitle(
                BASE + 4, MediaType.Tv, "Paper Moons", 2022, listOf("Comedy", SHOW),
                "A failing puppet theatre gets one last season to sell out the house, with a cast that can barely stand each other.",
                network = "Lumen", runtime = 28, communityRating = 7.6, votes = 980,
                seasons = listOf(12), list = ListStatus.Watching, watched = 8, palette = 3,
            ),
            DemoTitle(
                BASE + 5, MediaType.Tv, "Northbound Signal", 2026, listOf("Science Fiction"),
                "A radio operator at an Arctic station picks up a message that hasn't been sent yet.",
                network = "Meridian+", runtime = 48, communityRating = 8.3, votes = 310, ended = false,
                seasons = listOf(8), upcoming = 8, list = ListStatus.PlanToWatch, palette = 7,
            ),
            DemoTitle(
                BASE + 6, MediaType.Tv, "Quarry Street", 2022, listOf("Crime", SHOW),
                "One street, one summer, one stolen truck of marble, told from every house on the block.",
                network = "Northshore TV", runtime = 50, communityRating = 7.7, votes = 3150,
                seasons = listOf(6, 6), list = ListStatus.Completed, watched = 12, rating = 8, palette = 4,
            ),
            DemoTitle(
                BASE + 7, MediaType.Tv, "Fable Court", 2024, listOf("Fantasy", "Comedy"),
                "A small-claims court for fairy-tale creatures, where the judge has been a frog for eleven years.",
                network = "Lumen", runtime = 30, communityRating = 8.2, votes = 1740,
                seasons = listOf(10), list = ListStatus.OnHold, watched = 3, palette = 5,
            ),
            DemoTitle(
                BASE + 8, MediaType.Tv, "Tidewater Bureau", 2026, listOf("Crime"),
                "A two-person police unit that only investigates what the sea brings in.",
                network = "Meridian+", runtime = 44, communityRating = 7.9, votes = 620,
                seasons = listOf(8), list = ListStatus.Dropped, watched = 2, palette = 7,
            ),
            // Anime in the lists
            DemoTitle(
                BASE + 21, MediaType.Anime, "Starlit Cartographer", 2025, listOf("Adventure", "Fantasy"),
                "A girl who maps the night sky discovers the stars move whenever she finishes a chart.",
                network = "Studio Kiriko", runtime = 24, communityRating = 8.4, votes = 2900, ended = false,
                seasons = listOf(24), upcoming = 5, list = ListStatus.Watching, watched = 17, rating = 9, palette = 5,
            ),
            DemoTitle(
                BASE + 22, MediaType.Anime, "Copper Fox Academy", 2023, listOf("Comedy", "School"),
                "The only school for shape-shifters has one rule: be yourself during exams.",
                network = "Studio Hanabi", runtime = 24, communityRating = 7.8, votes = 1500,
                seasons = listOf(12), list = ListStatus.PlanToWatch, palette = 11,
            ),
            DemoTitle(
                BASE + 23, MediaType.Anime, "Ninefold Garden", 2022, listOf(SHOW, "Slice of Life"),
                "Nine neighbours, one shared garden, and a year of seasons told one plant at a time.",
                network = "Studio Kiriko", runtime = 23, communityRating = 8.7, votes = 3400,
                seasons = listOf(13), list = ListStatus.Completed, watched = 13, rating = 10, palette = 9,
            ),
            // Movies in the lists
            DemoTitle(
                BASE + 41, MediaType.Movie, "The Quiet Meridian", 2025, listOf(SHOW),
                "A retired astronomer drives across the country to see one last eclipse with the son she hasn't spoken to in years.",
                runtime = 118, communityRating = 8.5, votes = 5100, list = ListStatus.PlanToWatch, palette = 4,
            ),
            DemoTitle(
                BASE + 42, MediaType.Movie, "Velvet Horizon", 2024, listOf("Science Fiction", "Romance"),
                "Two pilots on opposite ends of a failing space elevator talk each other through the longest night of their lives.",
                runtime = 126, communityRating = 8.2, votes = 4300, list = ListStatus.PlanToWatch, palette = 5,
            ),
            DemoTitle(
                BASE + 43, MediaType.Movie, "Glass Harvest", 2023, listOf("Thriller"),
                "A greenhouse inspector finds a crop that shouldn't exist, and a company that would rather she hadn't.",
                runtime = 104, communityRating = 7.6, votes = 2700, list = ListStatus.PlanToWatch, palette = 6,
            ),
            DemoTitle(
                BASE + 44, MediaType.Movie, "Cobalt Summer", 2026, listOf("Comedy", SHOW),
                "Three sisters inherit a beach motel with one guest who refuses to check out.",
                runtime = 98, communityRating = 7.7, votes = 640, releaseInDays = 9, ended = false, list = ListStatus.PlanToWatch, palette = 7,
            ),
            DemoTitle(
                BASE + 45, MediaType.Movie, "Last Train to Aldermoor", 2024, listOf("Mystery"),
                "Eight passengers, one snowed-in train, and a conductor who knows all of their names.",
                runtime = 112, communityRating = 7.9, votes = 3900, list = ListStatus.Completed, rating = 8, palette = 8,
            ),
            DemoTitle(
                BASE + 46, MediaType.Movie, "A Field of Small Suns", 2023, listOf(SHOW, "Family"),
                "A beekeeper teaches her grandson to read the weather from the hives in their last summer on the farm.",
                runtime = 109, communityRating = 8.0, votes = 2100, list = ListStatus.Completed, palette = 9,
            ),
            DemoTitle(
                BASE + 47, MediaType.Movie, "Saltwater Static", 2022, listOf("Horror"),
                "A pirate radio crew on an abandoned sea fort keeps hearing a station that isn't theirs.",
                runtime = 95, communityRating = 6.8, votes = 1200, list = ListStatus.Dropped, palette = 1,
            ),
            // In the catalogue only: Discover, Search, recommendations
            DemoTitle(
                BASE + 61, MediaType.Tv, "Glasshouse Lane", 2025, listOf(SHOW),
                "A botanist returns to her hometown to run the glasshouse her mother left to the whole village.",
                network = "Lumen", runtime = 45, communityRating = 8.1, votes = 1320, ended = false,
                seasons = listOf(8), upcoming = 2, palette = 9,
            ),
            DemoTitle(
                BASE + 62, MediaType.Tv, "The Understudies", 2024, listOf("Comedy"),
                "The backup cast of a West End musical has to go on, every single night.",
                network = "Northshore TV", runtime = 32, communityRating = 7.8, votes = 2210,
                seasons = listOf(8, 8), palette = 10,
            ),
            DemoTitle(
                BASE + 63, MediaType.Tv, "Wild Meridian", 2023, listOf("Western", SHOW),
                "A surveyor mapping a new railway line keeps finding towns that aren't on any map.",
                network = "Meridian+", runtime = 55, communityRating = 8.0, votes = 1870,
                seasons = listOf(10), palette = 11,
            ),
            DemoTitle(
                BASE + 64, MediaType.Tv, "Ironwood Pass", 2026, listOf("Adventure"),
                "Mountain rescuers on the busiest pass in the north, one storm at a time.",
                network = "Aurora Network", runtime = 47, communityRating = 7.5, votes = 410, ended = false,
                seasons = listOf(10), upcoming = 6, palette = 8,
            ),
            DemoTitle(
                BASE + 81, MediaType.Anime, "Moonlit Relay", 2026, listOf("Sports", SHOW),
                "A night-time relay team trains on empty city streets for a race nobody believes in.",
                network = "Studio Hanabi", runtime = 24, communityRating = 8.0, votes = 880, ended = false,
                seasons = listOf(12), upcoming = 4, palette = 3,
            ),
            DemoTitle(
                BASE + 82, MediaType.Anime, "Cloud Forge", 2024, listOf("Fantasy", "Action"),
                "A blacksmith apprentice forges weapons out of weather.",
                network = "Studio Kiriko", runtime = 24, communityRating = 7.9, votes = 2600,
                seasons = listOf(24), palette = 2,
            ),
            DemoTitle(
                BASE + 101, MediaType.Movie, "Amber Station", 2025, listOf("Science Fiction"),
                "The last crew of a mining station has 48 hours to decide who goes home.",
                runtime = 121, communityRating = 7.4, votes = 3300, palette = 11,
            ),
            DemoTitle(
                BASE + 102, MediaType.Movie, "The Long Thaw", 2024, listOf(SHOW),
                "When the lake ice breaks for the first time in a century, a village has to remember how to fish.",
                runtime = 101, communityRating = 7.8, votes = 1700, palette = 1,
            ),
            DemoTitle(
                BASE + 103, MediaType.Movie, "Orchid Protocol", 2026, listOf("Thriller", "Action"),
                "A florist with a forgotten past is asked to deliver one last bouquet.",
                runtime = 114, communityRating = 7.2, votes = 290, releaseInDays = 18, ended = false, palette = 10,
            ),
        )

    private val byId = titles.associateBy { it.id }

    fun title(id: Long): DemoTitle? = byId[id]

    private val episodeTitles =
        listOf(
            "Small Hours", "Cold Open", "Glass Houses", "Almost Spring", "The Long Way Round", "Low Tide", "Paper Walls",
            "Second Light", "What the River Knew", "Borrowed Time", "The Quiet Part", "Northern Lines", "Salt and Iron",
            "A Door in the Fog", "Half Past Never", "Old Maps", "The Visitor", "Open Water", "Late Bloomers", "Static",
            "Homecoming", "The Keeper's Daughter", "Dead Reckoning", "Night Market", "First Frost", "The Last Ferry",
        )

    /** What time episodes air: 19:00 UTC, the same weekday for a show. */
    private fun airTime(day: Instant): Instant = day.truncatedTo(ChronoUnit.DAYS).plus(Duration.ofHours(19))

    /**
     * The episodes of [title]: aired ones weekly up to last week (an ended show stopped a year
     * ago), upcoming ones weekly from tomorrow.
     */
    fun episodes(
        title: DemoTitle,
        now: Instant,
    ): List<DemoEpisode> {
        if (!title.type.isEpisodic) return emptyList()
        val anime = title.type == MediaType.Anime
        val lastAired = if (title.ended) airTime(now.minus(Duration.ofDays(400))) else airTime(now.minus(Duration.ofDays(6)))
        val firstUpcoming = airTime(now.plus(Duration.ofDays(1)))
        var index = 0
        return title.seasons.flatMapIndexed { s, count ->
            (1..count).map { n ->
                val i = index++
                val date =
                    if (i < title.aired) {
                        lastAired.minus(Duration.ofDays(7L * (title.aired - 1 - i)))
                    } else {
                        firstUpcoming.plus(Duration.ofDays(7L * (i - title.aired)))
                    }
                DemoEpisode(
                    season = if (anime) null else s + 1,
                    number = n,
                    title = episodeTitles[((title.id + i * 7) % episodeTitles.size).toInt()],
                    date = date,
                )
            }
        }
    }

    /** A movie's release date: [DemoTitle.releaseInDays] from now, or its year. */
    fun releaseDate(
        title: DemoTitle,
        now: Instant,
    ): Instant =
        title.releaseInDays?.takeIf { it > 0 }?.let { airTime(now.plus(Duration.ofDays(it.toLong()))) }
            ?: Instant.parse("${title.year}-04-15T00:00:00Z").coerceAtMost(now.minus(Duration.ofDays(30)))
}
