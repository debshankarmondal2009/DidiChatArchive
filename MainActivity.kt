package com.didi.chatarchive

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.didi.chatarchive.ui.ChatArchiveTheme
import com.didi.chatarchive.ui.ChatScreen
import com.didi.chatarchive.ui.ImportScreen
import com.didi.chatarchive.ui.PreviewScreen
import com.didi.chatarchive.ui.WorkingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent { ChatArchiveTheme { App() } }
    }
}

@Composable
private fun App(vm: ChatViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.onFilePicked(uri)
    }
    when (val s = state) {
        UiState.Loading -> WorkingScreen("Opening your chat…")
        is UiState.Working -> WorkingScreen(s.label)
        is UiState.NeedsImport -> ImportScreen(
            error = s.error, canGoBack = s.canGoBack,
            onPick = { picker.launch(arrayOf("*/*")) }, onBack = vm::backToChat
        )
        is UiState.Preview -> PreviewScreen(s.result, s.sourceName, onConfirm = vm::confirmImport, onCancel = vm::showImport)
        is UiState.Ready -> ChatScreen(s.chat, onImportAgain = vm::showImport)
    }
}
