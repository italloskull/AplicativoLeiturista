package com.example.oaplicativo.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Menu : Screen("menu")
    object CustomerList : Screen("customer_list")
    object CustomerForm : Screen(route = "customer_form?customerId={customerId}") {
        fun createRoute(customerId: String? = null) = "customer_form?customerId=$customerId"
    }
    object EconomyUpdateList : Screen("economy_update_list")
    object EconomyUpdateForm : Screen(route = "economy_update_form?itemId={itemId}") {
        fun createRoute(itemId: String? = null) = "economy_update_form?itemId=$itemId"
    }
    object UserRegistration : Screen("user_registration")
    object RecadastroForm : Screen("recadastro_form")
    object VisitasDashboard : Screen("visitas_dashboard")
    object AdminDashboard : Screen("admin_dashboard")
}
