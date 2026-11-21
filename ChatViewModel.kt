package com.didi.chatarchive

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.didi.chatarchive.data.ArchiveReader
import com.didi.chatarchive.data.ChatData
import com.didi.chatarchive.data.ChatStore
import com.didi.chatarchive.model.ImportException
import com.didi.chatarchive.model.ParseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState {
    data object Loading : UiState
    data class Working(val label: String) : UiState
    data class NeedsImport(val error: String? = null, val canGoBack: Boolean = false) : UiState
    data class Preview(val result: ParseResult, val sourceName: String) : UiState
    data class Ready(val chat: ChatData) : UiState
}

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ChatStore(app)
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var current: ChatData? = null

    init {
        viewModelScope.launch {
            _state.value = withContext(Dispatchers.Default) {
                if (!store.hasChat()) UiState.NeedsImport()
                else try {
                    ChatData(store.load()).also { current = it }.let { UiState.Ready(it) }
                } catch (e: Exception) {
                    UiState.NeedsImport("The saved chat could not be read (${e.message}). Please import again.")
                }
            }
        }
    }

    fun onFilePicked(uri: Uri) {
        _state.value = UiState.Working("Reading the archive…")
        viewModelScope.launch {
            _state.value = try {
                val src = withContext(Dispatchers.Default) { ArchiveReader.read(getApplication(), uri) }
                UiState.Preview(src.result, src.displayName)
            } catch (e: ImportException) {
                UiState.NeedsImport(e.message, current != null)
            } catch (e: Exception) {
                UiState.NeedsImport("Could not read that file: ${e.message}", current != null)
            }
        }
    }

    fun confirmImport() {
        val p = _state.value as? UiState.Preview ?: return
        _state.value = UiState.Working("Saving the conversation on this device…")
        viewModelScope.launch {
            _state.value = try {
                val data = withContext(Dispatchers.Default) {
                    store.save(p.result.messages, p.sourceName)
                    ChatData(p.result.messages)
                }
                current = data
                UiState.Ready(data)
            } catch (e: Exception) {
                UiState.NeedsImport("Saving failed: ${e.message}", current != null)
            }
        }
    }

    fun showImport() { _state.value = UiState.NeedsImport(null, current != null) }

    fun backToChat() { current?.let { _state.value = UiState.Ready(it) } }
}
