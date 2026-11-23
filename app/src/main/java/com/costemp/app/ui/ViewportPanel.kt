package com.costemp.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.costemp.app.studio.StudioState
import com.costemp.app.ui.theme.CostempPalette
import com.costemp.app.viewport.ViewportView

/**
 * Hosts the OpenGL surface and the HUD chips drawn over it.
 *
 * State flows one way: the view model publishes mesh/tool/flags, this composable
 * forwards them to the GL view, and gestures update the camera inside the renderer.
 */
@Composable
fun ViewportPanel(
    state: StudioState,
    onResetCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewportView by remember { mutableStateOf<ViewportView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(CostempPalette.ViewportTop, CostempPalette.ViewportBottom),
                ),
            ),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                ViewportView(context).also { view ->
                    view.tool = state.tool
                    view.setWireframe(state.wireframe)
                    view.setGrid(state.grid)
                    state.mesh?.let(view::submitMesh)
                    view.applyCameraToken(state.cameraResetToken)
                    viewportView = view
                }
            },
            update = { view ->
                view.tool = state.tool
                view.setWireframe(state.wireframe)
                view.setGrid(state.grid)
                state.mesh?.let(view::submitMesh)
                view.applyCameraToken(state.cameraResetToken)
            },
        )

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> viewportView?.onResume()
                    Lifecycle.Event.ON_PAUSE -> viewportView?.onPause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                viewportView?.onPause()
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Chip("PERSPECTIVE")
            Chip(state.tool.label.uppercase(), color = CostempPalette.Accent)
            if (state.wireframe) Chip("WIREFRAME", color = CostempPalette.Accent)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        ) {
            Text(
                text = state.tool.hint,
                color = CostempPalette.TextSecondary,
                fontSize = 10.sp,
            )
            Text(
                text = "Two fingers pan · pinch zooms · double tap frames",
                color = CostempPalette.TextSecondary.copy(alpha = 0.6f),
                fontSize = 10.sp,
            )
        }

        TextButton(
            onClick = onResetCamera,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(CostempPalette.PanelRaised.copy(alpha = 0.85f)),
        ) {
            Text("FRAME", color = CostempPalette.TextPrimary, fontSize = 10.sp, letterSpacing = 1.sp)
        }
    }
}
