package com.costemp.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.costemp.app.studio.StudioViewModel
import com.costemp.app.ui.CostempApp
import com.costemp.app.ui.theme.CostempPalette
import com.costemp.app.ui.theme.CostempTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StudioViewModel by viewModels { StudioViewModel.factory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            CostempTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = CostempPalette.Workspace) {
                    CostempApp(viewModel)
                }
            }
        }
    }
}
