package com.veryhevy.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.veryhevy.VeryHevyApp
import com.veryhevy.ui.tracker.TrackerScreen
import com.veryhevy.ui.tracker.TrackerViewModel
import com.veryhevy.ui.history.HistoryScreen
import com.veryhevy.ui.history.HistoryViewModel
import com.veryhevy.ui.history.WorkoutDetailScreen
import com.veryhevy.ui.routines.RoutinesListScreen
import com.veryhevy.ui.routines.RoutineEditScreen
import com.veryhevy.ui.routines.RoutinesViewModel
import com.veryhevy.ui.home.HomeScreen
import com.veryhevy.ui.home.HomeViewModel
import com.veryhevy.ui.settings.SettingsScreen
import com.veryhevy.ui.settings.SettingsViewModel

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val TRAINING = "training"
    const val ROUTINES = "routines"
    const val SETTINGS = "settings"
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    TopLevelDestination(Routes.HISTORY, "History", Icons.Filled.History, Icons.Outlined.History),
    TopLevelDestination(Routes.TRAINING, "Training", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    TopLevelDestination(Routes.ROUTINES, "Routines", Icons.Filled.EventNote, Icons.Outlined.EventNote),
    TopLevelDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Activity-scoped ViewModels — created once, kept alive across tab switches.
    // (Popping with saveState destroys per-destination ViewModels, which caused
    // a full re-query + recomposition on every navigation.)
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as VeryHevyApp
    val homeVm: HomeViewModel = viewModel(factory = viewModelFactory { initializer { HomeViewModel(app) } })
    val historyVm: HistoryViewModel = viewModel(factory = viewModelFactory { initializer { HistoryViewModel(app) } })
    val trackerVm: TrackerViewModel = viewModel(factory = viewModelFactory { initializer { TrackerViewModel(app) } })
    val routinesVm: RoutinesViewModel = viewModel(factory = viewModelFactory { initializer { RoutinesViewModel(app) } })
    val settingsVm: SettingsViewModel = viewModel(factory = viewModelFactory { initializer { SettingsViewModel(app) } })

    Scaffold(
        bottomBar = {
            Column {
                VhDivider()
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                topLevelDestinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(tween(160)) + scaleIn(initialScale = 0.96f, animationSpec = tween(160))
            },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = {
                fadeIn(tween(160)) + scaleIn(initialScale = 0.96f, animationSpec = tween(160))
            },
            popExitTransition = { fadeOut(tween(120)) },
        ) {
            composable(Routes.HOME) {
                HomeScreen(homeVm)
            }
            composable(Routes.HISTORY) {
                HistoryScreen(historyVm, onOpenWorkout = { navController.navigate("workout/$it") })
            }
            composable(
                "workout/{id}",
                enterTransition = {
                    slideInHorizontally(tween(250)) { it / 4 } + fadeIn(tween(250))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(220))
                },
            ) { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                WorkoutDetailScreen(workoutId = id, vm = historyVm, onBack = { navController.popBackStack() })
            }
            composable(Routes.TRAINING) {
                TrackerScreen(trackerVm)
            }
            composable(Routes.ROUTINES) {
                RoutinesListScreen(routinesVm, onOpenRoutine = { navController.navigate("routine/$it") })
            }
            composable(
                "routine/{id}",
                enterTransition = {
                    slideInHorizontally(tween(250)) { it / 4 } + fadeIn(tween(250))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(220))
                },
            ) { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                RoutineEditScreen(
                    routineId = id,
                    vm = routinesVm,
                    onBack = { navController.popBackStack() },
                    onWorkoutStarted = {
                        navController.navigate(Routes.TRAINING) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(settingsVm)
            }
        }
    }
}
