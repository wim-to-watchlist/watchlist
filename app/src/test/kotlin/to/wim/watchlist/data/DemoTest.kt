package to.wim.watchlist.data

import to.wim.watchlist.data.api.Parsers
import to.wim.watchlist.data.api.asObject
import to.wim.watchlist.data.demo.DemoCatalog
import to.wim.watchlist.data.demo.DemoSimkl
import to.wim.watchlist.data.model.EpisodeRef
import to.wim.watchlist.data.model.Images
import to.wim.watchlist.data.model.ListStatus
import to.wim.watchlist.data.model.MediaType
import to.wim.watchlist.data.model.Onboarding
import to.wim.watchlist.data.model.Screen
import to.wim.watchlist.data.model.SignedOutStep
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DemoTest {
    private val now = Instant.parse("2026-11-21T12:00:00Z")
    private val demo = DemoSimkl("Demo") { now }

    private fun get(
        path: String,
        params: Map<String, String?> = emptyMap(),
    ) = demo.respond("GET", path, params, null)

    private fun post(
        path: String,
        body: JsonElement? = null,
        params: Map<String, String?> = emptyMap(),
    ) = demo.respond("POST", path, params, body)

    private fun library() = Parsers.library(get("/sync/all-items/", mapOf("extended" to "full")))

    private fun lantern() = DemoCatalog.titles.first { it.title == "The Lantern Keepers" }

    private fun body(
        id: Long,
        block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit = {},
    ) = buildJsonObject {
        putJsonArray("shows") {
            addJsonObject {
                putJsonObject("ids") { put("simkl", id) }
                block()
            }
        }
    }

    @Test
    fun everyListHasTitles() {
        val items = library()
        MediaType.entries.forEach { type ->
            assertTrue("no $type", items.any { it.type == type })
        }
        listOf(ListStatus.Watching, ListStatus.PlanToWatch, ListStatus.Completed, ListStatus.Dropped).forEach { status ->
            assertTrue("no $status", items.any { it.status == status })
        }
    }

    @Test
    fun idsCannotMatchRealTitles() {
        DemoCatalog.titles.forEach { assertTrue(it.id > 9_000_000_000L) }
        assertEquals(DemoCatalog.titles.size, DemoCatalog.titles.map { it.id }.toSet().size)
    }

    @Test
    fun upNextStartsAfterTheLastWatchedEpisode() {
        val item = library().first { it.ids.simkl == lantern().id }
        assertEquals(EpisodeRef(2, 4), item.nextToWatch?.ref)
        assertEquals(13, item.watchedEpisodes)
        assertEquals(3, item.notAiredEpisodes)
        assertTrue(item.isWatched(2, 3))
        assertFalse(item.isWatched(2, 4))
    }

    @Test
    fun episodesAirWeeklyAroundNow() {
        val episodes = Parsers.episodes(get("/tv/episodes/${lantern().id}", mapOf("extended" to "full")))
        assertEquals(20, episodes.size)
        assertEquals(17, episodes.count { it.aired })
        assertTrue(episodes.filter { !it.aired }.all { Instant.parse(it.date).isAfter(now) })
        assertTrue(episodes.all { it.image?.startsWith("demo:") == true })
    }

    @Test
    fun watchingAnEpisodeShowsInTheNextSync() {
        val before = Parsers.activityStamps(post("/sync/activities").asObject())
        post("/sync/history", body(lantern().id) { putJsonArray("seasons") { addJsonObject { put("number", 2); putJsonArray("episodes") { addJsonObject { put("number", 4) } } } } })
        val after = Parsers.activityStamps(post("/sync/activities").asObject())
        assertNotEquals(before.all, after.all)
        // Only what changed since the last sync comes back.
        val changed = Parsers.library(get("/sync/all-items/", mapOf("date_from" to before.all)))
        assertEquals(listOf(lantern().id), changed.map { it.ids.simkl })
        assertEquals(EpisodeRef(2, 5), changed.single().nextToWatch?.ref)
        // Nothing changed since then.
        assertEquals(JsonNull, get("/sync/all-items/", mapOf("date_from" to after.all)))
    }

    @Test
    fun ratingsAndListChanges() {
        val movie = DemoCatalog.titles.first { it.title == "The Quiet Meridian" }
        val before = Parsers.activityStamps(post("/sync/activities").asObject())
        post("/sync/ratings", buildJsonObject { putJsonArray("movies") { addJsonObject { put("rating", 7); putJsonObject("ids") { put("simkl", movie.id) } } } })
        val rated = Parsers.library(post("/sync/ratings/", params = mapOf("date_from" to before.rated)))
        assertEquals(7, rated.single { it.ids.simkl == movie.id }.userRating)

        post("/sync/history/remove", buildJsonObject { putJsonArray("movies") { addJsonObject { putJsonObject("ids") { put("simkl", movie.id) } } } })
        val after = Parsers.activityStamps(post("/sync/activities").asObject())
        assertNotEquals(before.removed, after.removed)
        assertFalse("${MediaType.Movie.name}:${movie.id}" in Parsers.libraryKeys(get("/sync/all-items/", mapOf("extended" to "simkl_ids_only"))))

        val catalogOnly = DemoCatalog.titles.first { it.list == null && it.type == MediaType.Tv }
        post("/sync/add-to-list", body(catalogOnly.id) { put("to", "plantowatch") })
        assertEquals(ListStatus.PlanToWatch, library().first { it.ids.simkl == catalogOnly.id }.status)
    }

    @Test
    fun pausedEpisodeGoesAwayOnceWatched() {
        val paused = Parsers.playback(get("/sync/playback")) { MediaType.Tv }.single()
        assertEquals("Harbor Lights", paused.title)
        post(
            "/sync/history",
            body(paused.simklId) {
                putJsonArray("seasons") {
                    addJsonObject {
                        put("number", paused.episode!!.season!!)
                        putJsonArray("episodes") { addJsonObject { put("number", paused.episode!!.episode) } }
                    }
                }
            },
        )
        assertTrue(Parsers.playback(get("/sync/playback")) { MediaType.Tv }.isEmpty())
    }

    @Test
    fun catalogueAnswersEveryScreen() {
        DemoCatalog.titles.forEach { t ->
            val details = Parsers.details(get("/${t.type.catalogPath}/${t.id}", mapOf("extended" to "full")).asObject()!!, t.type)
            assertEquals(t.title, details?.title)
            assertTrue(details!!.recommendations.isNotEmpty())
        }
        MediaType.entries.forEach { type ->
            CatalogRepository.sections(type).forEach { section ->
                val (path, params) = section.sources.first()
                val items = Parsers.summaries(demo.respond("GET", path, params, null), type)
                if ("premieres/soon" !in path) assertTrue("empty: $path", items.isNotEmpty())
                assertTrue(items.all { it.type == type })
            }
            assertNotNull(Parsers.randomId(post("/search/random/", params = mapOf("type" to type.searchPath))))
        }
        assertEquals(listOf("Harbor Lights"), Parsers.summaries(get("/search/tv", mapOf("q" to "harbor")), MediaType.Tv).map { it.title })
        assertTrue(Parsers.summaries(get("/search/movie", mapOf("q" to "harbor")), MediaType.Movie).isEmpty())
    }

    @Test
    fun calendarListsTheComingWeeks() {
        val tv = Parsers.calendar(get("https://data.simkl.in/calendar/tv.json"), MediaType.Tv)
        assertTrue(tv.any { it.simklId == lantern().id && it.date.isAfter(now) })
        val movies = Parsers.calendar(get("https://data.simkl.in/calendar/movie_release.json"), MediaType.Movie)
        assertTrue(movies.any { it.title == "Cobalt Summer" })
    }

    @Test
    fun profileAndStatsComeFromTheDemoLists() {
        val user = Parsers.user(post("/users/settings"))
        assertEquals("Demo", user?.name)
        assertNull(user?.avatar)
        val stats = Parsers.stats(post("/users/1/stats"))!!
        assertTrue(stats.totalMinutes > 0)
        val movies = stats.types.first { it.type == MediaType.Movie }
        assertEquals(library().count { it.type == MediaType.Movie && it.status == ListStatus.Completed }, movies.counts.first { it.status == ListStatus.Completed }.count)
    }

    @Test
    fun demoPicturesAreNotDownloaded() {
        val poster = DemoCatalog.titles.first().poster
        assertEquals(poster, Images.poster(poster))
        assertEquals(poster, Images.poster(poster, large = true))
        assertFalse(Images.poster(poster)!!.startsWith("http"))
    }

    @Test
    fun onboardingOrder() {
        // New: sign in with SIMKL (the subscription screen follows the first sync), or the demo.
        assertEquals(Screen.SignIn, Onboarding.screen(signedIn = false, SignedOutStep.SignIn))
        assertEquals(Screen.Demo, Onboarding.screen(signedIn = false, SignedOutStep.Demo))
        // Signed in: the app, which shows the subscription screen itself when there is none.
        SignedOutStep.entries.forEach { assertEquals(Screen.App, Onboarding.screen(signedIn = true, it)) }
    }
}
