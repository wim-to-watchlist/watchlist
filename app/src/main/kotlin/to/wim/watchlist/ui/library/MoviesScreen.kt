package to.wim.watchlist.ui.library

import to.wim.watchlist.ui.components.label
import androidx.compose.ui.res.stringResource
import to.wim.watchlist.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import to.wim.watchlist.data.NavTab
import to.wim.watchlist.data.model.MediaType
import to.wim.watchlist.data.model.MovieShelf
import to.wim.watchlist.data.model.Shelves
import to.wim.watchlist.ui.LocalNavigator
import to.wim.watchlist.ui.theme.EmptyState
import to.wim.watchlist.ui.components.rememberApp
import to.wim.watchlist.ui.main.MainTopBar
import kotlinx.coroutines.launch
import to.wim.watchlist.ui.components.PosterGridSkeleton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen() {
    val app = rememberApp()
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val state by app.library.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    // Sort and filter choices are kept per shelf (by its position in MovieShelf), so they stay
    // with their tab when Continue watching comes and goes.
    val options = rememberTabOptions(MovieShelf.entries.map { TabSpec.of(it) })
    val paused = remember(state) { Shelves.pausedMovies(state.items, state.playback) }
    val shelves = MovieShelf.entries.filter { it != MovieShelf.ContinueWatching || paused.isNotEmpty() }
    val pagerState = rememberPagerState { shelves.size }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val byShelf =
        remember(state, options.version, paused) {
            val byStatus = state.items.filter { it.type == MediaType.Movie }.groupBy { it.status }
            MovieShelf.entries.associateWith { shelf ->
                val tab = shelf.ordinal
                shelf.status?.let { status -> options.sort(tab).sort(options.filter(tab).apply(byStatus[status].orEmpty())) } ?: paused
            }
        }

    LaunchedEffect(state.error) { state.error?.let { snackbar.showSnackbar(it) } }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = bottomPadding())) },
        topBar = {
            Column {
                MainTopBar(NavTab.Movies.label(), scrollBehavior) {
                    val tab = shelves.getOrElse(pagerState.currentPage) { shelves.first() }.ordinal
                    val spec = options.spec(tab)
                    if (spec.hasFilters) FilterButton(options.filter(tab), spec) { options.setFilter(tab, it) }
                    if (spec.sorts.size > 1) SortButton(options.sort(tab), spec.sorts) { options.setSort(tab, it) }
                }
                ShelfTabRow(
                    selected = pagerState.currentPage,
                    tabs = shelves.map { shelf -> shelf.label() to (byShelf[shelf]?.size ?: 0) },
                    onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                )
            }
        },
    ) { padding ->
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) { page ->
            val shelf = shelves.getOrElse(page) { shelves.first() }
            val items = byShelf[shelf].orEmpty()
            PullToRefreshBox(
                isRefreshing = state.syncing,
                onRefresh = { scope.launch { app.library.sync() } },
                modifier = Modifier.fillMaxSize(),
            ) {
                val filter = options.filter(shelf.ordinal)
                if (items.isEmpty() && filter.active) {
                    EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.lib_no_matches)) {
                        Button(onClick = { options.setFilter(shelf.ordinal, LibraryFilter()) }) { Text(stringResource(R.string.lib_clear_filters)) }
                    }
                } else if (state.firstSync) {
                    PosterGridSkeleton(bottomPadding())
                } else if (items.isEmpty() && state.loaded && !state.syncing) {
                    val (icon, title) =
                        when (shelf) {
                            // Only shown while there are paused movies; this is for the moment the last one goes.
                            MovieShelf.ContinueWatching -> Icons.Rounded.PlayCircle to R.string.lib_empty_other_text
                            MovieShelf.PlanToWatch -> Icons.Rounded.Bookmark to R.string.lib_empty_plan_movies
                            MovieShelf.Completed -> Icons.Rounded.CheckCircle to R.string.lib_empty_watched_movies
                            MovieShelf.Dropped -> Icons.Rounded.Block to R.string.lib_empty_dropped_movies
                        }
                    EmptyState(
                        icon,
                        stringResource(title),
                        body = if (shelf == MovieShelf.PlanToWatch) stringResource(R.string.lib_empty_plan_movies_text) else null,
                    )
                } else {
                    LibraryGrid(
                        items = items,
                        state = state,
                        onOpen = { navigator.openDetail(it.type, it.ids.simkl, it.title, it.poster) },
                        showProgress = false,
                        showAiring = false,
                    )
                }
            }
        }
    }
}
