package com.example.oaplicativo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val authRepository = AuthRepositoryImpl.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            authRepository.loadProfileFromCache(applicationContext)
        }
        
        com.example.oaplicativo.data.repository.EconomyRepositoryImpl.getInstance().initialize(applicationContext)
        com.example.oaplicativo.data.repository.CustomerRepositoryImpl.getInstance().initialize(applicationContext)
        
        enableEdgeToEdge()
        setContent {
            SanitationApp()
        }
    }
}
