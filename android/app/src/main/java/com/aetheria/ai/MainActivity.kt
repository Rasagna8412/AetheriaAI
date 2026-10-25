package com.aetheria.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aetheria.ai.ui.navigation.Screen
import com.aetheria.ai.ui.screens.chat.ChatScreen
import com.aetheria.ai.ui.screens.chat.ChatViewModel
import com.aetheria.ai.ui.screens.hub.ModelHubScreen
import com.aetheria.ai.ui.screens.hub.ModelHubViewModel
import com.aetheria.ai.ui.theme.AetheriaTheme
import com.aetheria.ai.ui.theme.DarkBackground

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AetheriaApplication

        val chatViewModel = ChatViewModel(
            chatDao = app.database.chatDao(),
            customModelDao = app.database.customModelDao(),
            networkMonitor = app.networkMonitor,
            modelDownloader = app.modelDownloader,
            cloudApiClient = app.cloudApiClient,
            offlineEngine = app.offlineEngine
        )

        val modelHubViewModel = ModelHubViewModel(
            customModelDao = app.database.customModelDao(),
            modelDownloader = app.modelDownloader,
            cloudApiClient = app.cloudApiClient
        )

        setContent {
            AetheriaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Chat.route
                    ) {
                        composable(Screen.Chat.route) {
                            ChatScreen(
                                viewModel = chatViewModel,
                                onNavigateToModelHub = {
                                    navController.navigate(Screen.ModelHub.route)
                                }
                            )
                        }

                        composable(Screen.ModelHub.route) {
                            ModelHubScreen(
                                viewModel = modelHubViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
