package com.costemp.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.costemp.app.BuildConfig
import com.costemp.app.ui.theme.CostempPalette
import com.costemp.core.project.Project
import com.costemp.core.project.ProjectSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NewProjectDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CostempPalette.PanelRaised,
        titleContentColor = CostempPalette.TextPrimary,
        textContentColor = CostempPalette.TextSecondary,
        title = { Text("New project", fontSize = 16.sp) },
        text = {
            Column {
                Text("Name this costume build. Measurements start from the reference body.", fontSize = 12.sp)
                Spacer(Modifier.heightIn(min = 12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    singleLine = true,
                    placeholder = { Text(Project.DEFAULT_NAME, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onCreate(name) }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }) { Text("Create", color = CostempPalette.Accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = CostempPalette.TextSecondary) }
        },
    )
}

@Composable
fun OpenProjectDialog(
    projects: List<ProjectSummary>,
    currentId: String?,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CostempPalette.PanelRaised,
        titleContentColor = CostempPalette.TextPrimary,
        textContentColor = CostempPalette.TextSecondary,
        title = { Text("Projects", fontSize = 16.sp) },
        text = {
            if (projects.isEmpty()) {
                Text("No saved projects yet.", fontSize = 12.sp)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(projects, key = { it.id }) { project ->
                        ProjectRow(
                            project = project,
                            isCurrent = project.id == currentId,
                            onOpen = { onOpen(project.id) },
                            onDelete = { onDelete(project.id) },
                        )
                        HorizontalDivider(color = CostempPalette.Outline)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = CostempPalette.Accent) }
        },
    )
}

@Composable
private fun ProjectRow(
    project: ProjectSummary,
    isCurrent: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val formatter = remember { SimpleDateFormat("d MMM yyyy · HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) CostempPalette.AccentSoft else Color.Transparent)
            .clickable(onClick = onOpen)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .heightIn(min = 26.dp)
                .background(if (isCurrent) CostempPalette.Accent else CostempPalette.Outline),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                project.name,
                color = CostempPalette.TextPrimary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${String.format(Locale.US, "%.1f", project.heightCm)} cm · ${formatter.format(Date(project.updatedAt))}",
                color = CostempPalette.TextSecondary,
                fontSize = 10.sp,
            )
        }
        TextButton(onClick = onDelete) {
            Text("Delete", color = CostempPalette.TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CostempPalette.PanelRaised,
        titleContentColor = CostempPalette.TextPrimary,
        textContentColor = CostempPalette.TextSecondary,
        title = { Text("Costemp", fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Open-source cosplay studio: measurement-driven mannequin, armor fitting, " +
                        "retopology, papercraft unfolding and foam patterns.",
                    fontSize = 12.sp,
                )
                Text("Version ${BuildConfig.VERSION_NAME}", color = CostempPalette.TextSecondary, fontSize = 11.sp)
                Text(
                    "This build covers the Phase 1 foundation: project manager, measurement model, " +
                        "procedural mannequin and the studio workspace. Fabrication workflows arrive in " +
                        "later phases.",
                    fontSize = 11.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = CostempPalette.Accent) }
        },
    )
}
