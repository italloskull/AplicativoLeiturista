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
import com.example.oaplicativo.ui.screens.customer_list.CustomerListScreen
import com.example.oaplicativo.ui.screens.economy_update.EconomyUpdateListScreen
import com.example.oaplicativo.ui.screens.economy_update.EconomyUpdateScreen
import com.example.oaplicativo.ui.screens.login.LoginScreen
import com.example.oaplicativo.ui.screens.menu.MenuScreen
import com.example.oaplicativo.ui.screens.recadastro.RecadastroFormScreen
import com.example.oaplicativo.ui.screens.user_registration.UserRegistrationScreen
import com.example.oaplicativo.ui.screens.visitas.VisitasScreen
import com.example.oaplicativo.util.SecurityUtils
import kotlinx.coroutines.launch

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val authRepository: AuthRepository = AuthRepositoryImpl.getInstance()
    val context = androidx.compose.ui.platform.LocalContext.current

    val onLogoutGlobal = {
        scope.launch {
            authRepository.logout()
            SecurityUtils.clearCredentials(context)
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

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
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onNavigateToRecadastro = {
                    navController.navigate(Screen.CustomerList.route)
                },
                onNavigateToEconomias = {
                    navController.navigate(Screen.EconomyUpdateList.route)
                },
                onNavigateToVisitas = {
                    navController.navigate(Screen.VisitasDashboard.route)
                },
                onNavigateToUserRegistration = {
                    navController.navigate(Screen.UserRegistration.route)
                },
                onLogout = { onLogoutGlobal() }
            )
        }

        composable(route = Screen.CustomerList.route) {
            CustomerListScreen(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onAddCustomer = {
                    navController.navigate(Screen.RecadastroForm.route)
                },
                onCustomerClick = { customer ->
                    navController.navigate(Screen.CustomerForm.createRoute(customer.id))
                },
                onNavigateToUserRegistration = {
                    navController.navigate(Screen.UserRegistration.route)
                },
                onLogout = { onLogoutGlobal() }
            )
        }

        composable(route = Screen.VisitasDashboard.route) {
            VisitasScreen(
                onBack = { navController.popBackStack() }
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
            RecadastroFormScreen(
                customerId = customerId,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onLogout = { onLogoutGlobal() },
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
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onLogout = { onLogoutGlobal() },
                onBack = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack()
                }
            )
        }
    }
}
