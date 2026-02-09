package com.jmalinen.blackjack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jmalinen.blackjack.ui.screens.GameScreen
import com.jmalinen.blackjack.ui.screens.SettingsScreen
import com.jmalinen.blackjack.viewmodel.GameViewModel
import com.jmalinen.blackjack.viewmodel.SettingsViewModel

@Composable
fun BlackjackNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val gameViewModel: GameViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    NavHost(navController = navController, startDestination = "settings") {
        composable("settings") {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                onStartGame = { rules ->
                    gameViewModel.startGame(rules)
                    navController.navigate("game") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("game") {
            GameScreen(
                viewModel = gameViewModel,
                onNavigateToSettings = {
                    navController.popBackStack("settings", inclusive = false)
                }
            )
        }
    }
}
