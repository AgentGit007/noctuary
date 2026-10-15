/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.agentgit007.noctuary.ui.nav

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.agentgit007.noctuary.NoctuaryViewModel
import io.github.agentgit007.noctuary.ui.components.RetroBackdrop
import io.github.agentgit007.noctuary.ui.components.hardShadow
import io.github.agentgit007.noctuary.ui.editor.EditorScreen
import io.github.agentgit007.noctuary.ui.journal.JournalScreen
import io.github.agentgit007.noctuary.ui.settings.SettingsScreen
import io.github.agentgit007.noctuary.ui.stats.StatsScreen
import io.github.agentgit007.noctuary.ui.theme.LocalAccents
import io.github.agentgit007.noctuary.ui.theme.LocalPaletteColors

object Routes {
    const val JOURNAL = "journal"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{id}"
    fun editor(id: Long) = "editor/$id"
}

/** Height of the poster nav bar above the system navigation inset. */
private val PosterNavHeight = 64.dp

/**
 * Kosmos navigation: a concrete strip of equal cells divided by ink rules,
 * the active cell printed solid ink, like a tram-ticket punch board.
 */
@Composable
private fun PosterNavBar(
    destinations: List<Destination>,
    route: String?,
    onSelect: (Destination) -> Unit,
) {
    val palette = LocalPaletteColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(palette.surfaceHigh)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(palette.ink)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(PosterNavHeight - 2.dp)
                .selectableGroup()
        ) {
            destinations.forEachIndexed { i, d ->
                if (i > 0) {
                    Box(
                        Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(palette.ink)
                    )
                }
                val selected = route == d.route
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (selected) palette.ink else Color.Transparent)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { if (!selected) onSelect(d) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
                ) {
                    val fg = if (selected) palette.onInk else palette.onBackground
                    Box(
                        Modifier
                            .size(8.dp)
                            .then(
                                if (selected) {
                                    Modifier.background(fg)
                                } else {
                                    Modifier.border(1.5.dp, fg)
                                }
                            )
                    )
                    Text(
                        text = d.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = fg,
                    )
                }
            }
        }
    }
}

private data class Destination(val route: String, val label: String, val icon: ImageVector)

@Composable
fun NoctuaryApp(vm: NoctuaryViewModel) {
    val navController = rememberNavController()
    val settings by vm.settings.collectAsState()
    val message by vm.message.collectAsState()
    val accents = LocalAccents.current
    val palette = LocalPaletteColors.current

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val onEditor = route == Routes.EDITOR

    val destinations = listOf(
        Destination(Routes.JOURNAL, "JOURNAL", Icons.Outlined.AutoAwesome),
        Destination(Routes.STATS, "PATTERNS", Icons.Outlined.Insights),
        Destination(Routes.SETTINGS, "SETTINGS", Icons.Outlined.Tune),
    )

    fun openDream(id: Long) {
        vm.openEditor(id)
        navController.navigate(Routes.editor(id))
    }

    Box(Modifier.fillMaxSize()) {
        RetroBackdrop(
            scanlines = settings.scanlines,
            intensity = if (onEditor) 0.55f else 1f,
            skylineLift = if (onEditor) 0.dp else PosterNavHeight,
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = palette.onBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!onEditor && palette.poster) {
                    PosterNavBar(
                        destinations = destinations,
                        route = route,
                        onSelect = { d ->
                            navController.navigate(d.route) {
                                popUpTo(Routes.JOURNAL) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                } else if (!onEditor) {
                    NavigationBar(
                        containerColor = palette.surface.copy(alpha = 0.92f),
                        contentColor = palette.onBackground,
                    ) {
                        destinations.forEach { d ->
                            NavigationBarItem(
                                selected = route == d.route,
                                onClick = {
                                    if (route != d.route) {
                                        navController.navigate(d.route) {
                                            popUpTo(Routes.JOURNAL) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(d.icon, contentDescription = d.label) },
                                label = { Text(d.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = palette.background,
                                    selectedTextColor = accents.mint,
                                    indicatorColor = accents.mint,
                                    unselectedIconColor = palette.onSurfaceMuted,
                                    unselectedTextColor = palette.onSurfaceMuted,
                                ),
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (route == Routes.JOURNAL && palette.poster) {
                    Surface(
                        onClick = { openDream(0L) },
                        modifier = Modifier
                            .hardShadow(palette.ink, shape = CircleShape)
                            .size(58.dp),
                        shape = CircleShape,
                        color = palette.fillFor(accents.blossom),
                        contentColor = palette.onFill,
                        border = BorderStroke(2.dp, palette.ink),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Add, contentDescription = "Record a dream")
                        }
                    }
                } else if (route == Routes.JOURNAL) {
                    FloatingActionButton(
                        onClick = { openDream(0L) },
                        containerColor = accents.lilac,
                        contentColor = palette.background,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Record a dream")
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.JOURNAL,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Routes.JOURNAL) {
                    JournalScreen(
                        vm = vm,
                        contentPadding = padding,
                        onOpenDream = ::openDream,
                    )
                }
                composable(Routes.STATS) {
                    StatsScreen(
                        vm = vm,
                        contentPadding = padding,
                        onOpenDream = ::openDream,
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(vm = vm, contentPadding = padding)
                }
                composable(
                    route = Routes.EDITOR,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L
                    LaunchedEffect(id) { vm.openEditor(id) }
                    EditorScreen(
                        vm = vm,
                        contentPadding = padding,
                        onDone = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
