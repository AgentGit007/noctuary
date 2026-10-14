/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary

import android.app.Application
import android.content.Context
import io.github.agentgit007.noctuary.data.DreamRepository
import io.github.agentgit007.noctuary.data.NoctuaryDatabase
import io.github.agentgit007.noctuary.data.SettingsStore

class AppContainer(context: Context) {
    val repository: DreamRepository = DreamRepository(NoctuaryDatabase.get(context).dreamDao())
    val settings: SettingsStore = SettingsStore(context)
}

class NoctuaryApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
