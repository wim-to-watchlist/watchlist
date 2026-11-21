package to.wim.watchlist

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import to.wim.watchlist.data.AccountRepository
import to.wim.watchlist.data.CatalogRepository
import to.wim.watchlist.data.EpisodeStore
import to.wim.watchlist.data.LibraryRepository
import to.wim.watchlist.data.SettingsStore
import to.wim.watchlist.data.SubscriptionRepository
import to.wim.watchlist.data.api.SimklApi
import to.wim.watchlist.data.api.TmdbApi
import to.wim.watchlist.data.demo.DemoSimkl
import to.wim.watchlist.data.model.SignedOutStep
import to.wim.watchlist.ui.demo.DemoArtFetcher
import to.wim.watchlist.work.NewEpisodesWorker
import to.wim.watchlist.work.TrialReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class WatchlistApp :
    Application(),
    SingletonImageLoader.Factory,
    Configuration.Provider {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(Cache(File(cacheDir, "http"), 50L * 1024 * 1024))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val settings by lazy { SettingsStore(this) }
    val tmdb by lazy { TmdbApi(http) }
    val subscription by lazy { SubscriptionRepository(this, settings, scope, BuildConfig.REQUIRE_SUBSCRIPTION) }

    /**
     * The SIMKL side of the app: the user's account, or the demo's pretend one while the demo runs.
     * Screens read these through the getters below, so the same screens show either.
     */
    private inner class Graph(
        context: Context,
        graphScope: CoroutineScope,
        demo: DemoSimkl?,
    ) {
        val api = SimklApi(http, settings, demo)
        val episodes = EpisodeStore(context, api)
        val account = AccountRepository(context, api)
        val library = LibraryRepository(context, api, settings, episodes, graphScope) { account.settingsChanged() }
        val catalog = CatalogRepository(api, tmdb, episodes)
    }

    private val live by lazy { Graph(this, scope, demo = null) }
    @Volatile private var demoGraph: Graph? = null
    private var demoScope: CoroutineScope? = null

    val api: SimklApi get() = (demoGraph ?: live).api
    val episodes: EpisodeStore get() = (demoGraph ?: live).episodes
    val account: AccountRepository get() = (demoGraph ?: live).account
    val library: LibraryRepository get() = (demoGraph ?: live).library
    val catalog: CatalogRepository get() = (demoGraph ?: live).catalog

    /** Where someone who isn't signed in is (see [Onboarding]); kept in memory only. */
    val signedOutStep = MutableStateFlow(SignedOutStep.Welcome)

    /** Its files go to a folder of their own in the cache, removed when the demo ends. */
    private val demoDir get() = File(cacheDir, "demo")

    /**
     * Starts the demo: a fresh pretend SIMKL account with made-up titles. Only for someone who
     * isn't signed in, so the user's own lists are never touched.
     */
    fun startDemo() {
        endDemoGraph()
        demoDir.deleteRecursively()
        demoDir.mkdirs()
        val files =
            object : ContextWrapper(this) {
                override fun getFilesDir(): File = demoDir
            }
        val demoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { this.demoScope = it }
        demoGraph = Graph(files, demoScope, DemoSimkl(getString(R.string.demo_profile_name)))
        signedOutStep.value = SignedOutStep.Demo
    }

    /** Ends the demo and forgets everything done in it; [next] is where the user goes. */
    fun endDemo(next: SignedOutStep = SignedOutStep.Welcome) {
        endDemoGraph()
        signedOutStep.value = next
    }

    private fun endDemoGraph() {
        demoScope?.cancel()
        demoScope = null
        demoGraph = null
        demoDir.deleteRecursively()
    }

    val inDemo: Boolean get() = demoGraph != null

    /** Debug builds: Settings › Subscription › Preview shows the subscription screen with sample plans. */
    val paywallPreview = MutableStateFlow(false)

    /** "Not now" on the TMDB token card: hidden until the app restarts. */
    val tmdbBannerHiddenForNow = MutableStateFlow(false)

    /** The latest sign-in redirect from simkl.com, until the login screen has handled it. */
    val authRedirect = MutableStateFlow<Uri?>(null)

    // WorkManager starts on first use: at app start only while notifications are on.
    override val workManagerConfiguration: Configuration get() = Configuration.Builder().build()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Before the libraries' start-up providers run, so their crashes are reported too.
        CrashReporter.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        // A demo that was running when Android stopped the app is not continued.
        runCatching { demoDir.deleteRecursively() }
        to.wim.watchlist.ui.components.Format.context = this
        runCatching { NewEpisodesWorker.createChannels(this) }
        runCatching { TrialReminderWorker.createChannel(this) }
        scope.launch { runCatching { NewEpisodesWorker.restore(this@WatchlistApp) } }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(DemoArtFetcher.Factory())
                add(OkHttpNetworkFetcherFactory(callFactory = { http }))
            }
            .crossfade(true)
            .build()

    suspend fun logout() {
        // "Sign out" in the demo leaves the demo.
        if (inDemo) return endDemo()
        settings.setToken(null)
        library.clear()
        account.clear()
        episodes.clear()
        NewEpisodesWorker.cancel(this)
        // Which episodes were announced belongs to the account that signed out.
        settings.setNotifiedEpisodes(emptySet())
        settings.setRememberedEpisodes(emptySet())
    }
}
