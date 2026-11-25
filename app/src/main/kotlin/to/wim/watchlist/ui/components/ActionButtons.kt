package to.wim.watchlist.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** One button in [ActionButtons]: filled (tonal) for the main action, otherwise outlined. */
data class ActionButton(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    /** Read by TalkBack instead of [label] when the label alone is unclear. */
    val description: String? = null,
    val filled: Boolean = false,
)

/**
 * Buttons side by side at equal width when every label fits at full size, otherwise under each
 * other at full width. Shrinking the label made "Mark watched" smaller than "Check in" and still
 * cut it in German at large text (AI testers).
 */
@Composable
fun ActionButtons(buttons: List<ActionButton>) {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val widths =
            remember(buttons.map { it.label }, style, density) {
                buttons.map { with(density) { measurer.measure(it.label, style, maxLines = 1).size.width.toDp().value } }
            }
        if (LargeText.buttonsFit(widths, maxWidth.value)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                buttons.forEach { Button(it, Modifier.weight(1f)) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                buttons.forEach { Button(it, Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
private fun Button(
    button: ActionButton,
    modifier: Modifier,
) {
    val described = button.description?.let { d -> modifier.semantics { contentDescription = d } } ?: modifier
    val content: @Composable RowScope.() -> Unit = {
        Icon(button.icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(button.label, textAlign = TextAlign.Center)
    }
    if (button.filled) {
        FilledTonalButton(onClick = button.onClick, modifier = described, content = content)
    } else {
        OutlinedButton(onClick = button.onClick, modifier = described, content = content)
    }
}
