package to.wim.watchlist.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import to.wim.watchlist.data.AppSettings
import to.wim.watchlist.data.model.Plus
import to.wim.watchlist.ui.components.LocalPlus
import to.wim.watchlist.ui.subscription.SubscriptionScreen
import to.wim.watchlist.ui.components.rememberApp
import to.wim.watchlist.ui.detail.DetailScreen
import to.wim.watchlist.ui.person.PersonScreen
import to.wim.watchlist.ui.login.LoginScreen
import to.wim.watchlist.ui.main.MainScreen
import to.wim.watchlist.ui.profile.ProfileScreen
import to.wim.watchlist.ui.search.SearchScreen
import to.wim.watchlist.ui.profile.RatedGridScreen
import to.wim.watchlist.ui.profile.RatingsScreen
import to.wim.watchlist.ui.settings.SettingsScreen
import to.wim.watchlist.ui.theme.Motion
import to.wim.watchlist.ui.theme.WatchlistTheme

@Composable
fun AppRoot(
    requested: Route.Detail?,
    onRequestHandled: () -> Unit,
) {
    val app = rememberApp()
    val settings by app.settings.settings.collectAsState(initial = null)
    val current = settings ?: return Box(Modifier.fillMaxSize()) // DataStore loads in a few ms.
    // Every setting but light or dark mode is part of Plus: without it the app uses the defaults.
    val subscription by app.subscription.state.collectAsState()
    val previewLocked by app.previewLocked.collectAsState()
    val shown = Plus.settings(Plus.unlocked(subscription.access, previewLocked), current)

    WatchlistTheme(mode = shown.theme, dynamicColor = shown.dynamicColor) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (current.accessToken == null) {
                LoginScreen()
            } else {
                Subscribed { LoggedIn(shown, requested, onRequestHandled) }
            }
        }
    }
}

/**
 * The app, free or with Watchlist Plus ([Plus]): [LocalPlus] tells the screens which. The
 * subscription screen opens on top when a locked feature asks for it. Google Play is asked again
 * each time the app comes to the foreground, so a purchase, renewal or cancellation elsewhere shows
 * without a restart.
 */
@Composable
private fun Subscribed(content: @Composable () -> Unit) {
    val app = rememberApp()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { app.subscription.refresh() }
    }
    val state by app.subscription.state.collectAsState()
    val preview by app.paywallPreview.collectAsState()
    if (preview) {
        BackHandler { app.paywallPreview.value = false }
        return SubscriptionScreen(preview = true)
    }
    val previewLocked by app.previewLocked.collectAsState()
    val plus = Plus.unlocked(state.access, previewLocked)
    val paywall by app.showPaywall.collectAsState()
    // Subscribed (or the trial started): the screen that asked for it opens unlocked.
    LaunchedEffect(plus) { if (plus && !previewLocked) app.showPaywall.value = false }
    // The paywall promises a reminder before the trial ends. On Android 13 and later that needs the
    // notification permission, so it is asked right after a trial starts in this session.
    val context = LocalContext.current
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var wasLocked by remember { mutableStateOf(!plus) }
    LaunchedEffect(plus) {
        if (plus && wasLocked && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            (app.settings.trialEndsAt() ?: 0L) > System.currentTimeMillis()
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        wasLocked = !plus
    }
    CompositionLocalProvider(LocalPlus provides plus) {
        Box(Modifier.fillMaxSize()) {
            content()
            if (paywall) {
                BackHandler { app.showPaywall.value = false }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Debug "Preview the free version": sample plans, as Play has none for test builds.
                    SubscriptionScreen(preview = previewLocked)
                }
            }
        }
    }
}

@Composable
private fun LoggedIn(
    settings: AppSettings,
    requested: Route.Detail?,
    onRequestHandled: () -> Unit,
) {
    val app = rememberApp()
    val navigator = rememberSaveable(saver = Navigator.Saver) { Navigator() }
    val stateHolder = rememberSaveableStateHolder()

    // Sync whenever the app comes to the foreground, so changes made elsewhere (paused episodes,
    // other apps and devices) show up without pulling to refresh; at most every 15 minutes.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { app.library.syncIfStale() }
    }
    LaunchedEffect(requested) {
        if (requested != null) {
            navigator.push(requested)
            onRequestHandled()
        }
    }
    BackHandler(enabled = navigator.stack.size > 1) { navigator.pop() }
    // Plus ended while a Plus-only page (a person, all ratings of a type) was open: leave it.
    val plus = LocalPlus.current
    LaunchedEffect(plus, navigator.current) {
        if (!plus && (navigator.current is Route.Person || navigator.current is Route.RatedGrid)) navigator.pop()
    }

    // Material 3 shared-axis X (see Motion.sharedAxisX).
    val axisOffset = with(LocalDensity.current) { 30.dp.roundToPx() }
    CompositionLocalProvider(LocalNavigator provides navigator) {
        AnimatedContent(
            targetState = navigator.current,
            transitionSpec = {
                Motion.sharedAxisX(forward = navigator.stack.contains(initialState), offsetPx = axisOffset)
            },
            contentKey = { it.key },
            label = "navigation",
        ) { route ->
            stateHolder.SaveableStateProvider(route.key) {
                when (route) {
                    Route.Main -> MainScreen(settings)
                    is Route.Detail -> DetailScreen(route, settings)
                    Route.Search -> SearchScreen(settings.typeList, Plus.tmdbKey(LocalPlus.current, settings.tmdbKey))
                    Route.Profile -> ProfileScreen(settings.types)
                    Route.Settings -> SettingsScreen(settings)
                    Route.Ratings -> RatingsScreen(settings.typeList)
                    is Route.RatedGrid -> RatedGridScreen(route.type)
                    is Route.Person -> PersonScreen(route, settings)
                }
            }
        }
    }
    // Forget the saved state of screens that were popped.
    val keys = navigator.stack.map { it.key }
    val knownKeys = remember { mutableSetOf<String>() }
    LaunchedEffect(keys) {
        knownKeys.filterNot { it in keys }.forEach { stateHolder.removeState(it) }
        knownKeys.clear()
        knownKeys.addAll(keys)
    }
}
