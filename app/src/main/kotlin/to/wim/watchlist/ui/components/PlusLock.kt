package to.wim.watchlist.ui.components

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import to.wim.watchlist.R
import to.wim.watchlist.ui.theme.readableWidth

/** True when Watchlist Plus is open (subscribed or in the free trial); see `Plus`. */
val LocalPlus = staticCompositionLocalOf { true }

/**
 * Locked content: blurred (Android 12 and later; older versions can't blur, so it is faded
 * instead), hidden from TalkBack, and a tap opens the subscription screen instead of the item.
 */
@Composable
fun Locked(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val app = rememberApp()
    Box(modifier) {
        Box(Modifier.plusBlur()) { content() }
        Box(Modifier.matchParentSize().clearAndSetSemantics {}.clickable(interactionSource = null, indication = null) { app.showPaywall.value = true })
    }
}

/**
 * The blur of every locked part, for a single element such as a number on a card whose labels stay
 * readable: blurred on Android 12 and later, faded on older versions (they can't blur), and hidden
 * from TalkBack.
 */
fun Modifier.plusBlur(): Modifier =
    clearAndSetSemantics {}
        .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(10.dp).alpha(0.8f) else Modifier.alpha(0.12f))

/**
 * A locked row of a screen (trailers, similar titles): the row blurred, with a small button on top
 * that says it is part of Plus and opens the subscription screen.
 */
@Composable
fun LockedRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val app = rememberApp()
    Box(modifier, contentAlignment = Alignment.Center) {
        Locked { content() }
        FilledTonalButton(onClick = { app.showPaywall.value = true }) {
            Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.plus_locked))
        }
    }
}

/** The card on a locked Plus feature: what it does for the user and the way to the trial. */
@Composable
fun PlusCard(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    val app = rememberApp()
    val state by app.subscription.state.collectAsState()
    val trial = state.plans.isEmpty() || state.plans.any { it.trialDays != null }
    ElevatedCard(modifier.readableWidth(480.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = { app.showPaywall.value = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 48.dp),
            ) { Text(stringResource(if (trial) R.string.sub_start_trial else R.string.plus_see_plans)) }
        }
    }
}
