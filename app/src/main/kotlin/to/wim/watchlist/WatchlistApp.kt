package to.wim.watchlist

import android.app.Application
import android.content.Context
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
import to.wim.watchlist.ui.widget.UpNextWidgetProvider
import to.wim.watchlist.work.NewEpisodesWorker
import to.wim.watchlist.work.TrialReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.Job
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
    val api by lazy { SimklApi(http, settings) }
    val episodes by lazy { EpisodeStore(this, api) }
    val account by lazy { AccountRepository(this, api) }
    val library by lazy { LibraryRepository(this, api, settings, episodes, scope) { account.settingsChanged() } }
    val tmdb by lazy { TmdbApi(http) }
    val subscription by lazy { SubscriptionRepository(this, settings, scope, BuildConfig.REQUIRE_SUBSCRIPTION) }
    val catalog by lazy { CatalogRepository(api, tmdb, episodes) }

    /** Debug builds: Settings › Subscription › Preview shows the subscription screen with sample plans. */
    val paywallPreview = MutableStateFlow(false)

    /** The subscription screen, opened from a locked Plus feature; closes again with back or "Not now". */
    val showPaywall = MutableStateFlow(false)

    /** Debug builds: Settings › Subscription › Preview the free version locks the Plus features. */
    val previewLocked = MutableStateFlow(false)

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
        to.wim.watchlist.ui.components.Format.context = this
        runCatching { NewEpisodesWorker.createChannels(this) }
        runCatching { TrialReminderWorker.createChannel(this) }
        scope.launch { runCatching { NewEpisodesWorker.restore(this@WatchlistApp) } }
        redrawWidgetOnChanges()
    }

    /** Keeps the Up next widget in step with the lists, the settings and the subscription. */
    @OptIn(FlowPreview::class)
    private fun redrawWidgetOnChanges() {
        scope.launch {
            combine(library.state, settings.settings, subscription.state, previewLocked) { _, _, _, _ -> }
                .debounce(1_000)
                .collect { runCatching { UpNextWidgetProvider.update(this@WatchlistApp, force = false) } }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { http })) }
            .crossfade(true)
            .build()

    /**
     * Signs out and deletes what belongs to the account. Runs in the app's own scope, so leaving the
     * profile screen can't stop it halfway; the token goes last, because removing it swaps the app
     * to the sign-in screen.
     */
    fun logout(): Job =
        scope.launch {
            NewEpisodesWorker.cancel(this@WatchlistApp)
            library.clear()
            account.clear()
            episodes.clear()
            // Which episodes were announced belongs to the account that signed out.
            settings.setNotifiedEpisodes(emptySet())
            settings.setRememberedEpisodes(emptySet())
            runCatching { http.cache?.evictAll() }
            settings.setToken(null)
        }
}
