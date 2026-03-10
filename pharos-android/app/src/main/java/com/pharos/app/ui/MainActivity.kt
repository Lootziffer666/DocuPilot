package com.pharos.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pharos.app.ui.navigation.Screen
import com.pharos.app.ui.navigation.bottomNavItems
import com.pharos.app.ui.screen.dashboard.DashboardScreen
import com.pharos.app.ui.screen.dashboard.DashboardViewModel
import com.pharos.app.ui.screen.files.FileDetailScreen
import com.pharos.app.ui.screen.files.FilesScreen
import com.pharos.app.ui.screen.files.FilesViewModel
import com.pharos.app.ui.screen.folder.FolderScreen
import com.pharos.app.ui.screen.folder.FolderViewModel
import com.pharos.app.ui.screen.projects.ProjectDetailScreen
import com.pharos.app.ui.screen.projects.ProjectsScreen
import com.pharos.app.ui.screen.projects.ProjectsViewModel
import com.pharos.app.ui.screen.settings.SettingsScreen
import com.pharos.app.ui.screen.settings.SettingsViewModel
import com.pharos.app.ui.theme.PharosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PharosTheme {
                PharosMainContent()
            }
        }
    }
}

@Composable
private fun PharosMainContent() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val vm: DashboardViewModel = viewModel(factory = DashboardViewModelFactory())
                DashboardScreen(vm)
            }

            composable(Screen.Folders.route) {
                val vm: FolderViewModel = viewModel(factory = FolderViewModelFactory())
                FolderScreen(vm)
            }

            composable(Screen.Files.route) {
                val vm: FilesViewModel = viewModel(factory = FilesViewModelFactory())
                FilesScreen(vm) { fileId ->
                    navController.navigate(Screen.FileDetail.createRoute(fileId))
                }
            }

            composable(
                route = Screen.FileDetail.route,
                arguments = listOf(navArgument("fileId") { type = NavType.StringType })
            ) { backStackEntry ->
                val fileId = backStackEntry.arguments?.getString("fileId") ?: return@composable
                val vm: FilesViewModel = viewModel(factory = FilesViewModelFactory())
                FileDetailScreen(
                    fileId = fileId,
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Projects.route) {
                val vm: ProjectsViewModel = viewModel(factory = ProjectsViewModelFactory())
                ProjectsScreen(vm) { projectId ->
                    navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                }
            }

            composable(
                route = Screen.ProjectDetail.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val vm: ProjectsViewModel = viewModel(factory = ProjectsViewModelFactory())
                ProjectDetailScreen(
                    projectId = projectId,
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                val vm: SettingsViewModel = viewModel(factory = SettingsViewModelFactory())
                SettingsScreen(vm)
            }
        }
    }
}

// ViewModel Factories using the AppContainer

private class DashboardViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = appContainer()
        return DashboardViewModel(
            folderRepository = app.folderRepository,
            fileRepository = app.fileRepository,
            settingsRepository = app.settingsRepository,
            scanUseCase = app.scanUseCase,
            analysisUseCase = app.analysisUseCase,
            projectClusteringUseCase = app.projectClusteringUseCase,
            masterfileUseCase = app.masterfileUseCase
        ) as T
    }
}

private class FolderViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FolderViewModel(appContainer().folderRepository) as T
    }
}

private class FilesViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = appContainer()
        return FilesViewModel(
            fileRepository = app.fileRepository,
            analysisRepository = app.analysisRepository,
            analysisUseCase = app.analysisUseCase
        ) as T
    }
}

private class ProjectsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = appContainer()
        return ProjectsViewModel(
            projectRepository = app.projectRepository,
            analysisRepository = app.analysisRepository
        ) as T
    }
}

private class SettingsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = appContainer()
        return SettingsViewModel(
            settingsRepository = app.settingsRepository,
            aiApiProvider = app.aiApiProvider
        ) as T
    }
}

private fun appContainer(): com.pharos.app.AppContainer {
    return com.pharos.app.PharosApp.instance.appContainer
}
