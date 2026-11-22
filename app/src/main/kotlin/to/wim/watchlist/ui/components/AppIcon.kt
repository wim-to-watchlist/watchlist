package to.wim.watchlist.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import to.wim.watchlist.R

/**
 * The app's logo inside the app: the gradient TV with the check on its own, with no plate
 * or box behind it.
 */
@Composable
fun AppIcon(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = modifier)
}
