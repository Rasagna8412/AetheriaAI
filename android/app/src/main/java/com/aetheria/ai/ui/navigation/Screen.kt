package com.aetheria.ai.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Chat : Screen("chat", "Chat")
    object ModelHub : Screen("model_hub", "Model Hub")
    object Settings : Screen("settings", "Settings")
}
