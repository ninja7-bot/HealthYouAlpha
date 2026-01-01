package com.healthyou.test.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyou.test.model.ChatResponse
import com.healthyou.test.model.HistoryItem
import com.healthyou.test.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch


data class UiMessage(
    val who: String,
    val text: String,
    val isCrisis: Boolean = false
)

class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages: StateFlow<List<UiMessage>> = _messages

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val conversationHistory = mutableListOf<HistoryItem>()

    fun sendMessage(userInput: String) {
        val text = userInput.trim()
        if (text.isEmpty()) return

        val userMsg = UiMessage("You", text)
        _messages.value = _messages.value + userMsg
        conversationHistory.add(HistoryItem(is_user = true, content = text))

        val placeholderAiMsg = UiMessage("HealthYou", "...")
        _messages.value = _messages.value + placeholderAiMsg

        viewModelScope.launch {
            var fullGeneratedText = ""

            repository.streamLocalChat(text)
                .flowOn(Dispatchers.IO)
                .onStart { _loading.value = true }
                .onCompletion { _loading.value = false }
                .catch { e ->
                    updateLastMessage("⚠️ Error: ${e.localizedMessage ?: "Connection failed"}")
                }
                .collect { token ->
                    if (fullGeneratedText.isEmpty()) {
                        fullGeneratedText = token
                    } else {
                        fullGeneratedText += token
                    }

                    updateLastMessage(fullGeneratedText)
                }

            if (fullGeneratedText.isNotEmpty()) {
                conversationHistory.add(HistoryItem(is_user = false, content = fullGeneratedText))
            }
        }
    }

    private fun updateLastMessage(newText: String) {
        val currentList = _messages.value.toMutableList()
        if (currentList.isNotEmpty()) {
            val lastMsg = currentList.last()
            // Create copy with new text
            currentList[currentList.lastIndex] = lastMsg.copy(text = newText)
            _messages.value = currentList
        }
    }

    fun sendCloudMessage(userInput: String) {
        val text = userInput.trim()
        if (text.isEmpty()) return

        val userMsg = UiMessage("You", text)
        _messages.value = _messages.value + userMsg
        conversationHistory.add(HistoryItem(is_user = true, content = text))

        viewModelScope.launch {
            _loading.value = true
            val result = repository.sendMessage(text, conversationHistory.toList())
            _loading.value = false

            if (result.isSuccess) {
                val data: ChatResponse? = result.getOrNull()
                val reply = data?.response?.trim().takeUnless { it.isNullOrEmpty() }
                    ?: "I’m here with you, but I didn’t catch that."

                val crisis = data?.crisis == true
                val aiMsg = UiMessage("HealthYou", reply!!, isCrisis = crisis)
                _messages.value = _messages.value + aiMsg
                conversationHistory.add(HistoryItem(is_user = false, content = reply))
            } else {
                val errText = result.exceptionOrNull()?.localizedMessage ?: "Network error"
                val sysMsg = UiMessage("System", "⚠️ $errText")
                _messages.value = _messages.value + sysMsg
            }
        }
    }

    fun clearConversation() {
        conversationHistory.clear()
        _messages.value = emptyList()
    }

    fun getHistory(): List<HistoryItem> = conversationHistory.toList()
}