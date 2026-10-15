/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.agentgit007.noctuary.ui.nav.NoctuaryApp
import io.github.agentgit007.noctuary.ui.theme.NoctuaryTheme
import io.github.agentgit007.noctuary.ui.theme.isNight

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val vm: NoctuaryViewModel = viewModel(factory = NoctuaryViewModel.Factory)
            val settings by vm.settings.collectAsState()
            // Follow the app's own day/night choice, not only the system's,
            // so status-bar icons stay readable on the paper (day) themes.
            val night = isNight(settings.themeMode)
            LaunchedEffect(night) {
                val bars = if (night) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            NoctuaryTheme(themeMode = settings.themeMode, palette = settings.palette) {
                NoctuaryApp(vm)
            }
        }
    }
}
