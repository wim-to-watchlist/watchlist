package to.wim.watchlist.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import to.wim.watchlist.R
import to.wim.watchlist.data.model.SignedOutStep
import to.wim.watchlist.ui.components.AppIcon
import to.wim.watchlist.ui.components.rememberApp
import to.wim.watchlist.ui.theme.readableWidth

/**
 * The first screen for someone new: the demo first (the whole app with made-up titles), then the
 * free trial. Signing in with SIMKL comes after subscribing (see Onboarding).
 */
@Composable
fun WelcomeScreen() {
    val app = rememberApp()
    val subscription by app.subscription.state.collectAsState()
    val plans = subscription.plans
    val subscribeLabel = if (plans.isNotEmpty() && plans.none { it.trialDays != null }) R.string.sub_subscribe else R.string.sub_start_trial

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        AppIcon(Modifier.size(96.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.screens_login_tagline),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { app.startDemo() }, modifier = Modifier.readableWidth(480.dp).height(56.dp)) {
            Text(stringResource(R.string.demo_try))
        }
        Text(
            stringResource(R.string.demo_try_note),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { app.signedOutStep.value = SignedOutStep.Subscribe },
            modifier = Modifier.readableWidth(480.dp).height(56.dp),
        ) {
            Text(stringResource(subscribeLabel))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.demo_welcome_note),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
