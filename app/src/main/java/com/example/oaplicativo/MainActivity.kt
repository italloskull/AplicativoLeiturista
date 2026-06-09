package com.example.oaplicativo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.oaplicativo.ui.theme.OAplicativoTheme

class MainActivity : androidx.activity.ComponentActivity() {
    companion object {
        private var _contextReference: android.content.Context? = null
        val contextReference: android.content.Context? get() = _contextReference
    }

    private val authRepository = com.example.oaplicativo.data.repository.AuthRepositoryImpl.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _contextReference = applicationContext
        
        // SÊNIOR FIX: Carregamento do perfil assim que a Activity inicia
        authRepository.loadProfileFromCache(applicationContext)
        
        // Ativa o desenho por baixo das barras do sistema (Inicia o Edge-to-Edge)
        enableEdgeToEdge()
        
        setContent {
            OAplicativoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SanitationApp()
                }
            }
        }
    }
}
