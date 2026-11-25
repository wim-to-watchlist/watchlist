package to.wim.watchlist.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import to.wim.watchlist.BuildConfig
import to.wim.watchlist.R
import to.wim.watchlist.data.AppSettings
import to.wim.watchlist.data.NavConfig
import to.wim.watchlist.data.SubscriptionRepository
import to.wim.watchlist.data.ThemeMode
import to.wim.watchlist.data.api.TmdbKey
import to.wim.watchlist.data.model.MediaType
import to.wim.watchlist.data.model.NotifyChannel
import to.wim.watchlist.data.toggled
import to.wim.watchlist.ui.LocalNavigator
import to.wim.watchlist.ui.components.Format
import to.wim.watchlist.ui.components.LocalPlus
import to.wim.watchlist.ui.components.label
import to.wim.watchlist.ui.components.openUrl
import to.wim.watchlist.ui.components.rememberApp
import to.wim.watchlist.ui.main.icon
import to.wim.watchlist.ui.theme.AppearanceSettings
import to.wim.watchlist.ui.theme.DragHandle
import to.wim.watchlist.ui.theme.LockedSwitch
import to.wim.watchlist.ui.theme.ReorderableColumn
import to.wim.watchlist.ui.theme.SettingItem
import to.wim.watchlist.ui.theme.SettingsBlock
import to.wim.watchlist.ui.theme.SettingsFooter
import to.wim.watchlist.ui.theme.SettingsScaffold
import to.wim.watchlist.ui.theme.SettingsSection
import to.wim.watchlist.ui.theme.SwitchSetting
import to.wim.watchlist.work.NewEpisodesWorker
import java.time.Instant
import kotlinx.coroutines.launch


@Composable
fun SettingsScreen(settings: AppSettings) {
    val app = rememberApp()
    val plus = LocalPlus.current
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun enableNotifications() {
        scope.launch {
            app.settings.setNotifications(true)
            NewEpisodesWorker.enable(context)
        }
    }
    // Android's own notification settings, read again each time Settings comes back into view.
    var android by remember { mutableStateOf(AndroidNotifications.read(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { android = AndroidNotifications.read(context) }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            android = AndroidNotifications.read(context)
            if (granted) {
                enableNotifications()
            } else if (Build.VERSION.SDK_INT >= 33 && !permissionAskable(context)) {
                // Android no longer shows the question: the switch is in its settings now.
                AndroidNotifications.openAppSettings(context)
            }
        }

    // Every setting but light or dark mode is part of Plus: in the free version each one stays
    // readable, its switch greyed out behind a lock, and a tap opens the plans.
    val locked: (() -> Unit)? = if (plus) null else ({ app.showPaywall.value = true })
    val lockedLabel = stringResource(R.string.plus_locked)

    SettingsScaffold(
        title = stringResource(R.string.screens_settings),
        backLabel = stringResource(R.string.screens_back),
        onBack = { navigator.pop() },
    ) {
        item(key = "types") {
            SettingsSection(title = stringResource(R.string.screens_settings_types)) {
                SettingsBlock {
                    Text(
                        stringResource(R.string.screens_settings_types_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MediaType.entries.forEach { type ->
                    val on = type in settings.types
                    SwitchSetting(
                        title = type.label(),
                        // Says why the last switch that is on can't be turned off.
                        summary = if (on && settings.types.size == 1) stringResource(R.string.screens_settings_last_one_on) else null,
                        checked = on,
                        // At least one type stays on.
                        enabled = !on || settings.types.size > 1,
                        onCheckedChange = { scope.launch { app.settings.setTypes(settings.types.toggled(type, it)) } },
                        onLocked = locked,
                        lockedLabel = lockedLabel,
                    )
                }
            }
        }

        item(key = "library") {
            SettingsSection(title = stringResource(R.string.screens_settings_library)) {
                SwitchSetting(
                    title = stringResource(R.string.screens_settings_hide_specials),
                    summary = null,
                    checked = settings.hideSpecials,
                    onCheckedChange = { scope.launch { app.settings.setHideSpecials(it) } },
                    onLocked = locked,
                    lockedLabel = lockedLabel,
                )
                SwitchSetting(
                    title = stringResource(R.string.screens_settings_continue_latest),
                    summary = stringResource(R.string.screens_settings_continue_latest_sub),
                    checked = settings.continueFromLatest,
                    onCheckedChange = { on ->
                        scope.launch {
                            app.settings.setContinueFromLatest(on)
                            if (on) app.library.refreshLatestNext()
                        }
                    },
                    onLocked = locked,
                    lockedLabel = lockedLabel,
                )
            }
        }

        item(key = "notifications") {
            // New-episode notifications are part of Plus: in the free version the switch opens the subscription screen.
            val on = plus && settings.notifications && android.appEnabled
            SettingsSection(title = stringResource(R.string.screens_settings_notifications_section)) {
                SwitchSetting(
                    title = stringResource(R.string.screens_settings_notifications),
                    summary = stringResource(R.string.screens_settings_notifications_sub),
                    // Off whenever Android blocks the app's notifications, so turning it on asks again.
                    checked = on,
                    onCheckedChange = { turnOn ->
                        when {
                            !plus -> app.showPaywall.value = true
                            !turnOn ->
                                scope.launch {
                                    app.settings.setNotifications(false)
                                    NewEpisodesWorker.cancel(context)
                                }
                            Build.VERSION.SDK_INT >= 33 && !permissionGranted(context) -> permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            // Allowed, but switched off in Android's settings: only Android can turn it back on.
                            !android.appEnabled -> {
                                enableNotifications()
                                AndroidNotifications.openAppSettings(context)
                            }
                            else -> enableNotifications()
                        }
                    },
                    onLocked = locked,
                    lockedLabel = lockedLabel,
                )
                if (on) {
                    // One row per type that is on in Content, showing Android's own switch for its channel.
                    NotifyChannel.entries.filter { it.type in settings.types }.forEach { channel ->
                        SettingItem(
                            title = stringResource(channel.titleRes()),
                            summary =
                                stringResource(
                                    if (android.channelOn(channel)) R.string.screens_settings_channel_on else R.string.screens_settings_channel_off,
                                ),
                            onClick = { AndroidNotifications.openChannelSettings(context, channel) },
                            trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) },
                        )
                    }
                }
                SettingItem(
                    title = stringResource(R.string.screens_settings_android_notifications),
                    summary = stringResource(R.string.screens_settings_android_notifications_sub),
                    onClick = { AndroidNotifications.openAppSettings(context) },
                    trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) },
                )
            }
        }

        // TMDB (cast, people) is part of Plus, so the free version doesn't mention it at all.
        if (plus) item(key = "tmdb") {
            var editing by rememberSaveable { mutableStateOf(false) }
            SettingsSection(title = stringResource(R.string.screens_settings_tmdb)) {
                SettingItem(
                    title = stringResource(R.string.screens_settings_tmdb_key),
                    summary =
                        stringResource(
                            when {
                                TmdbKey.parse(settings.tmdbKey) != null -> R.string.screens_settings_tmdb_key_on
                                // The API Key that version 1.6.0 accepted: it has to be replaced.
                                settings.tmdbKey != null -> R.string.screens_settings_tmdb_key_old
                                else -> R.string.screens_settings_tmdb_key_off
                            },
                        ),
                    onClick = { editing = true },
                )
            }
            if (editing) TmdbKeyDialog(current = settings.tmdbKey, onDismiss = { editing = false })
        }

        item(key = "bottom-bar") {
            // Tabs of media types that are off aren't listed; they come back when the type does.
            val shown = settings.nav.available(settings.types)
            val shownOn = shown.count { it in settings.nav.enabled }
            SettingsSection(title = stringResource(R.string.screens_settings_bottom_bar)) {
                // Reordering and hiding tabs is part of Plus.
                if (!plus) {
                    // The default tabs, readable, each switch locked; they can't be dragged.
                    shown.forEach { tab ->
                        ListItem(
                            headlineContent = { Text(tab.label()) },
                            leadingContent = { Icon(tab.icon(true), null) },
                            trailingContent = { LockedSwitch(tab in settings.nav.enabled, lockedLabel) },
                            modifier = Modifier.clickable(role = Role.Button) { app.showPaywall.value = true },
                        )
                    }
                    return@SettingsSection
                }
                ReorderableColumn(
                    items = shown,
                    key = { it },
                    onMove = { from, to -> scope.launch { app.settings.setNav(settings.nav.moved(shown[from], to, shown)) } },
                ) { tab ->
                    val enabled = tab in settings.nav.enabled
                    val tabLabel = tab.label()
                    ListItem(
                        headlineContent = { Text(tab.label()) },
                        supportingContent =
                            if (enabled && shownOn == 1) {
                                { Text(stringResource(R.string.screens_settings_last_one_on)) }
                            } else {
                                null
                            },
                        leadingContent = { Icon(tab.icon(enabled), null) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = enabled,
                                    // Screen readers say which tab the switch turns on or off.
                                    modifier = Modifier.semantics { contentDescription = tabLabel },
                                    // At least one destination stays on.
                                    enabled = !enabled || shownOn > 1,
                                    onCheckedChange = { on ->
                                        val set = if (on) settings.nav.enabled + tab else settings.nav.enabled - tab
                                        scope.launch { app.settings.setNav(NavConfig(settings.nav.order, set)) }
                                    },
                                )
                                DragHandle(handleModifier)
                            }
                        },
                    )
                }
            }
        }

        item(key = "appearance") {
            SettingsSection(title = stringResource(R.string.screens_settings_appearance)) {
                AppearanceSettings(
                    themeLabel = stringResource(R.string.screens_settings_theme),
                    themeOptions = ThemeMode.entries.map { it.label() },
                    selectedTheme = settings.theme.ordinal,
                    onThemeSelect = { index -> scope.launch { app.settings.setTheme(ThemeMode.entries[index]) } },
                    dynamicColorTitle = stringResource(R.string.screens_settings_dynamic_color),
                    // Colours from the wallpaper are part of Plus; light and dark stay free.
                    dynamicColorSummary = stringResource(R.string.screens_settings_dynamic_color_sub),
                    dynamicColor = plus && settings.dynamicColor,
                    onDynamicColorChange = { on -> if (plus) scope.launch { app.settings.setDynamicColor(on) } else app.showPaywall.value = true },
                    dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    onDynamicColorLocked = locked,
                    lockedLabel = lockedLabel,
                )
            }
        }

        item(key = "credits") {
            // Where the data comes from. With Plus, TMDB's attribution (its approved logo, unchanged,
            // and its notice) is in a credits section, whether or not a token is set. The free
            // version uses no TMDB data, so it doesn't show it.
            SettingsSection(title = stringResource(R.string.screens_settings_credits)) {
                CreditItem(
                    logo = painterResource(R.drawable.simkl_logo),
                    logoDescription = stringResource(R.string.screens_simkl_logo),
                    title = stringResource(R.string.screens_credit_simkl_title),
                    text = stringResource(R.string.screens_credit_simkl_text),
                    onClick = { context.openUrl("https://simkl.com/") },
                )
                if (plus) CreditItem(
                    logo = painterResource(R.drawable.tmdb_logo_square),
                    logoDescription = stringResource(R.string.screens_tmdb_logo),
                    title = stringResource(R.string.screens_credit_tmdb_title),
                    text = stringResource(R.string.screens_credit_tmdb_text) + "\n" + stringResource(R.string.screens_tmdb_notice),
                    onClick = { context.openUrl("https://www.themoviedb.org/") },
                )
            }
        }

        item(key = "privacy") {
            // The same policy the Play Store links to, so nobody has to leave the app to find it.
            SettingsSection(title = stringResource(R.string.screens_settings_privacy)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.screens_privacy_policy)) },
                    supportingContent = { Text(stringResource(R.string.screens_privacy_policy_text)) },
                    leadingContent = { Icon(Icons.Rounded.Policy, null) },
                    modifier = Modifier.clickable(role = Role.Button) { context.openUrl(PRIVACY_POLICY_URL) },
                )
            }
        }

        if (BuildConfig.REQUIRE_SUBSCRIPTION) {
            item(key = "subscription") {
                SettingsSection(title = stringResource(R.string.sub_settings_section)) {
                    if (!plus) {
                        SettingItem(
                            title = stringResource(R.string.plus_get),
                            summary = stringResource(R.string.plus_get_summary),
                            onClick = { app.showPaywall.value = true },
                        )
                    }
                    val trialEnd by produceState<Long?>(null) { value = app.settings.trialEndsAt() }
                    SettingItem(
                        title = stringResource(R.string.sub_manage),
                        summary =
                            trialEnd?.takeIf { it > System.currentTimeMillis() }?.let {
                                stringResource(R.string.sub_settings_trial, Format.date(Instant.ofEpochMilli(it)))
                            } ?: stringResource(R.string.sub_settings_summary),
                        onClick = { context.openUrl(SubscriptionRepository.manageUrl(context.packageName)) },
                        trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) },
                    )
                }
            }
        }

        if (BuildConfig.DEBUG) {
            item(key = "subscription_preview") {
                SettingsSection(title = stringResource(R.string.sub_settings_section)) {
                    // As in release builds, so the free-version preview shows where to get Plus.
                    if (!plus) {
                        SettingItem(
                            title = stringResource(R.string.plus_get),
                            summary = stringResource(R.string.plus_get_summary),
                            onClick = { app.showPaywall.value = true },
                        )
                    }
                    SettingItem(
                        title = stringResource(R.string.sub_preview),
                        summary = stringResource(R.string.sub_preview_summary),
                        onClick = { app.paywallPreview.value = true },
                    )
                    val previewLocked by app.previewLocked.collectAsState()
                    SwitchSetting(
                        title = stringResource(R.string.plus_preview_locked),
                        summary = stringResource(R.string.plus_preview_locked_summary),
                        checked = previewLocked,
                        onCheckedChange = { app.previewLocked.value = it },
                    )
                }
            }
        }

        item(key = "footer") {
            SettingsFooter(stringResource(R.string.screens_settings_version, BuildConfig.VERSION_NAME))
        }
    }
}

/** A source of the app's data in Settings › Credits: its logo (unchanged, never tinted), name and what it provides. */
@Composable
private fun CreditItem(
    logo: Painter,
    logoDescription: String,
    title: String,
    text: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(text) },
        leadingContent = {
            // On a white tile, so the black SIMKL logo stays visible in the dark theme.
            Image(
                logo,
                logoDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White),
            )
        },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) },
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

private fun NotifyChannel.titleRes(): Int =
    when (this) {
        NotifyChannel.Tv -> R.string.screens_settings_channel_tv
        NotifyChannel.Anime -> R.string.screens_settings_channel_anime
        NotifyChannel.Movies -> R.string.screens_settings_channel_movies
    }

@RequiresApi(33)
private fun permissionGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/** False once the user said no twice: Android then refuses silently, so the app opens its settings. */
@RequiresApi(33)
private fun permissionAskable(context: Context): Boolean =
    (context as? Activity)?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ?: true

/** The public privacy policy (a copy of PRIVACY.md on the project's website). */
private const val PRIVACY_POLICY_URL = "https://wim-to-watchlist.github.io/privacy/"
