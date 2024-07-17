package com.example.ble_serv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable


import androidx.navigation.compose.rememberNavController
import com.example.ble_serv.ui.navigation.Screens.HomeScreen
import com.example.ble_serv.ui.screens.ConnectionScreen
import com.example.ble_serv.ui.screens.HomeScrеen

@Composable
fun Navigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = HomeScreen.route){
        composable(HomeScreen.route){
            HomeScrеen(navController)
        }

        composable(Screens.ConnectionScreen.route){
            ConnectionScreen()
        }
    }

}

sealed class Screens(val route: String){
    object HomeScreen: Screens("home")
    object ConnectionScreen: Screens("connection")
}