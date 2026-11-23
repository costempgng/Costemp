package com.costemp.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.costemp.app.studio.StudioState
import com.costemp.app.studio.StudioTool
import com.costemp.app.ui.theme.CostempPalette

/**
 * Vertical navigation rail: camera tools plus the two view toggles that change what
 * the viewport draws.
 */
@Composable
fun ToolRail(
    state: StudioState,
    onToolSelected: (StudioTool) -> Unit,
    onResetCamera: () -> Unit,
    onToggleWireframe: () -> Unit,
    onToggleGrid: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xE6111116))
            .border(1.dp, CostempPalette.Outline, RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        StudioTool.entries.forEach { tool ->
            RailButton(
                glyph = when (tool) {
                    StudioTool.ORBIT -> StudioGlyph.ORBIT
                    StudioTool.PAN -> StudioGlyph.PAN
                    StudioTool.ZOOM -> StudioGlyph.ZOOM
                },
                label = tool.label,
                active = state.tool == tool,
                onClick = { onToolSelected(tool) },
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            color = CostempPalette.Outline,
        )

        RailButton(
            glyph = StudioGlyph.RESET,
            label = "Frame",
            active = false,
            onClick = onResetCamera,
        )
        RailButton(
            glyph = StudioGlyph.WIREFRAME,
            label = "Wire",
            active = state.wireframe,
            onClick = onToggleWireframe,
        )
        RailButton(
            glyph = StudioGlyph.GRID,
            label = "Grid",
            active = state.grid,
            onClick = onToggleGrid,
        )
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun RailButton(
    glyph: StudioGlyph,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) CostempPalette.AccentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            StudioGlyphIcon(
                glyph = glyph,
                tint = if (active) CostempPalette.Accent else CostempPalette.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label.uppercase(),
                fontSize = 8.sp,
                letterSpacing = 0.6.sp,
                fontWeight = FontWeight.Medium,
                color = if (active) CostempPalette.Accent else CostempPalette.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
