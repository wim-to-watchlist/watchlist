package to.wim.watchlist.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import to.wim.watchlist.R

/**
 * The app's own icon, as the launcher shows it: the gradient TV with the checkmark on its soft
 * violet plate (the adaptive icon's background and foreground layers; the safe zone leaves the
 * margin).
 */
@Composable
fun AppIcon(modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(28.dp), shadowElevation = 4.dp) {
        Box {
            Image(
                painterResource(R.drawable.ic_launcher_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}
