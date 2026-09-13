package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.voice.LiveVoiceScreen
import com.example.ui.theme.GeminiBackground

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isLiveVoiceActive by viewModel.isLiveVoiceActive.collectAsState()

    // When in Settings or secondary views, Android back navigates back to Home Chat
    BackHandler(enabled = currentTab != AppTab.CHAT) {
        viewModel.selectTab(AppTab.CHAT)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GeminiBackground)
    ) {
        when (currentTab) {
            AppTab.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.selectTab(AppTab.CHAT) }
                )
            }
            AppTab.HISTORY -> {
                HistoryScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.selectTab(AppTab.CHAT) }
                )
            }
            else -> {
                ChatScreen(
                    viewModel = viewModel,
                    onOpenSettings = { viewModel.selectTab(AppTab.SETTINGS) },
                    onOpenHistory = { viewModel.selectTab(AppTab.HISTORY) }
                )
            }
        }

        // Overlaid Fullscreen Nova Live Voice (when activated)
        AnimatedVisibility(
            visible = isLiveVoiceActive,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            LiveVoiceScreen(viewModel = viewModel)
        }
    }
}
