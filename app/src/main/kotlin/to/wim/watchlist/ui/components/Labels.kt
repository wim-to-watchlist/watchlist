package to.wim.watchlist.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import to.wim.watchlist.R
import to.wim.watchlist.data.NavTab
import to.wim.watchlist.data.ThemeMode
import to.wim.watchlist.data.model.Genre
import to.wim.watchlist.data.model.ListStatus
import to.wim.watchlist.data.model.MediaType
import to.wim.watchlist.data.model.MovieShelf
import to.wim.watchlist.data.model.Shelf
import to.wim.watchlist.data.model.ShowStatus

/** Localised names of the data layer's enums (the data layer itself has no Android resources). */
val NavTab.labelRes: Int
    @StringRes get() =
        when (this) {
            NavTab.Tv -> R.string.tab_tv
            NavTab.Anime -> R.string.tab_anime
            NavTab.Movies -> R.string.tab_movies
            NavTab.Discover -> R.string.tab_discover
            NavTab.Calendar -> R.string.tab_calendar
        }

@Composable
fun NavTab.label(): String = stringResource(labelRes)

@Composable
fun Shelf.label(): String =
    stringResource(
        when (this) {
            Shelf.UpNext -> R.string.shelf_up_next
            Shelf.Watching -> R.string.status_watching
            Shelf.PlanToWatch -> R.string.status_plan_to_watch
            // Completed shows: SIMKL's own name for the list ("Watched" is used for episodes).
            Shelf.Watched -> R.string.status_completed
            Shelf.OnHold -> R.string.status_on_hold
            Shelf.Dropped -> R.string.status_dropped
        },
    )

@Composable
fun MovieShelf.label(): String =
    stringResource(
        when (this) {
            MovieShelf.ContinueWatching -> R.string.lib_continue_watching
            MovieShelf.PlanToWatch -> R.string.status_plan_to_watch
            MovieShelf.Completed -> R.string.status_watched
            MovieShelf.Dropped -> R.string.status_dropped
        },
    )

@Composable
fun MediaType.label(): String =
    stringResource(
        when (this) {
            MediaType.Tv -> R.string.tab_tv
            MediaType.Anime -> R.string.tab_anime
            MediaType.Movie -> R.string.tab_movies
        },
    )

/** The type of one title, for a badge on it (singular, unlike the tab names). */
@Composable
fun MediaType.oneLabel(): String =
    stringResource(
        when (this) {
            MediaType.Tv -> R.string.type_one_tv
            MediaType.Anime -> R.string.tab_anime
            MediaType.Movie -> R.string.type_one_movie
        },
    )

@Composable
fun ListStatus.label(type: MediaType): String =
    stringResource(
        when (this) {
            ListStatus.Watching -> R.string.status_watching
            ListStatus.PlanToWatch -> R.string.status_plan_to_watch
            ListStatus.OnHold -> R.string.status_on_hold
            ListStatus.Completed -> if (type == MediaType.Movie) R.string.status_watched else R.string.status_completed
            ListStatus.Dropped -> R.string.status_dropped
        },
    )

@Composable
fun ThemeMode.label(): String =
    stringResource(
        when (this) {
            ThemeMode.System -> R.string.theme_system
            ThemeMode.Light -> R.string.theme_light
            ThemeMode.Dark -> R.string.theme_dark
        },
    )

val ShowStatus.label: Int
    @StringRes get() =
        when (this) {
            ShowStatus.Airing -> R.string.detail_status_airing
            ShowStatus.Ended -> R.string.detail_status_ended
            ShowStatus.Cancelled -> R.string.detail_status_cancelled
            ShowStatus.Returning -> R.string.detail_status_returning
            ShowStatus.Tba -> R.string.detail_status_tba
            ShowStatus.Upcoming -> R.string.detail_status_upcoming
            ShowStatus.InProduction -> R.string.detail_status_in_production
            ShowStatus.Planned -> R.string.detail_status_planned
            ShowStatus.Released -> R.string.detail_status_released
        }

val Genre.label: Int
    @StringRes get() =
        when (this) {
            Genre.Action -> R.string.genre_action
            Genre.Adventure -> R.string.genre_adventure
            Genre.Animation -> R.string.genre_animation
            Genre.Anime -> R.string.genre_anime
            Genre.Biography -> R.string.genre_biography
            Genre.Children -> R.string.genre_children
            Genre.Comedy -> R.string.genre_comedy
            Genre.Crime -> R.string.genre_crime
            Genre.Documentary -> R.string.genre_documentary
            Genre.Drama -> R.string.genre_drama
            Genre.Family -> R.string.genre_family
            Genre.Fantasy -> R.string.genre_fantasy
            Genre.GameShow -> R.string.genre_game_show
            Genre.History -> R.string.genre_history
            Genre.Horror -> R.string.genre_horror
            Genre.MiniSeries -> R.string.genre_mini_series
            Genre.Music -> R.string.genre_music
            Genre.Musical -> R.string.genre_musical
            Genre.Mystery -> R.string.genre_mystery
            Genre.News -> R.string.genre_news
            Genre.Reality -> R.string.genre_reality
            Genre.Romance -> R.string.genre_romance
            Genre.ScienceFiction -> R.string.genre_science_fiction
            Genre.Soap -> R.string.genre_soap
            Genre.Sport -> R.string.genre_sport
            Genre.Suspense -> R.string.genre_suspense
            Genre.TalkShow -> R.string.genre_talk_show
            Genre.Thriller -> R.string.genre_thriller
            Genre.War -> R.string.genre_war
            Genre.Western -> R.string.genre_western
        }
