package com.healthyou.test.network

import com.healthyou.test.model.ChatRequest
import com.healthyou.test.model.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("/chat")
    suspend fun chat(
        @Header("Authorization") authorization: String?,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
