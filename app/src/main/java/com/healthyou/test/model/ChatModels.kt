package com.healthyou.test.model

/**
 * Represents a single turn in the conversation history.
 * Matches the server expected shape: { "is_user": true|false, "content": "..." }
 */
data class HistoryItem(
    val is_user: Boolean,
    val content: String
)

/**
 * Request body sent to /chat
 */
data class ChatRequest(
    val message: String,
    val history: List<HistoryItem> = emptyList()
)

/**
 * Response returned by /chat
 */
data class ChatResponse(
    val response: String? = null,
    val timestamp: String? = null,
    val crisis: Boolean? = false,
    val model: String? = null,
    val error: String? = null
)
