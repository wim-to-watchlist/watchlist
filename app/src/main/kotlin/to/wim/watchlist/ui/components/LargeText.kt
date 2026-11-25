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

    /**
     * Lines a card's title or episode line may take: one at normal text size, two once the text
     * is set larger, so "Grey's An…" can be read in full.
     */
    fun cardLines(fontScale: Float): Int = if (fontScale > 1.05f) 2 else 1

    /** The narrowest a rating tile may get at normal text size; it grows with the text. */
    /** Room a tab adds around its label: 16dp on each side. */
    const val TAB_PADDING_DP = 32f

    /**
     * Whether tabs whose content is [contentWidthsDp] wide all fit side by side in [availableDp] at
     * the same width (the widest decides), so the row can show every tab at once.
     */
    fun tabsFit(
        contentWidthsDp: List<Float>,
        availableDp: Float,
    ): Boolean {
        val widest = contentWidthsDp.maxOrNull() ?: return true
        return (widest + TAB_PADDING_DP) * contentWidthsDp.size <= availableDp
    }

    const val MIN_TILE_WIDTH_DP = 84f

    /**
     * How many rating tiles share a line [widthDp] wide (8 dp apart) at [fontScale]: as many as
     * fit at their narrowest, at least one. The rest go to the next line, so no tile is squeezed.
     */
    fun tilesPerRow(
        widthDp: Float,
        fontScale: Float,
        gapDp: Float = 8f,
    ): Int {
        val tile = MIN_TILE_WIDTH_DP * fontScale.coerceAtLeast(1f)
        return ((widthDp + gapDp) / (tile + gapDp)).toInt().coerceAtLeast(1)
    }

    /**
     * The largest text size from [maxSp] down to [minSp] (in [stepSp] steps) at which [fits] holds,
     * or [minSp] when none does. The bottom bar uses one size for all its labels, so they stay even.
     */
    fun fitSize(
        maxSp: Float,
        minSp: Float = MIN_LABEL_SP,
        stepSp: Float = 0.5f,
        fits: (Float) -> Boolean,
    ): Float {
        var size = maxSp
        while (size > minSp) {
            if (fits(size)) return size
            size -= stepSp
        }
        return minSp
    }
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
