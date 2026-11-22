package to.wim.watchlist.ui.subscription

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import to.wim.watchlist.R
import to.wim.watchlist.data.SubscriptionRepository
import to.wim.watchlist.data.model.PaywallPlan
import to.wim.watchlist.data.model.PlanPeriod
import to.wim.watchlist.data.model.PlayStatus
import to.wim.watchlist.data.model.SubscriptionRules
import to.wim.watchlist.ui.components.AppIcon
import to.wim.watchlist.ui.components.Format
import to.wim.watchlist.ui.components.openUrl
import to.wim.watchlist.ui.components.rememberApp
import to.wim.watchlist.ui.theme.AppShapes
import to.wim.watchlist.ui.theme.readableWidth
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Watchlist Plus, opened from a locked feature: what Plus adds, the yearly plan (with its free trial
 * when Google Play still offers it) and the monthly one, when the trial reminds and charges, and the
 * terms Google Play requires an app to state (when it charges, that it renews, how to cancel).
 * "Not now" goes back to the free version.
 */
@Composable
fun SubscriptionScreen(
    /** Debug builds: sample plans, the buttons do nothing, and "Close preview" goes back. */
    preview: Boolean = false,
) {
    val app = rememberApp()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by app.subscription.state.collectAsState()
    val plans = if (preview) SubscriptionRules.SAMPLE_PLANS else state.plans
    var selected by rememberSaveable { mutableStateOf(PlanPeriod.Yearly) }
    val plan = plans.firstOrNull { it.period == selected } ?: plans.firstOrNull()
    val trialDays = plans.firstNotNullOfOrNull { it.trialDays }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        AppIcon(Modifier.size(80.dp))
        Text(
            trialDays?.let { pluralStringResource(R.plurals.sub_title_trial, it, it) } ?: stringResource(R.string.sub_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Column(Modifier.readableWidth(480.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Feature(Icons.AutoMirrored.Rounded.PlaylistPlay, stringResource(R.string.sub_feature_up_next))
            Feature(Icons.Rounded.Notifications, stringResource(R.string.sub_feature_notifications))
            Feature(Icons.Rounded.CalendarMonth, stringResource(R.string.sub_feature_calendar))
            Feature(Icons.Rounded.BarChart, stringResource(R.string.sub_feature_stats))
            Feature(Icons.Rounded.Casino, stringResource(R.string.sub_feature_surprise))
            Feature(Icons.Rounded.Star, stringResource(R.string.sub_feature_ratings))
            Feature(Icons.Rounded.Movie, stringResource(R.string.sub_feature_trailers))
            Feature(Icons.Rounded.Palette, stringResource(R.string.sub_feature_colors))
            Feature(Icons.Rounded.SpaceDashboard, stringResource(R.string.sub_feature_bottom_bar))
        }
        val notice =
            when {
                preview -> null
                state.status == PlayStatus.Pending -> R.string.sub_pending
                state.status == PlayStatus.Unavailable -> R.string.sub_unavailable
                plan == null && state.status == PlayStatus.Inactive -> R.string.sub_not_loaded
                else -> null
            }
        if (notice != null) {
            Note(stringResource(notice))
        } else if (plan == null) {
            CircularProgressIndicator()
        } else {
            run {
                Column(Modifier.readableWidth(480.dp).selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    plans.forEach { p -> PlanOption(p, plans, selected = p == plan) { selected = p.period } }
                }
                plan.trialDays?.let { Timeline(it, plan.price) }
                Button(
                    onClick = { if (!preview) activity?.let { app.subscription.purchase(it, plan) } },
                    enabled = !state.purchasing,
                    modifier = Modifier.readableWidth(480.dp).height(56.dp),
                ) {
                    if (state.purchasing) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(if (plan.trialDays != null) R.string.sub_start_trial else R.string.sub_subscribe))
                    }
                }
                if (state.purchaseFailed) {
                    Text(stringResource(R.string.sub_failed), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
                Text(
                    stringResource(
                        when {
                            plan.trialDays != null -> R.string.sub_terms_trial
                            plan.period == PlanPeriod.Monthly -> R.string.sub_terms_monthly
                            else -> R.string.sub_terms
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.readableWidth(480.dp),
                )
            }
        }
        TextButton(onClick = { context.openUrl(SubscriptionRepository.manageUrl(context.packageName)) }) {
            Text(stringResource(R.string.sub_manage))
        }
        if (preview) {
            OutlinedButton(onClick = {
                app.paywallPreview.value = false
                app.showPaywall.value = false
            }) { Text(stringResource(R.string.sub_preview_close)) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { scope.launch { app.subscription.refresh() } }) { Text(stringResource(R.string.sub_check_again)) }
                TextButton(onClick = { app.showPaywall.value = false }) { Text(stringResource(R.string.plus_not_now)) }
            }
        }
    }
}

/** A plan to pick: its name and price, and for the yearly plan its price per month, saving and trial. */
@Composable
private fun PlanOption(
    plan: PaywallPlan,
    all: List<PaywallPlan>,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val monthly = all.firstOrNull { it.period == PlanPeriod.Monthly }
    Surface(
        shape = AppShapes.Item,
        color = if (selected) colors.secondaryContainer else colors.surfaceContainerLow,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
        modifier =
            Modifier.fillMaxWidth().selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(if (plan.period == PlanPeriod.Yearly) R.string.sub_plan_yearly else R.string.sub_plan_monthly),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(if (plan.period == PlanPeriod.Yearly) R.string.sub_price else R.string.sub_price_monthly, plan.price),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (plan.period == PlanPeriod.Yearly) {
                    SubscriptionRules.perMonth(plan, locale)?.let {
                        Text(
                            stringResource(R.string.sub_per_month, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                plan.trialDays?.let { Label(pluralStringResource(R.plurals.sub_days_free, it, it), colors.tertiaryContainer, colors.onTertiaryContainer) }
                if (plan.period == PlanPeriod.Yearly && monthly != null) {
                    SubscriptionRules.savingsPercent(plan, monthly)?.let {
                        Label(stringResource(R.string.sub_save, it), colors.primary, colors.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(
    text: String,
    container: Color,
    content: Color,
) {
    Surface(shape = MaterialTheme.shapes.small, color = container, contentColor = content) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

/** What happens during the free trial: full access today, a reminder before it ends, the first charge. */
@Composable
private fun Timeline(
    trialDays: Int,
    price: String,
) {
    val today = Instant.now()
    val reminderDay = (trialDays - SubscriptionRules.REMINDER_DAYS_BEFORE).coerceAtLeast(1)
    Column(Modifier.readableWidth(480.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Step(Icons.Rounded.LockOpen, stringResource(R.string.sub_step_today), stringResource(R.string.sub_step_today_text))
        Step(
            Icons.Rounded.NotificationsActive,
            stringResource(R.string.sub_step_day, reminderDay, Format.date(today.plus(reminderDay.toLong(), ChronoUnit.DAYS))),
            stringResource(R.string.sub_step_reminder_text),
        )
        Step(
            Icons.Rounded.CreditCard,
            stringResource(R.string.sub_step_day, trialDays, Format.date(today.plus(trialDays.toLong(), ChronoUnit.DAYS))),
            stringResource(R.string.sub_step_charge_text, price),
        )
    }
}

@Composable
private fun Step(
    icon: ImageVector,
    title: String,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Feature(
    icon: ImageVector,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.readableWidth(480.dp))
}
