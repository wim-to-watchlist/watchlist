package to.wim.watchlist.data.demo

import to.wim.watchlist.data.api.arr
import to.wim.watchlist.data.api.asObject
import to.wim.watchlist.data.api.int
import to.wim.watchlist.data.api.long
import to.wim.watchlist.data.api.obj
import to.wim.watchlist.data.api.objects
import to.wim.watchlist.data.api.str
import to.wim.watchlist.data.model.Dates
import to.wim.watchlist.data.model.ListStatus
import to.wim.watchlist.data.model.MediaType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.Duration
import java.time.Instant

/**
 * A pretend SIMKL for the demo: answers the same requests as api.simkl.com and data.simkl.in with
 * the made-up titles of [DemoCatalog], in SIMKL's JSON format, so every screen runs its normal
 * code. Changes (watched, ratings, lists) are kept in memory only and show up in the next sync,
 * as they would on SIMKL. Nothing leaves the phone. Plain Kotlin so it is unit tested.
 */
class DemoSimkl(
    /** The demo profile's name, in the phone's language. */
    private val profileName: String,
    private val clock: () -> Instant = Instant::now,
) {
    private class Entry(
        var status: ListStatus,
        val watched: MutableSet<Pair<Int, Int>>,
        var rating: Int?,
        var lastWatchedAt: Instant?,
        val addedAt: Instant,
        var changed: Long,
        var rated: Long,
    )

    private class Paused(
        val id: Long,
        val title: DemoTitle,
        val season: Int?,
        val episode: Int,
        val progress: Double,
    )

    private val lock = Any()
    private val started = clock()

    /** Bumped by every change; the /sync/activities timestamps are made from it. */
    private var version = 1L
    private var removedVersion = 1L
    private var ratedVersion = 1L
    private var playbackVersion = 1L
    private val entries = LinkedHashMap<Long, Entry>()
    private val paused = mutableListOf<Paused>()

    init {
        DemoCatalog.titles.forEachIndexed { i, t ->
            val list = t.list ?: return@forEachIndexed
            val episodes = DemoCatalog.episodes(t, started)
            val watched = episodes.take(t.watched).map { it.seasonOrOne to it.number }.toMutableSet()
            val lastWatched =
                when {
                    t.watched > 0 || (t.type == MediaType.Movie && list == ListStatus.Completed) -> started.minus(Duration.ofHours(3L + i * 19L))
                    else -> null
                }
            entries[t.id] = Entry(list, watched, t.rating, lastWatched, started.minus(Duration.ofDays(30L + i * 11L)), 1, 1)
        }
        // A paused episode for "Continue watching": the one after the last watched of Harbor Lights.
        DemoCatalog.titles.firstOrNull { it.title == "Harbor Lights" }?.let { t ->
            val next = DemoCatalog.episodes(t, started)[t.watched]
            paused += Paused(1, t, next.season, next.number, 62.0)
        }
    }

    private fun stamp(v: Long): String = BASE_STAMP.plusSeconds(v).toString()

    private fun versionOf(stamp: String?): Long = Dates.parse(stamp)?.let { Duration.between(BASE_STAMP, it).seconds } ?: 0

    /**
     * Answers one request: [path] as SimklApi builds it (relative to api.simkl.com, or a full
     * data.simkl.in URL), its query [params] and JSON [body]. Unknown requests get an empty answer.
     */
    fun respond(
        method: String,
        path: String,
        params: Map<String, String?>,
        body: JsonElement?,
    ): JsonElement =
        synchronized(lock) {
            val p = path.removePrefix("https://api.simkl.com").trimEnd('/')
            when {
                p.startsWith("https://data.simkl.in/calendar/") -> calendar(p)
                p.startsWith("https://data.simkl.in/discover/") -> discover(p)
                p == "/sync/activities" -> activities()
                p == "/sync/all-items" -> if (params["extended"] == "simkl_ids_only") library(null, idsOnly = true) else library(params["date_from"])
                p == "/sync/ratings" && body == null -> ratings(params["date_from"])
                p == "/sync/playback" -> playback()
                p.startsWith("/sync/playback/") && method == "DELETE" -> deletePlayback(p.substringAfterLast('/').toLongOrNull())
                p == "/sync/add-to-list" -> change(body) { t, item -> addToList(t, item) }
                p == "/sync/history" -> change(body) { t, item -> history(t, item, watched = true) }
                p == "/sync/history/remove" -> change(body) { t, item -> history(t, item, watched = false) }
                p == "/sync/ratings/remove" -> change(body) { t, _ -> rate(t, null) }
                p.startsWith("/sync/ratings") -> change(body) { t, item -> rate(t, item.int("rating")) }
                p == "/scrobble/checkin" -> JsonObject(emptyMap())
                p == "/ratings/all" -> communityRatings()
                p == "/users/settings" -> user()
                p.startsWith("/users/") && p.endsWith("/stats") -> stats()
                p == "/search/random" -> random(MediaType.fromApi(params["type"]) ?: MediaType.Tv)
                p == "/search/id" -> JsonArray(emptyList())
                p.startsWith("/search/") -> search(MediaType.fromApi(p.substringAfterLast('/')) ?: MediaType.Tv, params["q"].orEmpty())
                else -> catalogue(p)
            }
        }

    // ---- Sync ---------------------------------------------------------------------------------

    private fun activities(): JsonElement =
        buildJsonObject {
            put("all", stamp(version))
            putJsonObject("settings") { put("all", stamp(1)) }
            listOf("tv_shows", "anime", "movies").forEach { key ->
                putJsonObject(key) {
                    put("all", stamp(version))
                    put("removed_from_list", stamp(removedVersion))
                    put("rated_at", stamp(ratedVersion))
                    put("playback", stamp(playbackVersion))
                }
            }
        }

    private fun library(
        dateFrom: String?,
        idsOnly: Boolean = false,
    ): JsonElement {
        val since = dateFrom?.let { versionOf(it) }
        val chosen = entries.filter { (_, e) -> since == null || e.changed > since }
        return grouped(chosen.keys) { t -> if (idsOnly) idsOnlyEntry(t) else libraryEntry(t) }
    }

    private fun ratings(dateFrom: String?): JsonElement {
        val since = versionOf(dateFrom)
        return grouped(entries.filter { it.value.rated > since }.keys) { libraryEntry(it) }
    }

    /** SIMKL's {"shows": [...], "anime": [...], "movies": [...]} answer for [ids]. */
    private fun grouped(
        ids: Collection<Long>,
        entry: (DemoTitle) -> JsonObject,
    ): JsonElement {
        val titles = ids.mapNotNull { DemoCatalog.title(it) }
        if (titles.isEmpty()) return JsonNull
        return buildJsonObject {
            listOf(MediaType.Tv to "shows", MediaType.Anime to "anime", MediaType.Movie to "movies").forEach { (type, key) ->
                val mine = titles.filter { it.type == type }
                if (mine.isNotEmpty()) put(key, JsonArray(mine.map(entry)))
            }
        }
    }

    private fun mediaBlock(t: DemoTitle): JsonObject =
        buildJsonObject {
            put("title", t.title)
            put("year", t.year)
            put("poster", t.poster)
            putJsonObject("ids") {
                put("simkl", t.id)
                put("slug", slug(t))
            }
        }

    private fun idsOnlyEntry(t: DemoTitle) = buildJsonObject { put(if (t.type == MediaType.Movie) "movie" else "show", mediaBlock(t)) }

    private fun libraryEntry(t: DemoTitle): JsonObject {
        val e = entries.getValue(t.id)
        val now = clock()
        val episodes = DemoCatalog.episodes(t, started)
        return buildJsonObject {
            put("status", e.status.api)
            e.rating?.let { put("user_rating", it) }
            put("added_to_watchlist_at", e.addedAt.toString())
            e.lastWatchedAt?.let { put("last_watched_at", it.toString()) }
            if (t.type == MediaType.Anime) put("anime_type", "tv")
            put(if (t.type == MediaType.Movie) "movie" else "show", mediaBlock(t))
            if (!t.type.isEpisodic) return@buildJsonObject
            val aired = episodes.filter { !it.date.isAfter(now) }
            val last = episodes.lastOrNull { (it.seasonOrOne to it.number) in e.watched }
            last?.let { put("last_watched", label(it)) }
            aired.firstOrNull { (it.seasonOrOne to it.number) !in e.watched }?.let { next ->
                putJsonObject("next_to_watch_info") {
                    next.season?.let { put("season", it) }
                    put("episode", next.number)
                    put("title", next.title)
                    put("date", next.date.toString())
                }
            }
            put("watched_episodes_count", e.watched.size)
            put("total_episodes_count", episodes.size)
            put("not_aired_episodes_count", episodes.size - aired.size)
            putJsonArray("seasons") {
                e.watched.groupBy({ it.first }, { it.second }).toSortedMap().forEach { (season, numbers) ->
                    addJsonObject {
                        put("number", season)
                        putJsonArray("episodes") { numbers.sorted().forEach { addJsonObject { put("number", it) } } }
                    }
                }
            }
        }
    }

    private fun playback(): JsonElement =
        buildJsonArray {
            paused.forEach { p ->
                addJsonObject {
                    put("id", p.id)
                    put("progress", p.progress)
                    put("paused_at", started.minus(Duration.ofHours(20)).toString())
                    put("type", "episode")
                    putJsonObject("episode") {
                        p.season?.let { put("season", it) }
                        put("episode", p.episode)
                        DemoCatalog.episodes(p.title, started).firstOrNull { it.season == p.season && it.number == p.episode }?.let { put("title", it.title) }
                    }
                    put(if (p.title.type == MediaType.Anime) "anime" else "show", mediaBlock(p.title))
                }
            }
        }

    private fun deletePlayback(id: Long?): JsonElement {
        if (paused.removeAll { it.id == id }) playbackVersion = ++version
        return JsonNull
    }

    // ---- Changes ------------------------------------------------------------------------------

    /** Applies [apply] to every title in a /sync/... request body, then bumps the activities. */
    private fun change(
        body: JsonElement?,
        apply: (DemoTitle, JsonObject) -> Unit,
    ): JsonElement {
        val obj = body.asObject() ?: return JsonObject(emptyMap())
        listOf("shows", "movies", "anime").forEach { key ->
            obj[key].objects().forEach { item ->
                val id = item.obj("ids")?.long("simkl") ?: return@forEach
                val title = DemoCatalog.title(id) ?: return@forEach
                version++
                apply(title, item)
            }
        }
        return JsonObject(emptyMap())
    }

    private fun entryFor(
        t: DemoTitle,
        status: ListStatus,
    ): Entry = entries.getOrPut(t.id) { Entry(status, mutableSetOf(), null, null, clock(), version, 0) }

    private fun addToList(
        t: DemoTitle,
        item: JsonObject,
    ) {
        val status = ListStatus.fromApi(item.str("to")) ?: return
        val e = entryFor(t, status)
        e.status = status
        if (status == ListStatus.Completed && t.type.isEpisodic) {
            DemoCatalog.episodes(t, started).filter { !it.date.isAfter(clock()) }.forEach { e.watched += it.seasonOrOne to it.number }
        }
        e.changed = version
    }

    private fun history(
        t: DemoTitle,
        item: JsonObject,
        watched: Boolean,
    ) {
        val refs =
            item["seasons"].objects().flatMap { s -> s["episodes"].objects().mapNotNull { e -> e.int("number")?.let { (s.int("number") ?: 1) to it } } } +
                item["episodes"].objects().mapNotNull { e -> e.int("number")?.let { 1 to it } }
        if (!watched && refs.isEmpty()) {
            // Removing a whole title from the history takes it off every list.
            if (entries.remove(t.id) != null) removedVersion = version
            paused.removeAll { it.title.id == t.id }
            return
        }
        val now = clock()
        val e = entryFor(t, if (t.type == MediaType.Movie) ListStatus.Completed else ListStatus.Watching)
        val episodes = DemoCatalog.episodes(t, started)
        val touched = if (refs.isEmpty()) episodes.filter { !it.date.isAfter(now) }.map { it.seasonOrOne to it.number } else refs
        if (watched) {
            e.watched += touched
            e.lastWatchedAt = now
            paused.removeAll { p -> p.title.id == t.id && ((p.season ?: 1) to p.episode) in touched }
            playbackVersion = version
            when {
                t.type == MediaType.Movie -> e.status = ListStatus.Completed
                e.status == ListStatus.PlanToWatch -> e.status = ListStatus.Watching
            }
            if (t.type.isEpisodic && t.ended && episodes.all { (it.seasonOrOne to it.number) in e.watched }) e.status = ListStatus.Completed
        } else {
            e.watched -= touched.toSet()
        }
        e.changed = version
    }

    private fun rate(
        t: DemoTitle,
        rating: Int?,
    ) {
        val e = entryFor(t, if (t.type == MediaType.Movie) ListStatus.Completed else ListStatus.Watching)
        e.rating = rating
        e.rated = version
        e.changed = version
        ratedVersion = version
    }

    // ---- Catalogue ----------------------------------------------------------------------------

    private fun summary(t: DemoTitle): JsonObject =
        buildJsonObject {
            put("title", t.title)
            put("year", t.year)
            put("poster", t.poster)
            putJsonObject("ids") {
                put("simkl", t.id)
                put("slug", slug(t))
            }
            putJsonObject("ratings") { putJsonObject("simkl") { put("rating", t.communityRating) } }
            t.network?.let { put("network", it) }
            if (t.type == MediaType.Anime) put("anime_type", "tv")
            if (t.type.isEpisodic) put("total_episodes", t.total)
            put("overview", t.overview)
            put("type", if (t.type == MediaType.Movie) "movie" else if (t.type == MediaType.Anime) "anime" else "tv")
        }

    private fun catalogue(path: String): JsonElement {
        val parts = path.trim('/').split('/')
        val type = MediaType.entries.firstOrNull { it.catalogPath == parts.firstOrNull() } ?: return JsonArray(emptyList())
        return when {
            parts.size == 3 && parts[1] == "episodes" -> episodes(parts[2].toLongOrNull())
            parts.size == 2 && parts[1].toLongOrNull() != null -> details(parts[1].toLong()) ?: JsonNull
            // /tv/airing, /tv/premieres/new, /tv/best/year, ...
            else -> list(type, path)
        }
    }

    private fun details(id: Long): JsonElement? {
        val t = DemoCatalog.title(id) ?: return null
        val now = clock()
        val episodes = DemoCatalog.episodes(t, started)
        return buildJsonObject {
            put("title", t.title)
            put("year", t.year)
            put("poster", t.poster)
            put("fanart", t.fanart)
            put("overview", t.overview)
            putJsonArray("genres") { t.genres.forEach { add(JsonPrimitive(it)) } }
            put(
                "status",
                when {
                    t.type == MediaType.Movie -> if (DemoCatalog.releaseDate(t, now).isAfter(now)) "upcoming" else "released"
                    t.ended -> "ended"
                    else -> "airing"
                },
            )
            t.network?.let { put("network", it) }
            put("country", "gb")
            put("runtime", t.runtime)
            val first = if (t.type == MediaType.Movie) DemoCatalog.releaseDate(t, now) else episodes.firstOrNull()?.date
            first?.let { put(if (t.type == MediaType.Movie) "released" else "first_aired", it.toString().take(10)) }
            if (t.type.isEpisodic) {
                put("total_episodes", t.total)
                if (!t.ended) {
                    episodes.firstOrNull { it.date.isAfter(now) }?.let { next ->
                        putJsonObject("airs") {
                            put("day", next.date.atZone(java.time.ZoneOffset.UTC).dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH))
                            put("time", "19:00")
                            put("timezone", "UTC")
                        }
                    }
                }
            }
            if (t.type == MediaType.Anime) put("anime_type", "tv")
            putJsonObject("ratings") {
                putJsonObject("simkl") {
                    put("rating", t.communityRating)
                    put("votes", t.votes)
                }
            }
            put("rank", 40 + (t.id % 97).toInt() * 3)
            putJsonObject("ids") {
                put("simkl", t.id)
                put("slug", slug(t))
            }
            putJsonArray("users_recommendations") {
                DemoCatalog.titles.filter { it.type == t.type && it.id != t.id }.sortedBy { (it.id * 31 + t.id) % 17 }.take(6).forEach { add(summary(it)) }
            }
        }
    }

    private fun episodes(id: Long?): JsonElement {
        val t = id?.let { DemoCatalog.title(it) } ?: return JsonArray(emptyList())
        val now = clock()
        return buildJsonArray {
            DemoCatalog.episodes(t, started).forEach { e ->
                addJsonObject {
                    e.season?.let { put("season", it) }
                    put("episode", e.number)
                    put("title", e.title)
                    put("img", "${DemoCatalog.SCHEME}episode/${t.id}/${e.seasonOrOne}/${e.number}")
                    put("date", e.date.toString())
                    put("aired", !e.date.isAfter(now))
                    put("type", "episode")
                }
            }
        }
    }

    /** A Discover list: the titles of [type], in an order that differs per list. */
    private fun list(
        type: MediaType,
        key: String,
    ): JsonElement {
        val now = clock()
        val seed = key.hashCode().toLong()
        val titles =
            DemoCatalog.titles.filter { it.type == type }.let { all ->
                when {
                    "premieres/soon" in key -> all.filter { t -> DemoCatalog.episodes(t, started).firstOrNull()?.date?.isAfter(now) == true }
                    "airing" in key || "premieres/new" in key -> all.filter { !it.ended }
                    else -> all
                }
            }
        return JsonArray(titles.sortedBy { Math.floorMod(it.id * 7919 + seed, 101L) }.map(::summary))
    }

    private fun discover(url: String): JsonElement {
        val type =
            when {
                "/dvd/" in url || "/movies/" in url -> MediaType.Movie
                "/anime/" in url -> MediaType.Anime
                else -> MediaType.Tv
            }
        return list(type, url)
    }

    private fun calendar(url: String): JsonElement {
        val now = clock()
        val until = now.plus(Duration.ofDays(35))
        return buildJsonArray {
            when {
                url.endsWith("movie_release.json") ->
                    DemoCatalog.titles.filter { it.type == MediaType.Movie }.forEach { t ->
                        val date = DemoCatalog.releaseDate(t, now)
                        if (date.isAfter(now) && date.isBefore(until)) {
                            addJsonObject {
                                put("title", t.title)
                                put("poster", t.poster)
                                put("release_date", date.toString())
                                putJsonObject("ids") { put("simkl_id", t.id) }
                            }
                        }
                    }
                else -> {
                    val type = if (url.endsWith("anime.json")) MediaType.Anime else MediaType.Tv
                    DemoCatalog.titles.filter { it.type == type }.forEach { t ->
                        DemoCatalog.episodes(t, started).filter { it.date.isAfter(now.minus(Duration.ofDays(2))) && it.date.isBefore(until) }.forEach { e ->
                            addJsonObject {
                                put("title", t.title)
                                put("poster", t.poster)
                                put("date", e.date.toString())
                                putJsonObject("ids") { put("simkl_id", t.id) }
                                putJsonObject("episode") {
                                    e.season?.let { put("season", it) }
                                    put("episode", e.number)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun search(
        type: MediaType,
        query: String,
    ): JsonElement {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return JsonArray(emptyList())
        return JsonArray(DemoCatalog.titles.filter { it.type == type && q in it.title.lowercase() }.map(::summary))
    }

    private fun random(type: MediaType): JsonElement {
        val unwatched = DemoCatalog.titles.filter { it.type == type && entries[it.id]?.status != ListStatus.Completed }
        val pick = unwatched.getOrNull(Math.floorMod(clock().toEpochMilli() / 1000, unwatched.size.coerceAtLeast(1).toLong()).toInt())
        return pick?.let { buildJsonObject { put("id", it.id) } } ?: JsonNull
    }

    private fun communityRatings(): JsonElement =
        buildJsonArray {
            entries.keys.mapNotNull { DemoCatalog.title(it) }.forEach { t ->
                addJsonObject {
                    put("id", t.id)
                    put("type", if (t.type == MediaType.Movie) "movie" else if (t.type == MediaType.Anime) "anime" else "tv")
                    put(
                        "release_status",
                        when {
                            t.type == MediaType.Movie -> if (DemoCatalog.releaseDate(t, clock()).isAfter(clock())) "upcoming" else "released"
                            t.ended -> "ended"
                            else -> "airing"
                        },
                    )
                    putJsonObject("simkl") { put("rating", t.communityRating) }
                }
            }
        }

    // ---- User ---------------------------------------------------------------------------------

    private fun user(): JsonElement =
        buildJsonObject {
            putJsonObject("user") {
                put("name", profileName)
                put("joined_at", started.minus(Duration.ofDays(420)).toString())
            }
            putJsonObject("account") {
                put("id", DEMO_USER_ID)
                put("type", "free")
                put("timezone", "UTC")
            }
        }

    /** The profile's statistics, counted from the demo lists as they are now. */
    private fun stats(): JsonElement {
        val now = clock()
        val weekAgo = now.minus(Duration.ofDays(7))
        var total = 0L
        var lastWeek = 0L
        val blocks =
            listOf(MediaType.Tv to "tv", MediaType.Anime to "anime", MediaType.Movie to "movies").associate { (type, key) ->
                val mine = entries.mapNotNull { (id, e) -> DemoCatalog.title(id)?.takeIf { it.type == type }?.let { it to e } }
                var minutes = 0L
                val block =
                    buildJsonObject {
                        ListStatus.forType(type).forEach { status ->
                            val withStatus = mine.filter { it.second.status == status }
                            putJsonObject(status.api) {
                                put("count", withStatus.size)
                                if (type.isEpisodic) {
                                    put("watched_episodes_count", withStatus.sumOf { it.second.watched.size })
                                    put("left_to_watch_mins", withStatus.sumOf { (t, e) -> (t.aired - e.watched.size).coerceAtLeast(0).toLong() * t.runtime })
                                }
                            }
                        }
                    }
                mine.forEach { (t, e) ->
                    val watched = if (type == MediaType.Movie) (if (e.status == ListStatus.Completed) t.runtime.toLong() else 0L) else e.watched.size.toLong() * t.runtime
                    minutes += watched
                    if (e.lastWatchedAt?.isAfter(weekAgo) == true) lastWeek += t.runtime
                }
                total += minutes
                key to buildJsonObject {
                    put("total_mins", minutes)
                    block.forEach { (k, v) -> put(k, v) }
                }
            }
        return buildJsonObject {
            put("total_mins", total)
            putJsonObject("watched_last_week") { put("total_mins", lastWeek) }
            blocks.forEach { (k, v) -> put(k, v) }
        }
    }

    private fun slug(t: DemoTitle) = t.title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    private fun label(e: DemoEpisode) = if (e.season != null) "S%02dE%02d".format(java.util.Locale.ROOT, e.season, e.number) else "E%02d".format(java.util.Locale.ROOT, e.number)

    companion object {
        private val BASE_STAMP: Instant = Instant.parse("2026-01-01T00:00:00Z")
        const val DEMO_USER_ID = 1L
    }
}
