package com.healthyou.test.repository

import android.util.Log
import com.healthyou.test.model.ChatRequest
import com.healthyou.test.model.ChatResponse
import com.healthyou.test.model.HistoryItem
import com.healthyou.test.network.ApiService
import com.healthyou.test.network.StreamingApiService
import com.healthyou.test.util.SecureStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException

class ChatRepository(
    private val api: ApiService,
    private val secureStorage: SecureStorage,
    private val streamingApi: StreamingApiService = StreamingApiService()
) {
    companion object {
        private const val TAG = "ChatRepository"
        private const val MAX_ATTEMPTS = 3
        private const val BASE_DELAY_MS = 400L
    }

    fun streamLocalChat(prompt: String): Flow<String> {
        Log.d(TAG, "Starting local stream for prompt: $prompt")
        return streamingApi.streamResponse(prompt)
    }

    suspend fun sendMessage(message: String, history: List<HistoryItem>): Result<ChatResponse> {
        if (message.isBlank()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty"))
        }

        val token = secureStorage.getAuthToken()
        val authHeader = token?.let { "Bearer $it" }

        val request = ChatRequest(message = message, history = history)

        var attempt = 0
        var lastException: Exception? = null

        while (attempt < MAX_ATTEMPTS) {
            try {
                val resp = api.chat(authHeader, request)

                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null) {
                        return Result.success(body)
                    } else {
                        return Result.failure(IOException("Empty response body"))
                    }
                } else {
                    val code = resp.code()
                    val errText = try {
                        resp.errorBody()?.string() ?: resp.message()
                    } catch (e: Exception) {
                        resp.message()
                    }
                    val msg = "HTTP $code: $errText"
                    Log.w(TAG, "Non-success response: $msg")
                    return Result.failure(IOException(msg))
                }
            } catch (e: HttpException) {
                lastException = e
                Log.w(TAG, "HttpException on attempt ${attempt + 1}: ${e.message}")
            } catch (e: IOException) {
                lastException = e
                Log.w(TAG, "IOException on attempt ${attempt + 1}: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error sending chat", e)
                return Result.failure(e)
            }

            attempt++
            val delayMs = BASE_DELAY_MS * (1 shl (attempt - 1))
            Log.d(TAG, "Retrying in ${delayMs}ms (attempt $attempt/$MAX_ATTEMPTS)")
            delay(delayMs)
        }

        return Result.failure(lastException ?: IOException("Unknown network error"))
    }
}