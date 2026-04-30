package com.example.oaplicativo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.domain.repository.AuthRepository
import com.example.oaplicativo.ui.screens.customer_form.CustomerFormScreen
import com.example.oaplicativo.ui.screens.customer_list.CustomerListScreen
import com.example.oaplicativo.ui.screens.economy_update.EconomyUpdateListScreen
import com.example.oaplicativo.ui.screens.economy_update.EconomyUpdateScreen
import com.example.oaplicativo.ui.screens.login.LoginScreen
import com.example.oaplicativo.ui.screens.menu.MenuScreen
import com.example.oaplicativo.ui.screens.recadastro.RecadastroFormScreen
import com.example.oaplicativo.ui.screens.user_registration.UserRegistrationScreen
import com.example.oaplicativo.util.SecurityUtils
import kotlinx.coroutines.launch

@Composable
fun SetupNavGraph(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val authRepository: AuthRepository = AuthRepositoryImpl.getInstance()
    val context = androidx.compose.ui.platform.LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.Menu.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        
        composable(route = Screen.Menu.route) {
            MenuScreen(
                onNavigateToRecadastro = {
                    navController.navigate(Screen.CustomerList.route)
                },
                onNavigateToNovoRecadastro = {
                    navController.navigate(Screen.RecadastroForm.route)
                },
                onNavigateToEconomias = {
                    navController.navigate(Screen.EconomyUpdateList.route)
                },
                onLogout = {
                    scope.launch {
                        authRepository.logout()
                        SecurityUtils.clearCredentials(context)
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Menu.route) { inclusive = true }
                        }
                    }
                }
            )
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
                },
                onLogout = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.EconomyUpdateList.route) {
            EconomyUpdateListScreen(
                onBack = { navController.popBackStack() },
                onAddClick = { navController.navigate(Screen.EconomyUpdateForm.createRoute()) },
                onItemClick = { itemId -> 
                    navController.navigate(Screen.EconomyUpdateForm.createRoute(itemId))
                }
            )
        }

        composable(
            route = Screen.EconomyUpdateForm.route,
            arguments = listOf(navArgument("itemId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            EconomyUpdateScreen(
                itemId = itemId,
                onBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
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

        composable(route = Screen.RecadastroForm.route) {
            RecadastroFormScreen(
                onBack = { navController.popBackStack() },
                onSave = { 
                    // To be implemented: save logic
                    navController.popBackStack()
                }
            )
        }
    }
}