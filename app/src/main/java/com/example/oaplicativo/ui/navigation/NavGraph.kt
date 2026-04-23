package com.example.oaplicativo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.oaplicativo.ui.screens.customer_form.CustomerFormScreen
import com.example.oaplicativo.ui.screens.customer_list.CustomerListScreen
import com.example.oaplicativo.ui.screens.login.LoginScreen
import com.example.oaplicativo.ui.screens.user_registration.UserRegistrationScreen

@Composable
fun SetupNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.CustomerList.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        composable(route = Screen.CustomerList.route) {
            CustomerListScreen(
                onAddCustomer = {
                    navController.navigate(Screen.CustomerForm.createRoute())
                },
                onCustomerClick = { customer ->
                    navController.navigate(Screen.CustomerForm.createRoute(customer.id))
                },
                onNavigateToUserRegistration = {
                    navController.navigate(Screen.UserRegistration.route)
                }
            )
        }
        composable(
            route = Screen.CustomerForm.route,
            arguments = listOf(navArgument("customerId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId")
            CustomerFormScreen(
                customerId = customerId,
                onSaveSuccess = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(route = Screen.UserRegistration.route) {
            UserRegistrationScreen(
                onRegistrationSuccess = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}