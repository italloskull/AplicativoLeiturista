package com.example.oaplicativo.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object CustomerList : Screen("customer_list")
    object CustomerForm : Screen("customer_form?customerId={customerId}") {
        fun createRoute(customerId: String? = null) = "customer_form?customerId=$customerId"
    }
    object UserRegistration : Screen("user_registration")
}