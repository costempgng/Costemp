package com.costemp.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.costemp.app.studio.StudioState
import com.costemp.app.studio.StudioViewModel
import com.costemp.app.ui.theme.CostempPalette
import com.costemp.core.measurement.LengthUnit

/** Bottom readout: history controls, selection, mesh totals and model dimensions. */
@Composable
fun StatusBar(state: StudioState, viewModel: StudioViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CostempPalette.Panel)
            .navigationBarsPadding()
            .height(30.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = viewModel::undo,
            enabled = state.canUndo,
            modifier = Modifier.size(26.dp),
        ) {
            StudioGlyphIcon(
                glyph = StudioGlyph.UNDO,
                tint = if (state.canUndo) CostempPalette.TextPrimary else CostempPalette.TextSecondary.copy(alpha = 0.35f),
                modifier = Modifier.size(15.dp),
            )
        }
        IconButton(
            onClick = viewModel::redo,
            enabled = state.canRedo,
            modifier = Modifier.size(26.dp),
        ) {
            StudioGlyphIcon(
                glyph = StudioGlyph.REDO,
                tint = if (state.canRedo) CostempPalette.TextPrimary else CostempPalette.TextSecondary.copy(alpha = 0.35f),
                modifier = Modifier.size(15.dp),
            )
        }

        StatusText("Mannequin", color = CostempPalette.TextPrimary)
        StatusSeparator()
        StatusText(state.statusMessage ?: state.projectName, maxWidthFraction = true)

        Spacer(Modifier.weight(1f))

        state.stats?.let { stats ->
            StatusText(
                "${stats.triangles} tris  ·  ${stats.vertices} verts",
                color = CostempPalette.TextSecondary,
            )
            StatusSeparator()
            val unit = state.unit
            StatusText(
                "${LengthUnit.formatNumber(unit.fromCentimetres(stats.widthCm.toDouble()))}" +
                    " × ${LengthUnit.formatNumber(unit.fromCentimetres(stats.heightCm.toDouble()))}" +
                    " × ${LengthUnit.formatNumber(unit.fromCentimetres(stats.depthCm.toDouble()))} ${unit.symbol}",
                color = CostempPalette.TextSecondary,
            )
        }
    }
}

@Composable
private fun StatusText(
    text: String,
    color: Color = CostempPalette.TextSecondary,
    maxWidthFraction: Boolean = false,
) {
    Text(
        text = text,
        color = color,
        style = TechnicalText.copy(fontSize = 10.sp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = if (maxWidthFraction) Modifier.width(200.dp) else Modifier,
    )
}

@Composable
private fun StatusSeparator() {
    Text("│", color = CostempPalette.OutlineStrong, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 8.dp))
}
