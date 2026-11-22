package to.wim.watchlist.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import to.wim.watchlist.R
import to.wim.watchlist.WatchlistApp
import to.wim.watchlist.data.model.Access
import to.wim.watchlist.data.model.UpNextWidget
import to.wim.watchlist.data.model.WidgetContent
import to.wim.watchlist.ui.MainActivity

/**
 * The Up next home-screen widget (Watchlist Plus). Drawn with RemoteViews from the cached library
 * ([UpNextWidget] decides what it shows); it never calls SIMKL. The app redraws it when the lists,
 * the settings or the subscription change ([WatchlistApp]); Android asks every hour.
 */
class UpNextWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
    ) {
        val app = context.applicationContext as WatchlistApp
        val pending = goAsync()
        app.scope.launch {
            try {
                runCatching { update(app) }
            } finally {
                pending.finish()
            }
            // Not known yet in a fresh process: ask Google Play (not SIMKL), then draw again.
            if (app.subscription.knownAccess() == Access.Checking) {
                runCatching { app.subscription.refresh() }
                runCatching { update(app) }
            }
        }
    }

    companion object {
        private val ROWS = listOf(R.id.row0, R.id.row1, R.id.row2, R.id.row3, R.id.row4)
        private val TITLES = listOf(R.id.row0_title, R.id.row1_title, R.id.row2_title, R.id.row3_title, R.id.row4_title)
        private val EPISODES = listOf(R.id.row0_episode, R.id.row1_episode, R.id.row2_episode, R.id.row3_episode, R.id.row4_episode)

        private val lock = Mutex()

        @Volatile private var drawn: WidgetContent? = null

        /**
         * Redraws every Up next widget on the home screen; does nothing when there is none. Without
         * [force] it skips drawing when the widget would look the same (the app calls it often).
         */
        suspend fun update(
            app: WatchlistApp,
            force: Boolean = true,
        ) = lock.withLock {
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, UpNextWidgetProvider::class.java))
            if (ids.isEmpty()) return@withLock
            app.library.load()
            val settings = app.settings.settings.first()
            val plus = UpNextWidget.unlocked(app.subscription.knownAccess(), app.previewLocked.value)
            val content = UpNextWidget.content(plus, app.library.state.value, settings.types, settings.continueFromLatest)
            if (!force && content == drawn) return@withLock
            manager.updateAppWidget(ids, views(app, content))
            drawn = content
        }

        private fun views(
            context: Context,
            content: WidgetContent,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_up_next)
            val openApp = activity(context, 0, Intent(context, MainActivity::class.java))
            views.setOnClickPendingIntent(R.id.header, openApp)
            ROWS.forEach { views.setViewVisibility(it, View.GONE) }
            views.setViewVisibility(R.id.more, View.GONE)
            when (content) {
                WidgetContent.Locked -> {
                    views.setViewVisibility(R.id.message, View.VISIBLE)
                    views.setTextViewText(R.id.message, context.getString(R.string.widget_locked))
                    views.setOnClickPendingIntent(R.id.message, activity(context, 1, MainActivity.paywallIntent(context)))
                }
                WidgetContent.Empty -> {
                    views.setViewVisibility(R.id.message, View.VISIBLE)
                    views.setTextViewText(R.id.message, context.getString(R.string.widget_empty))
                    views.setOnClickPendingIntent(R.id.message, openApp)
                }
                is WidgetContent.Shows -> {
                    views.setViewVisibility(R.id.message, View.GONE)
                    content.rows.forEachIndexed { i, row ->
                        views.setViewVisibility(ROWS[i], View.VISIBLE)
                        views.setTextViewText(TITLES[i], row.title)
                        views.setTextViewText(EPISODES[i], row.episode.orEmpty())
                        views.setViewVisibility(EPISODES[i], if (row.episode == null) View.GONE else View.VISIBLE)
                        views.setOnClickPendingIntent(ROWS[i], activity(context, 10 + i, MainActivity.detailIntent(context, row.type, row.simklId)))
                    }
                    if (content.more > 0) {
                        views.setViewVisibility(R.id.more, View.VISIBLE)
                        views.setTextViewText(R.id.more, context.resources.getQuantityString(R.plurals.widget_more, content.more, content.more))
                    }
                }
            }
            return views
        }

        private fun activity(
            context: Context,
            requestCode: Int,
            intent: Intent,
        ): PendingIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
