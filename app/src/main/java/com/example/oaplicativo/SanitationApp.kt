package com.example.oaplicativo

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.oaplicativo.ui.navigation.SetupNavGraph

@Composable
fun SanitationApp() {
    val navController = rememberNavController()
    SetupNavGraph(navController = navController)
}