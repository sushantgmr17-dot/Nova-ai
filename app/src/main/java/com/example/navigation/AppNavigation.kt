package com.example.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Chat : Screen("chat")
    object History : Screen("history")
    object Settings : Screen("settings")
    object ImageGeneration : Screen("image")
    object Admin : Screen("admin")
    object Homework : Screen("homework")
    object Voice : Screen("voice")
    object Login : Screen("login")
    object SignUp : Screen("signup")
}
