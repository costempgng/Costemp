package com.costemp.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.costemp.app.studio.StudioViewModel
import com.costemp.app.ui.theme.CostempPalette

/**
 * The studio workspace.
 *
 * Layout follows the spec: a top menu bar, a left tool rail, the 3D viewport and a
 * collapsible properties panel, with the status bar along the bottom. Phones collapse
 * the rail to a floating strip and present properties in a bottom sheet; tablets at
 * 840dp and up show all three columns.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostempApp(viewModel: StudioViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showNewProject by rememberSaveable { mutableStateOf(false) }
    var showOpenProject by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val json = viewModel.exportProjectJson()
            val written = if (json == null) {
                false
            } else {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray())
                    } ?: error("No output stream")
                }.isSuccess
            }
            viewModel.showStatus(if (written) "Project exported" else "Export failed")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text == null) {
                viewModel.showStatus("Could not read that file")
            } else {
                viewModel.importProjectJson(text)
            }
        }
    }

    BoxWithConstraints(modifier.fillMaxSize().background(CostempPalette.Workspace)) {
        val isTablet = maxWidth >= 840.dp

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopMenuBar(
                state = state,
                viewModel = viewModel,
                onNewProject = { showNewProject = true },
                onOpenProject = { showOpenProject = true },
                onExport = {
                    exportLauncher.launch("${state.project?.name ?: "costemp"}.costemp.json")
                },
                onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                onAbout = { showAbout = true },
            )
            HorizontalDivider(color = CostempPalette.Outline, thickness = 1.dp)

            Row(Modifier.weight(1f).fillMaxWidth()) {
                if (isTablet) {
                    ToolRail(
                        state = state,
                        onToolSelected = viewModel::setTool,
                        onResetCamera = viewModel::requestCameraReset,
                        onToggleWireframe = viewModel::toggleWireframe,
                        onToggleGrid = viewModel::toggleGrid,
                        modifier = Modifier
                            .padding(10.dp)
                            .width(62.dp)
                            .fillMaxHeight(),
                    )
                }

                Box(Modifier.weight(1f).fillMaxHeight()) {
                    ViewportPanel(state = state, onResetCamera = viewModel::requestCameraReset)
                    if (!isTablet) {
                        ToolRail(
                            state = state,
                            onToolSelected = viewModel::setTool,
                            onResetCamera = viewModel::requestCameraReset,
                            onToggleWireframe = viewModel::toggleWireframe,
                            onToggleGrid = viewModel::toggleGrid,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(8.dp)
                                .width(56.dp),
                        )
                    }
                }

                if (isTablet && state.propertiesVisible) {
                    PropertiesPanel(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.width(340.dp).fillMaxHeight(),
                    )
                }
            }

            StatusBar(state = state, viewModel = viewModel)
        }

        if (!isTablet && state.propertiesVisible) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
            ModalBottomSheet(
                onDismissRequest = viewModel::toggleProperties,
                sheetState = sheetState,
                containerColor = CostempPalette.Panel,
            ) {
                PropertiesPanel(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxHeight(0.72f),
                )
            }
        }
    }

    if (showNewProject) {
        NewProjectDialog(
            onCreate = { name ->
                viewModel.newProject(name)
                showNewProject = false
            },
            onDismiss = { showNewProject = false },
        )
    }

    if (showOpenProject) {
        OpenProjectDialog(
            projects = state.savedProjects,
            currentId = state.project?.id,
            onOpen = { id ->
                viewModel.openProject(id)
                showOpenProject = false
            },
            onDelete = viewModel::deleteProject,
            onDismiss = { showOpenProject = false },
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

}
