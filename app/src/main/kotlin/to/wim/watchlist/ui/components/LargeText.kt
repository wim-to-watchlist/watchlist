package to.wim.watchlist.ui.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * Layout rules for large text sizes (Android's font size setting up to 200%), found by the AI
 * testers: short labels in a fixed row get cut, and one-word titles split mid-word.
 */
object LargeText {
    /** Poster tiles grow with the text, up to half as wide again, so a one-word title fits on a line. */
    const val MAX_POSTER_WIDTH_FACTOR = 1.5f

    /** How much wider a poster tile is at [fontScale] than at normal text size. */
    fun posterWidthFactor(fontScale: Float): Float = fontScale.coerceIn(1f, MAX_POSTER_WIDTH_FACTOR)

    /** The smallest size a label shrinks to before it is cut (scaled by the font setting too). */
    const val MIN_LABEL_SP = 8f
}

/**
 * A one-line label in a row of fixed width (bottom bar, fixed tabs): with large text it shrinks
 * until it fits instead of being cut ("Calen", "Entdeck").
 */
@Composable
fun FitLabel(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    Text(
        text,
        modifier = modifier,
        style = style,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = LargeText.MIN_LABEL_SP.sp, maxFontSize = if (style.fontSize.isSpecified) style.fontSize else 14.sp),
    )
}
