package to.wim.watchlist.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import to.wim.watchlist.R
import to.wim.watchlist.data.model.LibraryItem
import to.wim.watchlist.data.model.MediaType
import to.wim.watchlist.data.model.Plus
import to.wim.watchlist.data.model.PreviewGroup
import to.wim.watchlist.data.model.Ratings
import to.wim.watchlist.ui.LocalNavigator
import to.wim.watchlist.ui.Route
import to.wim.watchlist.ui.components.LocalPlus
import to.wim.watchlist.ui.components.Locked
import to.wim.watchlist.ui.components.PlusCard
import to.wim.watchlist.ui.components.PosterTile
import to.wim.watchlist.ui.components.label
import to.wim.watchlist.ui.components.rememberApp
import to.wim.watchlist.ui.library.LibraryGrid
import to.wim.watchlist.ui.library.SectionHeader
import to.wim.watchlist.ui.theme.EmptyState

/** "My ratings" from the profile: a row of rated titles per media type, highest rating first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingsScreen(types: List<MediaType>) {
    val app = rememberApp()
    val navigator = LocalNavigator.current
    val state by app.library.state.collectAsState()
    val rows = remember(state.items, types) { types.associateWith { Ratings.rated(state.items, it) }.filterValues { it.isNotEmpty() } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screens_ratings)) },
                navigationIcon = { IconButton(onClick = { navigator.pop() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.screens_back)) } },
            )
        },
    ) { padding ->
        if (rows.isEmpty()) {
            EmptyState(
                Icons.Rounded.StarOutline,
                stringResource(R.string.screens_ratings_empty),
                Modifier.padding(padding),
                body = stringResource(R.string.screens_ratings_empty_text),
            )
        } else {
            val plus = LocalPlus.current
            // Free version: the first rated title as it is, a card about Plus, the other titles
            // blurred. The type headers and their counts stay readable.
            val groups = remember(rows, plus) { if (plus) null else Plus.previewGroups(rows.toList()) }
            LazyColumn(
                contentPadding =
                    PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
                modifier = Modifier.fillMaxSize(),
            ) {
                (groups ?: rows.map { (type, items) -> PreviewGroup(type, items.size, items, emptyList()) }).forEachIndexed { i, group ->
                    val type = group.key
                    item(key = type.name) {
                        Column {
                            SectionHeader(type.label(), Modifier.padding(horizontal = 8.dp), group.total) {
                                if (plus) navigator.push(Route.RatedGrid(type)) else app.showPaywall.value = true
                            }
                            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(group.shown, key = { it.key }) { item ->
                                    RatedTile(item, Modifier.width(120.dp)) {
                                        navigator.openDetail(item.type, item.ids.simkl, item.title, item.poster)
                                    }
                                }
                                items(group.locked, key = { it.key }) { item -> Locked { RatedTile(item, Modifier.width(120.dp)) {} } }
                            }
                        }
                    }
                    if (groups != null && i == 0) {
                        item(key = "plus") {
                            PlusCard(
                                title = stringResource(R.string.plus_ratings_title),
                                text = stringResource(R.string.plus_ratings_text),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** All rated titles of one type in a grid, opened from a row's header on [RatingsScreen]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatedGridScreen(type: MediaType) {
    val app = rememberApp()
    val navigator = LocalNavigator.current
    val state by app.library.state.collectAsState()
    val items = remember(state.items, type) { Ratings.rated(state.items, type) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screens_ratings_of, type.label())) },
                navigationIcon = { IconButton(onClick = { navigator.pop() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.screens_back)) } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            if (items.isEmpty()) {
                EmptyState(Icons.Rounded.StarOutline, stringResource(R.string.screens_ratings_empty))
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

@Composable
private fun RatedTile(
    item: LibraryItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PosterTile(
        title = item.title,
        poster = item.poster,
        subtitle = listOfNotNull(item.userRating?.let { "★ $it" }, item.year?.toString()).joinToString(" · "),
        onClick = onClick,
        modifier = modifier,
    )
}
