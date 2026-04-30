package com.example.oaplicativo.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Menu : Screen("menu")
    object CustomerList : Screen("customer_list")
    object CustomerForm : Screen("customer_form?customerId={customerId}") {
        fun createRoute(customerId: String? = null) = if (customerId != null) "customer_form?customerId=$customerId" else "customer_form"
    }
    object EconomyUpdateList : Screen("economy_update_list")
    object EconomyUpdateForm : Screen("economy_update_form?itemId={itemId}") {
        fun createRoute(itemId: String? = null) = if (itemId != null) "economy_update_form?itemId=$itemId" else "economy_update_form"
    }
    object UserRegistration : Screen("user_registration")
    object RecadastroForm : Screen("recadastro_form")
}