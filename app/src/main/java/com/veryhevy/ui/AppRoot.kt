package com.veryhevy.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
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
    TopLevelDestination(Routes.ROUTINES, "Routines", Icons.Filled.Star, Icons.Outlined.Star),
    TopLevelDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
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
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(tween(240)) + scaleIn(initialScale = 0.94f, animationSpec = tween(240))
            },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = {
                fadeIn(tween(240)) + scaleIn(initialScale = 0.94f, animationSpec = tween(240))
            },
            popExitTransition = { fadeOut(tween(180)) },
        ) {
            composable(Routes.HOME) {
                val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as VeryHevyApp
                val vm: HomeViewModel = viewModel(
                    factory = viewModelFactory { initializer { HomeViewModel(app) } }
                )
                HomeScreen(vm)
            }
            composable(Routes.HISTORY) {
                val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as VeryHevyApp
                val vm: HistoryViewModel = viewModel(
                    factory = viewModelFactory { initializer { HistoryViewModel(app) } }
                )
                HistoryScreen(vm, onOpenWorkout = { navController.navigate("workout/$it") })
            }
            composable(
                "workout/{id}",
                enterTransition = {
                    slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(320))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(300))
                },
            ) { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as VeryHevyApp
                val vm: HistoryViewModel = viewModel(
                    factory = viewModelFactory { initializer { HistoryViewModel(app) } }
                )
                WorkoutDetailScreen(workoutId = id, vm = vm, onBack = { navController.popBackStack() })
            }
            composable(Routes.TRAINING) {
                val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as VeryHevyApp
                val vm: TrackerViewModel = viewModel(
                    factory = viewModelFactory { initializer { TrackerViewModel(app) } }
                )
                TrackerScreen(vm)
            }
            composable(Routes.ROUTINES) {
                val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as VeryHevyApp
                val vm: RoutinesViewModel = viewModel(
                    factory = viewModelFactory { initializer { RoutinesViewModel(app) } }
                )
                RoutinesListScreen(vm, onOpenRoutine = { navController.navigate("routine/$it") })
            }
            composable(
                "routine/{id}",
                enterTransition = {
                    slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(320))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(300))
                },
            ) { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as VeryHevyApp
                val vm: RoutinesViewModel = viewModel(
                    factory = viewModelFactory { initializer { RoutinesViewModel(app) } }
                )
                RoutineEditScreen(
                    routineId = id,
                    vm = vm,
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
                val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as VeryHevyApp
                val vm: SettingsViewModel = viewModel(
                    factory = viewModelFactory { initializer { SettingsViewModel(app) } }
                )
                SettingsScreen(vm)
            }
        }
    }
}
