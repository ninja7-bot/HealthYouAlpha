package com.healthyou.test.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class StreamingApiService {
    private val client = OkHttpClient()

    private val baseUrl = "API_URL"

    fun streamResponse(prompt: String): Flow<String> = flow {
        val jsonBody = JSONObject().apply {
            put("prompt", prompt)
            put("stream", true)
            put("max_length", 512)
        }

        val requestBody = jsonBody.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder().url(baseUrl).post(requestBody).build()

        // Run network call on IO thread
        val response = withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }

        if (!response.isSuccessful) throw IOException("Server Error: ${response.code}")

        val source = response.body?.source() ?: throw IOException("Empty Body")

        try {
            while (!source.exhausted()) {
                val token = source.readUtf8()
                if (token.isNotEmpty()) emit(token)
            }
        } finally {
            response.close()
        }
    }
}