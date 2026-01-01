package com.healthyou.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.healthyou.test.network.NetworkModule
import com.healthyou.test.repository.ChatRepository
import com.healthyou.test.ui.ChatScreen
import com.healthyou.test.ui.theme.HealthYouTheme
import com.healthyou.test.util.SecureStorage
import com.healthyou.test.viewmodel.ChatViewModel
import com.healthyou.test.viewmodel.ChatViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val api = NetworkModule.createApi()
        val secureStorage = SecureStorage(applicationContext)
        val repo = ChatRepository(api, secureStorage)

        val factory = ChatViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory).get(ChatViewModel::class.java)

        setContent {
            HealthYouTheme {
                ChatScreen(viewModel = viewModel)
            }
        }
    }
}
