package com.costemp.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.costemp.app.studio.StudioState
import com.costemp.app.studio.StudioViewModel
import com.costemp.app.ui.theme.CostempPalette

/**
 * The application menu. Every entry is wired to a real action in [StudioViewModel];
 * unavailable actions are disabled rather than silently doing nothing.
 */
@Composable
fun TopMenuBar(
    state: StudioState,
    viewModel: StudioViewModel,
    onNewProject: () -> Unit,
    onOpenProject: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(CostempPalette.Panel)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(CostempPalette.Accent, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "COSTEMP",
            color = CostempPalette.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(Modifier.width(14.dp))

        MenuButton("File") { close ->
            MenuItem("New project…", onClick = { close(); onNewProject() })
            MenuItem("Open project…", onClick = { close(); onOpenProject() })
            MenuItem("Save project", onClick = { close(); viewModel.saveNow() })
            MenuItem("Import project…", onClick = { close(); onImport() })
            MenuItem("Export project…", onClick = { close(); onExport() })
        }
        MenuButton("Edit") { close ->
            MenuItem("Undo", enabled = state.canUndo, onClick = { close(); viewModel.undo() })
            MenuItem("Redo", enabled = state.canRedo, onClick = { close(); viewModel.redo() })
            MenuItem("Reset measurements", onClick = { close(); viewModel.resetMeasurements() })
        }
        MenuButton("View") { close ->
            MenuItem(
                if (state.wireframe) "Hide wireframe" else "Show wireframe",
                onClick = { close(); viewModel.toggleWireframe() },
            )
            MenuItem(
                if (state.grid) "Hide grid" else "Show grid",
                onClick = { close(); viewModel.toggleGrid() },
            )
            MenuItem("Frame mannequin", onClick = { close(); viewModel.requestCameraReset() })
            MenuItem(
                if (state.propertiesVisible) "Hide properties" else "Show properties",
                onClick = { close(); viewModel.toggleProperties() },
            )
        }
        MenuButton("Help") { close ->
            MenuItem("About Costemp", onClick = { close(); onAbout() })
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = state.projectName,
            color = CostempPalette.TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp),
        )
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = viewModel::toggleProperties, modifier = Modifier.size(32.dp)) {
            StudioGlyphIcon(
                glyph = StudioGlyph.PANEL,
                tint = if (state.propertiesVisible) CostempPalette.Accent else CostempPalette.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun MenuButton(label: String, content: @Composable ColumnScope.(close: () -> Unit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            text = label,
            color = if (expanded) CostempPalette.TextPrimary else CostempPalette.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CostempPalette.PanelRaised),
        ) {
            content { expanded = false }
        }
    }
}

@Composable
private fun MenuItem(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                fontSize = 12.sp,
                color = if (enabled) CostempPalette.TextPrimary else CostempPalette.TextSecondary.copy(alpha = 0.5f),
            )
        },
        enabled = enabled,
        onClick = onClick,
    )
}
